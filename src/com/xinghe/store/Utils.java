package com.xinghe.store;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.text.ClipboardManager;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.security.MessageDigest;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * 兼容工具类（支持 Android 1.5 / API 3 及以上）。
 * 注意：Android 1.5 只有 Build.VERSION.SDK（字符串）没有 SDK_INT，
 * 因此 API 级别判断全部走字符串解析。
 */
public final class Utils {

    private static int sSdk = -1;

    private Utils() {
    }

    /** 当前系统 API 级别（Android 1.5 上为 3） */
    public static int sdkInt() {
        if (sSdk < 0) {
            try {
                sSdk = Integer.parseInt(Build.VERSION.SDK);
            } catch (Exception e) {
                sSdk = 3;
            }
        }
        return sSdk;
    }

    public static boolean hasApi(int level) {
        return sdkInt() >= level;
    }

    public static void toast(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_LONG).show();
    }

    public static void toast(Context c, int resId) {
        Toast.makeText(c, resId, Toast.LENGTH_LONG).show();
    }

    public static void toastShort(Context c, String msg) {
        Toast.makeText(c, msg, Toast.LENGTH_SHORT).show();
    }

    public static void toastShort(Context c, int resId) {
        Toast.makeText(c, resId, Toast.LENGTH_SHORT).show();
    }

    /**
     * 复制文本到剪贴板。
     * API 11+ 走 android.content.ClipboardManager（反射，因为编译基准是 API 3），
     * 以下走 android.text.ClipboardManager（1.5 时代 API，新版系统仍兼容）。
     */
    public static void copyToClipboard(Context c, String text) {
        if (hasApi(11)) {
            try {
                Object cm = c.getSystemService("clipboard");
                cm.getClass().getMethod("setText", CharSequence.class).invoke(cm, text);
                return;
            } catch (Exception e) {
                // 落到旧实现
            }
        }
        try {
            // 旧版剪贴板服务名同为 "clipboard"（Context.CLIPBOARD_SERVICE 是 API 11 常量，编译基准是 API 3 故用字面量）
            ClipboardManager cm = (ClipboardManager) c.getSystemService("clipboard");
            cm.setText(text);
        } catch (Exception e) {
            // ignore
        }
    }

    /** 读取流全部内容 */
    public static byte[] readAll(InputStream in) throws IOException {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        copy(in, out);
        return out.toByteArray();
    }

    public static void copy(InputStream in, OutputStream out) throws IOException {
        byte[] buf = new byte[4096];
        int n;
        while ((n = in.read(buf)) != -1) {
            out.write(buf, 0, n);
        }
    }

    public static void closeQuietly(Closeable c) {
        if (c != null) {
            try {
                c.close();
            } catch (IOException e) {
                // ignore
            }
        }
    }

    public static String md5(String s) {
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            byte[] d = md.digest(s.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length; i++) {
                int b = d[i] & 0xff;
                if (b < 16) sb.append('0');
                sb.append(Integer.toHexString(b));
            }
            return sb.toString();
        } catch (Exception e) {
            return String.valueOf(s.hashCode());
        }
    }

    private static final SimpleDateFormat FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm");

    public static String formatTime(String iso) {
        if (iso == null || iso.length() == 0) return "";
        try {
            String s = iso.replace('T', ' ');
            int dot = s.indexOf('.');
            if (dot > 0) s = s.substring(0, dot);
            if (s.length() > 19) s = s.substring(0, 19);
            Date d = FMT.parse(s);
            return FMT.format(d);
        } catch (Exception e) {
            return iso;
        }
    }

    /** 把 "1.0.3" 解析成可比较的整数 */
    public static int versionToInt(String v) {
        if (v == null) return 0;
        try {
            String[] parts = v.split("\\.");
            int major = parts.length > 0 ? Integer.parseInt(parts[0]) : 0;
            int minor = parts.length > 1 ? Integer.parseInt(parts[1]) : 0;
            int patch = parts.length > 2 ? Integer.parseInt(parts[2]) : 0;
            return major * 10000 + minor * 100 + patch;
        } catch (Exception e) {
            return 0;
        }
    }

    /** 用浏览器打开网址 */
    public static void openUrl(Activity a, String url) {
        if (url == null || url.length() == 0) {
            toastShort(a, "链接为空");
            return;
        }
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            a.startActivity(i);
        } catch (Exception e) {
            toastShort(a, "没有可用的浏览器");
        }
    }

    /** 文件名安全化 */
    public static String safeFileName(String name) {
        if (name == null) return "app";
        return name.replaceAll("[\\\\/:*?\"<>|\\s]", "_");
    }
}
