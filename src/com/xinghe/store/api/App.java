package com.xinghe.store.api;

import org.json.JSONException;
import org.json.JSONObject;

/** 应用模型（/old/apps 返回的归一化字段） */
public class App {

    public String id;
    public String name;
    public String desc;
    public String icon;
    public String iconUrl;
    public String platform;      // ios / android / pwa
    public String platformRaw;
    public String downloadUrl;
    public String originalUrl;
    public boolean direct;       // 是否文件直链
    public String size;
    public String version;
    public String category;
    public String uploader;
    public String minVersion;
    public String arch;
    public int downloadCount;
    public double rating;
    public int coinsPrice;

    public String baiduUrl;
    public String quarkUrl;
    public String xunleiUrl;
    public String feijigeUrl;

    public static App fromJson(JSONObject o) throws JSONException {
        App a = new App();
        a.id = o.optString("id", "");
        a.name = o.optString("name", "");
        a.desc = o.optString("desc", "");
        a.icon = o.optString("icon", "");
        a.iconUrl = o.optString("icon_url", "");
        a.platform = o.optString("platform", "");
        a.platformRaw = o.optString("platform_raw", "");
        a.downloadUrl = o.optString("download_url", "");
        a.originalUrl = o.optString("original_url", "");
        a.direct = o.optBoolean("direct", false);
        a.size = o.optString("size", "");
        a.version = o.optString("version", "");
        a.category = o.optString("category", "");
        a.uploader = o.optString("uploader", "");
        a.minVersion = o.optString("min_version", "");
        a.arch = o.optString("arch", "");
        a.downloadCount = o.optInt("download_count", 0);
        a.rating = o.optDouble("rating", 0);
        a.coinsPrice = o.optInt("coins_price", 0);

        JSONObject nd = o.optJSONObject("netdisks");
        if (nd != null) {
            a.baiduUrl = nd.optString("百度网盘", "");
            a.quarkUrl = nd.optString("夸克网盘", "");
            a.xunleiUrl = nd.optString("迅雷网盘", "");
            a.feijigeUrl = nd.optString("小飞机网盘", "");
        }
        return a;
    }

    public String platformLabel() {
        if ("android".equals(platform)) return "安卓";
        if ("ios".equals(platform)) return "iOS";
        if ("pwa".equals(platform)) return "PWA";
        return platformRaw == null || platformRaw.length() == 0 ? platform : platformRaw;
    }

    /** 列表副标题：大小 · 版本 · 下载量 */
    public String metaLine() {
        StringBuilder sb = new StringBuilder();
        if (size != null && size.length() > 0) sb.append(size);
        if (version != null && version.length() > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append("v").append(version);
        }
        if (downloadCount > 0) {
            if (sb.length() > 0) sb.append(" · ");
            sb.append(downloadCount).append("次下载");
        }
        return sb.toString();
    }

    public String ratingText() {
        if (rating <= 0) return "暂无评分";
        return String.format("%.1f", rating);
    }
}
