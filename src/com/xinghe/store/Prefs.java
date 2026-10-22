package com.xinghe.store;

import android.content.Context;
import android.content.SharedPreferences;

/** 本地偏好存储（登录令牌、用户信息） */
public final class Prefs {

    private static final String NAME = "xinghe_prefs";

    private static final String KEY_TOKEN = "token";
    private static final String KEY_EMAIL = "email";
    private static final String KEY_NAME = "name";
    private static final String KEY_BIO = "bio";
    private static final String KEY_COINS = "coins";
    private static final String KEY_SORT = "sort";
    private static final String KEY_LAST_TAB = "last_tab";

    private Prefs() {
    }

    private static SharedPreferences sp(Context c) {
        return c.getSharedPreferences(NAME, Context.MODE_PRIVATE);
    }

    public static String getToken(Context c) {
        return sp(c).getString(KEY_TOKEN, "");
    }

    public static void setToken(Context c, String token) {
        sp(c).edit().putString(KEY_TOKEN, token).commit();
    }

    public static String getEmail(Context c) {
        return sp(c).getString(KEY_EMAIL, "");
    }

    public static void setEmail(Context c, String email) {
        sp(c).edit().putString(KEY_EMAIL, email).commit();
    }

    public static String getName(Context c) {
        return sp(c).getString(KEY_NAME, "");
    }

    public static void setName(Context c, String name) {
        sp(c).edit().putString(KEY_NAME, name).commit();
    }

    public static String getBio(Context c) {
        return sp(c).getString(KEY_BIO, "");
    }

    public static void setBio(Context c, String bio) {
        sp(c).edit().putString(KEY_BIO, bio).commit();
    }

    public static int getCoins(Context c) {
        return sp(c).getInt(KEY_COINS, 0);
    }

    public static void setCoins(Context c, int coins) {
        sp(c).edit().putInt(KEY_COINS, coins).commit();
    }

    public static String getSort(Context c) {
        return sp(c).getString(KEY_SORT, "downloads");
    }

    public static void setSort(Context c, String sort) {
        sp(c).edit().putString(KEY_SORT, sort).commit();
    }

    public static int getLastTab(Context c) {
        return sp(c).getInt(KEY_LAST_TAB, 0);
    }

    public static void setLastTab(Context c, int tab) {
        sp(c).edit().putInt(KEY_LAST_TAB, tab).commit();
    }

    public static boolean isLoggedIn(Context c) {
        return getToken(c).length() > 0;
    }

    public static void clearUser(Context c) {
        sp(c).edit()
                .remove(KEY_TOKEN)
                .remove(KEY_EMAIL)
                .remove(KEY_NAME)
                .remove(KEY_BIO)
                .remove(KEY_COINS)
                .commit();
    }
}
