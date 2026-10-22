package com.xinghe.store;

import android.app.Application;
import android.content.Context;

import com.xinghe.store.api.Api;
import com.xinghe.store.data.DB;
import com.xinghe.store.ui.ImageLoader;

/** 全局 Application：会话令牌、图片加载器、数据库 */
public class XingheApp extends Application {

    public static ImageLoader imageLoader;
    public static DB db;

    @Override
    public void onCreate() {
        super.onCreate();
        Api.token = Prefs.getToken(this);
        imageLoader = new ImageLoader(this);
        db = new DB(this);
    }

    public static Context ctx;
}
