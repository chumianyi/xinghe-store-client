package com.xinghe.store;

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
 * 登录页。
 */
public class LoginActivity extends BaseActivity {

    private EditText emailInput;
    private EditText passwordInput;
    private Button submit;
    private TextView toRegister;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.login);

        emailInput = (EditText) findViewById(R.id.login_email);
        passwordInput = (EditText) findViewById(R.id.login_password);
        submit = (Button) findViewById(R.id.btn_login);
        toRegister = (TextView) findViewById(R.id.btn_to_register);

        String savedEmail = Prefs.getEmail(this);
        if (savedEmail.length() > 0) {
            emailInput.setText(savedEmail);
        }

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                login();
            }
        });

        toRegister.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                startActivity(new Intent(LoginActivity.this, RegisterActivity.class));
            }
        });
    }

    private void login() {
        final String email = emailInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();
        if (email.length() == 0 || password.length() == 0) {
            Utils.toastShort(this, "请输入邮箱和密码");
            return;
        }
        submit.setEnabled(false);
        submit.setText(R.string.loading);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.login(email, password);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                submit.setEnabled(true);
                submit.setText(R.string.login_submit);
                if (result == null) {
                    Utils.toastShort(LoginActivity.this, getString(R.string.network_error));
                    return;
                }
                if (!result.optBoolean("ok", false)) {
                    Utils.toastShort(LoginActivity.this,
                            result.optString("msg", "登录失败"));
                    return;
                }
                String token = result.optString("token", "");
                if (token.length() == 0) {
                    Utils.toastShort(LoginActivity.this, "登录返回异常");
                    return;
                }
                Api.token = token;
                Prefs.setToken(LoginActivity.this, token);
                Prefs.setEmail(LoginActivity.this, result.optString("email", email));
                Prefs.setName(LoginActivity.this, result.optString("name", ""));
                Prefs.setCoins(LoginActivity.this, result.optInt("coins", 0));
                Utils.toastShort(LoginActivity.this, "登录成功");
                setResult(RESULT_OK);
                finish();
            }
        }.execute();
    }
}
