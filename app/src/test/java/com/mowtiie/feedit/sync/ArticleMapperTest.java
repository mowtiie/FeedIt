package com.mowtiie.feedit.sync;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.parser.ParsedArticle;

import org.junit.Test;

import java.util.Arrays;
import java.util.List;

public class ArticleMapperTest {

    private static ParsedArticle parsed(String guid, String link, Long publishedAt) {
        ParsedArticle p = new ParsedArticle();
        p.setGuid(guid);
        p.setLink(link);
        p.setTitle("title-" + guid);
        p.setAuthor("author");
        p.setSummary("summary");
        p.setContent("<p>content</p>");
        p.setImageUrl("https://example.com/i.png");
        p.setPublishedAt(publishedAt);
        return p;
    }

    @Test
    public void copiesEveryFieldAndStampsFeedAndFetchTime() {
        List<Article> result = ArticleMapper.fromParsed(7L,
                Arrays.asList(parsed("g1", "https://example.com/1", 1000L)), 5000L, null);

        assertEquals(1, result.size());
        Article a = result.get(0);
        assertEquals(7L, a.getFeedId());
        assertEquals("g1", a.getGuid());
        assertEquals("title-g1", a.getTitle());
        assertEquals("https://example.com/1", a.getLink());
        assertEquals("author", a.getAuthor());
        assertEquals("summary", a.getSummary());
        assertEquals("<p>content</p>", a.getContent());
        assertEquals("https://example.com/i.png", a.getImageUrl());
        assertEquals(Long.valueOf(1000L), a.getPublishedAt());
        assertEquals(5000L, a.getFetchedAt());
    }

    @Test
    public void missingGuidFallsBackToLink() {
        List<Article> result = ArticleMapper.fromParsed(1L,
                Arrays.asList(parsed(null, "https://example.com/x", 1L)), 1L, null);

        assertEquals("https://example.com/x", result.get(0).getGuid());
    }

    @Test
    public void dropsItemsPublishedBeforeCutoff_keepsUndatedAndBoundary() {
        List<Article> result = ArticleMapper.fromParsed(1L, Arrays.asList(
                parsed("old", "l1", 99L),
                parsed("edge", "l2", 100L),
                parsed("new", "l3", 500L),
                parsed("undated", "l4", null)), 1L, 100L);

        assertEquals(3, result.size());
        assertEquals("edge", result.get(0).getGuid());
        assertEquals("new", result.get(1).getGuid());
        assertEquals("undated", result.get(2).getGuid());
        assertNull(result.get(2).getPublishedAt());
    }

    @Test
    public void noCutoffKeepsEverything() {
        List<Article> result = ArticleMapper.fromParsed(1L, Arrays.asList(
                parsed("a", "l1", 1L), parsed("b", "l2", 2L)), 1L, null);

        assertEquals(2, result.size());
    }
}
