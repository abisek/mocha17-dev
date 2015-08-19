package com.mocha17.slayer;

import android.app.Application;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.mocha17.slayer.notification.NotificationListener;
import com.mocha17.slayer.utils.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class SlayerApp extends Application {
    private static SlayerApp instance;

    private AtomicBoolean isNotificationListenerRunning = new AtomicBoolean();
    SharedPreferences defaultSharedPreferences;

    public static SlayerApp getInstance() {
        synchronized (SlayerApp.class) {
            return instance;
        }
    }

    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SlayerApp.class) {
            instance = this;
        }
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        /*Following two checks are important enough to be done in Application.onCreate():
        1. Set 'Enable shake detection' to true if it is never set. (We are not overwriting
        user's preference; just setting it to true by default.)
        2. Start the NotificationListenerService if user has enabled 'read aloud'*/
        String enableShakeDetection = getString(R.string.pref_key_android_wear);
        if (!defaultSharedPreferences.contains(enableShakeDetection)) {
            Logger.d(this, "Shake Detection was never set before, setting it to true");
            defaultSharedPreferences.edit().putBoolean(enableShakeDetection, true).apply();
        }
        boolean prefGlobalReadAloud = defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_global_read_aloud), false);
        if (prefGlobalReadAloud && !isNotificationListenerRunning()) {
            /*At application start, the service isn't running even though globalReadAloud is
            selected by user. Start it now. */
            Logger.d(this, "Starting NotificationListener");
            NotificationListener.start(this);
        }
    }

    public boolean isNotificationListenerRunning() {
        return isNotificationListenerRunning.get();
    }

    public void setNotificationListenerRunning(boolean isRunning) {
        isNotificationListenerRunning.set(isRunning);
    }
}