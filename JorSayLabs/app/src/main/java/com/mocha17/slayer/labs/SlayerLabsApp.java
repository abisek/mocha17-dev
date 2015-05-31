package com.mocha17.slayer.labs;

import android.app.Application;

/**
 * Created by mocha on 5/31/15.
 */
public class SlayerLabsApp extends Application {
    private static SlayerLabsApp instance;

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SlayerLabsApp.class) {
            instance = this;
        }
    }

    public static SlayerLabsApp getInstance() {
        synchronized (SlayerLabsApp.class) {
            return instance;
        }
    }
}
