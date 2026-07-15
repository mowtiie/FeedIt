package com.mowtiie.feedit.parser;

import java.util.List;

public class OpmlExportEntry {

    private final String feedUrl;
    private final String title;
    private final String siteUrl;
    private final List<String> tagNames;

    public OpmlExportEntry(String feedUrl, String title, String siteUrl, List<String> tagNames) {
        this.feedUrl = feedUrl;
        this.title = title;
        this.siteUrl = siteUrl;
        this.tagNames = tagNames;
    }

    public String getFeedUrl() {
        return feedUrl;
    }

    public String getTitle() {
        return title;
    }

    public String getSiteUrl() {
        return siteUrl;
    }

    public List<String> getTagNames() {
        return tagNames;
    }
}
