package com.xinghe.store;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.webkit.DownloadListener;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.xinghe.store.net.Downloader;

/**
 * 应用内置浏览器：网盘链接的解析与文件下载全部在应用内完成，
 * 不跳转到外部浏览器。检测到下载请求时先弹确认框，完成后跳转下载记录。
 */
public class WebActivity extends BaseActivity {

    private WebView webView;
    private ProgressBar progressBar;
    private String pageUrl;
    private String appId = "";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.web);

        pageUrl = getIntent().getStringExtra("url");
        String title = getIntent().getStringExtra("title");
        appId = getIntent().getStringExtra("app_id");
        if (appId == null) appId = "";

        setupBackButton();
        if (title != null && title.length() > 0) {
            setHeaderTitle(title);
        } else {
            setHeaderTitle(R.string.web_title);
        }

        progressBar = (ProgressBar) findViewById(R.id.web_progress);
        webView = (WebView) findViewById(R.id.web_view);

        WebSettings settings = webView.getSettings();
        settings.setJavaScriptEnabled(true);
        settings.setSupportZoom(true);
        settings.setBuiltInZoomControls(true);
        settings.setUseWideViewPort(true);
        settings.setAllowFileAccess(true);
        settings.setSaveFormData(true);
        // setDomStorageEnabled 是 API 7 才有，反射启用（新版网盘页面需要）
        if (Utils.hasApi(7)) {
            try {
                WebSettings.class.getMethod("setDomStorageEnabled", boolean.class)
                        .invoke(settings, Boolean.TRUE);
            } catch (Exception e) {
                // ignore
            }
        }

        webView.setWebViewClient(new WebViewClient() {
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if (url == null) return false;
                String lower = url.toLowerCase();
                if (lower.startsWith("http://") || lower.startsWith("https://")) {
                    return false; // 应用内加载
                }
                // market://、alipays:// 等 scheme 交给外部应用
                try {
                    startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse(url)));
                } catch (Exception e) {
                    Toast.makeText(WebActivity.this, R.string.need_browser,
                            Toast.LENGTH_SHORT).show();
                }
                return true;
            }

            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                progressBar.setVisibility(View.VISIBLE);
                progressBar.setProgress(5);
            }

            public void onPageFinished(WebView view, String url) {
                progressBar.setVisibility(View.GONE);
            }
        });

        webView.setWebChromeClient(new WebChromeClient() {
            public void onProgressChanged(WebView view, int newProgress) {
                if (newProgress >= 100) {
                    progressBar.setVisibility(View.GONE);
                } else {
                    progressBar.setVisibility(View.VISIBLE);
                    progressBar.setProgress(newProgress);
                }
            }
        });

        // 下载检测：网盘页点击下载时回调
        webView.setDownloadListener(new DownloadListener() {
            public void onDownloadStart(String url, String userAgent,
                                        String contentDisposition, String mimetype,
                                        long contentLength) {
                showDownloadDialog(url, contentDisposition, mimetype, contentLength);
            }
        });

        if (pageUrl != null && pageUrl.length() > 0) {
            webView.loadUrl(pageUrl);
        } else {
            finish();
        }
    }

    private void showDownloadDialog(final String url, final String contentDisposition,
                                    final String mimetype, final long contentLength) {
        String name = Utils.fileNameFromDisposition(contentDisposition, url);
        if (name == null || name.length() == 0) {
            name = "xinghe_" + System.currentTimeMillis()
                    + (mimetype != null && mimetype.contains("android.package") ? ".apk" : "");
        }
        final String fileName = name;

        View view = getLayoutInflater().inflate(R.layout.download_dialog, null);
        ((TextView) view.findViewById(R.id.dlg_file_name)).setText(fileName);
        ((TextView) view.findViewById(R.id.dlg_file_size)).setText(contentLength > 0
                ? Utils.formatSize(contentLength) : getString(R.string.unknown_size));
        ((TextView) view.findViewById(R.id.dlg_file_type)).setText(mimeLabel(mimetype, fileName));
        ((TextView) view.findViewById(R.id.dlg_file_url)).setText(url);

        new AlertDialog.Builder(this)
                .setTitle(R.string.download_confirm_title)
                .setView(view)
                .setPositiveButton(R.string.btn_download_now, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        startDownload(url, fileName);
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private String mimeLabel(String mime, String fileName) {
        String m = mime != null ? mime : Utils.mimeOfFile(fileName);
        if (m.contains("android.package")) return getString(R.string.type_apk);
        if (fileName != null && fileName.toLowerCase().endsWith(".ipa")) return getString(R.string.type_ipa);
        if (m.startsWith("image/")) return "图片";
        if (m.startsWith("video/")) return "视频";
        if (m.contains("zip") || m.contains("rar") || m.contains("7z")) return "压缩包";
        return getString(R.string.type_other);
    }

    private void startDownload(final String url, final String fileName) {
        Toast.makeText(this, getString(R.string.downloading, fileName),
                Toast.LENGTH_SHORT).show();
        Downloader.download(this, url, fileName, appId, new Downloader.Callback() {
            public void onFinished(java.io.File file, String mime, String error) {
                if (error != null) {
                    Toast.makeText(WebActivity.this,
                            getString(R.string.download_failed) + "：" + error,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                Toast.makeText(WebActivity.this, R.string.download_saved,
                        Toast.LENGTH_SHORT).show();
                // 下载完毕 → 跳转下载记录
                Intent i = new Intent(WebActivity.this, DownloadsActivity.class);
                startActivity(i);
            }
        });
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (keyCode == KeyEvent.KEYCODE_BACK && webView != null && webView.canGoBack()) {
            webView.goBack();
            return true;
        }
        return super.onKeyDown(keyCode, event);
    }

    @Override
    protected void onDestroy() {
        if (webView != null) {
            try {
                webView.stopLoading();
                webView.clearCache(true);
                webView.setDownloadListener(null);
            } catch (Exception e) {
                // ignore
            }
        }
        super.onDestroy();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.web_refresh);
        menu.add(0, 2, 0, R.string.web_copy_link);
        menu.add(0, 3, 0, R.string.web_open_external);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case 1:
                if (webView != null) webView.reload();
                return true;
            case 2:
                if (webView != null && webView.getUrl() != null) {
                    Utils.copyToClipboard(this, webView.getUrl());
                    Utils.toastShort(this, R.string.copied);
                }
                return true;
            case 3:
                if (webView != null && webView.getUrl() != null) {
                    Utils.openUrl(this, webView.getUrl());
                }
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
