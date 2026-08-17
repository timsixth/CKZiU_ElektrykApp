package com.example.planlekcji.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.util.Log;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;

import java.lang.reflect.Type;

public class DatabaseCacheManager extends SQLiteOpenHelper {
    private static final String TAG = "DatabaseCacheManager";
    private static final String DATABASE_NAME = "app_cache.db";
    private static final int DATABASE_VERSION = 1;

    private static final String TABLE_CACHE = "cache_entries";
    private static final String COLUMN_KEY = "cache_key";
    private static final String COLUMN_DATA = "json_data";
    private static final String COLUMN_UPDATED_AT = "updated_at";

    private static DatabaseCacheManager instance;
    private final Gson gson;

    private DatabaseCacheManager(Context context) {
        super(context.getApplicationContext(), DATABASE_NAME, null, DATABASE_VERSION);
        this.gson = new GsonBuilder()
                .setDateFormat("yyyy-MM-dd HH:mm:ss")
                .create();
    }

    public static synchronized DatabaseCacheManager getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new DatabaseCacheManager(context.getApplicationContext());
        }
        return instance;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String createTable = "CREATE TABLE " + TABLE_CACHE + " (" +
                COLUMN_KEY + " TEXT PRIMARY KEY, " +
                COLUMN_DATA + " TEXT NOT NULL, " +
                COLUMN_UPDATED_AT + " INTEGER NOT NULL)";
        db.execSQL(createTable);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_CACHE);
        onCreate(db);
    }

    public synchronized void saveRawJson(String key, String json) {
        if (key == null || json == null) return;
        try {
            SQLiteDatabase db = getWritableDatabase();
            ContentValues values = new ContentValues();
            values.put(COLUMN_KEY, key);
            values.put(COLUMN_DATA, json);
            values.put(COLUMN_UPDATED_AT, System.currentTimeMillis());
            db.insertWithOnConflict(TABLE_CACHE, null, values, SQLiteDatabase.CONFLICT_REPLACE);
        } catch (Exception e) {
            Log.e(TAG, "Error saving raw JSON for key: " + key, e);
        }
    }

    public synchronized String getRawJson(String key) {
        if (key == null) return null;
        Cursor cursor = null;
        try {
            SQLiteDatabase db = getReadableDatabase();
            cursor = db.query(TABLE_CACHE, new String[]{COLUMN_DATA},
                    COLUMN_KEY + " = ?", new String[]{key},
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                return cursor.getString(0);
            }
        } catch (Exception e) {
            Log.e(TAG, "Error getting raw JSON for key: " + key, e);
        } finally {
            if (cursor != null) cursor.close();
        }
        return null;
    }

    public synchronized void saveObject(String key, Object object) {
        if (key == null || object == null) return;
        try {
            String json = gson.toJson(object);
            saveRawJson(key, json);
        } catch (Exception e) {
            Log.e(TAG, "Error saving object for key: " + key, e);
        }
    }

    public synchronized <T> T getObject(String key, Type type) {
        String json = getRawJson(key);
        if (json == null) return null;
        try {
            return gson.fromJson(json, type);
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing object for key: " + key, e);
            return null;
        }
    }

    public synchronized <T> T getObject(String key, Class<T> clazz) {
        String json = getRawJson(key);
        if (json == null) return null;
        try {
            return gson.fromJson(json, clazz);
        } catch (Exception e) {
            Log.e(TAG, "Error deserializing class for key: " + key, e);
            return null;
        }
    }

    public Gson getGson() {
        return gson;
    }
}
