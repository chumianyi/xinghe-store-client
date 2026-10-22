package com.xinghe.store;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TabHost;
import android.widget.TextView;
import android.widget.Toast;

import com.xinghe.store.api.Api;
import com.xinghe.store.api.App;
import com.xinghe.store.ui.AppListAdapter;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 主界面：全部 / 安卓 / iOS / PWA 四个标签页 + 分页加载。
 */
public class Xinghe extends BaseActivity {

    private static final String[] TABS = {"all", "android", "ios", "pwa"};
    private static final String[] TAB_LABELS = {"全部", "安卓", "iOS", "PWA"};

    private TabHost tabHost;
    private ListView listView;
    private TextView emptyView;
    private View footer;
    private TextView footerText;
    private AppListAdapter adapter;

    private String platform = "all";
    private String sort = "downloads";
    private int page = 1;
    private int pages = 1;
    private boolean loading = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);

        sort = Prefs.getSort(this);

        // 头部按钮
        findViewById(R.id.btn_header_search).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(Xinghe.this, SearchActivity.class));
            }
        });
        findViewById(R.id.btn_header_me).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (Prefs.isLoggedIn(Xinghe.this)) {
                    startActivity(new Intent(Xinghe.this, ProfileActivity.class));
                } else {
                    startActivity(new Intent(Xinghe.this, LoginActivity.class));
                }
            }
        });

        listView = (ListView) findViewById(R.id.list_apps);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        // 底部"加载更多"
        LayoutInflater inflater = LayoutInflater.from(this);
        footer = inflater.inflate(R.layout.list_footer, listView, false);
        footerText = (TextView) footer.findViewById(R.id.footer_text);
        footer.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                if (!loading && page < pages) {
                    loadPage(page + 1, true);
                }
            }
        });
        listView.addFooterView(footer, null, true);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                App a = (App) parent.getItemAtPosition(position);
                if (a == null) return;
                Intent i = new Intent(Xinghe.this, AppDetails.class);
                i.putExtra("app_id", a.id);
                startActivity(i);
            }
        });

        adapter = new AppListAdapter(this, XingheApp.imageLoader);
        listView.setAdapter(adapter);

        // 标签页
        tabHost = (TabHost) findViewById(R.id.tabhost);
        tabHost.setup();
        for (int i = 0; i < TABS.length; i++) {
            final String tag = TABS[i];
            TabHost.TabSpec spec = tabHost.newTabSpec(tag);
            // setIndicator(View) 是 API 4 才有，Android 1.5 上走反射，失败则退回文字标签
            try {
                TextView indicator = (TextView) inflater.inflate(R.layout.tab_indicator, null);
                indicator.setText(TAB_LABELS[i]);
                TabHost.TabSpec.class.getMethod("setIndicator", View.class)
                        .invoke(spec, indicator);
            } catch (Exception e) {
                spec.setIndicator(TAB_LABELS[i]);
            }
            spec.setContent(new TabHost.TabContentFactory() {
                public View createTabContent(String t) {
                    return listView;
                }
            });
            tabHost.addTab(spec);
        }
        int lastTab = Prefs.getLastTab(this);
        if (lastTab >= 0 && lastTab < TABS.length) {
            platform = TABS[lastTab];
        }
        tabHost.setCurrentTabByTag(platform);
        tabHost.setOnTabChangedListener(new TabHost.OnTabChangeListener() {
            public void onTabChanged(String tabId) {
                if (tabId == null) return;
                platform = tabId;
                for (int i = 0; i < TABS.length; i++) {
                    if (TABS[i].equals(tabId)) Prefs.setLastTab(Xinghe.this, i);
                }
                page = 1;
                pages = 1;
                adapter.setData(null);
                loadPage(1, false);
            }
        });

        loadPage(1, false);
    }

    private void loadPage(final int targetPage, final boolean append) {
        if (loading) return;
        loading = true;
        if (!append) {
            emptyView.setText(R.string.loading);
            adapter.setData(null);
        } else {
            footerText.setText(R.string.loading);
        }
        new AsyncTask<Void, Void, JSONObject>() {
            private String error = null;

            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.apps(platform, "", sort, targetPage, 10);
                } catch (Exception e) {
                    error = e.getMessage();
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                loading = false;
                if (result == null) {
                    if (!append) {
                        emptyView.setText(R.string.network_error);
                        footerText.setText(R.string.load_more);
                    } else {
                        footerText.setText(R.string.retry);
                        Toast.makeText(Xinghe.this, R.string.network_error, Toast.LENGTH_SHORT).show();
                    }
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
                    page = result.optInt("page", targetPage);
                    pages = result.optInt("pages", 1);
                    if (append) {
                        adapter.append(apps);
                    } else {
                        adapter.setData(apps);
                        if (apps.isEmpty()) {
                            emptyView.setText(R.string.empty);
                        }
                    }
                    if (page >= pages) {
                        footerText.setText(R.string.no_more);
                        footer.setEnabled(false);
                    } else {
                        footerText.setText(R.string.load_more);
                        footer.setEnabled(true);
                    }
                } catch (Exception e) {
                    emptyView.setText(R.string.network_error);
                }
            }
        }.execute();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.menu_refresh);
        menu.add(0, 2, 0, R.string.sort_downloads);
        menu.add(0, 3, 0, R.string.sort_rating);
        menu.add(0, 4, 0, R.string.sort_name);
        menu.add(0, 5, 0, R.string.menu_ranking);
        menu.add(0, 6, 0, R.string.menu_favorites);
        menu.add(0, 7, 0, R.string.menu_history);
        menu.add(0, 8, 0, R.string.menu_submit);
        menu.add(0, 9, 0, R.string.menu_messages);
        menu.add(0, 10, 0, R.string.menu_settings);
        menu.add(0, 11, 0, R.string.menu_about);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        Intent i;
        switch (item.getItemId()) {
            case 1:
                page = 1;
                pages = 1;
                adapter.setData(null);
                loadPage(1, false);
                return true;
            case 2:
                sort = "downloads";
                Prefs.setSort(this, sort);
                reloadCurrent();
                return true;
            case 3:
                sort = "rating";
                Prefs.setSort(this, sort);
                reloadCurrent();
                return true;
            case 4:
                sort = "name";
                Prefs.setSort(this, sort);
                reloadCurrent();
                return true;
            case 5:
                startActivity(new Intent(this, RankingActivity.class));
                return true;
            case 6:
                startActivity(new Intent(this, FavoritesActivity.class));
                return true;
            case 7:
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            case 8:
                if (!Prefs.isLoggedIn(this)) {
                    Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    return true;
                }
                startActivity(new Intent(this, SubmitActivity.class));
                return true;
            case 9:
                if (!Prefs.isLoggedIn(this)) {
                    Toast.makeText(this, R.string.login_required, Toast.LENGTH_SHORT).show();
                    startActivity(new Intent(this, LoginActivity.class));
                    return true;
                }
                startActivity(new Intent(this, MessagesActivity.class));
                return true;
            case 10:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case 11:
                showAbout();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void reloadCurrent() {
        page = 1;
        pages = 1;
        adapter.setData(null);
        loadPage(1, false);
    }

    private void showAbout() {
        String ver = "1.0.0";
        try {
            ver = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
        } catch (Exception e) {
            // ignore
        }
        new android.app.AlertDialog.Builder(this)
                .setTitle(R.string.menu_about)
                .setMessage(getString(R.string.about_text, ver))
                .setPositiveButton(R.string.ok, null)
                .show();
    }
}
