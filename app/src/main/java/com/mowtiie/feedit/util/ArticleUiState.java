package com.mowtiie.feedit.util;

import com.mowtiie.feedit.model.Article;

public class ArticleUiState {

    private final Article article;
    private final String feedTitle;
    private final int feedOpenMode;

    public ArticleUiState(Article article, String feedTitle, int feedOpenMode) {
        this.article = article;
        this.feedTitle = feedTitle;
        this.feedOpenMode = feedOpenMode;
    }

    public Article getArticle() {
        return article;
    }

    public String getFeedTitle() {
        return feedTitle;
    }

    public int getFeedOpenMode() {
        return feedOpenMode;
    }
}