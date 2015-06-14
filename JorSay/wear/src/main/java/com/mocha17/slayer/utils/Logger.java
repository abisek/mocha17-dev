package com.mocha17.slayer.utils;

import android.util.Log;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class Logger {
    public static final String TAG = "SLAYER";

    public static final void v(String msg) {
        Log.v(TAG, msg);
    }

    public static final void d(String msg) {
        Log.d(TAG, msg);
    }

    public static final void i(String msg) {
        Log.i(TAG, msg);
    }

    public static final void w(String msg) {
        Log.w(TAG, msg);
    }

    public static final void e(String msg) {
        Log.e(TAG, msg);
    }

    public static final void v(Object c, String msg) {
        Log.v(TAG, c.getClass().getSimpleName() + " " + msg);
    }

    public static final void d(Object c, String msg) {
        Log.d(TAG, c.getClass().getSimpleName() + " " + msg);
    }

    public static final void i(Object c, String msg) {
        Log.i(TAG, c.getClass().getSimpleName() + " " + msg);
    }

    public static final void w(Object c, String msg) {
        Log.w(TAG, c.getClass().getSimpleName() + " " + msg);
    }

    public static final void e(Object c, String msg) {
        Log.e(TAG, c.getClass().getSimpleName() + " " + msg);
    }
}
