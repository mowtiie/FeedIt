package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;
import android.text.Spanned;

import androidx.annotation.NonNull;
import androidx.core.text.HtmlCompat;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.data.RepositoryCallback;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.util.GlideImageGetter;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class ArticleViewModel extends AndroidViewModel {

    private static final int CONTENT_HORIZONTAL_PADDING_DP = 20;

    private final FeedRepository repository;
    private final MutableLiveData<Article> article = new MutableLiveData<>();
    private final MutableLiveData<Spanned> renderedContent = new MutableLiveData<>();
    private final MutableLiveData<Integer> imageLoadTick = new MutableLiveData<>(0);
    private final ExecutorService renderExecutor = Executors.newSingleThreadExecutor();
    private final AtomicInteger loadGeneration = new AtomicInteger();
    private GlideImageGetter imageGetter;

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

    /** Changes each time an inline image finishes loading, so the view can re-layout its text. */
    public LiveData<Integer> getImageLoadTick() {
        return imageLoadTick;
    }

    /** Picks the HTML to show: full content when present, otherwise the summary. */
    static String htmlOf(Article article) {
        return article.getContent() != null ? article.getContent() : article.getSummary();
    }

    @Override
    protected void onCleared() {
        renderExecutor.shutdownNow();
        if (imageGetter != null) {
            imageGetter.cancelAll();
        }
    }

    public void load(long articleId) {
        load(articleId, false);
    }

    /** Loads an article; {@code markRead} is used when stepping to it from another article. */
    public void load(long articleId, boolean markRead) {
        int generation = loadGeneration.incrementAndGet();
        if (imageGetter != null) {
            imageGetter.cancelAll();
            imageGetter = null;
        }
        renderedContent.setValue(null);
        repository.getArticleById(articleId, new RepositoryCallback<Article>() {
            @Override
            public void onComplete(Article result) {
                if (generation != loadGeneration.get()) {
                    return;
                }
                if (markRead && result != null && !result.isRead()) {
                    repository.setArticleRead(result.getId(), true);
                    result.setRead(true);
                }
                article.setValue(result);
                renderContent(result, generation);
            }

            @Override
            public void onError(Exception e) {
                article.setValue(null);
            }
        });
    }

    private void renderContent(Article loaded, int generation) {
        String html = loaded != null ? htmlOf(loaded) : null;
        if (html == null) {
            return;
        }
        float density = getApplication().getResources().getDisplayMetrics().density;
        int maxWidth = getApplication().getResources().getDisplayMetrics().widthPixels
                - (int) (2 * CONTENT_HORIZONTAL_PADDING_DP * density);
        GlideImageGetter getter = new GlideImageGetter(getApplication(), maxWidth, loaded.getImageUrl(),
                () -> imageLoadTick.setValue(imageLoadTick.getValue() + 1));
        imageGetter = getter;
        renderExecutor.execute(() -> {
            Spanned spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT, getter, null);
            if (generation == loadGeneration.get()) {
                renderedContent.postValue(spanned);
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
