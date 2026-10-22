package com.xinghe.store.ui;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Environment;
import android.os.Handler;
import android.os.Looper;
import android.text.TextUtils;
import android.util.Log;
import android.widget.ImageView;

import com.xinghe.store.Utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.lang.ref.SoftReference;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 异步图片加载器：内存软引用缓存 + SD 卡磁盘缓存。
 * 兼容 Android 1.5（不用 LruCache / getExternalCacheDir 等高版本 API）。
 */
public class ImageLoader {

    private static final String TAG = "ImageLoader";
    private static final int MAX_DISK_MB = 30;

    private final Map<String, SoftReference<Bitmap>> memCache =
            new HashMap<String, SoftReference<Bitmap>>();
    private final File cacheDir;
    private final ExecutorService pool = Executors.newFixedThreadPool(2);
    private final Handler handler = new Handler(Looper.getMainLooper());

    public ImageLoader(Context c) {
        File dir = null;
        try {
            if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
                dir = new File(Environment.getExternalStorageDirectory(),
                        "Android/data/" + c.getPackageName() + "/cache/icons");
            }
        } catch (Exception e) {
            dir = null;
        }
        if (dir == null) {
            dir = new File(c.getCacheDir(), "icons");
        }
        cacheDir = dir;
        try {
            if (!cacheDir.exists()) cacheDir.mkdirs();
        } catch (Exception e) {
            // ignore
        }
    }

    public void display(String url, ImageView iv, int fallbackRes) {
        if (TextUtils.isEmpty(url)) {
            iv.setImageResource(fallbackRes);
            return;
        }
        iv.setTag(url);
        Bitmap bmp = getFromMem(url);
        if (bmp != null) {
            iv.setImageBitmap(bmp);
            return;
        }
        iv.setImageResource(fallbackRes);
        pool.execute(new LoadTask(url, iv, fallbackRes));
    }

    private Bitmap getFromMem(String url) {
        SoftReference<Bitmap> ref = memCache.get(url);
        if (ref != null) {
            return ref.get();
        }
        return null;
    }

    private class LoadTask implements Runnable {
        private final String url;
        private final ImageView iv;
        private final int fallback;

        LoadTask(String url, ImageView iv, int fallback) {
            this.url = url;
            this.iv = iv;
            this.fallback = fallback;
        }

        public void run() {
            Bitmap bmp = null;
            File f = new File(cacheDir, Utils.md5(url) + ".img");
            try {
                if (f.exists() && f.length() > 0) {
                    bmp = BitmapFactory.decodeFile(f.getAbsolutePath());
                } else {
                    bmp = download(url);
                    if (bmp != null) saveToDisk(f, bmp);
                }
            } catch (Throwable t) {
                Log.w(TAG, "load fail: " + url, t);
            }
            if (bmp != null) {
                memCache.put(url, new SoftReference<Bitmap>(bmp));
                final Bitmap result = bmp;
                handler.post(new Runnable() {
                    public void run() {
                        Object tag = iv.getTag();
                        if (url.equals(tag)) {
                            iv.setImageBitmap(result);
                        }
                    }
                });
            } else {
                handler.post(new Runnable() {
                    public void run() {
                        Object tag = iv.getTag();
                        if (url.equals(tag)) {
                            iv.setImageResource(fallback);
                        }
                    }
                });
            }
        }
    }

    private Bitmap download(String url) {
        HttpURLConnection conn = null;
        try {
            conn = (HttpURLConnection) new URL(url).openConnection();
            conn.setConnectTimeout(12000);
            conn.setReadTimeout(12000);
            conn.setInstanceFollowRedirects(true);
            if (conn.getResponseCode() != 200) return null;
            InputStream in = conn.getInputStream();
            Bitmap bmp = BitmapFactory.decodeStream(in);
            in.close();
            return bmp;
        } catch (Throwable t) {
            return null;
        } finally {
            if (conn != null) conn.disconnect();
        }
    }

    private void saveToDisk(File f, Bitmap bmp) {
        FileOutputStream out = null;
        try {
            out = new FileOutputStream(f);
            bmp.compress(Bitmap.CompressFormat.PNG, 100, out);
            out.flush();
            trimDiskCache();
        } catch (Exception e) {
            // ignore
        } finally {
            Utils.closeQuietly(out);
        }
    }

    /** 磁盘缓存超限时按时间淘汰最旧的一半 */
    private synchronized void trimDiskCache() {
        try {
            File[] files = cacheDir.listFiles();
            if (files == null) return;
            long total = 0;
            for (File f : files) total += f.length();
            if (total <= MAX_DISK_MB * 1024L * 1024L) return;
            java.util.Arrays.sort(files, new java.util.Comparator<File>() {
                public int compare(File a, File b) {
                    return Long.valueOf(a.lastModified()).compareTo(b.lastModified());
                }
            });
            int toDelete = files.length / 2;
            for (int i = 0; i < toDelete; i++) files[i].delete();
        } catch (Exception e) {
            // ignore
        }
    }

    /** 清空磁盘缓存 */
    public void clearDiskCache() {
        try {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) f.delete();
            }
        } catch (Exception e) {
            // ignore
        }
        memCache.clear();
    }

    /** 磁盘缓存大小（字节） */
    public long diskCacheSize() {
        long total = 0;
        try {
            File[] files = cacheDir.listFiles();
            if (files != null) {
                for (File f : files) total += f.length();
            }
        } catch (Exception e) {
            // ignore
        }
        return total;
    }
}
