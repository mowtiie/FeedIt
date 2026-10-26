package com.mowtiie.feedit.model;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class ArticleTest {

    @Test
    public void copyConstructor_copiesEveryField() {
        Article original = new Article();
        original.setId(1L);
        original.setFeedId(2L);
        original.setGuid("guid");
        original.setTitle("title");
        original.setLink("https://example.com/a");
        original.setAuthor("author");
        original.setSummary("summary");
        original.setContent("<p>content</p>");
        original.setImageUrl("https://example.com/i.png");
        original.setPublishedAt(1000L);
        original.setFetchedAt(2000L);
        original.setRead(true);
        original.setStarred(true);

        Article copy = new Article(original);

        assertNotSame(original, copy);
        assertEquals(1L, copy.getId());
        assertEquals(2L, copy.getFeedId());
        assertEquals("guid", copy.getGuid());
        assertEquals("title", copy.getTitle());
        assertEquals("https://example.com/a", copy.getLink());
        assertEquals("author", copy.getAuthor());
        assertEquals("summary", copy.getSummary());
        assertEquals("<p>content</p>", copy.getContent());
        assertEquals("https://example.com/i.png", copy.getImageUrl());
        assertEquals(Long.valueOf(1000L), copy.getPublishedAt());
        assertEquals(2000L, copy.getFetchedAt());
        assertTrue(copy.isRead());
        assertTrue(copy.isStarred());
    }

    @Test
    public void copyIsIndependentOfTheOriginal() {
        Article original = new Article();
        Article copy = new Article(original);

        copy.setRead(true);

        assertFalse(original.isRead());
    }
}
