package com.xinghe.store;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.RatingBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xinghe.store.api.Api;
import com.xinghe.store.api.App;
import com.xinghe.store.net.Downloader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.File;

/**
 * 应用详情：信息展示、下载解析、网盘链接、收藏、评论。
 */
public class AppDetails extends BaseActivity {

    private String appId;
    private App app;

    private ImageView icon;
    private TextView name;
    private TextView uploader;
    private TextView tags;
    private TextView desc;
    private LinearLayout netdiskButtons;
    private LinearLayout commentList;
    private TextView commentTitle;
    private View commentBox;
    private RatingBar ratingBar;
    private EditText commentContent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.appdetails);

        appId = getIntent().getStringExtra("app_id");
        if (appId == null) {
            finish();
            return;
        }

        setupBackButton();
        setHeaderTitle(R.string.app_name);

        icon = (ImageView) findViewById(R.id.detail_icon);
        name = (TextView) findViewById(R.id.detail_name);
        uploader = (TextView) findViewById(R.id.detail_uploader);
        tags = (TextView) findViewById(R.id.detail_tags);
        desc = (TextView) findViewById(R.id.detail_desc);
        netdiskButtons = (LinearLayout) findViewById(R.id.netdisk_buttons);
        commentList = (LinearLayout) findViewById(R.id.comment_list);
        commentTitle = (TextView) findViewById(R.id.comment_title);
        commentBox = findViewById(R.id.comment_box);
        ratingBar = (RatingBar) findViewById(R.id.comment_rating);
        commentContent = (EditText) findViewById(R.id.comment_content);

        // 收藏按钮
        final TextView favBtn = (TextView) findViewById(R.id.btn_favorite);
        favBtn.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                toggleFavorite();
            }
        });

        // 下载按钮
        findViewById(R.id.btn_download).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doDownload();
            }
        });

        // 写评论
        findViewById(R.id.btn_write_comment).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!Prefs.isLoggedIn(AppDetails.this)) {
                    Toast.makeText(AppDetails.this, R.string.login_required, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(AppDetails.this, LoginActivity.class));
                    return;
                }
                commentBox.setVisibility(commentBox.getVisibility() == View.VISIBLE ? View.GONE : View.VISIBLE);
            }
        });

        findViewById(R.id.btn_submit_comment).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                submitComment();
            }
        });

        loadDetail();
        loadComments();
    }

    private void loadDetail() {
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... p) {
                try {
                    return Api.app(appId);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null || !result.optBoolean("ok")) {
                    Utils.toastShort(AppDetails.this, R.string.network_error);
                    return;
                }
                try {
                    app = App.fromJson(result.getJSONObject("app"));
                } catch (Exception e) {
                    return;
                }
                renderDetail();
                updateFavoriteButton();
                XingheApp.db.addHistory(appId, app.name, "浏览");
            }
        }.execute();
    }

    private void renderDetail() {
        setHeaderTitle(app.name);
        name.setText(app.name);
        uploader.setText("上传者：" + (app.uploader == null || app.uploader.length() == 0
                ? getString(R.string.label_unknown) : app.uploader));
        tags.setText(app.platformLabel() + (app.version != null && app.version.length() > 0
                ? " · v" + app.version : ""));
        desc.setText(app.desc == null || app.desc.length() == 0 ? "（暂无简介）" : app.desc);

        meta(R.id.meta_size, app.size);
        meta(R.id.meta_version, app.version);
        meta(R.id.meta_platform, app.platformLabel());
        meta(R.id.meta_min_version, app.minVersion);
        meta(R.id.meta_arch, app.arch);
        meta(R.id.meta_category, app.category);
        meta(R.id.meta_download_count, app.downloadCount > 0 ? app.downloadCount + " 次" : "0");
        meta(R.id.meta_rating, app.ratingText());
        meta(R.id.meta_coins, app.coinsPrice > 0 ? app.coinsPrice + " 金币" : getString(R.string.label_free));

        XingheApp.imageLoader.display(app.icon, icon, R.drawable.ic_app_default);

        // 网盘链接按钮
        netdiskButtons.removeAllViews();
        addNetdiskButton(app.originalUrl, "原始链接");
        addNetdiskButton(app.baiduUrl, "百度网盘");
        addNetdiskButton(app.quarkUrl, "夸克网盘");
        addNetdiskButton(app.xunleiUrl, "迅雷网盘");
        addNetdiskButton(app.feijigeUrl, "小飞机网盘");
    }

    private void meta(int resId, String value) {
        TextView t = (TextView) findViewById(resId);
        if (value == null || value.length() == 0) {
            t.setText(R.string.label_unknown);
        } else {
            t.setText(value);
        }
    }

    private void addNetdiskButton(final String url, final String label) {
        if (url == null || url.length() == 0) return;
        Button b = new Button(this);
        b.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.FILL_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        b.setText(label);
        b.setTextSize(14);
        b.setBackgroundResource(R.drawable.btn_secondary);
        b.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                // 网盘链接统一进应用内置浏览器解析，不跳出应用
                openInAppWeb(url, label);
            }
        });
        netdiskButtons.addView(b);
    }

    /** 在内置浏览器中打开网盘/解析链接 */
    private void openInAppWeb(String url, String title) {
        Intent i = new Intent(AppDetails.this, WebActivity.class);
        i.putExtra("url", url);
        i.putExtra("title", title);
        i.putExtra("app_id", appId);
        startActivity(i);
    }

    // ---------- 下载 ----------

    private void doDownload() {
        if (app == null) return;
        XingheApp.db.addHistory(appId, app.name, "下载");
        final ProgressDialog dlg = ProgressDialog.show(this, null,
                "正在解析下载地址…", true, true);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... p) {
                try {
                    return Api.download(appId);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                dlg.dismiss();
                if (result == null) {
                    Utils.toastShort(AppDetails.this, R.string.network_error);
                    return;
                }
                if (!result.optBoolean("ok")) {
                    String msg = result.optString("msg", "解析失败");
                    Utils.toastShort(AppDetails.this, msg);
                    if (result.optBoolean("need_login")) {
                        startActivity(new Intent(AppDetails.this, LoginActivity.class));
                    }
                    return;
                }
                String url = result.optString("download_url", "");
                boolean direct = result.optBoolean("direct", false);
                if (url.length() == 0) {
                    Utils.toastShort(AppDetails.this, "未获取到下载地址");
                    return;
                }
                if (direct) {
                    // 文件直链：应用内下载器直接下载
                    String fileName = urlFileName(url, app.name);
                    Utils.toastShort(AppDetails.this,
                            getString(R.string.download_started, fileName));
                    Downloader.download(AppDetails.this, url, fileName, appId,
                            new Downloader.Callback() {
                                public void onFinished(File file, String mime, String error) {
                                    if (error != null) {
                                        Utils.toastShort(AppDetails.this,
                                                getString(R.string.download_failed) + "：" + error);
                                        return;
                                    }
                                    Utils.toast(AppDetails.this,
                                            getString(R.string.download_done, file.getName()));
                                    notifyDone(file.getName());
                                    // 下载完毕 → 跳转下载记录（APK 可安装 / IPA 可分享）
                                    startActivity(new Intent(AppDetails.this,
                                            DownloadsActivity.class));
                                }
                            });
                } else {
                    // 网盘/解析页：进内置浏览器，检测到下载再弹窗
                    openInAppWeb(url, getString(R.string.web_title));
                }
            }
        }.execute();
    }

    /** 直链文件名：优先取应用名 + 链接扩展名 */
    private static String urlFileName(String url, String appName) {
        String ext = extOf(url);
        String base = Utils.safeFileName(appName != null && appName.length() > 0
                ? appName : "download");
        if (base.contains(".")) return base;
        return ext != null ? base + ext : base;
    }

    private static String extOf(String url) {
        if (url == null) return null;
        String path = Uri.parse(url).getLastPathSegment();
        if (path == null) return null;
        int dot = path.lastIndexOf('.');
        if (dot < 0 || dot == path.length() - 1) return null;
        String ext = path.substring(dot).toLowerCase();
        if (ext.length() > 6) return null;
        return ext;
    }

    private void notifyDone(String fileName) {
        try {
            Notification n = new Notification(R.drawable.ic_launcher,
                    getString(R.string.download_done, fileName), System.currentTimeMillis());
            File f = new File(Environment.getExternalStorageDirectory(), "xinghe/" + fileName);
            Intent i = new Intent(Intent.ACTION_VIEW);
            i.setDataAndType(Utils.fileUri(this, f), Utils.mimeOfFile(fileName));
            if (Utils.hasApi(24)) {
                i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            }
            n.setLatestEventInfo(this, getString(R.string.app_name),
                    getString(R.string.download_done, fileName),
                    android.app.PendingIntent.getActivity(this, 0, i, 0));
            n.flags |= Notification.FLAG_AUTO_CANCEL;
            NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
            nm.notify(1, n);
        } catch (Exception e) {
            // 通知失败不影响主流程
        }
    }

    // ---------- 收藏 ----------

    private void toggleFavorite() {
        if (app == null) return;
        if (XingheApp.db.isFavorite(appId)) {
            XingheApp.db.removeFavorite(appId);
            Utils.toastShort(this, "已取消收藏");
        } else {
            XingheApp.db.addFavorite(appId, app.name, app.icon, app.platform);
            Utils.toastShort(this, R.string.detail_favorited);
        }
        updateFavoriteButton();
    }

    private void updateFavoriteButton() {
        TextView favBtn = (TextView) findViewById(R.id.btn_favorite);
        if (XingheApp.db.isFavorite(appId)) {
            favBtn.setText(R.string.detail_favorite_remove);
        } else {
            favBtn.setText(R.string.detail_favorite_add);
        }
    }

    // ---------- 评论 ----------

    private void loadComments() {
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... p) {
                try {
                    return Api.comments(appId);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null) return;
                commentList.removeAllViews();
                double avg = result.optDouble("avg_rating", 0);
                int count = result.optInt("count", 0);
                commentTitle.setText(getString(R.string.detail_comments)
                        + "（" + count + "条 · 平均 " + (avg > 0 ? String.format("%.1f", avg) : "暂无") + "分）");
                JSONArray arr = result.optJSONArray("comments");
                if (arr == null || arr.length() == 0) {
                    TextView t = new TextView(AppDetails.this);
                    t.setText("还没有评论，来抢沙发吧~");
                    t.setTextColor(getResources().getColor(R.color.text_hint));
                    t.setTextSize(13);
                    t.setPadding(0, 8, 0, 8);
                    commentList.addView(t);
                    return;
                }
                LayoutInflater inflater = LayoutInflater.from(AppDetails.this);
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject c = arr.optJSONObject(i);
                    if (c == null) continue;
                    View item = inflater.inflate(R.layout.comment_item, commentList, false);
                    ((TextView) item.findViewById(R.id.cmt_user))
                            .setText(c.optString("username", "匿名"));
                    ((TextView) item.findViewById(R.id.cmt_content))
                            .setText(c.optString("content", ""));
                    ((TextView) item.findViewById(R.id.cmt_time))
                            .setText(Utils.formatTime(c.optString("created_at", "")));
                    TextView rating = (TextView) item.findViewById(R.id.cmt_rating);
                    int r = c.optInt("rating", 0);
                    rating.setText(stars(r));
                    commentList.addView(item);
                }
            }
        }.execute();
    }

    private static String stars(int r) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < 5; i++) {
            sb.append(i < r ? "★" : "☆");
        }
        return sb.toString();
    }

    private void submitComment() {
        String content = commentContent.getText().toString().trim();
        int rating = (int) ratingBar.getRating();
        if (rating < 1) rating = 5;
        if (content.length() == 0) {
            Utils.toastShort(this, "请输入评论内容");
            return;
        }
        if (content.length() > 500) content = content.substring(0, 500);
        final String fContent = content;
        final int fRating = rating;
        final ProgressDialog dlg = ProgressDialog.show(this, null, "提交中…", true, true);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... p) {
                try {
                    return Api.comment(appId, fContent, fRating);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                dlg.dismiss();
                if (result == null) {
                    Utils.toastShort(AppDetails.this, R.string.network_error);
                    return;
                }
                Utils.toastShort(AppDetails.this, result.optString("msg", ""));
                if (result.optBoolean("ok")) {
                    commentContent.setText("");
                    commentBox.setVisibility(View.GONE);
                    loadComments();
                }
            }
        }.execute();
    }
}
