package com.xinghe.store;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.AdapterView;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.xinghe.store.api.Api;
import com.xinghe.store.api.App;
import com.xinghe.store.ui.AppListAdapter;
import com.xinghe.store.ui.ImageLoader;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索页：按名称 / 简介 / 分类搜索。
 */
public class SearchActivity extends BaseActivity {

    private EditText input;
    private ListView listView;
    private TextView emptyView;
    private AppListAdapter adapter;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);
        setupBackButton();

        input = (EditText) findViewById(R.id.search_input);
        listView = (ListView) findViewById(R.id.list_results);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        adapter = new AppListAdapter(this, XingheApp.imageLoader);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App a = (App) parent.getItemAtPosition(position);
                if (a == null) return;
                Intent i = new Intent(SearchActivity.this, AppDetails.class);
                i.putExtra("app_id", a.id);
                startActivity(i);
            }
        });

        // 回车即搜索
        input.setOnKeyListener(new View.OnKeyListener() {
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (keyCode == KeyEvent.KEYCODE_ENTER && event.getAction() == KeyEvent.ACTION_UP) {
                    doSearch();
                    return true;
                }
                return false;
            }
        });

        findViewById(R.id.btn_do_search).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                doSearch();
            }
        });

        // 关键词联想：输入超过 2 个字符自动搜索
        input.addTextChangedListener(new android.text.TextWatcher() {
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {
            }

            public void onTextChanged(CharSequence s, int start, int before, int count) {
            }

            public void afterTextChanged(android.text.Editable s) {
                if (s.length() >= 2) {
                    doSearch();
                } else if (s.length() == 0) {
                    adapter.setData(null);
                }
            }
        });
    }

    private void doSearch() {
        final String q = input.getText().toString().trim();
        if (q.length() == 0) {
            Toast.makeText(this, R.string.empty, Toast.LENGTH_SHORT).show();
            return;
        }
        if (loading) return;
        loading = true;
        emptyView.setText(R.string.loading);
        adapter.setData(null);
        new AsyncTask<Void, Void, JSONObject>() {
            private String error = null;

            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.apps("all", q, "downloads", 1, 30);
                } catch (Exception e) {
                    error = e.getMessage();
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                loading = false;
                if (result == null) {
                    emptyView.setText(R.string.network_error);
                    return;
                }
                try {
                    List<App> apps = new ArrayList<App>();
                    JSONArray arr = result.optJSONArray("apps");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            apps.add(App.fromJson(arr.getJSONObject(i)));
                        }
                    }
                    adapter.setData(apps);
                    if (apps.isEmpty()) {
                        emptyView.setText(R.string.empty);
                    }
                } catch (Exception e) {
                    emptyView.setText(R.string.network_error);
                }
            }
        }.execute();
    }
}
