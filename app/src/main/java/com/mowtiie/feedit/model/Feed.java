package com.mowtiie.feedit.model;

public class Feed {

    public static final int OPEN_MODE_IN_APP = 0;
    public static final int OPEN_MODE_BROWSER = 1;

    private long id;
    private String url;
    private String title;
    private String siteUrl;
    private String description;
    private String faviconUrl;
    private String imageUrl;
    private String etag;
    private String lastModified;
    private Long lastFetched;
    private boolean notifyNew;
    private int openMode;
    private long createdAt;

    public Feed() {
    }

    public long getId() {
        return id;
    }

    public void setId(long id) {
        this.id = id;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
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

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getFaviconUrl() {
        return faviconUrl;
    }

    public void setFaviconUrl(String faviconUrl) {
        this.faviconUrl = faviconUrl;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public String getLastModified() {
        return lastModified;
    }

    public void setLastModified(String lastModified) {
        this.lastModified = lastModified;
    }

    public Long getLastFetched() {
        return lastFetched;
    }

    public void setLastFetched(Long lastFetched) {
        this.lastFetched = lastFetched;
    }

    public boolean isNotifyNew() {
        return notifyNew;
    }

    public void setNotifyNew(boolean notifyNew) {
        this.notifyNew = notifyNew;
    }

    public int getOpenMode() {
        return openMode;
    }

    public void setOpenMode(int openMode) {
        this.openMode = openMode;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }
}
