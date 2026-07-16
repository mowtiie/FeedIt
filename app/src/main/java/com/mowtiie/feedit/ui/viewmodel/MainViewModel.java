package com.mowtiie.feedit.ui.viewmodel;

import android.app.Application;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MediatorLiveData;

import com.mowtiie.feedit.data.ArticleDao;
import com.mowtiie.feedit.data.FeedRepository;
import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;
import com.mowtiie.feedit.util.ArticleUiState;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainViewModel extends AndroidViewModel {

    private static final String PREFS_NAME = "feedit_prefs";
    private static final String KEY_SORT_ORDER = "sort_order";

    private final FeedRepository repository;
    private final SharedPreferences prefs;

    private final MediatorLiveData<List<ArticleUiState>> articleUiStates = new MediatorLiveData<>();

    private Long scopeFeedId = null;
    private Long scopeTagId = null;
    private boolean unreadOnly = false;
    private boolean starredOnly = false;
    private String searchQuery = null;
    private ArticleDao.SortOrder sortOrder;

    private List<Article> latestArticles = new ArrayList<>();
    private List<FeedTags> latestFeeds = new ArrayList<>();

    public MainViewModel(@NonNull Application application) {
        super(application);
        repository = FeedRepository.getInstance(application);
        prefs = application.getSharedPreferences(PREFS_NAME, Application.MODE_PRIVATE);
        sortOrder = ArticleDao.SortOrder.valueOf(
                prefs.getString(KEY_SORT_ORDER, ArticleDao.SortOrder.NEWEST.name()));

        articleUiStates.addSource(repository.getArticles(), articles -> {
            latestArticles = articles;
            articleUiStates.setValue(buildUiStates());
        });
        articleUiStates.addSource(repository.getFeedsWithTags(), feeds -> {
            latestFeeds = feeds;
            articleUiStates.setValue(buildUiStates());
        });

        repository.loadTags();
        repository.loadFeedsWithTags();
        refresh();
    }

    public LiveData<List<ArticleUiState>> getArticleUiStates() {
        return articleUiStates;
    }

    public LiveData<List<Tag>> getTags() {
        return repository.getTags();
    }

    public LiveData<List<FeedTags>> getFeedsWithTags() {
        return repository.getFeedsWithTags();
    }

    public void selectAll() {
        scopeFeedId = null;
        scopeTagId = null;
        unreadOnly = false;
        starredOnly = false;
        refresh();
    }

    public void selectUnread() {
        scopeFeedId = null;
        scopeTagId = null;
        unreadOnly = true;
        starredOnly = false;
        refresh();
    }

    public void selectStarred() {
        scopeFeedId = null;
        scopeTagId = null;
        starredOnly = true;
        unreadOnly = false;
        refresh();
    }

    public void selectTag(long tagId) {
        scopeTagId = tagId;
        scopeFeedId = null;
        refresh();
    }

    public void selectFeed(long feedId) {
        scopeFeedId = feedId;
        scopeTagId = null;
        refresh();
    }

    public void applyReadStateFilter(boolean unreadOnly, boolean starredOnly) {
        this.unreadOnly = unreadOnly;
        this.starredOnly = starredOnly;
        refresh();
    }

    public void setSearchQuery(String query) {
        this.searchQuery = query;
        refresh();
    }

    public void setSortOrder(ArticleDao.SortOrder order) {
        this.sortOrder = order;
        prefs.edit().putString(KEY_SORT_ORDER, order.name()).apply();
        refresh();
    }

    public ArticleDao.SortOrder getSortOrder() {
        return sortOrder;
    }

    public boolean isUnreadOnly() {
        return unreadOnly;
    }

    public boolean isStarredOnly() {
        return starredOnly;
    }

    public void toggleRead(Article article) {
        repository.setArticleRead(article.getId(), !article.isRead());
        refresh();
    }

    public void markRead(Article article) {
        if (!article.isRead()) {
            repository.setArticleRead(article.getId(), true);
            refresh();
        }
    }

    public void toggleStarred(Article article) {
        repository.setArticleStarred(article.getId(), !article.isStarred());
        refresh();
    }

    public void markAllRead() {
        repository.markAllRead(scopeFeedId, scopeTagId);
        refresh();
    }

    public void refresh() {
        repository.loadArticles(scopeFeedId, scopeTagId, unreadOnly, starredOnly, searchQuery, sortOrder);
    }

    private List<ArticleUiState> buildUiStates() {
        Map<Long, FeedTags> feedById = new HashMap<>();
        for (FeedTags ft : latestFeeds) {
            feedById.put(ft.getFeed().getId(), ft);
        }
        List<ArticleUiState> result = new ArrayList<>();
        for (Article article : latestArticles) {
            FeedTags ft = feedById.get(article.getFeedId());
            String feedTitle = (ft != null && ft.getFeed().getTitle() != null) ? ft.getFeed().getTitle() : "";
            int openMode = ft != null ? ft.getFeed().getOpenMode() : 0;
            result.add(new ArticleUiState(article, feedTitle, openMode));
        }
        return result;
    }
}
