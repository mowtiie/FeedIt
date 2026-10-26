package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.feedit.R;
import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.net.FeedAutoDiscovery;
import com.mowtiie.feedit.net.FeedFetcher;
import com.mowtiie.feedit.net.FetchResult;
import com.mowtiie.feedit.parser.FeedParser;
import com.mowtiie.feedit.parser.ParsedFeedMeta;

import com.mowtiie.feedit.sync.ArticleMapper;
import com.mowtiie.feedit.util.PrefsKeys;
import com.mowtiie.feedit.sync.RetentionPolicy;

import java.io.ByteArrayInputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class FeedEditViewModel extends AndroidViewModel {

    private final FeedRepository repository;
    private final ExecutorService ioExecutor = Executors.newSingleThreadExecutor();
    private final FeedFetcher fetcher = new FeedFetcher();
    private final FeedParser parser = new FeedParser();
    private final FeedAutoDiscovery discovery = new FeedAutoDiscovery();

    private long existingFeedId = 0;
    private boolean prefilled = false;
    private Feed workingFeed = new Feed();
    private final Set<Long> selectedTagIds = new HashSet<>();

    private final MutableLiveData<Boolean> loading = new MutableLiveData<>(false);
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();
    private final MutableLiveData<Boolean> saved = new MutableLiveData<>(false);
    private final MediatorLiveData<Feed> formState = new MediatorLiveData<>();
    private final MutableLiveData<Set<Long>> selectedTagIdsLive = new MutableLiveData<>(new HashSet<>());

    public FeedEditViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);
        workingFeed.setCreatedAt(System.currentTimeMillis());
        workingFeed.setOpenMode(application.getSharedPreferences(PrefsKeys.PREFS_NAME, Application.MODE_PRIVATE)
                .getInt(PrefsKeys.DEFAULT_OPEN_MODE, Feed.OPEN_MODE_IN_APP));

        repository.loadTags();
        repository.loadFeedsWithTags();

        formState.addSource(repository.getFeedsWithTags(), feeds -> {
            if (existingFeedId == 0 || prefilled) {
                return;
            }
            for (FeedTags ft : feeds) {
                if (ft.getFeed().getId() == existingFeedId) {
                    prefilled = true;
                    workingFeed = ft.getFeed();
                    selectedTagIds.clear();
                    for (Tag tag : ft.getTags()) {
                        selectedTagIds.add(tag.getId());
                    }
                    selectedTagIdsLive.setValue(new HashSet<>(selectedTagIds));
                    formState.setValue(workingFeed);
                    break;
                }
            }
        });
    }

    public void setEditingFeedId(long feedId) {
        this.existingFeedId = feedId;
    }

    public boolean isEditing() {
        return existingFeedId != 0;
    }

    public LiveData<List<Tag>> getAllTags() {
        return repository.getTags();
    }

    public LiveData<Boolean> isLoading() {
        return loading;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> getSaved() {
        return saved;
    }

    public LiveData<Feed> getFormState() {
        return formState;
    }

    public LiveData<Set<Long>> getSelectedTagIds() {
        return selectedTagIdsLive;
    }

    public void toggleTag(long tagId) {
        if (selectedTagIds.contains(tagId)) {
            selectedTagIds.remove(tagId);
        } else {
            selectedTagIds.add(tagId);
        }
        selectedTagIdsLive.setValue(new HashSet<>(selectedTagIds));
    }

    public void createAndSelectTag(String name) {
        Tag tag = new Tag(name, null);
        repository.saveTag(tag, new RepositoryCallback<Long>() {
            @Override
            public void onComplete(Long tagId) {
                selectedTagIds.add(tagId);
                selectedTagIdsLive.postValue(new HashSet<>(selectedTagIds));
            }

            @Override
            public void onError(Exception e) {
                errorMessage.postValue("Couldn't create tag: " + e.getMessage());
            }
        });
    }

    public void setNotifyNew(boolean value) {
        workingFeed.setNotifyNew(value);
    }

    public int getInitialOpenMode() {
        return workingFeed.getOpenMode();
    }

    public void setOpenMode(int mode) {
        workingFeed.setOpenMode(mode);
    }

    public void save(String urlInput, String titleInput) {
        if (urlInput == null || urlInput.trim().isEmpty()) {
            errorMessage.setValue("Enter a feed URL");
            return;
        }
        String trimmedUrl = urlInput.trim();
        if (!trimmedUrl.matches("(?i)^https?://.*")) {
            trimmedUrl = "https://" + trimmedUrl;
        }
        String trimmedTitle = titleInput != null ? titleInput.trim() : "";

        if (isEditing()) {
            workingFeed.setUrl(trimmedUrl);
            if (!trimmedTitle.isEmpty()) {
                workingFeed.setTitle(trimmedTitle);
            }
            persistFeed(workingFeed, null);
            return;
        }

        loading.setValue(true);
        String finalTrimmedUrl = trimmedUrl;
        ioExecutor.execute(() -> {
            try {
                String resolvedUrl = resolveFeedUrl(finalTrimmedUrl);
                FetchResult fetchResult = fetcher.fetch(resolvedUrl, null, null);
                if (!fetchResult.isSuccess()) {
                    postError("Couldn't reach that feed: " + fetchResult.getErrorMessage());
                    return;
                }

                ParsedFeedMeta meta = parser.parse(new ByteArrayInputStream(fetchResult.getBody()));

                Feed feed = new Feed();
                feed.setUrl(resolvedUrl);
                feed.setTitle(!trimmedTitle.isEmpty() ? trimmedTitle : meta.getTitle());
                feed.setSiteUrl(meta.getSiteUrl());
                feed.setDescription(meta.getDescription());
                feed.setImageUrl(meta.getImageUrl());
                feed.setEtag(fetchResult.getNewEtag());
                feed.setLastModified(fetchResult.getNewLastModified());
                feed.setLastFetched(System.currentTimeMillis());
                feed.setNotifyNew(workingFeed.isNotifyNew());
                feed.setOpenMode(workingFeed.getOpenMode());
                feed.setCreatedAt(System.currentTimeMillis());

                persistFeed(feed, meta);
            } catch (Exception e) {
                postError(describeReadError(e));
            }
        });
    }

    private String describeReadError(Exception e) {
        String message = e.getMessage();
        if (e instanceof org.xmlpull.v1.XmlPullParserException
                && message != null && message.startsWith("Unrecognized feed root element")) {
            return getApplication().getString(R.string.error_not_a_feed);
        }
        return "Couldn't read that feed: " + message;
    }

    private String resolveFeedUrl(String input) {
        List<FeedAutoDiscovery.Discovery> found = discovery.discover(input);
        if (!found.isEmpty()) {
            return found.get(0).feedUrl;
        }
        return input;
    }

    private void persistFeed(Feed feed, ParsedFeedMeta meta) {
        repository.saveFeed(feed, new ArrayList<>(selectedTagIds), new RepositoryCallback<Long>() {
            @Override
            public void onComplete(Long feedId) {
                if (meta == null || meta.getArticles().isEmpty()) {
                    finishSaved();
                    return;
                }
                List<Article> articles = ArticleMapper.fromParsed(
                        feedId, meta.getArticles(), System.currentTimeMillis(),
                        RetentionPolicy.cutoffMillis(getApplication()));
                repository.insertArticles(articles, new RepositoryCallback<Integer>() {
                    @Override
                    public void onComplete(Integer count) {
                        finishSaved();
                    }

                    @Override
                    public void onError(Exception e) {
                        finishSaved();
                    }
                });
            }

            @Override
            public void onError(Exception e) {
                postError("Couldn't save feed: " + e.getMessage());
            }
        });
    }

    private void finishSaved() {
        loading.postValue(false);
        saved.postValue(true);
    }

    private void postError(String message) {
        loading.postValue(false);
        errorMessage.postValue(message);
    }
}