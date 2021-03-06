package com.mocha17.slayer.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.media.AudioManager;
import android.text.TextUtils;

import com.mocha17.slayer.R;
import com.mocha17.slayer.tts.snooze.SnoozeReadAloud;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Chaitanya on 6/15/15.
 */
public class Status {

    private String statusText;
    private boolean isReadAloud;

    private Status() {
        //private Constructor to prevent instantiation
    }

    private Status (String statusText, boolean isReadAloud) {
        this.statusText = statusText;
        this.isReadAloud = isReadAloud;
    }

    public String getStatusText() {
        return statusText;
    }

    public boolean isReadAloud() {
        return isReadAloud;
    }

    public static Status getStatus(Context context, SharedPreferences sharedPreferences) {
        StringBuilder sb = new StringBuilder();

        //Start with snooze
        if (SnoozeReadAloud.get().isActive()) {
            sb.append(context.getString(R.string.status_not_reading_aloud));
            String snoozeText = getSnoozeText(context, SnoozeReadAloud.get().getMilliUntilFinished());
            if (!TextUtils.isEmpty(snoozeText)) {
                sb.append("\n").append(snoozeText);
            }
            return new Status(sb.toString(), false/*isReadAloud*/);
        }

        //Snooze isn't active. Start with 'global read aloud'
        String key;
        key = context.getString(R.string.pref_key_global_read_aloud);
        if (!sharedPreferences.getBoolean(key, false)) {
            //Not reading notifications aloud, return the appropriate Status
            sb.append(context.getString(R.string.status_not_reading_aloud))
                    .append("\n")
                    .append(context.getString(R.string.status_error_read_aloud,
                            context.getString(R.string.pref_global_read_aloud)));
            return new Status(sb.toString(), false/*isReadAloud*/);
        }

        //'global read aloud' is true. Check for other error conditions
        //1. Check for volume
        key = context.getString(R.string.pref_key_max_volume);
        if (!sharedPreferences.getBoolean(key, false)) {
            //Maximum volume isn't enabled. Check if device volume is 0
            AudioManager audioManager =
                    (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
            if (audioManager.getStreamVolume(AudioManager.STREAM_MUSIC) == 0) {
                sb.append(context.getString(R.string.status_not_reading_aloud))
                        .append("\n")
                        .append(context.getString(R.string.status_error_volume,
                                context.getString(R.string.pref_max_volume)));
                return new Status(sb.toString(), false/*isReadAloud*/);
            }
        }
        //2. Check for selected apps
        key = context.getString(R.string.pref_key_all_apps);
        if (!sharedPreferences.getBoolean(key, false)) {
            key = context.getString(R.string.pref_key_apps);
            Set<String> apps = sharedPreferences.getStringSet(key, new HashSet<String>());
            if (apps == null || apps.isEmpty()) {
                sb.append(context.getString(R.string.status_not_reading_aloud))
                        .append("\n")
                        .append(context.getString(R.string.status_error_apps));
                return new Status(sb.toString(), false/*isReadAloud*/);
            }
        }

        //'Not reading' conditions handled. Proceed with generating the status
        sb.append(context.getString(R.string.status_reading_aloud));

        //Add volume info
        sb.append(" ");
        key = context.getString(R.string.pref_key_max_volume);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(context.getString(R.string.status_max_volume_on));
        } else {
            sb.append(context.getString(R.string.status_max_volume_off));
        }

        //Add Android Wear info
        sb.append(" ");
        key = context.getString(R.string.pref_key_android_wear);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(context.getString(R.string.status_android_wear_on));
        } else {
            sb.append(context.getString(R.string.status_android_wear_off));
        }

        //Add 'apps' info
        sb.append(" ");
        key = context.getString(R.string.pref_key_all_apps);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(context.getString(R.string.status_apps, context.getString(R.string.all)));
        } else {
            key = context.getString(R.string.pref_key_apps);
            Set<String> apps = sharedPreferences.getStringSet(key, new HashSet<String>());
            if (apps.size() == 1) {
                sb.append(context.getString(R.string.status_one_app));
            } else {
                sb.append(context.getString(R.string.status_apps, Integer.toString(apps.size())));
            }
        }

        //And so on for other features as and when they are added

        return new Status(sb.toString(), true/*isReadAloud*/);
    }

    private static final String getSnoozeText(Context context, long milliUntilFinished) {
        if (milliUntilFinished <= 0) {
            return null;
        }
        Resources r = context.getResources();
        int secondsUntilFinished = roundTo10(milliUntilFinished/1000);
        int minutesUntilFinished = secondsUntilFinished / 60;
        if (minutesUntilFinished == 0) {
            return r.getString(R.string.status_snooze_time_left,
                    secondsUntilFinished + " " + r.getString(R.string.status_seconds));
        } else {
            String timeLeft = minutesUntilFinished + " " + r.getQuantityString(
                    R.plurals.status_minute, minutesUntilFinished);
            secondsUntilFinished = secondsUntilFinished - (minutesUntilFinished * 60);
            if (secondsUntilFinished != 0) {
                timeLeft = timeLeft.concat(" " + secondsUntilFinished + " " +
                        r.getString(R.string.status_seconds));
            }
            return context.getResources().getString(R.string.status_snooze_time_left, timeLeft);
        }
    }

    /** Rounds input to nearest multiple of 10. 21 would be rounded to 20 and 26 to 30.*/
    private static int roundTo10(long number) {
        return Math.round((number + 5)/ 10) * 10;
    }
}