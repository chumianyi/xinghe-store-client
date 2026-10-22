package com.xinghe.store;

import android.app.Activity;
import android.view.View;
import android.widget.TextView;

/** Activity 基类：标题栏公共逻辑 */
public class BaseActivity extends Activity {

    protected void setupBackButton() {
        View btn = findViewById(R.id.btn_back);
        if (btn != null) {
            btn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    finish();
                }
            });
        }
    }

    protected void setHeaderTitle(String title) {
        TextView t = (TextView) findViewById(R.id.header_title);
        if (t != null) t.setText(title);
    }

    protected void setHeaderTitle(int resId) {
        TextView t = (TextView) findViewById(R.id.header_title);
        if (t != null) t.setText(resId);
    }
}
