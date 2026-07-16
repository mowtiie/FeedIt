package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.model.Article;

public class ArticleViewModel extends AndroidViewModel {

    private final FeedRepository repository;
    private final MutableLiveData<Article> article = new MutableLiveData<>();

    public ArticleViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);
    }

    public LiveData<Article> getArticle() {
        return article;
    }

    public void load(long articleId) {
        repository.getArticleById(articleId, new RepositoryCallback<Article>() {
            @Override
            public void onComplete(Article result) {
                article.setValue(result);
            }

            @Override
            public void onError(Exception e) {
                article.setValue(null);
            }
        });
    }

    public void toggleStarred() {
        Article current = article.getValue();
        if (current == null) {
            return;
        }
        boolean newValue = !current.isStarred();
        repository.setArticleStarred(current.getId(), newValue);
        current.setStarred(newValue);
        article.setValue(current);
    }

    public void toggleRead() {
        Article current = article.getValue();
        if (current == null) {
            return;
        }
        boolean newValue = !current.isRead();
        repository.setArticleRead(current.getId(), newValue);
        current.setRead(newValue);
        article.setValue(current);
    }
}