package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;

import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.model.Article;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class ArticleViewModel extends AndroidViewModel {

    private final FeedRepository repository;
    private final MutableLiveData<Article> article = new MutableLiveData<>();
    private final MutableLiveData<Spanned> renderedContent = new MutableLiveData<>();
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();

    public ArticleViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);
    }

    public LiveData<Article> getArticle() {
        return article;
    }

    public LiveData<Spanned> getRenderedContent() {
        return renderedContent;
    }

    /** Picks the HTML to show: full content when present, otherwise the summary. */
    static String htmlOf(Article article) {
        return article.getContent() != null ? article.getContent() : article.getSummary();
    }

    @Override
    protected void onCleared() {
        renderExecutor.shutdownNow();
    }

    public void load(long articleId) {
        repository.getArticleById(articleId, new RepositoryCallback<Article>() {
            @Override
            public void onComplete(Article result) {
                article.setValue(result);
                renderContent(result);
            }

            @Override
            public void onError(Exception e) {
                article.setValue(null);
            }
        });
    }

    private void renderContent(Article loaded) {
        String html = loaded != null ? htmlOf(loaded) : null;
        if (html == null) {
            return;
        }
        renderExecutor.execute(() ->
                renderedContent.postValue(HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)));
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