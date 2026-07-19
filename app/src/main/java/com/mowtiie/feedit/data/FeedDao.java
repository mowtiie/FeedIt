package com.mowtiie.feedit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mowtiie.feedit.model.Feed;
import com.mowtiie.feedit.model.FeedTags;
import com.mowtiie.feedit.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class FeedDao {

    private final DatabaseHelper dbHelper;

    public FeedDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertFeed(Feed feed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = toContentValues(feed);
        return db.insertOrThrow("feeds", null, values);
    }

    public int updateFeed(Feed feed) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = toContentValues(feed);
        return db.update("feeds", values, "id = ?",
                new String[]{String.valueOf(feed.getId())});
    }

    public void deleteFeed(long feedId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("feeds", "id = ?", new String[]{String.valueOf(feedId)});
    }

    public Feed getFeedById(long feedId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("feeds", null, "id = ?",
                new String[]{String.valueOf(feedId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToFeed(cursor);
            }
            return null;
        }
    }

    public List<Feed> getAllFeeds() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Feed> feeds = new ArrayList<>();
        try (Cursor cursor = db.query("feeds", null, null, null,
                null, null, "title COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                feeds.add(cursorToFeed(cursor));
            }
        }
        return feeds;
    }

    public List<FeedTags> getAllFeedsWithTags() {
        List<Feed> feeds = getAllFeeds();
        List<FeedTags> result = new ArrayList<>();
        for (Feed feed : feeds) {
            result.add(new FeedTags(feed, getTagsForFeed(feed.getId())));
        }
        return result;
    }

    public List<Tag> getTagsForFeed(long feedId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Tag> tags = new ArrayList<>();
        String sql = "SELECT t.* FROM tags t " +
                "INNER JOIN feed_tags ft ON ft.tag_id = t.id " +
                "WHERE ft.feed_id = ? " +
                "ORDER BY t.name COLLATE NOCASE ASC";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(feedId)})) {
            while (cursor.moveToNext()) {
                tags.add(TagDao.cursorToTag(cursor));
            }
        }
        return tags;
    }

    public void addTagToFeed(long feedId, long tagId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("feed_id", feedId);
        values.put("tag_id", tagId);
        db.insertWithOnConflict("feed_tags", null, values, SQLiteDatabase.CONFLICT_IGNORE);
    }

    public void removeTagFromFeed(long feedId, long tagId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("feed_tags", "feed_id = ? AND tag_id = ?",
                new String[]{String.valueOf(feedId), String.valueOf(tagId)});
    }

    public void setTagsForFeed(long feedId, List<Long> tagIds) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.beginTransaction();
        try {
            db.delete("feed_tags", "feed_id = ?", new String[]{String.valueOf(feedId)});
            for (Long tagId : tagIds) {
                ContentValues values = new ContentValues();
                values.put("feed_id", feedId);
                values.put("tag_id", tagId);
                db.insertWithOnConflict("feed_tags", null, values, SQLiteDatabase.CONFLICT_IGNORE);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    static Feed cursorToFeed(Cursor cursor) {
        Feed feed = new Feed();
        feed.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        feed.setUrl(cursor.getString(cursor.getColumnIndexOrThrow("url")));
        feed.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        feed.setSiteUrl(cursor.getString(cursor.getColumnIndexOrThrow("site_url")));
        feed.setDescription(cursor.getString(cursor.getColumnIndexOrThrow("description")));
        feed.setFaviconUrl(cursor.getString(cursor.getColumnIndexOrThrow("favicon_url")));
        feed.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        feed.setEtag(cursor.getString(cursor.getColumnIndexOrThrow("etag")));
        feed.setLastModified(cursor.getString(cursor.getColumnIndexOrThrow("last_modified")));
        int lastFetchedIdx = cursor.getColumnIndexOrThrow("last_fetched");
        feed.setLastFetched(cursor.isNull(lastFetchedIdx) ? null : cursor.getLong(lastFetchedIdx));
        feed.setNotifyNew(cursor.getInt(cursor.getColumnIndexOrThrow("notify_new")) != 0);
        feed.setOpenMode(cursor.getInt(cursor.getColumnIndexOrThrow("open_mode")));
        feed.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        return feed;
    }

    private ContentValues toContentValues(Feed feed) {
        ContentValues values = new ContentValues();
        values.put("url", feed.getUrl());
        values.put("title", feed.getTitle());
        values.put("site_url", feed.getSiteUrl());
        values.put("description", feed.getDescription());
        values.put("favicon_url", feed.getFaviconUrl());
        values.put("image_url", feed.getImageUrl());
        values.put("etag", feed.getEtag());
        values.put("last_modified", feed.getLastModified());
        if (feed.getLastFetched() != null) {
            values.put("last_fetched", feed.getLastFetched());
        } else {
            values.putNull("last_fetched");
        }
        values.put("notify_new", feed.isNotifyNew() ? 1 : 0);
        values.put("open_mode", feed.getOpenMode());
        values.put("created_at", feed.getCreatedAt());
        return values;
    }

    public void setNotifyNew(long feedId, boolean enabled) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("notify_new", enabled ? 1 : 0);
        db.update("feeds", values, "id = ?", new String[]{String.valueOf(feedId)});
    }
}
