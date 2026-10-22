package com.xinghe.store;

import android.app.AlertDialog;
import android.content.DialogInterface;
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

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/**
 * 下载记录页：下载完成后自动跳转到这里。
 * 点击列表项：APK 直接安装；IPA 调起分享；其他文件用查看器打开。
 */
public class DownloadsActivity extends BaseActivity {

    private ListView listView;
    private TextView emptyView;
    private DownloadAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.downloads);
        setupBackButton();
        setHeaderTitle(R.string.downloads_title);

        listView = (ListView) findViewById(R.id.list_downloads);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        adapter = new DownloadAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                openDownload(position);
            }
        });
        listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
                confirmDelete(position);
                return true;
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
        if (XingheApp.db == null) {
            emptyView.setText(R.string.empty);
            return;
        }
        List<String[]> rows = XingheApp.db.listDownloads();
        // 过滤掉文件已不存在的记录
        List<String[]> alive = new ArrayList<String[]>();
        for (int i = 0; i < rows.size(); i++) {
            String[] row = rows.get(i);
            File f = new File(row[3]);
            if (f.exists()) {
                alive.add(row);
            } else {
                XingheApp.db.deleteDownload(row[0]);
            }
        }
        adapter.setData(alive);
        if (alive.isEmpty()) {
            emptyView.setText(R.string.downloads_empty);
        }
    }

    private void openDownload(int position) {
        String[] row = (String[]) adapter.getItem(position);
        if (row == null) return;
        File f = new File(row[3]);
        if (!f.exists()) {
            Utils.toastShort(this, R.string.download_file_missing);
            load();
            return;
        }
        String name = row[2] != null ? row[2] : f.getName();
        String lower = name.toLowerCase();
        if (lower.endsWith(".apk")) {
            // APK：直接安装
            Utils.installApk(this, f);
        } else if (lower.endsWith(".ipa")) {
            // IPA：分享
            Utils.shareFile(this, f);
        } else {
            Utils.openFile(this, f);
        }
    }

    private void confirmDelete(final int position) {
        final String[] row = (String[]) adapter.getItem(position);
        if (row == null) return;
        new AlertDialog.Builder(this)
                .setTitle(R.string.delete)
                .setMessage("确定删除这条下载记录吗？（不会删除文件本身）")
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface d, int which) {
                        if (XingheApp.db != null) {
                            XingheApp.db.deleteDownload(row[0]);
                        }
                        load();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        menu.add(0, 1, 0, R.string.downloads_clear);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == 1) {
            if (XingheApp.db != null) {
                XingheApp.db.clearDownloads();
                load();
                Utils.toastShort(this, "已清空下载记录");
            }
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    /** 下载记录适配器 */
    static class DownloadAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private final List<String[]> data = new ArrayList<String[]>();

        DownloadAdapter(android.content.Context c) {
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
                convertView = inflater.inflate(R.layout.download_item, parent, false);
            }
            String[] row = data.get(position);
            TextView name = (TextView) convertView.findViewById(R.id.dl_name);
            TextView meta = (TextView) convertView.findViewById(R.id.dl_meta);
            TextView action = (TextView) convertView.findViewById(R.id.dl_action);

            name.setText(row[2] == null ? "" : row[2]);
            long size = 0;
            long ts = 0;
            try {
                size = Long.parseLong(row[5]);
                ts = Long.parseLong(row[6]);
            } catch (NumberFormatException e) {
                // ignore
            }
            java.text.SimpleDateFormat fmt = new java.text.SimpleDateFormat("MM-dd HH:mm");
            meta.setText((size > 0 ? Utils.formatSize(size) : "")
                    + (ts > 0 ? " · " + fmt.format(new java.util.Date(ts)) : ""));

            String lower = row[2] != null ? row[2].toLowerCase() : "";
            if (lower.endsWith(".apk")) {
                action.setText(R.string.install);
                action.setTextColor(0xFF34A853);
            } else if (lower.endsWith(".ipa")) {
                action.setText(R.string.share);
                action.setTextColor(0xFF4A6CF7);
            } else {
                action.setText(R.string.open);
                action.setTextColor(0xFF8A8CA8);
            }
            return convertView;
        }
    }
}
