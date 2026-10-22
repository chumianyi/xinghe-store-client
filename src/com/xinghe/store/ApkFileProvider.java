package com.xinghe.store;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.ParcelFileDescriptor;

import java.io.File;
import java.io.FileNotFoundException;

/**
 * 极简 FileProvider：把 /sdcard/xinghe/ 下的下载文件以 content:// 暴露，
 * 供 APK 安装与 IPA 分享使用（API 24+ 禁止 file:// 跨应用传递）。
 *
 * manifest 注册：
 *   &lt;provider android:name=".ApkFileProvider"
 *       android:authorities="com.xinghe.store.fileprovider"
 *       android:exported="false"
 *       android:grantUriPermissions="true" /&gt;
 *
 * Uri 形如：content://com.xinghe.store.fileprovider/files/&lt;文件名&gt;
 */
public class ApkFileProvider extends ContentProvider {

    @Override
    public boolean onCreate() {
        return true;
    }

    /** 只允许访问下载目录下的文件，并做路径穿越防护 */
    private File resolve(Uri uri) throws FileNotFoundException {
        String name = uri.getLastPathSegment();
        if (name == null || name.length() == 0) {
            throw new FileNotFoundException("empty name");
        }
        // 防御 ../ 与绝对路径
        if (name.indexOf('/') >= 0 || name.indexOf('\\') >= 0
                || name.equals(".") || name.equals("..")) {
            throw new FileNotFoundException("bad name");
        }
        File dir = new File(Environment.getExternalStorageDirectory(), "xinghe");
        File f = new File(dir, name);
        String dirPath = dir.getAbsolutePath();
        String filePath = f.getAbsolutePath();
        if (!filePath.startsWith(dirPath + File.separator)) {
            throw new FileNotFoundException("outside download dir");
        }
        if (!f.exists()) {
            throw new FileNotFoundException("not found: " + name);
        }
        return f;
    }

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode)
            throws FileNotFoundException {
        File f = resolve(uri);
        return ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY);
    }

    @Override
    public AssetFileDescriptor openAssetFile(Uri uri, String mode)
            throws FileNotFoundException {
        File f = resolve(uri);
        return new AssetFileDescriptor(
                ParcelFileDescriptor.open(f, ParcelFileDescriptor.MODE_READ_ONLY), 0,
                AssetFileDescriptor.UNKNOWN_LENGTH);
    }

    @Override
    public String getType(Uri uri) {
        return Utils.mimeOfFile(uri.getLastPathSegment());
    }

    @Override
    public Cursor query(Uri uri, String[] projection, String selection,
                        String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection,
                      String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }
}
