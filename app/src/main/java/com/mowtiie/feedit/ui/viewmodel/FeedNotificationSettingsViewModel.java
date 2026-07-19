package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.model.FeedTags;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

public class FeedNotificationSettingsViewModel extends AndroidViewModel {

    public enum SortOrder {
        NAME_ASC, NAME_DESC, NOTIFY_ON_FIRST, NOTIFY_OFF_FIRST
    }

    private final FeedRepository repository;
    private final MediatorLiveData<List<FeedTags>> displayedFeeds = new MediatorLiveData<>();

    private List<FeedTags> latestFeeds = new ArrayList<>();
    private String searchQuery = null;
    private SortOrder sortOrder = SortOrder.NAME_ASC;

    public FeedNotificationSettingsViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);

        displayedFeeds.addSource(repository.getFeedsWithTags(), feeds -> {
            latestFeeds = feeds != null ? feeds : new ArrayList<>();
            displayedFeeds.setValue(buildDisplayedList());
        });

        repository.loadFeedsWithTags();
    }

    public LiveData<List<FeedTags>> getDisplayedFeeds() {
        return displayedFeeds;
    }

    public String getSearchQuery() {
        return searchQuery;
    }

    public SortOrder getSortOrder() {
        return sortOrder;
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        displayedFeeds.setValue(buildDisplayedList());
    }

    public void setSortOrder(SortOrder sortOrder) {
        this.sortOrder = sortOrder;
        displayedFeeds.setValue(buildDisplayedList());
    }

    public void setFeedNotifyNew(long feedId, boolean enabled) {
        repository.setFeedNotifyNew(feedId, enabled);
    }

    private List<FeedTags> buildDisplayedList() {
        List<FeedTags> result = new ArrayList<>(latestFeeds);

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            String needle = searchQuery.trim().toLowerCase();
            result.removeIf(item -> {
                String title = item.getFeed().getTitle();
                return title == null || !title.toLowerCase().contains(needle);
            });
        }

        Comparator<FeedTags> comparator;
        switch (sortOrder) {
            case NAME_DESC:
                comparator = Comparator.comparing(
                                (FeedTags f) -> nullToEmpty(f.getFeed().getTitle()), String.CASE_INSENSITIVE_ORDER)
                        .reversed();
                break;
            case NOTIFY_ON_FIRST:
                comparator = Comparator.comparing((FeedTags f) -> !f.getFeed().isNotifyNew())
                        .thenComparing(f -> nullToEmpty(f.getFeed().getTitle()), String.CASE_INSENSITIVE_ORDER);
                break;
            case NOTIFY_OFF_FIRST:
                comparator = Comparator.comparing((FeedTags f) -> f.getFeed().isNotifyNew())
                        .thenComparing(f -> nullToEmpty(f.getFeed().getTitle()), String.CASE_INSENSITIVE_ORDER);
                break;
            case NAME_ASC:
            default:
                comparator = Comparator.comparing(
                        (FeedTags f) -> nullToEmpty(f.getFeed().getTitle()), String.CASE_INSENSITIVE_ORDER);
                break;
        }
        result.sort(comparator);

        return result;
    }

    private static String nullToEmpty(String s) {
        return s != null ? s : "";
    }
}