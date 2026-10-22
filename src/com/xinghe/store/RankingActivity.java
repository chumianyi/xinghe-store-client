package com.xinghe.store;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.xinghe.store.api.Api;
import com.xinghe.store.api.App;
import com.xinghe.store.ui.AppListAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 排行榜页：按真实下载量取前 50。
 */
public class RankingActivity extends BaseActivity {

    private ListView listView;
    private TextView emptyView;
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_page);
        setupBackButton();
        setHeaderTitle(R.string.menu_ranking);

        listView = (ListView) findViewById(R.id.list_view);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        adapter = new AppListAdapter(this, XingheApp.imageLoader);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App a = (App) parent.getItemAtPosition(position);
                if (a == null) return;
                Intent i = new Intent(RankingActivity.this, AppDetails.class);
                i.putExtra("app_id", a.id);
                startActivity(i);
            }
        });

        load();
    }

    private void load() {
        emptyView.setText(R.string.loading);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.ranking();
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null) {
                    emptyView.setText(R.string.network_error);
                    return;
                }
                try {
                    List<App> apps = new ArrayList<App>();
                    JSONArray arr = result.optJSONArray("ranking");
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
