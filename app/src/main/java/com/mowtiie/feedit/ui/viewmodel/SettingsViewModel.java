package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;
import android.database.sqlite.SQLiteConstraintException;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.feedit.data.DatabaseHelper;
import com.mowtiie.feedit.data.FeedDao;
import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.TagDao;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.parser.OpmlEntry;
import com.mowtiie.feedit.parser.OpmlExportEntry;
import com.mowtiie.feedit.parser.OpmlParser;
import com.mowtiie.feedit.parser.OpmlWriter;
import com.mowtiie.feedit.sync.SyncScheduler;
import com.mowtiie.feedit.util.PrefsKeys;
import com.mowtiie.feedit.util.TagColorPicker;
import com.mowtiie.feedit.util.ThemeManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SettingsViewModel extends AndroidViewModel {

    private final SharedPreferences prefs;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();

    private final MutableLiveData<Boolean> wifiOnly = new MutableLiveData<>();
    private final MutableLiveData<Boolean> notificationsEnabled = new MutableLiveData<>();
    private final MutableLiveData<String> darkMode = new MutableLiveData<>();
    private final MutableLiveData<Boolean> opmlBusy = new MutableLiveData<>(false);
    private final MutableLiveData<String> opmlMessage = new MutableLiveData<>();

    public SettingsViewModel(@NonNull Application application) {
        super(application);
        prefs = application.getSharedPreferences(PrefsKeys.PREFS_NAME, Application.MODE_PRIVATE);
        wifiOnly.setValue(prefs.getBoolean(PrefsKeys.SYNC_WIFI_ONLY, false));
        notificationsEnabled.setValue(prefs.getBoolean(PrefsKeys.NOTIFICATIONS_ENABLED, true));
        darkMode.setValue(prefs.getString(PrefsKeys.DARK_MODE, "system"));
    }

    public LiveData<Boolean> getWifiOnly() {
        return wifiOnly;
    }

    public LiveData<Boolean> getNotificationsEnabled() {
        return notificationsEnabled;
    }

    public LiveData<String> getDarkMode() {
        return darkMode;
    }

    public LiveData<Boolean> getOpmlBusy() {
        return opmlBusy;
    }

    public LiveData<String> getOpmlMessage() {
        return opmlMessage;
    }

    public void setWifiOnly(boolean value) {
        prefs.edit().putBoolean(PrefsKeys.SYNC_WIFI_ONLY, value).apply();
        wifiOnly.setValue(value);
        SyncScheduler.schedulePeriodicSync(getApplication());
    }

    public void setNotificationsEnabled(boolean value) {
        prefs.edit().putBoolean(PrefsKeys.NOTIFICATIONS_ENABLED, value).apply();
        notificationsEnabled.setValue(value);
    }

    public void setDarkMode(String mode) {
        prefs.edit().putString(PrefsKeys.DARK_MODE, mode).apply();
        darkMode.setValue(mode);
        ThemeManager.applyDarkMode(mode);
    }

    public void exportOpml(Uri targetUri) {
        opmlBusy.setValue(true);
        ioExecutor.execute(() -> {
            try {
                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplication());
                FeedDao feedDao = new FeedDao(dbHelper);
                List<FeedTags> feeds = feedDao.getAllFeedsWithTags();

                List<OpmlExportEntry> entries = new ArrayList<>();
                for (FeedTags ft : feeds) {
                    List<String> tagNames = new ArrayList<>();
                    for (Tag tag : ft.getTags()) {
                        tagNames.add(tag.getName());
                    }
                    entries.add(new OpmlExportEntry(
                            ft.getFeed().getUrl(), ft.getFeed().getTitle(), ft.getFeed().getSiteUrl(), tagNames));
                }

                try (OutputStream out = getApplication().getContentResolver().openOutputStream(targetUri)) {
                    if (out == null) {
                        throw new IOException("Couldn't open the chosen file for writing");
                    }
                    new OpmlWriter().write(out, entries);
                }

                opmlMessage.postValue("Exported " + entries.size() + " feeds");
            } catch (Exception e) {
                opmlMessage.postValue("Export failed: " + e.getMessage());
            } finally {
                opmlBusy.postValue(false);
            }
        });
    }

    public void importOpml(Uri sourceUri) {
        opmlBusy.setValue(true);
        ioExecutor.execute(() -> {
            try (InputStream in = getApplication().getContentResolver().openInputStream(sourceUri)) {
                if (in == null) {
                    throw new IOException("Couldn't open the chosen file for reading");
                }
                List<OpmlEntry> entries = new OpmlParser().parse(in);

                DatabaseHelper dbHelper = DatabaseHelper.getInstance(getApplication());
                FeedDao feedDao = new FeedDao(dbHelper);
                TagDao tagDao = new TagDao(dbHelper);

                Map<String, Long> tagIdByLowerName = new HashMap<>();
                for (Tag tag : tagDao.getAllTags()) {
                    tagIdByLowerName.put(tag.getName().toLowerCase(Locale.US), tag.getId());
                }

                int imported = 0;
                for (OpmlEntry entry : entries) {
                    if (entry.getFeedUrl() == null || entry.getFeedUrl().trim().isEmpty()) {
                        continue;
                    }

                    List<Long> tagIds = new ArrayList<>();
                    for (String tagName : entry.getTagNames()) {
                        String key = tagName.toLowerCase(Locale.US);
                        Long tagId = tagIdByLowerName.get(key);
                        if (tagId == null) {
                            tagId = tagDao.insertTag(new Tag(tagName, TagColorPicker.PRESET_COLORS[0]));
                            tagIdByLowerName.put(key, tagId);
                        }
                        tagIds.add(tagId);
                    }

                    Feed feed = new Feed();
                    feed.setUrl(entry.getFeedUrl());
                    feed.setTitle(entry.getTitle());
                    feed.setSiteUrl(entry.getSiteUrl());
                    feed.setCreatedAt(System.currentTimeMillis());
                    feed.setNotifyNew(false);
                    feed.setOpenMode(Feed.OPEN_MODE_IN_APP);

                    try {
                        long feedId = feedDao.insertFeed(feed);
                        feedDao.setTagsForFeed(feedId, tagIds);
                        imported++;
                    } catch (SQLiteConstraintException alreadySubscribed) {
                    }
                }

                FeedRepository.getInstance(getApplication()).loadFeedsWithTags();
                opmlMessage.postValue("Imported " + imported + " feeds");
            } catch (Exception e) {
                opmlMessage.postValue("Import failed: " + e.getMessage());
            } finally {
                opmlBusy.postValue(false);
            }
        });
    }
}