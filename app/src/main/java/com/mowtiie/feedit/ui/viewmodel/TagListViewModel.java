package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.model.Tag;

import java.util.List;

public class TagListViewModel extends AndroidViewModel {

    private final FeedRepository repository;

    public TagListViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);
        repository.loadTags();
    }

    public LiveData<List<Tag>> getTags() {
        return repository.getTags();
    }

    public void saveTag(Tag tag, RepositoryCallback<Long> callback) {
        repository.saveTag(tag, callback);
    }

    public void deleteTag(long tagId, Runnable onDone) {
        repository.deleteTag(tagId, new RepositoryCallback<Void>() {
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
