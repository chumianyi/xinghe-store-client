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
import java.io.File;
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

    /** 人类可读的文件大小 */
    public static String formatSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        if (bytes < 1024L * 1024) return String.format("%.1f KB", bytes / 1024.0);
        if (bytes < 1024L * 1024 * 1024) return String.format("%.1f MB", bytes / 1048576.0);
        return String.format("%.2f GB", bytes / 1073741824.0);
    }

    /** 按扩展名推断 MIME */
    public static String mimeOfFile(String name) {
        String lower = name.toLowerCase();
        if (lower.endsWith(".apk")) return "application/vnd.android.package-archive";
        if (lower.endsWith(".ipa")) return "application/octet-stream";
        if (lower.endsWith(".zip")) return "application/zip";
        if (lower.endsWith(".rar")) return "application/x-rar-compressed";
        if (lower.endsWith(".7z")) return "application/x-7z-compressed";
        if (lower.endsWith(".png")) return "image/png";
        if (lower.endsWith(".jpg") || lower.endsWith(".jpeg")) return "image/jpeg";
        if (lower.endsWith(".txt")) return "text/plain";
        if (lower.endsWith(".pdf")) return "application/pdf";
        return "application/octet-stream";
    }

    /**
     * 取文件的对外 Uri。
     * API 24+ 上 file:// 会被 StrictMode 拒绝，走应用内置 ApkFileProvider。
     */
    public static Uri fileUri(Context c, File f) {
        if (hasApi(24)) {
            try {
                return Uri.parse("content://" + c.getPackageName() + ".fileprovider/files/"
                        + Uri.encode(f.getName()));
            } catch (Exception e) {
                // 落到 file://
            }
        }
        return Uri.fromFile(f);
    }

    /** 安装 APK */
    public static void installApk(Activity a, File apk) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(fileUri(a, apk), "application/vnd.android.package-archive");
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (hasApi(24)) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            a.startActivity(i);
        } catch (Exception e) {
            toastShort(a, "没有可安装此应用的程序");
        }
    }

    /** 分享文件（IPA 等） */
    public static void shareFile(Activity a, File f) {
        try {
            Intent i = new Intent(Intent.ACTION_SEND);
            i.setType(mimeOfFile(f.getName()));
            i.putExtra(Intent.EXTRA_STREAM, fileUri(a, f));
            i.putExtra(Intent.EXTRA_SUBJECT, f.getName());
            if (hasApi(24)) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            a.startActivity(Intent.createChooser(i, "分享 " + f.getName()));
        } catch (Exception e) {
            toastShort(a, "分享失败");
        }
    }

    /** 用其他应用打开文件 */
    public static void openFile(Activity a, File f) {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(fileUri(a, f), mimeOfFile(f.getName()));
            i.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            if (hasApi(24)) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            a.startActivity(i);
        } catch (Exception e) {
            toastShort(a, "没有可打开此文件的应用");
        }
    }

    /** 从 Content-Disposition 头里解析文件名，失败返回 null */
    public static String fileNameFromDisposition(String cd, String url) {
        if (cd != null) {
            java.util.regex.Matcher m =
                    java.util.regex.Pattern.compile("filename\\*?=([^;]+)", java.util.regex.Pattern.CASE_INSENSITIVE)
                            .matcher(cd);
            if (m.find()) {
                String name = m.group(1).trim().replace("\"", "");
                int semi = name.indexOf(';');
                if (semi > 0) name = name.substring(0, semi);
                // UTF-8''xxx 形式
                int idx = name.toLowerCase().indexOf("''");
                if (name.toLowerCase().startsWith("utf-8") && idx >= 0) {
                    try {
                        name = java.net.URLDecoder.decode(name.substring(idx + 2), "UTF-8");
                    } catch (Exception e) {
                        // ignore
                    }
                }
                if (name.length() > 0 && name.indexOf('/') < 0 && name.indexOf('\\') < 0) {
                    return name;
                }
            }
        }
        if (url != null) {
            String path = Uri.parse(url).getLastPathSegment();
            if (path != null && path.length() > 0 && path.indexOf('/') < 0 && path.indexOf('\\') < 0) {
                return path;
            }
        }
        return null;
    }
}
