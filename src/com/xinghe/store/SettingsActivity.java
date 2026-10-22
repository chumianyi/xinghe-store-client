package com.xinghe.store;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.xinghe.store.api.Api;

import org.json.JSONObject;

/**
 * 设置页：检查更新 / 清除缓存 / 关于。
 */
public class SettingsActivity extends BaseActivity {

    private TextView versionView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings);

        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        versionView = (TextView) findViewById(R.id.settings_version);
        String ver = "1.0.0";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            // ignore
        }
        versionView.setText("版本 v" + ver + "（API 3+）");

        findViewById(R.id.btn_check_update).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkUpdate();
            }
        });
        findViewById(R.id.btn_clear_cache).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                clearCache();
            }
        });
        findViewById(R.id.btn_about).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showAbout();
            }
        });
    }

    private void checkUpdate() {
        final String current = localVersion();
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.version();
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null || !result.optBoolean("ok", false)) {
                    Utils.toastShort(SettingsActivity.this, getString(R.string.network_error));
                    return;
                }
                String latest = result.optString("latest_version", "");
                int remoteCode = result.optInt("version_code", 0);
                int localCode = localVersionCode();
                if (latest.length() > 0
                        && (remoteCode > localCode
                        || Utils.versionToInt(latest) > Utils.versionToInt(current))) {
                    final String apkUrl = result.optString("apk_url", "");
                    String notes = result.optString("notes", "");
                    new AlertDialog.Builder(SettingsActivity.this)
                            .setTitle(getString(R.string.update_found, latest))
                            .setMessage(notes.length() > 0 ? notes : latest)
                            .setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
                                public void onClick(android.content.DialogInterface dialog, int which) {
                                    if (apkUrl.length() > 0) {
                                        Utils.openUrl(SettingsActivity.this, apkUrl);
                                    } else {
                                        Utils.toastShort(SettingsActivity.this,
                                                "暂未提供下载地址，请前往星河软件站获取");
                                    }
                                }
                            })
                            .setNegativeButton(R.string.cancel, null)
                            .show();
                } else {
                    Utils.toastShort(SettingsActivity.this, getString(R.string.update_latest));
                }
            }
        }.execute();
    }

    private String localVersion() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            return "1.0.0";
        }
    }

    private int localVersionCode() {
        try {
            return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
        } catch (Exception e) {
            return 1;
        }
    }

    private void clearCache() {
        if (XingheApp.imageLoader != null) {
            XingheApp.imageLoader.clearDiskCache();
        }
        Utils.toastShort(this, "缓存已清除");
    }

    private void showAbout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.menu_about)
                .setMessage(getString(R.string.about_text, localVersion()))
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
