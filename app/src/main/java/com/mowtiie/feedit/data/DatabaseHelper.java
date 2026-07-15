package com.mowtiie.feedit.data;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "feedit.db";
    private static final int DATABASE_VERSION = 1;

    private static volatile DatabaseHelper instance;

    private static final String CREATE_FEEDS =
            "CREATE TABLE feeds (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "url TEXT NOT NULL UNIQUE," +
                    "title TEXT," +
                    "site_url TEXT," +
                    "description TEXT," +
                    "favicon_url TEXT," +
                    "image_url TEXT," +
                    "etag TEXT," +
                    "last_modified TEXT," +
                    "last_fetched INTEGER," +
                    "notify_new INTEGER NOT NULL DEFAULT 0," +
                    "open_mode INTEGER NOT NULL DEFAULT 0," +
                    "created_at INTEGER NOT NULL" +
                    ")";

    private static final String CREATE_TAGS =
            "CREATE TABLE tags (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "name TEXT NOT NULL UNIQUE," +
                    "color TEXT," +
                    "created_at INTEGER NOT NULL" +
                    ")";

    private static final String CREATE_FEED_TAGS =
            "CREATE TABLE feed_tags (" +
                    "feed_id INTEGER NOT NULL," +
                    "tag_id INTEGER NOT NULL," +
                    "PRIMARY KEY (feed_id, tag_id)," +
                    "FOREIGN KEY (feed_id) REFERENCES feeds(id) ON DELETE CASCADE," +
                    "FOREIGN KEY (tag_id) REFERENCES tags(id) ON DELETE CASCADE" +
                    ")";

    private static final String CREATE_FEED_TAGS_INDEX =
            "CREATE INDEX idx_feed_tags_tag ON feed_tags(tag_id)";

    private static final String CREATE_ARTICLES =
            "CREATE TABLE articles (" +
                    "id INTEGER PRIMARY KEY AUTOINCREMENT," +
                    "feed_id INTEGER NOT NULL," +
                    "guid TEXT," +
                    "title TEXT," +
                    "link TEXT," +
                    "author TEXT," +
                    "summary TEXT," +
                    "content TEXT," +
                    "image_url TEXT," +
                    "published_at INTEGER," +
                    "fetched_at INTEGER NOT NULL," +
                    "is_read INTEGER NOT NULL DEFAULT 0," +
                    "is_starred INTEGER NOT NULL DEFAULT 0," +
                    "FOREIGN KEY (feed_id) REFERENCES feeds(id) ON DELETE CASCADE," +
                    "UNIQUE (feed_id, guid)" +
                    ")";

    private static final String CREATE_ARTICLES_FEED_INDEX =
            "CREATE INDEX idx_articles_feed ON articles(feed_id)";

    private static final String CREATE_ARTICLES_UNREAD_INDEX =
            "CREATE INDEX idx_articles_unread ON articles(is_read)";

    private static final String CREATE_ARTICLES_PUBLISHED_INDEX =
            "CREATE INDEX idx_articles_published ON articles(published_at)";

    private DatabaseHelper(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
    }

    public static synchronized DatabaseHelper getInstance(Context context) {
        if (instance == null) {
            instance = new DatabaseHelper(context);
        }
        return instance;
    }

    @Override
    public void onConfigure(SQLiteDatabase db) {
        super.onConfigure(db);
        db.setForeignKeyConstraintsEnabled(true);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_FEEDS);
        db.execSQL(CREATE_TAGS);
        db.execSQL(CREATE_FEED_TAGS);
        db.execSQL(CREATE_FEED_TAGS_INDEX);
        db.execSQL(CREATE_ARTICLES);
        db.execSQL(CREATE_ARTICLES_FEED_INDEX);
        db.execSQL(CREATE_ARTICLES_UNREAD_INDEX);
        db.execSQL(CREATE_ARTICLES_PUBLISHED_INDEX);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
    }
}