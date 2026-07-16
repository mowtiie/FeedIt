package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.model.FeedTags;

import java.util.List;

public class FeedListViewModel extends AndroidViewModel {

    private final FeedRepository repository;

    public FeedListViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);
        repository.loadFeedsWithTags();
    }

    public LiveData<List<FeedTags>> getFeeds() {
        return repository.getFeedsWithTags();
    }

    public void refresh() {
        repository.loadFeedsWithTags();
    }

    public void deleteFeed(long feedId, Runnable onDone) {
        repository.deleteFeed(feedId, new RepositoryCallback<Void>() {
            @Override
            public void onComplete(Void result) {
                if (onDone != null) {
                    onDone.run();
                }
            }

            @Override
            public void onError(Exception e) {
                if (onDone != null) {
                    onDone.run();
                }
            }
        });
    }
}
