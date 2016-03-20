package com.mocha17.slayer.tts.snooze;

import android.app.Notification;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.res.Resources;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.NotificationManagerCompat;
import android.support.v4.content.LocalBroadcastManager;

import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.tts.JorSayReader;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by Chaitanya on 8/24/15.
 */
public class SnoozeReadAloud {
    private static final int COUNTDOWN_INTERVAL_MILLI = 10000; //10 seconds

    private static final SnoozeReadAloud instance = new SnoozeReadAloud();

    private Context context;

    private CountDownTimer snoozeTimer;
    private AtomicBoolean snoozeTimerActive = new AtomicBoolean();
    private AtomicLong milliUntilSnoozeFinished = new AtomicLong();

    //For 'Snooze read aloud' notification
    private NotificationManagerCompat notificationManager;
    private static final String ACTION_SNOOZE_READ_ALOUD_1MIN =
            "com.mocha17.slayer.SnoozeReadAloud.SNOOZE_READ_ALOUD_1MIN";
    private static final String ACTION_SNOOZE_READ_ALOUD_3MIN =
            "com.mocha17.slayer.SnoozeReadAloud.SNOOZE_READ_ALOUD_3MIN";
    private static final String ACTION_SNOOZE_READ_ALOUD_5MIN =
            "com.mocha17.slayer.SnoozeReadAloud.SNOOZE_READ_ALOUD_5MIN";
    private static List<NotificationCompat.Action> snoozeActions;
    private static IntentFilter snoozeIntentFilter;
    private SnoozeRequestReceiver snoozeRequestReceiver;

    //For broadcasting snooze updates
    public static final String ACTION_SNOOZE_TIME_LEFT =
            "com.mocha17.slayer.SnoozeReadAloud.SNOOZE_TIME_LEFT";
    public static final String EXTRA_TIME_LEFT_MILLI = "extra_time_left_milli";
    public static final String ACTION_SNOOZE_FINISHED =
            "com.mocha17.slayer.SnoozeReadAloud.SNOOZE_FINISHED";

    public static SnoozeReadAloud get() {
        return instance;
    }

    private SnoozeReadAloud() {
        context = SlayerApp.getInstance().getApplicationContext();
        notificationManager = NotificationManagerCompat.from(context);
        snoozeIntentFilter = new IntentFilter();
        snoozeIntentFilter.addAction(ACTION_SNOOZE_READ_ALOUD_1MIN);
        snoozeIntentFilter.addAction(ACTION_SNOOZE_READ_ALOUD_3MIN);
        snoozeIntentFilter.addAction(ACTION_SNOOZE_READ_ALOUD_5MIN);
    }

    /**Starts counting down for snoozeDurationMilli.<br>Note that, the count down duration isn't
     * cumulative; calling this method stops any previous timers and starts a new one.*/
    public void start(long snoozeDurationMilli) {

        cancelIfActive();

        if (snoozeDurationMilli <= COUNTDOWN_INTERVAL_MILLI) {
            snoozeDurationMilli = COUNTDOWN_INTERVAL_MILLI;
        }
        snoozeTimer = new CountDownTimer(snoozeDurationMilli, COUNTDOWN_INTERVAL_MILLI) {
            @Override
            public void onTick(long millisUntilFinished) {
                milliUntilSnoozeFinished.set(millisUntilFinished);
                sendSnoozeUpdate();
            }

            @Override
            public void onFinish() {
                snoozeTimerActive.set(false);
                sendSnoozeUpdate();
            }
        }.start();
        snoozeTimerActive.set(true);
        milliUntilSnoozeFinished.set(snoozeDurationMilli);
        sendSnoozeUpdate();
    }

    private void sendSnoozeUpdate() {
        if (snoozeTimerActive.get()) {
            Intent snoozeTimeLeft = new Intent(ACTION_SNOOZE_TIME_LEFT);
            snoozeTimeLeft.putExtra(EXTRA_TIME_LEFT_MILLI, milliUntilSnoozeFinished.longValue());
            LocalBroadcastManager.getInstance(context).sendBroadcast(snoozeTimeLeft);
        } else {
            LocalBroadcastManager.getInstance(context).
                    sendBroadcast(new Intent(ACTION_SNOOZE_FINISHED));
        }
    }

    public void cancelIfActive() {
        if (snoozeTimer != null && snoozeTimerActive.get()) {
            Logger.d(instance, "canceling existing snooze timer");
            snoozeTimer.cancel();
            //Update state
            snoozeTimerActive.set(false);
            milliUntilSnoozeFinished.set(0);
            sendSnoozeUpdate();
        }
    }

    public boolean isActive() {
        return snoozeTimerActive.get();
    }

    public long getMilliUntilFinished() {
        if (snoozeTimerActive.get()) {
            return milliUntilSnoozeFinished.longValue();
        }
        return 0;
    }

    private class SnoozeRequestReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            int snoozeMin = 0;
            if (ACTION_SNOOZE_READ_ALOUD_1MIN.equals(intent.getAction())) {
                snoozeMin = 1;
            } else if (ACTION_SNOOZE_READ_ALOUD_3MIN.equals(intent.getAction())) {
                snoozeMin = 3;
            } else if (ACTION_SNOOZE_READ_ALOUD_5MIN.equals(intent.getAction())) {
                snoozeMin = 5;
            }
            if (snoozeMin != 0) {
                Logger.d(instance, "starting timer for " + snoozeMin + " min");
                cancelNotification();
                instance.start(snoozeMin * 60 * 1000);
                JorSayReader.snoozeReadAloud(context);
            }
        }
    }

    public void postNotification() {
        List<NotificationCompat.Action> snoozeActions = getSnoozeActions();
        if (snoozeActions != null && !snoozeActions.isEmpty()) {
            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setPriority(Notification.PRIORITY_HIGH)
                    .setSmallIcon(R.mipmap.pause)
                    .setColor(context.getResources().getColor(R.color.accent))
                    .setCategory(NotificationCompat.CATEGORY_STATUS)
                    .setContentTitle(context.getString(R.string.snooze_read_aloud_title))
                    .setTicker(context.getString(R.string.snooze_read_aloud_title))
                    .setContentText(context.getString(R.string.snooze_read_aloud_text))
                    .setAutoCancel(true);
            for (NotificationCompat.Action snoozeAction : snoozeActions) {
                builder.addAction(snoozeAction);
            }
            notificationManager.notify(Constants.NOTIFICATION_ID_READING_ALOUD, builder.build());

            //register the receiver for the notification actions
            snoozeRequestReceiver = new SnoozeRequestReceiver();
            context.registerReceiver(snoozeRequestReceiver, snoozeIntentFilter);
        }
    }

    public void cancelNotification() {
        notificationManager.cancel(Constants.NOTIFICATION_ID_READING_ALOUD);
        if (snoozeRequestReceiver != null) {
            context.unregisterReceiver(snoozeRequestReceiver);
        }
    }

    private List<NotificationCompat.Action> getSnoozeActions() {
        if (snoozeActions == null) {
            snoozeActions = new LinkedList<>();
            Resources res = context.getResources();

            //1 minute
            PendingIntent pendingIntentSnoozeOneMin = PendingIntent.getBroadcast(context,
                    Constants.REQUEST_CODE_SNOOZE_READ_ALOUD,
                    new Intent(ACTION_SNOOZE_READ_ALOUD_1MIN), PendingIntent.FLAG_UPDATE_CURRENT);
            snoozeActions.add(new NotificationCompat.Action.Builder(R.mipmap.one,
                    res.getQuantityString(R.plurals.minute, 1), pendingIntentSnoozeOneMin)
                    .build());

            //3 minutes
            PendingIntent pendingIntentSnoozeThreeMin = PendingIntent.getBroadcast(context,
                    Constants.REQUEST_CODE_SNOOZE_READ_ALOUD,
                    new Intent(ACTION_SNOOZE_READ_ALOUD_3MIN), PendingIntent.FLAG_UPDATE_CURRENT);
            snoozeActions.add(new NotificationCompat.Action.Builder(R.mipmap.three,
                    res.getQuantityString(R.plurals.minute, 3), pendingIntentSnoozeThreeMin)
                    .build());

            //5 minutes
            PendingIntent pendingIntentSnoozeFiveMin = PendingIntent.getBroadcast(context,
                    Constants.REQUEST_CODE_SNOOZE_READ_ALOUD,
                    new Intent(ACTION_SNOOZE_READ_ALOUD_5MIN), PendingIntent.FLAG_UPDATE_CURRENT);
            snoozeActions.add(new NotificationCompat.Action.Builder(R.mipmap.five,
                    res.getQuantityString(R.plurals.minute, 5), pendingIntentSnoozeFiveMin)
                    .build());
        }
        return snoozeActions;
    }
}