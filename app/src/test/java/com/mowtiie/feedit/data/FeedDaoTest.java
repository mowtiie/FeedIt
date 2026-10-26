package com.mowtiie.feedit.data;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import com.mowtiie.feedit.model.Feed;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.lang.reflect.Field;

@RunWith(RobolectricTestRunner.class)
@Config(application = android.app.Application.class)
public class FeedDaoTest {

    private Context context;

    @Before
    public void resetSingleton() throws Exception {
        Field instance = DatabaseHelper.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);
        context = RuntimeEnvironment.getApplication();
    }

    private static Feed feed(String url) {
        Feed feed = new Feed();
        feed.setUrl(url);
        feed.setTitle("Feed");
        feed.setCreatedAt(1L);
        return feed;
    }

    @Test
    public void lastError_roundTripsAndClears() {
        DatabaseHelper helper = DatabaseHelper.getInstance(context);
        FeedDao dao = new FeedDao(helper);
        long id = dao.insertFeed(feed("https://example.com/a"));
        assertNull(dao.getFeedById(id).getLastError());

        dao.setLastError(id, "HTTP 404 fetching https://example.com/a");
        assertEquals("HTTP 404 fetching https://example.com/a", dao.getFeedById(id).getLastError());

        Feed loaded = dao.getFeedById(id);
        loaded.setLastError(null);
        dao.updateFeed(loaded);
        assertNull(dao.getFeedById(id).getLastError());
    }

    @Test
    public void upgradeFromV1_addsLastErrorColumnAndKeepsFeeds() {
        SQLiteDatabase v1 = context.openOrCreateDatabase("feedit.db", Context.MODE_PRIVATE, null);
        v1.execSQL("CREATE TABLE feeds (id INTEGER PRIMARY KEY AUTOINCREMENT, url TEXT NOT NULL UNIQUE,"
                + " title TEXT, site_url TEXT, description TEXT, favicon_url TEXT, image_url TEXT,"
                + " etag TEXT, last_modified TEXT, last_fetched INTEGER,"
                + " notify_new INTEGER NOT NULL DEFAULT 0, open_mode INTEGER NOT NULL DEFAULT 0,"
                + " created_at INTEGER NOT NULL)");
        v1.execSQL("INSERT INTO feeds (url, title, created_at) VALUES ('https://old.example/feed', 'Old', 5)");
        v1.setVersion(1);
        v1.close();

        FeedDao dao = new FeedDao(DatabaseHelper.getInstance(context));

        Feed migrated = dao.getAllFeeds().get(0);
        assertEquals("https://old.example/feed", migrated.getUrl());
        assertNull(migrated.getLastError());
        dao.setLastError(migrated.getId(), "boom");
        assertEquals("boom", dao.getFeedById(migrated.getId()).getLastError());
    }
}
