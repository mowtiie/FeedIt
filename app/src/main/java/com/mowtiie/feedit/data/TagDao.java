package com.mowtiie.feedit.data;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.mowtiie.feedit.model.Tag;

import java.util.ArrayList;
import java.util.List;

public class TagDao {

    private final DatabaseHelper dbHelper;

    public TagDao(DatabaseHelper dbHelper) {
        this.dbHelper = dbHelper;
    }

    public long insertTag(Tag tag) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", tag.getName());
        values.put("color", tag.getColor());
        values.put("created_at", tag.getCreatedAt());
        return db.insertOrThrow("tags", null, values);
    }

    public int updateTag(Tag tag) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put("name", tag.getName());
        values.put("color", tag.getColor());
        return db.update("tags", values, "id = ?", new String[]{String.valueOf(tag.getId())});
    }

    public void deleteTag(long tagId) {
        SQLiteDatabase db = dbHelper.getWritableDatabase();
        db.delete("tags", "id = ?", new String[]{String.valueOf(tagId)});
    }

    public Tag getTagById(long tagId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        try (Cursor cursor = db.query("tags", null, "id = ?",
                new String[]{String.valueOf(tagId)}, null, null, null)) {
            if (cursor.moveToFirst()) {
                return cursorToTag(cursor);
            }
            return null;
        }
    }

    public List<Tag> getAllTags() {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        List<Tag> tags = new ArrayList<>();
        try (Cursor cursor = db.query("tags", null, null, null,
                null, null, "name COLLATE NOCASE ASC")) {
            while (cursor.moveToNext()) {
                tags.add(cursorToTag(cursor));
            }
        }
        return tags;
    }

    public int getUnreadCountForTag(long tagId) {
        SQLiteDatabase db = dbHelper.getReadableDatabase();
        String sql = "SELECT COUNT(*) FROM articles a " +
                "INNER JOIN feed_tags ft ON ft.feed_id = a.feed_id " +
                "WHERE ft.tag_id = ? AND a.is_read = 0";
        try (Cursor cursor = db.rawQuery(sql, new String[]{String.valueOf(tagId)})) {
            return cursor.moveToFirst() ? cursor.getInt(0) : 0;
        }
    }

    static Tag cursorToTag(Cursor cursor) {
        Tag tag = new Tag();
        tag.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        tag.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        tag.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
        tag.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        return tag;
    }
}