package com.mocha17.slayer.notification;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.preference.PreferenceManager;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.app.NotificationCompat;

import com.mocha17.slayer.MainActivity;
import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.communication.WearDataSender;
import com.mocha17.slayer.notification.db.NotificationDBOps;
import com.mocha17.slayer.tts.JorSayReader;
import com.mocha17.slayer.tts.snooze.SnoozeReadAloud;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;
import com.mocha17.slayer.utils.Status;
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

    private NotificationManager notificationManager;
    private AudioManager audioManager;

    //Action to be taken on received notification
    private enum NextAction {
        IGNORE,
        IGNORE_VOLUME,
        START_SHAKE_DETECTION,
        START_READ_ALOUD;
    }

    NotificationDBOps notificationDBOps;

    public static void start(Context context) {
        context.startService(new Intent(context, NotificationListener.class));
    }

    public static void stop(Context context) {
        context.stopService(new Intent(context, NotificationListener.class));
    }

    @Override
    public void onCreate() {
        super.onCreate();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        audioManager = (AudioManager) getSystemService(Context.AUDIO_SERVICE);

        notificationDBOps = NotificationDBOps.get(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        //Set the isRunning value in Application instance
        SlayerApp.getInstance().setNotificationListenerRunning(true);

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
        switch (getNextAction(statusBarNotification)) {
            case START_SHAKE_DETECTION:
                notificationDBOps.storeNotification(statusBarNotification);
                WearDataSender.startShakeDetection(this);
                break;
            case START_READ_ALOUD:
                notificationDBOps.storeNotification(statusBarNotification);
                JorSayReader.startReadAloud(this);
                break;
            case IGNORE_VOLUME:
                postVolumeErrorNotification(statusBarNotification);
                break;
            case IGNORE:
                //do nothing
            default:
                break;
        }
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        notificationDBOps.removeNotification(statusBarNotification.getPackageName(),
                statusBarNotification.getId());
    }

    private NextAction getNextAction(StatusBarNotification statusBarNotification) {
        if (SnoozeReadAloud.get().isActive()) {
            Logger.d(this, "reading aloud is snoozed, ignoring");
            return NextAction.IGNORE;
        }
        //Start with global_read_aloud
        if (!prefGlobalReadAloud) {
            Logger.d(this, "global_read_aloud is off, ignoring");
            return NextAction.IGNORE;
        }
        //global_read_aloud is on
        //Check packageName - 'all apps' is selected or the notification is from a selected app
        if (prefAllApps ||
                (prefSelectedPackages != null && !prefSelectedPackages.isEmpty() &&
                        prefSelectedPackages.contains(
                                statusBarNotification.getPackageName()))) {
            //For the notifications from selected apps,
            //check if volume settings prevent us from reading
            if (!prefMaxVolume && (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0)) {
                Logger.d(this, "Max volume isn't selected and device volume is at 0, ignoring");
                return NextAction.IGNORE_VOLUME;
            }
            /*Next, check if the notification should be ignored:
            - if it is for an ongoing operation
            - if it is posted with minimum priority
            - if it is our own 'reading aloud' notification*/
            if (shouldIgnore(statusBarNotification)) {
                return NextAction.IGNORE;
            }
            //We are good to take action on the notification
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

    private boolean shouldIgnore(StatusBarNotification statusBarNotification) {
        if (statusBarNotification == null) {
            return true; //should ignore
        }
        Notification notification = statusBarNotification.getNotification();
        /*We do not read non-clearable notifications, or notifications for foreground service, or
        notifications posted with minimum priority*/
        if (!statusBarNotification.isClearable() ||
                ((notification.flags & Notification.FLAG_FOREGROUND_SERVICE)
                        == Notification.FLAG_FOREGROUND_SERVICE) ||
                notification.priority == Notification.PRIORITY_MIN) {
            Logger.d(this, "Notification from " + statusBarNotification.getPackageName() +
                    " isn't clearable/is for foreground service/is posted with minimum priority, " +
                    "will be ignored");
            return true;
        }
        /*We do not read our own 'reading aloud' notification*/
        if (getPackageName().equals(statusBarNotification.getPackageName()) &&
                Constants.NOTIFICATION_ID_READING_ALOUD == statusBarNotification.getId()) {
            Logger.d(this, "'Reading-aloud' notification, will be ignored");
            return true;
        }
        return false;
    }

    private void postVolumeErrorNotification(StatusBarNotification statusBarNotification) {
        String packageName = statusBarNotification.getPackageName();
        //We are posting debug notifications from JorSay during development. This check is to avoid
        //posting volume error notification for notifications from JorSay. If we don't do this, we'd
        //be in an infinite loop, constantly posting this volume error notification.
        if (packageName.equals(getPackageName())) {
            return;
        }
        //Intent to start MainActivity from Notification
        PendingIntent notificationIntent = PendingIntent.getActivity(this,
                Constants.REQUEST_CODE_SHOW_MAIN_SCREEN,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        String text = getString(R.string.text_volume_notification, Utils.getAppName(packageName));
        String textDetails = new StringBuilder(text)
                .append(".\n")
                .append(getString(R.string.status_error_volume,
                        getString(R.string.pref_max_volume)))
                .toString();
        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_DEFAULT)
                .setSmallIcon(R.mipmap.info)
                /*.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.info))*/
                .setColor(getResources().getColor(R.color.accent))
                .setCategory(Notification.CATEGORY_ERROR)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setTicker(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(textDetails))
                .setAutoCancel(true)
                .setContentIntent(notificationIntent);

        notificationManager.notify(Constants.NOTIFICATION_ID_VOLUME, builder.build());
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_persistent_notification).equals(key)) {
            setPrefPersistentNotification(sharedPreferences);
            if (prefPersistentNotification && prefGlobalReadAloud) {
                    startForeground(Constants.PERSISTENT_NOTIFICATION_ID,
                            getPersistentNotification(defaultSharedPreferences));
            } else {
                stopForeground(true /*removeNotification*/);
            }
        } else {
            if (getString(R.string.pref_key_global_read_aloud).equals(key)) {
                setPrefGlobalReadAloud(sharedPreferences);
            } else if (getString(R.string.pref_key_all_apps).equals(key)) {
                setPrefAllApps(sharedPreferences);
            } else if (getString(R.string.pref_key_apps).equals(key)) {
                setPrefSelectedPackages(sharedPreferences);
            } else if (getString(R.string.pref_key_android_wear).equals(key)) {
                setPrefAndroidWear(sharedPreferences);
            } else if (getString(R.string.pref_key_max_volume).equals(key)) {
                setPrefMaxVolume(sharedPreferences);
            }
            if (prefPersistentNotification) {
                //Update the status Text for persistent notification if it is already posted
                updatePersistentNotification(sharedPreferences);
            }
        }
    }

    private Notification getPersistentNotification(SharedPreferences sharedPreferences) {
        Status status = Status.getStatus(this, sharedPreferences);
        String text = status.isReadAloud()?getString(R.string.status_reading_aloud):
                getString(R.string.status_not_reading_aloud);

        //Intent to start MainActivity from Notification
        PendingIntent notificationIntent = PendingIntent.getActivity(this,
                Constants.REQUEST_CODE_SHOW_MAIN_SCREEN,
                new Intent(this, MainActivity.class), PendingIntent.FLAG_UPDATE_CURRENT);

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this)
                .setPriority(Notification.PRIORITY_MIN) //so that an icon isn't seen in the top bar
                .setSmallIcon(R.mipmap.ic_notification)
                /*.setLargeIcon(BitmapFactory.decodeResource(getResources(),
                        R.mipmap.ic_notification))*/
                .setColor(getResources().getColor(R.color.accent))
                .setCategory(Notification.CATEGORY_SERVICE)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(text)
                .setTicker(text)
                .setStyle(new NotificationCompat.BigTextStyle().bigText(status.getStatusText()))
                .setContentIntent(notificationIntent);
        return builder.build();
    }

    private void updatePersistentNotification(SharedPreferences sharedPreferences) {
        notificationManager.notify(Constants.PERSISTENT_NOTIFICATION_ID,
                getPersistentNotification(sharedPreferences));
    }

    @Override
    public void onDestroy() {
        Logger.d(this, "onDestroy");

        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        stopForeground(true /*removeNotification*/);
        SlayerApp.getInstance().setNotificationListenerRunning(false);

        notificationDBOps.shutdown(this);

        super.onDestroy();
    }
}