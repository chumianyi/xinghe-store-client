package com.xinghe.store.net;

import com.xinghe.store.Utils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.zip.GZIPInputStream;

/**
 * HTTP 工具（GET）。
 * Android 1.5 的 HttpURLConnection 可用；gzip 手动解压。
 */
public final class Http {

    public static final int TIMEOUT = 15000;

    private Http() {
    }

    /** GET 文本（UTF-8），失败抛 IOException */
    public static String get(String urlStr) throws IOException {
        byte[] data = getBytes(urlStr);
        return new String(data, "UTF-8");
    }

    /** GET 二进制，失败抛 IOException */
    public static byte[] getBytes(String urlStr) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        try {
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setRequestProperty("Accept-Encoding", "gzip");
            conn.setInstanceFollowRedirects(true);
            conn.setUseCaches(false);

            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code);
            }

            InputStream in = conn.getInputStream();
            String enc = conn.getHeaderField("Content-Encoding");
            if (enc != null && enc.toLowerCase().contains("gzip")) {
                in = new GZIPInputStream(in);
            }
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            byte[] buf = new byte[4096];
            int n;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
            }
            in.close();
            return out.toByteArray();
        } finally {
            conn.disconnect();
        }
    }

    /** 下载到文件，返回保存路径 */
    public static String downloadToFile(String urlStr, String filePath,
                                        ProgressCallback cb) throws IOException {
        HttpURLConnection conn = (HttpURLConnection) new URL(urlStr).openConnection();
        InputStream in = null;
        OutputStream out = null;
        try {
            conn.setConnectTimeout(TIMEOUT);
            conn.setReadTimeout(TIMEOUT);
            conn.setRequestMethod("GET");
            conn.setInstanceFollowRedirects(true);
            int code = conn.getResponseCode();
            if (code < 200 || code >= 300) {
                throw new IOException("HTTP " + code);
            }
            int total = conn.getContentLength();
            in = conn.getInputStream();
            out = new java.io.FileOutputStream(filePath);
            byte[] buf = new byte[8192];
            int n;
            long done = 0;
            while ((n = in.read(buf)) != -1) {
                out.write(buf, 0, n);
                done += n;
                if (cb != null) cb.onProgress(done, total);
            }
            out.flush();
            return filePath;
        } finally {
            Utils.closeQuietly(in);
            Utils.closeQuietly(out);
            conn.disconnect();
        }
    }

    public interface ProgressCallback {
        void onProgress(long done, long total);
    }
}
