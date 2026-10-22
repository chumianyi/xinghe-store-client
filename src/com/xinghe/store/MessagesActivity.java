package com.xinghe.store;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.ListView;
import android.widget.TextView;

import com.xinghe.store.api.Api;
import com.xinghe.store.api.App;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * 消息通知页：展示自己上传应用收到的评论。
 */
public class MessagesActivity extends BaseActivity {

    private ListView listView;
    private TextView emptyView;
    private MessageAdapter adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.messages);
        setupBackButton();

        listView = (ListView) findViewById(R.id.list_messages);
        emptyView = (TextView) findViewById(R.id.empty_view);
        listView.setEmptyView(emptyView);

        adapter = new MessageAdapter(this);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                JSONObject m = (JSONObject) parent.getItemAtPosition(position);
                if (m == null) return;
                String appId = m.optString("app_id", "");
                if (appId.length() > 0) {
                    Intent i = new Intent(MessagesActivity.this, AppDetails.class);
                    i.putExtra("app_id", appId);
                    startActivity(i);
                }
            }
        });

        load();
    }

    private void load() {
        if (!Prefs.isLoggedIn(this)) {
            emptyView.setText(R.string.login_required);
            return;
        }
        emptyView.setText(R.string.loading);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.messages();
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
                    List<JSONObject> list = new ArrayList<JSONObject>();
                    JSONArray arr = result.optJSONArray("messages");
                    if (arr != null) {
                        for (int i = 0; i < arr.length(); i++) {
                            list.add(arr.getJSONObject(i));
                        }
                    }
                    adapter.setData(list);
                    if (list.isEmpty()) {
                        emptyView.setText(R.string.empty);
                    }
                } catch (Exception e) {
                    emptyView.setText(R.string.network_error);
                }
            }
        }.execute();
    }

    /** 消息列表适配器 */
    static class MessageAdapter extends BaseAdapter {

        private final LayoutInflater inflater;
        private final List<JSONObject> data = new ArrayList<JSONObject>();

        MessageAdapter(android.content.Context c) {
            this.inflater = LayoutInflater.from(c);
        }

        public void setData(List<JSONObject> list) {
            data.clear();
            if (list != null) data.addAll(list);
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
                convertView = inflater.inflate(R.layout.message_item, parent, false);
            }
            JSONObject m = data.get(position);
            TextView title = (TextView) convertView.findViewById(R.id.msg_title);
            TextView content = (TextView) convertView.findViewById(R.id.msg_content);
            TextView time = (TextView) convertView.findViewById(R.id.msg_time);

            String appName = m.optString("app_name", "");
            String from = m.optString("from_user", "");
            title.setText(appName + " · " + from);
            content.setText(m.optString("content", ""));
            time.setText(Utils.formatTime(m.optString("time", "")));
            return convertView;
        }
    }
}
