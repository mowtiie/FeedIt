package com.mowtiie.feedit.sync;

import com.mowtiie.feedit.model.Article;

import java.util.List;

public class FeedSyncResult {

    private final long feedId;
    private final String feedTitle;
    private final int openMode;
    private final List<Article> newArticles;

    public FeedSyncResult(long feedId, String feedTitle, int openMode, List<Article> newArticles) {
        this.feedId = feedId;
        this.feedTitle = feedTitle;
        this.openMode = openMode;
        this.newArticles = newArticles;
    }

    public long getFeedId() {
        return feedId;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public int getOpenMode() {
        return openMode;
    }

    public List<Article> getNewArticles() {
        return newArticles;
    }

    public int getNewArticleCount() {
        return newArticles.size();
    }
}