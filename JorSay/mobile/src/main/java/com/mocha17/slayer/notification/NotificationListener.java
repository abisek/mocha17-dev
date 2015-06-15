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
import com.mocha17.slayer.tts.JorSayReader;
import com.mocha17.slayer.utils.Logger;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class NotificationListener extends NotificationListenerService
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    SharedPreferences defaultSharedPreferences;
    /* We use these members for recording current state of settings. These are evaluated when the
    Service starts, and updated onSharedPreferenceChanged. Why this? We do not want to retrieve and
    process preference data for every notification, but we do need to check the parameters. This
    approach gives us that balance - data available for evaluating every notification, kept
    up-to-date by tracking preference change. */
    boolean prefGlobalReadAloud, prefAllApps, prefPersistentNotification, prefAndroidWear;
    Set<String> prefSelectedPackages;

    //Action to be taken on received notification
    private enum NextAction {
        IGNORE,
        START_SHAKE_DETECTION,
        START_READ_ALOUD;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);
        //TODO Add foreground notification based on user preference

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        initPrefValues(defaultSharedPreferences);

        if (prefGlobalReadAloud) {
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

    private NextAction getNextAction(StatusBarNotification statusBarNotification) {
        //Start with global_read_aloud
        if (!prefGlobalReadAloud) {
            Logger.d(this, "Global read_aloud is off, ignoring");
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
        } else if (getString(R.string.pref_key_android_wear).equals(key)) {
            setPrefAndroidWear(sharedPreferences);
        }
    }

    @Override
    public void onDestroy() {
        Logger.d(this, "onDestroy");
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDestroy();
    }
}