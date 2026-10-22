package com.xinghe.store;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.xinghe.store.api.Api;
import com.xinghe.store.api.App;
import com.xinghe.store.data.DB;
import com.xinghe.store.ui.AppListAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 收藏夹页（本地数据库）。
 */
public class FavoritesActivity extends BaseActivity {

    private ListView listView;
    private TextView emptyView;
    private AppListAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_page);
        setupBackButton();
        setHeaderTitle(R.string.menu_favorites);

        listView = (ListView) findViewById(R.id.list_view);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        adapter = new AppListAdapter(this, XingheApp.imageLoader);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App a = (App) parent.getItemAtPosition(position);
                if (a == null) return;
                Intent i = new Intent(FavoritesActivity.this, AppDetails.class);
                i.putExtra("app_id", a.id);
                startActivity(i);
            }
        });

        load();
    }

    @Override
    protected void onResume() {
        super.onResume();
        load();
    }

    private void load() {
        DB db = XingheApp.db;
        if (db == null) {
            emptyView.setText(R.string.empty);
            return;
        }
        List<String[]> rows = db.listFavorites();
        List<App> apps = new ArrayList<App>();
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            App a = new App();
            a.id = row[0];
            a.name = row[1];
            a.platform = row[3];
            // 图标统一走服务端（本地存的原始图标可能为空）
            a.icon = Api.iconUrl(a.id);
            apps.add(a);
        }
        adapter.setData(apps);
        if (apps.isEmpty()) {
            emptyView.setText(R.string.empty);
        }
    }
}
