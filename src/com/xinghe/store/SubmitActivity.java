package com.xinghe.store;

import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Spinner;

import com.xinghe.store.api.Api;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 投稿页：提交新应用（扣除 1 金币押金）。
 */
public class SubmitActivity extends BaseActivity {

    private EditText nameInput;
    private EditText versionInput;
    private EditText sizeInput;
    private Spinner platformSpinner;
    private EditText descInput;
    private EditText urlInput;
    private EditText categoryInput;
    private EditText iconInput;
    private EditText minVersionInput;
    private EditText archInput;
    private EditText baiduInput;
    private EditText quarkInput;
    private EditText xunleiInput;
    private EditText feijigeInput;
    private EditText coinsInput;
    private Button submit;

    private static final String[] PLATFORMS = {"android", "ios", "pwa"};

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.submit);

        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        nameInput = (EditText) findViewById(R.id.sub_name);
        versionInput = (EditText) findViewById(R.id.sub_version);
        sizeInput = (EditText) findViewById(R.id.sub_size);
        platformSpinner = (Spinner) findViewById(R.id.sub_platform);
        descInput = (EditText) findViewById(R.id.sub_desc);
        urlInput = (EditText) findViewById(R.id.sub_download_url);
        categoryInput = (EditText) findViewById(R.id.sub_category);
        iconInput = (EditText) findViewById(R.id.sub_icon_url);
        minVersionInput = (EditText) findViewById(R.id.sub_min_version);
        archInput = (EditText) findViewById(R.id.sub_arch);
        baiduInput = (EditText) findViewById(R.id.sub_baidu);
        quarkInput = (EditText) findViewById(R.id.sub_quark);
        xunleiInput = (EditText) findViewById(R.id.sub_xunlei);
        feijigeInput = (EditText) findViewById(R.id.sub_feijige);
        coinsInput = (EditText) findViewById(R.id.sub_coins);
        submit = (Button) findViewById(R.id.btn_submit_do);

        List<String> labels = new ArrayList<String>();
        labels.add("安卓 (android)");
        labels.add("iOS (ios)");
        labels.add("PWA (pwa)");
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_spinner_item, labels);
        spinnerAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        platformSpinner.setAdapter(spinnerAdapter);

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSubmit();
            }
        });
    }

    private void doSubmit() {
        if (!Prefs.isLoggedIn(this)) {
            Utils.toastShort(this, getString(R.string.login_required));
            return;
        }
        final String name = nameInput.getText().toString().trim();
        final String version = versionInput.getText().toString().trim();
        final String size = sizeInput.getText().toString().trim();
        if (name.length() == 0 || version.length() == 0 || size.length() == 0) {
            Utils.toastShort(this, "名称、版本号、大小为必填项");
            return;
        }

        final JSONObject params = new JSONObject();
        try {
            params.put("name", name);
            params.put("version", version);
            params.put("size", size);
            params.put("platform", PLATFORMS[platformSpinner.getSelectedItemPosition()]);
            params.put("desc", descInput.getText().toString().trim());
            params.put("download_url", urlInput.getText().toString().trim());
            params.put("category", categoryInput.getText().toString().trim());
            params.put("icon_url", iconInput.getText().toString().trim());
            params.put("min_version", minVersionInput.getText().toString().trim());
            params.put("arch", archInput.getText().toString().trim());
            params.put("baidu_url", baiduInput.getText().toString().trim());
            params.put("quark_url", quarkInput.getText().toString().trim());
            params.put("xunlei_url", xunleiInput.getText().toString().trim());
            params.put("feijige_url", feijigeInput.getText().toString().trim());
            String coinsStr = coinsInput.getText().toString().trim();
            int coins = 0;
            if (coinsStr.length() > 0) {
                try {
                    coins = Integer.parseInt(coinsStr);
                } catch (NumberFormatException e) {
                    coins = 0;
                }
            }
            if (coins < 0) coins = 0;
            if (coins > 3) coins = 3;
            params.put("coins_price", String.valueOf(coins));
        } catch (Exception e) {
            // JSONObject.put(String,String) 不会抛，忽略
        }

        submit.setEnabled(false);
        submit.setText(R.string.loading);
        new AsyncTask<JSONObject, Void, JSONObject>() {
            protected JSONObject doInBackground(JSONObject... ps) {
                try {
                    return Api.submitApp(ps[0]);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                submit.setEnabled(true);
                submit.setText(R.string.submit_do);
                if (result == null) {
                    Utils.toastShort(SubmitActivity.this, getString(R.string.network_error));
                    return;
                }
                Utils.toastShort(SubmitActivity.this, result.optString("msg", ""));
                if (result.optBoolean("ok", false)) {
                    // 刷新本地金币显示
                    new AsyncTask<Void, Void, JSONObject>() {
                        protected JSONObject doInBackground(Void... p) {
                            try {
                                return Api.me();
                            } catch (Exception e) {
                                return null;
                            }
                        }

                        protected void onPostExecute(JSONObject me) {
                            if (me != null && me.optBoolean("ok", false)) {
                                Prefs.setCoins(SubmitActivity.this, me.optInt("coins", 0));
                            }
                            finish();
                        }
                    }.execute();
                }
            }
        }.execute(params);
    }
}
