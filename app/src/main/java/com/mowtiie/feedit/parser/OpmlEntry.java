package com.mowtiie.feedit.parser;

import java.util.ArrayList;
import java.util.List;

public class OpmlEntry {

    private String feedUrl;
    private String title;
    private String siteUrl;
    private final List<String> tagNames = new ArrayList<>();

    public String getFeedUrl() {
        return feedUrl;
    }

    public void setFeedUrl(String feedUrl) {
        this.feedUrl = feedUrl;
    }

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

    public List<String> getTagNames() {
        return tagNames;
    }
}
