package com.mocha17.slayer.notification;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.communication.WearDataSender;
import com.mocha17.slayer.utils.Logger;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class NotificationListener extends NotificationListenerService {
    SharedPreferences defaultSharedPreferences;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //TODO Add foreground notification based on user preference

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if (defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_global_read_aloud), false)) {
            return Service.START_STICKY;
        } else {
            Logger.d(this, "onStartCommand, global_read_aloud is off, stopping self");
            stopSelf();
            return START_NOT_STICKY;
        }
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {
        /*TODO
        replace this with a Queue. We are recording the last notification
        posted using the App instance */
        if (defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_global_read_aloud), false)) {
            Logger.d(this, "onNotificationPosted initiating shake detection");
            SlayerApp.getInstance().setNotificationString(getNotifString(statusBarNotification));
            WearDataSender.startShakeDetection(this);
        } else {
            Logger.d(this, "onNotificationPosted global_read_aloud off," +
                    "shake detection wasn't initiated");
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        //we will remove the notification from queue here
    }

    private static String getNotifString(StatusBarNotification sbn) {
        //String ticker = sbn.getNotification().tickerText.toString();
        Bundle extras = sbn.getNotification().extras;
        Notification n = sbn.getNotification();
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(sbn.getPackageName())
                .append(", Title: ").append(extras.getString(Notification.EXTRA_TITLE))
                .append(", Text: ").append(extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = extras.getString(Notification.EXTRA_BIG_TEXT);
        if (!TextUtils.isEmpty(bigText)) {
            sb.append("Big text: ").append(bigText);
        }
        return sb.toString();
    }
}