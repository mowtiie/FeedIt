package com.mowtiie.feedit.parser;

import java.util.ArrayList;
import java.util.List;

public class ParsedFeedMeta {

    private String title;
    private String siteUrl;
    private String description;
    private String imageUrl;
    private final List<ParsedArticle> articles = new ArrayList<>();

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public void setSiteUrl(String siteUrl) {
        this.siteUrl = siteUrl;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public List<ParsedArticle> getArticles() {
        return articles;
    }
}
