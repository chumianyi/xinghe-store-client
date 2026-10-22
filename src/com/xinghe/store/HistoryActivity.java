package com.xinghe.store;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.xinghe.store.data.DB;

import java.util.ArrayList;
import java.util.List;

/**
 * 下载记录页（本地数据库）。
 */
public class HistoryActivity extends BaseActivity {

    private ListView listView;
    private TextView emptyView;
    private HistoryAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.list_page);
        setupBackButton();
        setHeaderTitle(R.string.menu_history);

        listView = (ListView) findViewById(R.id.list_view);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        adapter = new HistoryAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String[] row = (String[]) parent.getItemAtPosition(position);
                if (row == null) return;
                Intent i = new Intent(HistoryActivity.this, AppDetails.class);
                i.putExtra("app_id", row[0]);
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
        List<String[]> rows = db.listHistory();
        adapter.setData(rows);
        if (rows.isEmpty()) {
            emptyView.setText(R.string.empty);
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, "清空记录");
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            DB db = XingheApp.db;
            if (db != null) {
                db.clearHistory();
                load();
                Utils.toastShort(this, "已清空");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 记录列表适配器 */
    static class HistoryAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private final List<String[]> data = new ArrayList<String[]>();

        HistoryAdapter(android.content.Context c) {
            this.inflater = LayoutInflater.from(c);
        }

        public void setData(List<String[]> rows) {
            data.clear();
            if (rows != null) data.addAll(rows);
            notifyDataSetChanged();
        }

        public int getCount() {
            return data.size();
        }

        public Object getItem(int position) {
            return data.get(position);
        }

        public long getItemId(int position) {
            return position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            if (convertView == null) {
                convertView = inflater.inflate(R.layout.history_item, parent, false);
            }
            String[] row = data.get(position);
            TextView name = (TextView) convertView.findViewById(R.id.his_name);
            TextView meta = (TextView) convertView.findViewById(R.id.his_meta);
            TextView action = (TextView) convertView.findViewById(R.id.his_action);

            name.setText(row[1] == null ? "" : row[1]);
            long ts = 0;
            try {
                ts = Long.parseLong(row[3]);
            } catch (NumberFormatException e) {
                ts = 0;
            }
            java.text.SimpleDateFormat fmt =
                    new java.text.SimpleDateFormat("MM-dd HH:mm");
            meta.setText(ts > 0 ? fmt.format(new java.util.Date(ts)) : "");
            action.setText(row[2] == null ? "" : row[2]);
            return convertView;
        }
    }
}
