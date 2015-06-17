package com.mocha17.slayer.notification;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;
import android.text.TextUtils;

import com.mocha17.slayer.MainActivity;
import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.communication.WearDataSender;
import com.mocha17.slayer.tts.JorSayReader;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;
import com.mocha17.slayer.utils.Utils;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class NotificationListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences defaultSharedPreferences;
    /* We use these members for recording current state of settings. These are evaluated when the
    Service starts, and updated onSharedPreferenceChanged. Why this? We do not want to retrieve and
    process preference data for every notification, but we do need to check the parameters. This
    approach gives us that balance - data available for evaluating every notification, kept
    up-to-date by tracking preference change. */
    private boolean prefGlobalReadAloud, prefAllApps, prefMaxVolume,
            prefPersistentNotification, prefAndroidWear;
    private Set<String> prefSelectedPackages;

    private AudioManager audioManager;

    //Action to be taken on received notification
    private enum NextAction {
        IGNORE,
        START_SHAKE_DETECTION,
        START_READ_ALOUD;
    }

    @Override
    public void onCreate() {
        super.onCreate();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);
    }
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        initPrefValues(defaultSharedPreferences);

        if (prefGlobalReadAloud) {
            if (prefPersistentNotification) {
                startForeground(Constants.PERSISTENT_NOTIFICATION_ID,
                        getPersistentNotification(defaultSharedPreferences));
            } else {
                stopForeground(true/*removeNotification*/);
            }
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
        replace 'setNotificationString()' with a Queue. We are recording the last notification
        posted using the App instance */
        switch (getNextAction(statusBarNotification)) {
            case START_SHAKE_DETECTION:
                SlayerApp.getInstance().setNotificationString(
                        getNotifString(statusBarNotification));
                WearDataSender.startShakeDetection(this);
                break;
            case START_READ_ALOUD:
                SlayerApp.getInstance().setNotificationString(
                        getNotifString(statusBarNotification));
                JorSayReader.startReadAloud(this);
                break;
            case IGNORE:
            default:
                //do nothing
                break;

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

    private void initPrefValues(SharedPreferences sharedPreferences) {
        setPrefGlobalReadAloud(sharedPreferences);
        setPrefAllApps(sharedPreferences);
        setPrefSelectedPackages(sharedPreferences);
        setPrefPersistentNotification(sharedPreferences);
        setPrefAndroidWear(sharedPreferences);
        setPrefMaxVolume(sharedPreferences);
    }

    private void setPrefGlobalReadAloud(SharedPreferences sharedPreferences) {
        prefGlobalReadAloud = sharedPreferences.getBoolean(
                getString(R.string.pref_key_global_read_aloud), false);
    }

    private void setPrefAllApps(SharedPreferences sharedPreferences) {
        prefAllApps = sharedPreferences.getBoolean(
                getString(R.string.pref_key_all_apps), false);
    }

    private void setPrefSelectedPackages(SharedPreferences sharedPreferences) {
        prefSelectedPackages = sharedPreferences.getStringSet(
                getString(R.string.pref_key_apps), new HashSet<String>());
    }

    private void setPrefPersistentNotification(SharedPreferences sharedPreferences) {
        prefPersistentNotification = sharedPreferences.getBoolean(
                getString(R.string.pref_key_persistent_notification), false);
    }

    private void setPrefAndroidWear(SharedPreferences sharedPreferences) {
        prefAndroidWear = sharedPreferences.getBoolean(
                getString(R.string.pref_key_android_wear), false);
    }

    private void setPrefMaxVolume(SharedPreferences sharedPreferences) {
        prefMaxVolume = sharedPreferences.getBoolean(
                getString(R.string.pref_key_max_volume), false);
    }

    private NextAction getNextAction(StatusBarNotification statusBarNotification) {
        //Start with global_read_aloud
        if (!prefGlobalReadAloud) {
            Logger.d(this, "Global read_aloud is off, ignoring");
            return NextAction.IGNORE;
        }
        if (!prefMaxVolume && (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)) {
            Logger.d(this, "Max volume isn't selected and device volume is at 0, ignoring");
            return NextAction.IGNORE;
        }
        //global_read_aloud is on
        //Check packageName - 'all apps' is selected or the notification is from a selected app
        if (prefAllApps ||
                (prefSelectedPackages != null &&!prefSelectedPackages.isEmpty() &&
                        prefSelectedPackages.contains(
                                statusBarNotification.getPackageName()))) {
            if (prefAndroidWear) {
                //shake detection enabled
                return NextAction.START_SHAKE_DETECTION;
            } else {
                return NextAction.START_READ_ALOUD;
            }
        }
        Logger.d(this, "Package " + statusBarNotification.getPackageName() +
                " not selected, ignoring");
        return NextAction.IGNORE;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_global_read_aloud).equals(key)) {
            setPrefGlobalReadAloud(sharedPreferences);
        } else if (getString(R.string.pref_key_all_apps).equals(key)) {
            setPrefAllApps(sharedPreferences);
        } else if (getString(R.string.pref_key_apps).equals(key)) {
            setPrefSelectedPackages(sharedPreferences);
        } else if (getString(R.string.pref_key_persistent_notification).equals(key)) {
            setPrefPersistentNotification(sharedPreferences);
            if (prefPersistentNotification) {
                startForeground(Constants.PERSISTENT_NOTIFICATION_ID,
                        getPersistentNotification(defaultSharedPreferences));
            } else {
                stopForeground(true /*removeNotification*/);
            }
        } else if (getString(R.string.pref_key_android_wear).equals(key)) {
            setPrefAndroidWear(sharedPreferences);
        }  else if (getString(R.string.pref_key_max_volume).equals(key)) {
            setPrefMaxVolume(sharedPreferences);
        }
    }

    private Notification getPersistentNotification(SharedPreferences sharedPreferences) {
        //Intent to start MainActivity from Notification
        PendingIntent notificationIntent = PendingIntent.getActivity(this,
                Constants.PERSISTENT_NOTIFICATION_ACTION_REQUEST_CODE,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_MIN) //so that an icon isn't seen in the top bar
                .setSmallIcon(R.mipmap.ic_launcher)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(getString(R.string.persistent_notification_text))
                .setTicker(getString(R.string.persistent_notification_text))
                .setStyle(new NotificationCompat.BigTextStyle().bigText(
                        Utils.getStatusText(this, sharedPreferences)))
                .setContentIntent(notificationIntent);
        return builder.build();
    }

    @Override
    public void onDestroy() {
        Logger.d(this, "onDestroy");
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        stopForeground(true /*removeNotification*/);
        super.onDestroy();
    }
}