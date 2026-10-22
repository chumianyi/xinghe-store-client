package com.xinghe.store;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.xinghe.store.api.Api;

import org.json.JSONObject;

/**
 * 个人中心：用户信息 / 签到 / 编辑资料 / 消息 / 投稿 / 退出。
 */
public class ProfileActivity extends BaseActivity {

    private TextView nameView;
    private TextView emailView;
    private TextView bioView;
    private TextView coinsView;
    private Button checkinButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.profile);

        nameView = (TextView) findViewById(R.id.profile_name);
        emailView = (TextView) findViewById(R.id.profile_email);
        bioView = (TextView) findViewById(R.id.profile_bio);
        coinsView = (TextView) findViewById(R.id.profile_coins);
        checkinButton = (Button) findViewById(R.id.btn_checkin);

        // 头部返回
        View back = findViewById(R.id.btn_back);
        if (back != null) {
            back.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }

        findViewById(R.id.btn_checkin).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                checkin();
            }
        });
        findViewById(R.id.btn_edit_profile).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                showEditDialog();
            }
        });
        findViewById(R.id.btn_messages).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, MessagesActivity.class));
            }
        });
        findViewById(R.id.btn_submit).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(ProfileActivity.this, SubmitActivity.class));
            }
        });
        findViewById(R.id.btn_logout).setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                logout();
            }
        });

        renderLocal();
        refresh();
    }

    @Override
    protected void onResume() {
        super.onResume();
        refresh();
    }

    private void renderLocal() {
        nameView.setText(Prefs.getName(this).length() > 0
                ? Prefs.getName(this) : getString(R.string.not_logged_in));
        emailView.setText(Prefs.getEmail(this));
        String bio = Prefs.getBio(this);
        bioView.setText(bio.length() > 0 ? bio : "这个人很懒，什么都没有写");
        coinsView.setText("金币：" + Prefs.getCoins(this));
    }

    private void refresh() {
        if (!Prefs.isLoggedIn(this)) {
            finish();
            return;
        }
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.me();
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null || !result.optBoolean("ok", false)) {
                    return;
                }
                String name = result.optString("name", "");
                String bio = result.optString("bio", "");
                int coins = result.optInt("coins", 0);
                Prefs.setName(ProfileActivity.this, name);
                Prefs.setBio(ProfileActivity.this, bio);
                Prefs.setCoins(ProfileActivity.this, coins);
                renderLocal();
            }
        }.execute();
    }

    private void checkin() {
        checkinButton.setEnabled(false);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.checkin();
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                checkinButton.setEnabled(true);
                if (result == null) {
                    Utils.toastShort(ProfileActivity.this, getString(R.string.network_error));
                    return;
                }
                int coins = result.optInt("coins", -1);
                if (coins >= 0) {
                    Prefs.setCoins(ProfileActivity.this, coins);
                    renderLocal();
                }
                Utils.toastShort(ProfileActivity.this, result.optString("msg", ""));
            }
        }.execute();
    }

    private void showEditDialog() {
        android.view.LayoutInflater inflater = android.view.LayoutInflater.from(this);
        View view = inflater.inflate(R.layout.dialog_edit_profile, null);
        final EditText nameInput = (EditText) view.findViewById(R.id.dlg_name);
        final EditText bioInput = (EditText) view.findViewById(R.id.dlg_bio);
        nameInput.setText(Prefs.getName(this));
        bioInput.setText(Prefs.getBio(this));
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_edit)
                .setView(view)
                .setPositiveButton(R.string.profile_save, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        saveProfile(nameInput.getText().toString().trim(),
                                bioInput.getText().toString().trim());
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }

    private void saveProfile(final String name, final String bio) {
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.updateProfile(name, bio);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result == null) {
                    Utils.toastShort(ProfileActivity.this, getString(R.string.network_error));
                    return;
                }
                if (result.optBoolean("ok", false)) {
                    Prefs.setName(ProfileActivity.this, result.optString("name", name));
                    Prefs.setBio(ProfileActivity.this, result.optString("bio", bio));
                    renderLocal();
                }
                Utils.toastShort(ProfileActivity.this, result.optString("msg", ""));
            }
        }.execute();
    }

    private void logout() {
        new AlertDialog.Builder(this)
                .setTitle(R.string.profile_logout)
                .setMessage("确定要退出登录吗？")
                .setPositiveButton(R.string.ok, new android.content.DialogInterface.OnClickListener() {
                    public void onClick(android.content.DialogInterface dialog, int which) {
                        Prefs.clearUser(ProfileActivity.this);
                        Api.token = "";
                        Utils.toastShort(ProfileActivity.this, "已退出登录");
                        finish();
                    }
                })
                .setNegativeButton(R.string.cancel, null)
                .show();
    }
}
