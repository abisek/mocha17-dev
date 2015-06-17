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

    private String notificationString;

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
        isNotificationListenerRunning.compareAndSet(false, true);
    }

    public String getNotificationString() {
        return notificationString;
    }

    public void setNotificationString(String notificationString) {
        this.notificationString = notificationString;
    }
}
