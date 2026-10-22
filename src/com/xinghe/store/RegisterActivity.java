package com.xinghe.store;

import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import com.xinghe.store.api.Api;

import org.json.JSONObject;

/**
 * 注册页：QQ 邮箱 + 验证码 + 密码，注册成功后自动登录。
 */
public class RegisterActivity extends BaseActivity {

    private EditText emailInput;
    private EditText codeInput;
    private EditText passwordInput;
    private Button sendCodeButton;
    private Button submit;
    private CountDownTimer timer;
    private boolean sending = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.register);

        emailInput = (EditText) findViewById(R.id.reg_email);
        codeInput = (EditText) findViewById(R.id.reg_code);
        passwordInput = (EditText) findViewById(R.id.reg_password);
        sendCodeButton = (Button) findViewById(R.id.btn_send_code);
        submit = (Button) findViewById(R.id.btn_register);

        sendCodeButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                sendCode();
            }
        });

        submit.setOnClickListener(new View.OnClickListener() {
            public void onClick(View v) {
                register();
            }
        });
    }

    @Override
    protected void onDestroy() {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        super.onDestroy();
    }

    private void sendCode() {
        final String email = emailInput.getText().toString().trim();
        if (email.length() == 0) {
            Utils.toastShort(this, "请输入QQ邮箱");
            return;
        }
        if (!email.endsWith("@qq.com")) {
            Utils.toastShort(this, "仅支持 @qq.com 邮箱");
            return;
        }
        if (sending) return;
        sending = true;
        sendCodeButton.setEnabled(false);
        sendCodeButton.setText(R.string.loading);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.sendCode(email);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                sending = false;
                if (result == null) {
                    sendCodeButton.setEnabled(true);
                    sendCodeButton.setText(R.string.register_send_code);
                    Utils.toastShort(RegisterActivity.this, getString(R.string.network_error));
                    return;
                }
                Utils.toastShort(RegisterActivity.this,
                        result.optString("msg", ""));
                if (result.optBoolean("ok", false)) {
                    startCountdown();
                } else {
                    sendCodeButton.setEnabled(true);
                    sendCodeButton.setText(R.string.register_send_code);
                }
            }
        }.execute();
    }

    private void startCountdown() {
        timer = new CountDownTimer(60000, 1000) {
            public void onTick(long millisUntilFinished) {
                sendCodeButton.setText(getString(R.string.register_resend,
                        (int) (millisUntilFinished / 1000)));
            }

            public void onFinish() {
                sendCodeButton.setEnabled(true);
                sendCodeButton.setText(R.string.register_send_code);
            }
        };
        timer.start();
    }

    private void register() {
        final String email = emailInput.getText().toString().trim();
        final String code = codeInput.getText().toString().trim();
        final String password = passwordInput.getText().toString();
        if (email.length() == 0 || code.length() == 0 || password.length() == 0) {
            Utils.toastShort(this, "请填写完整");
            return;
        }
        if (!email.endsWith("@qq.com")) {
            Utils.toastShort(this, "仅支持 @qq.com 邮箱");
            return;
        }
        if (password.length() < 6) {
            Utils.toastShort(this, "密码至少6位");
            return;
        }
        submit.setEnabled(false);
        submit.setText(R.string.loading);
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.register(email, code, password);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                submit.setEnabled(true);
                submit.setText(R.string.register_submit);
                if (result == null) {
                    Utils.toastShort(RegisterActivity.this, getString(R.string.network_error));
                    return;
                }
                if (!result.optBoolean("ok", false)) {
                    Utils.toastShort(RegisterActivity.this,
                            result.optString("msg", "注册失败"));
                    return;
                }
                Utils.toastShort(RegisterActivity.this, "注册成功，正在自动登录…");
                autoLogin(email, password);
            }
        }.execute();
    }

    /** 注册接口不返回 token，成功后自动登录一次拿令牌 */
    private void autoLogin(final String email, final String password) {
        new AsyncTask<Void, Void, JSONObject>() {
            protected JSONObject doInBackground(Void... params) {
                try {
                    return Api.login(email, password);
                } catch (Exception e) {
                    return null;
                }
            }

            protected void onPostExecute(JSONObject result) {
                if (result != null && result.optBoolean("ok", false)) {
                    String token = result.optString("token", "");
                    if (token.length() > 0) {
                        Api.token = token;
                        Prefs.setToken(RegisterActivity.this, token);
                        Prefs.setEmail(RegisterActivity.this, result.optString("email", email));
                        Prefs.setName(RegisterActivity.this, result.optString("name", ""));
                        Prefs.setCoins(RegisterActivity.this, result.optInt("coins", 0));
                    }
                }
                setResult(RESULT_OK);
                finish();
            }
        }.execute();
    }
}
