package com.mowtiie.feedit.sync;

public class FeedSyncResult {

    private final long feedId;
    private final String feedTitle;
    private final int newArticleCount;

    public FeedSyncResult(long feedId, String feedTitle, int newArticleCount) {
        this.feedId = feedId;
        this.feedTitle = feedTitle;
        this.newArticleCount = newArticleCount;
    }

    public long getFeedId() {
        return feedId;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public int getNewArticleCount() {
        return newArticleCount;
    }
}
