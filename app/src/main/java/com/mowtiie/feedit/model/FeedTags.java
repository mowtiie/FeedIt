package com.mowtiie.feedit.model;

import java.util.ArrayList;
import java.util.List;

public class FeedTags {

    private Feed feed;
    private List<Tag> tags = new ArrayList<>();

    public FeedTags() {
    }

    public FeedTags(Feed feed, List<Tag> tags) {
        this.feed = feed;
        this.tags = tags;
    }

    public Feed getFeed() {
        return feed;
    }

    public void setFeed(Feed feed) {
        this.feed = feed;
    }

    public List<Tag> getTags() {
        return tags;
    }

    public void setTags(List<Tag> tags) {
        this.tags = tags;
    }
}
