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

    static Tag cursorToTag(Cursor cursor) {
        Tag tag = new Tag();
        tag.setId(cursor.getLong(cursor.getColumnIndexOrThrow("id")));
        tag.setName(cursor.getString(cursor.getColumnIndexOrThrow("name")));
        tag.setColor(cursor.getString(cursor.getColumnIndexOrThrow("color")));
        tag.setCreatedAt(cursor.getLong(cursor.getColumnIndexOrThrow("created_at")));
        return tag;
    }
}