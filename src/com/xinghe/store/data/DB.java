package com.xinghe.store.data;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.ArrayList;
import java.util.List;

/** 本地数据库：收藏夹 + 浏览记录 + 下载记录 */
public class DB extends SQLiteOpenHelper {

    private static final String DB_NAME = "xinghe.db";
    private static final int DB_VERSION = 2;

    private static final String T_FAV = "favorites";
    private static final String T_HIS = "history";
    private static final String T_DL = "downloads";

    public DB(Context c) {
        super(c, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE " + T_FAV + " ("
                + "app_id TEXT PRIMARY KEY,"
                + "name TEXT,"
                + "icon TEXT,"
                + "platform TEXT,"
                + "ts INTEGER)");
        db.execSQL("CREATE TABLE " + T_HIS + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "app_id TEXT,"
                + "name TEXT,"
                + "action TEXT,"
                + "ts INTEGER)");
        db.execSQL("CREATE TABLE " + T_DL + " ("
                + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                + "app_id TEXT,"
                + "name TEXT,"
                + "path TEXT,"
                + "mime TEXT,"
                + "size INTEGER,"
                + "ts INTEGER)");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldV, int newV) {
        // 只增不删：老用户升级后保留收藏与浏览记录
        if (oldV < 2) {
            db.execSQL("CREATE TABLE IF NOT EXISTS " + T_DL + " ("
                    + "id INTEGER PRIMARY KEY AUTOINCREMENT,"
                    + "app_id TEXT,"
                    + "name TEXT,"
                    + "path TEXT,"
                    + "mime TEXT,"
                    + "size INTEGER,"
                    + "ts INTEGER)");
        }
    }

    // ---------- 收藏 ----------

    public boolean isFavorite(String appId) {
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(T_FAV, new String[]{"app_id"}, "app_id=?",
                new String[]{appId}, null, null, null);
        boolean has = cur.moveToFirst();
        cur.close();
        return has;
    }

    public void addFavorite(String appId, String name, String icon, String platform) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("app_id", appId);
        v.put("name", name);
        v.put("icon", icon);
        v.put("platform", platform);
        v.put("ts", System.currentTimeMillis());
        // insertWithOnConflict 是 API 8 才有，这里用 delete+insert 兼容 1.5
        db.delete(T_FAV, "app_id=?", new String[]{appId});
        db.insert(T_FAV, null, v);
    }

    public void removeFavorite(String appId) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_FAV, "app_id=?", new String[]{appId});
    }

    public List<String[]> listFavorites() {
        List<String[]> out = new ArrayList<String[]>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(T_FAV, new String[]{"app_id", "name", "icon", "platform"},
                null, null, null, null, "ts DESC");
        while (cur.moveToNext()) {
            out.add(new String[]{
                    cur.getString(0), cur.getString(1), cur.getString(2), cur.getString(3)});
        }
        cur.close();
        return out;
    }

    // ---------- 下载/浏览记录 ----------

    public void addHistory(String appId, String name, String action) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("app_id", appId);
        v.put("name", name);
        v.put("action", action);
        v.put("ts", System.currentTimeMillis());
        db.insert(T_HIS, null, v);
    }

    public List<String[]> listHistory() {
        List<String[]> out = new ArrayList<String[]>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(T_HIS, new String[]{"app_id", "name", "action", "ts"},
                null, null, null, null, "ts DESC LIMIT 200");
        while (cur.moveToNext()) {
            out.add(new String[]{cur.getString(0), cur.getString(1),
                    cur.getString(2), String.valueOf(cur.getLong(3))});
        }
        cur.close();
        return out;
    }

    public void clearHistory() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_HIS, null, null);
    }

    // ---------- 下载记录 ----------

    public void addDownload(String appId, String name, String path, String mime, long size) {
        SQLiteDatabase db = getWritableDatabase();
        ContentValues v = new ContentValues();
        v.put("app_id", appId);
        v.put("name", name);
        v.put("path", path);
        v.put("mime", mime);
        v.put("size", size);
        v.put("ts", System.currentTimeMillis());
        db.insert(T_DL, null, v);
    }

    /** 返回 {id, app_id, name, path, mime, size, ts} */
    public List<String[]> listDownloads() {
        List<String[]> out = new ArrayList<String[]>();
        SQLiteDatabase db = getReadableDatabase();
        Cursor cur = db.query(T_DL,
                new String[]{"id", "app_id", "name", "path", "mime", "size", "ts"},
                null, null, null, null, "ts DESC LIMIT 500");
        while (cur.moveToNext()) {
            out.add(new String[]{
                    cur.getString(0), cur.getString(1), cur.getString(2),
                    cur.getString(3), cur.getString(4),
                    String.valueOf(cur.getLong(5)), String.valueOf(cur.getLong(6))});
        }
        cur.close();
        return out;
    }

    public void deleteDownload(String id) {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_DL, "id=?", new String[]{id});
    }

    public void clearDownloads() {
        SQLiteDatabase db = getWritableDatabase();
        db.delete(T_DL, null, null);
    }
}
