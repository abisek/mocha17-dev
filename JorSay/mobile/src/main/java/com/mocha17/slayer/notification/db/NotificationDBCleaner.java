package com.mocha17.slayer.notification.db;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import com.mocha17.slayer.utils.Logger;

/**
 * Created by Chaitanya on 8/1/15.
 */
public class NotificationDBCleaner extends BroadcastReceiver {
    private static final String ACTION_NOTIFICATION_DB_CLEANER =
            "com.mocha17.slayer.notification.db.NotificationDBCleaner";
    private static final int ALARM_INTENT_REQUEST_CODE = 1234;

    private static PendingIntent alarmIntent;

    public static void start(Context context) {
        Logger.d("NotificationDBCleaner - setting up alarm");

        Intent intent = new Intent(ACTION_NOTIFICATION_DB_CLEANER);
        alarmIntent = PendingIntent.getBroadcast(context, ALARM_INTENT_REQUEST_CODE, intent,
                PendingIntent.FLAG_UPDATE_CURRENT);

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.setInexactRepeating(AlarmManager.ELAPSED_REALTIME, 0 /*start now*/,
                AlarmManager.INTERVAL_DAY, alarmIntent);
    }

    public static void stop(Context context) {
        if (alarmIntent != null) {
            AlarmManager alarmManager =
                    (AlarmManager)context.getSystemService(Context.ALARM_SERVICE);
            Logger.d("NotificationDBCleaner - canceling alarm");
            alarmManager.cancel(alarmIntent);
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        if (ACTION_NOTIFICATION_DB_CLEANER.equals(intent.getAction())) {
            NotificationDBOps.get(context).removeReadNotifications();
        }
    }
}