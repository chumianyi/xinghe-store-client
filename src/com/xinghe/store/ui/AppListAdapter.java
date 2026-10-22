package com.xinghe.store.ui;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.TextView;

import com.xinghe.store.R;
import com.xinghe.store.api.App;

import java.util.ArrayList;
import java.util.List;

/** 应用列表适配器 */
public class AppListAdapter extends BaseAdapter {

    private final LayoutInflater inflater;
    private final ImageLoader loader;
    private final List<App> data = new ArrayList<App>();

    public AppListAdapter(Context c, ImageLoader loader) {
        this.inflater = LayoutInflater.from(c);
        this.loader = loader;
    }

    public void setData(List<App> apps) {
        data.clear();
        if (apps != null) data.addAll(apps);
        notifyDataSetChanged();
    }

    public void append(List<App> apps) {
        if (apps != null) {
            data.addAll(apps);
            notifyDataSetChanged();
        }
    }

    public List<App> getData() {
        return data;
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

    static class Holder {
        ImageView icon;
        TextView name;
        TextView platform;
        TextView desc;
        TextView meta;
        TextView coins;
    }

    public View getView(int position, View convertView, ViewGroup parent) {
        Holder h;
        if (convertView == null) {
            convertView = inflater.inflate(R.layout.applistitem, parent, false);
            h = new Holder();
            h.icon = (ImageView) convertView.findViewById(R.id.item_icon);
            h.name = (TextView) convertView.findViewById(R.id.item_name);
            h.platform = (TextView) convertView.findViewById(R.id.item_platform);
            h.desc = (TextView) convertView.findViewById(R.id.item_desc);
            h.meta = (TextView) convertView.findViewById(R.id.item_meta);
            h.coins = (TextView) convertView.findViewById(R.id.item_coins);
            convertView.setTag(h);
        } else {
            h = (Holder) convertView.getTag();
        }

        App a = data.get(position);
        h.name.setText(a.name == null ? "" : a.name);
        h.platform.setText(a.platformLabel());
        h.desc.setText(a.desc == null || a.desc.length() == 0 ? "（暂无简介）" : a.desc);
        h.meta.setText(a.metaLine());
        if (a.coinsPrice > 0) {
            h.coins.setText(a.coinsPrice + "金币");
            h.coins.setVisibility(View.VISIBLE);
        } else {
            h.coins.setVisibility(View.GONE);
        }
        if (loader != null) {
            loader.display(a.icon, h.icon, R.drawable.ic_app_default);
        } else {
            h.icon.setImageResource(R.drawable.ic_app_default);
        }
        return convertView;
    }
}
