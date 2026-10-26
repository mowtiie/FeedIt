package com.mowtiie.feedit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.content.Context;

import org.robolectric.RuntimeEnvironment;

import com.mowtiie.feedit.model.Article;
import com.mowtiie.feedit.model.Feed;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class)
public class ArticleDaoTest {

    private ArticleDao dao;
    private long feedId;

    @Before
    public void setUp() throws Exception {
        Field instance = DatabaseHelper.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        Context context = RuntimeEnvironment.getApplication();
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        dao = new ArticleDao(helper);

        Feed feed = new Feed();
        feed.setUrl("https://example.com/feed");
        feed.setTitle("Example");
        feed.setCreatedAt(1L);
        feedId = new FeedDao(helper).insertFeed(feed);
    }

    private Article article(String guid, Long publishedAt) {
        Article a = new Article();
        a.setFeedId(feedId);
        a.setGuid(guid);
        a.setTitle("title " + guid);
        a.setPublishedAt(publishedAt);
        a.setFetchedAt(1L);
        return a;
    }

    private List<Article> all() {
        return dao.getArticles(null, null, true, true, false, null, ArticleDao.SortOrder.NEWEST);
    }

    @Test
    public void setReadForIds_updatesOnlyGivenIds_andCanBeUndone() {
        List<Article> inserted = dao.insertAllOrIgnore(Arrays.asList(
                article("a", 1L), article("b", 2L), article("c", 3L)));
        long a = inserted.get(0).getId();
        long b = inserted.get(1).getId();

        dao.setReadForIds(Arrays.asList(a, b), true);

        assertTrue(dao.getArticleById(a).isRead());
        assertTrue(dao.getArticleById(b).isRead());
        assertFalse(dao.getArticleById(inserted.get(2).getId()).isRead());

        dao.setReadForIds(Arrays.asList(a, b), false);

        assertFalse(dao.getArticleById(a).isRead());
        assertFalse(dao.getArticleById(b).isRead());
    }

    @Test
    public void setReadForIds_handlesMoreIdsThanOneChunk() {
        List<Article> batch = new ArrayList<>();
        for (int i = 0; i < 1200; i++) {
            batch.add(article("g" + i, (long) i));
        }
        List<Long> ids = new ArrayList<>();
        for (Article a : dao.insertAllOrIgnore(batch)) {
            ids.add(a.getId());
        }

        dao.setReadForIds(ids, true);

        List<Article> stored = all();
        assertEquals(1200, stored.size());
        for (Article a : stored) {
            assertTrue(a.isRead());
        }
    }

    @Test
    public void setReadForIds_emptyIsNoOp() {
        dao.insertAllOrIgnore(Collections.singletonList(article("a", 1L)));
        dao.setReadForIds(Collections.<Long>emptyList(), true);
        assertFalse(all().get(0).isRead());
    }

    @Test
    public void deleteOlderThan_keepsStarredUndatedAndNewer() {
        List<Article> inserted = dao.insertAllOrIgnore(Arrays.asList(
                article("old", 100L),
                article("oldStarred", 100L),
                article("undated", null),
                article("boundary", 1000L),
                article("new", 5000L)));
        dao.setStarred(inserted.get(1).getId(), true);

        int deleted = dao.deleteOlderThan(1000L);

        assertEquals(1, deleted);
        List<String> remaining = new ArrayList<>();
        for (Article a : all()) {
            remaining.add(a.getGuid());
        }
        assertEquals(4, remaining.size());
        assertFalse(remaining.contains("old"));
        assertTrue(remaining.contains("oldStarred"));
        assertTrue(remaining.contains("undated"));
        assertTrue(remaining.contains("boundary"));
        assertTrue(remaining.contains("new"));
    }

    @Test
    public void insertAllOrIgnore_skipsDuplicateGuids() {
        dao.insertAllOrIgnore(Collections.singletonList(article("a", 1L)));

        List<Article> second = dao.insertAllOrIgnore(Arrays.asList(article("a", 1L), article("b", 2L)));

        assertEquals(1, second.size());
        assertEquals("b", second.get(0).getGuid());
        assertEquals(2, all().size());
    }

    @Test
    public void search_treatsPercentAndUnderscoreLiterally() {
        Article plain1 = article("1", 1L);
        Article plain2 = article("2", 2L);
        Article special = article("3", 3L);
        special.setTitle("100% done_ok");
        dao.insertAllOrIgnore(Arrays.asList(plain1, plain2, special));

        List<Article> percent = dao.getArticles(null, null, true, true, false, "100%", ArticleDao.SortOrder.NEWEST);
        List<Article> wildcard = dao.getArticles(null, null, true, true, false, "%", ArticleDao.SortOrder.NEWEST);
        List<Article> underscore = dao.getArticles(null, null, true, true, false, "e_o", ArticleDao.SortOrder.NEWEST);

        assertEquals(1, percent.size());
        assertEquals(1, wildcard.size());
        assertEquals(1, underscore.size());
    }
}
