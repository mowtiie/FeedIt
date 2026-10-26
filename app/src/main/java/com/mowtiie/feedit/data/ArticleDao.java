package com.mowtiie.feedit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.Keep;

import com.mowtiie.feedit.model.Article;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

public class ArticleDao {

    private static final int ID_CHUNK_SIZE = 500;

    @Keep
    public enum SortOrder {
        NEWEST, OLDEST, UNREAD_FIRST, BY_FEED
    }

    private final DatabaseHelper dbHelper;

    public ArticleDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public List<Article> insertAllOrIgnore(List<Article> articles) {
        if (articles == null || articles.isEmpty()) {
            return new ArrayList<>();
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        List<Article> insertedArticles = new ArrayList<>();
        db.beginTransaction();
        try {
            for (Article article : articles) {
                long id = db.insertWithOnConflict("articles", null,
                        toContentValues(article), SQLiteDatabase.CONFLICT_IGNORE);
                if (id != -1) {
                    article.setId(id);
                    insertedArticles.add(article);
                }
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
        return insertedArticles;
    }

    public void setRead(long articleId, boolean read) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_read", read ? 1 : 0);
        db.update("articles", values, "id = ?", new String[]{String.valueOf(articleId)});
    }

    public void setStarred(long articleId, boolean starred) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("is_starred", starred ? 1 : 0);
        db.update("articles", values, "id = ?", new String[]{String.valueOf(articleId)});
    }

    public void setReadForIds(Collection<Long> articleIds, boolean read) {
        if (articleIds == null || articleIds.isEmpty()) {
            return;
        }
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        List<Long> ids = new ArrayList<>(articleIds);
        db.beginTransaction();
        try {
            for (int start = 0; start < ids.size(); start += ID_CHUNK_SIZE) {
                List<Long> chunk = ids.subList(start, Math.min(start + ID_CHUNK_SIZE, ids.size()));
                String placeholders = String.join(",", Collections.nCopies(chunk.size(), "?"));
                Object[] args = new Object[chunk.size() + 1];
                args[0] = read ? 1 : 0;
                for (int i = 0; i < chunk.size(); i++) {
                    args[i + 1] = chunk.get(i);
                }
                db.execSQL("UPDATE articles SET is_read = ? WHERE id IN (" + placeholders + ")", args);
            }
            db.setTransactionSuccessful();
        } finally {
            db.endTransaction();
        }
    }

    /** Deletes unstarred articles published before the cutoff. Undated articles are never pruned. */
    public int deleteOlderThan(long cutoffMillis) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        return db.delete("articles", "is_starred = 0 AND published_at IS NOT NULL AND published_at < ?",
                new String[]{String.valueOf(cutoffMillis)});
    }

    public Article getArticleById(long articleId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("articles", null, "id = ?",
                new String[]{String.valueOf(articleId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToArticle(cursor);
            }
            return null;
        }
    }

    public List<Article> getArticles(Long feedId, Long tagId, boolean showRead, boolean showUnread, boolean starredOnly, String searchQuery, ArticleDao.SortOrder sortOrder) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Article> articles = new ArrayList<>();

        StringBuilder sql = new StringBuilder("SELECT DISTINCT a.* FROM articles a ");
        List<String> args = new ArrayList<>();

        if (tagId != null) {
            sql.append("INNER JOIN feed_tags ft ON ft.feed_id = a.feed_id ");
        }

        List<String> conditions = new ArrayList<>();
        if (tagId != null) {
            conditions.add("ft.tag_id = ?");
            args.add(String.valueOf(tagId));
        }

        if (feedId != null) {
            conditions.add("a.feed_id = ?");
            args.add(String.valueOf(feedId));
        }

        if (showRead != showUnread) {
            conditions.add(showRead ? "a.is_read = 1" : "a.is_read = 0");
        }

        if (starredOnly) {
            conditions.add("a.is_starred = 1");
        }

        if (searchQuery != null && !searchQuery.trim().isEmpty()) {
            conditions.add("(a.title LIKE ? ESCAPE '\\' OR a.content LIKE ? ESCAPE '\\')");
            String escaped = searchQuery.trim().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
            String like = "%" + escaped + "%";
            args.add(like);
            args.add(like);
        }

        if (!conditions.isEmpty()) {
            sql.append("WHERE ").append(String.join(" AND ", conditions)).append(" ");
        }

        sql.append("ORDER BY ").append(orderByClause(sortOrder));

        try (Cursor cursor = db.rawQuery(sql.toString(), args.toArray(new String[0]))) {
            while (cursor.moveToNext()) {
                articles.add(cursorToArticle(cursor));
            }
        }
        return articles;
    }

    private String orderByClause(SortOrder sortOrder) {
        switch (sortOrder) {
            case OLDEST:
                return "a.published_at ASC";
            case UNREAD_FIRST:
                return "a.is_read ASC, a.published_at DESC";
            case BY_FEED:
                return "a.feed_id ASC, a.published_at DESC";
            case NEWEST:
            default:
                return "a.published_at DESC";
        }
    }

    static Article cursorToArticle(Cursor cursor) {
        Article article = new Article();
        article.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        article.setFeedId(cursor.getLong(cursor.getColumnIndexOrThrow("feed_id")));
        article.setGuid(cursor.getString(cursor.getColumnIndexOrThrow("guid")));
        article.setTitle(cursor.getString(cursor.getColumnIndexOrThrow("title")));
        article.setLink(cursor.getString(cursor.getColumnIndexOrThrow("link")));
        article.setAuthor(cursor.getString(cursor.getColumnIndexOrThrow("author")));
        article.setSummary(cursor.getString(cursor.getColumnIndexOrThrow("summary")));
        article.setContent(cursor.getString(cursor.getColumnIndexOrThrow("content")));
        article.setImageUrl(cursor.getString(cursor.getColumnIndexOrThrow("image_url")));
        int publishedIdx = cursor.getColumnIndexOrThrow("published_at");
        article.setPublishedAt(cursor.isNull(publishedIdx) ? null : cursor.getLong(publishedIdx));
        article.setFetchedAt(cursor.getLong(cursor.getColumnIndexOrThrow("fetched_at")));
        article.setRead(cursor.getInt(cursor.getColumnIndexOrThrow("is_read")) != 0);
        article.setStarred(cursor.getInt(cursor.getColumnIndexOrThrow("is_starred")) != 0);
        return article;
    }

    private ContentValues toContentValues(Article article) {
        ContentValues values = new ContentValues();
        values.put("feed_id", article.getFeedId());
        values.put("guid", article.getGuid());
        values.put("title", article.getTitle());
        values.put("link", article.getLink());
        values.put("author", article.getAuthor());
        values.put("summary", article.getSummary());
        values.put("content", article.getContent());
        values.put("image_url", article.getImageUrl());
        if (article.getPublishedAt() != null) {
            values.put("published_at", article.getPublishedAt());
        } else {
            values.putNull("published_at");
        }
        values.put("fetched_at", article.getFetchedAt());
        values.put("is_read", article.isRead() ? 1 : 0);
        values.put("is_starred", article.isStarred() ? 1 : 0);
        return values;
    }
}