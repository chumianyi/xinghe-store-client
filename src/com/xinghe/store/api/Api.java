package com.xinghe.store.api;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import com.xinghe.store.net.Http;

/**
 * 星河软件站 /old/ 适配层接口封装。
 * 全部为纯 JSON GET（不需要 JSONP callback）。
 */
public final class Api {

    public static final String BASE = "http://103.236.99.177:24512/iosipa/old";

    /** 登录令牌，由 XingheApp 启动时从本地偏好载入 */
    public static String token = "";

    private Api() {
    }

    private static String enc(String s) {
        try {
            return URLEncoder.encode(s == null ? "" : s, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            return "";
        }
    }

    private static String url(String path, String params) {
        return BASE + path + "?" + params;
    }

    private static JSONObject json(String urlStr) throws IOException, JSONException {
        return new JSONObject(Http.get(urlStr));
    }

    // ---------- 应用 ----------

    /** 应用列表：platform=all/ios/android/pwa，sort=downloads/rating/name/new */
    public static JSONObject apps(String platform, String q, String sort, int page, int limit)
            throws IOException, JSONException {
        String p = "platform=" + enc(platform) + "&q=" + enc(q) + "&sort=" + enc(sort)
                + "&page=" + page + "&limit=" + limit + "&_t=" + System.currentTimeMillis();
        return json(url("/apps", p));
    }

    /** 首页聚合 */
    public static JSONObject home() throws IOException, JSONException {
        return json(url("/home", "_t=" + System.currentTimeMillis()));
    }

    /** 应用详情 */
    public static JSONObject app(String appId) throws IOException, JSONException {
        return json(url("/app", "app_id=" + enc(appId) + "&_t=" + System.currentTimeMillis()));
    }

    /** 排行榜 */
    public static JSONObject ranking() throws IOException, JSONException {
        return json(url("/ranking", "_t=" + System.currentTimeMillis()));
    }

    // ---------- 评论 ----------

    public static JSONObject comments(String appId) throws IOException, JSONException {
        return json(url("/comments", "app_id=" + enc(appId) + "&_t=" + System.currentTimeMillis()));
    }

    public static JSONObject comment(String appId, String content, int rating)
            throws IOException, JSONException {
        String p = "token=" + enc(token) + "&app_id=" + enc(appId)
                + "&content=" + enc(content) + "&rating=" + rating;
        return json(url("/comment", p));
    }

    // ---------- 下载 ----------

    public static JSONObject download(String appId) throws IOException, JSONException {
        String p = "app_id=" + enc(appId) + "&token=" + enc(token);
        return json(url("/download", p));
    }

    // ---------- 账号 ----------

    public static JSONObject sendCode(String email) throws IOException, JSONException {
        return json(url("/send_code", "email=" + enc(email)));
    }

    public static JSONObject register(String email, String code, String password)
            throws IOException, JSONException {
        String p = "email=" + enc(email) + "&code=" + enc(code) + "&password=" + enc(password);
        return json(url("/register", p));
    }

    public static JSONObject login(String email, String password)
            throws IOException, JSONException {
        String p = "email=" + enc(email) + "&password=" + enc(password);
        return json(url("/login", p));
    }

    public static JSONObject me() throws IOException, JSONException {
        return json(url("/me", "token=" + enc(token)));
    }

    public static JSONObject updateProfile(String name, String bio)
            throws IOException, JSONException {
        String p = "token=" + enc(token) + "&name=" + enc(name) + "&bio=" + enc(bio);
        return json(url("/update_profile", p));
    }

    public static JSONObject checkin() throws IOException, JSONException {
        return json(url("/checkin", "token=" + enc(token)));
    }

    public static JSONObject messages() throws IOException, JSONException {
        return json(url("/messages", "token=" + enc(token)));
    }

    public static JSONObject submitApp(JSONObject params) throws IOException, JSONException {
        StringBuilder sb = new StringBuilder();
        sb.append("token=").append(enc(token));
        JSONArray names = params.names();
        if (names != null) {
            for (int i = 0; i < names.length(); i++) {
                String k = names.getString(i);
                String v = params.optString(k, "");
                sb.append("&").append(enc(k)).append("=").append(enc(v));
            }
        }
        return json(url("/submit_app", sb.toString()));
    }

    // ---------- 版本 ----------

    public static JSONObject version() throws IOException, JSONException {
        return json(url("/version", "_t=" + System.currentTimeMillis()));
    }

    /** 图标地址（服务端无图标时自动生成） */
    public static String iconUrl(String appId) {
        return BASE + "/icon?app_id=" + enc(appId);
    }
}
