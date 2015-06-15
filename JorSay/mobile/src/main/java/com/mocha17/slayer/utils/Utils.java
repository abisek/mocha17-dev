package com.mocha17.slayer.utils;

import android.content.Context;
import android.content.SharedPreferences;

import com.mocha17.slayer.R;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Chaitanya on 6/15/15.
 */
public class Utils {
    public static String getStatusText(Context context, SharedPreferences sharedPreferences) {
        StringBuilder sb = new StringBuilder();
        String key;

        //1. Start with 'global read aloud'
        key = context.getString(R.string.pref_key_global_read_aloud);
        if (!sharedPreferences.getBoolean(key, false)) {
            //Not reading notifications aloud, return the appropriate String
            sb.append(context.getString(R.string.status_not_reading_aloud));
            return sb.toString();
        }
        //'global read aloud' is true from now on
        sb.append(context.getString(R.string.status_reading_aloud));

        //2. Add Android Wear info
        sb.append(" ");
        key = context.getString(R.string.pref_key_android_wear);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(context.getString(R.string.status_android_wear_on));
        } else {
            sb.append(context.getString(R.string.status_android_wear_off));
        }

        //3. Add 'apps' info
        sb.append(", ");
        key = context.getString(R.string.pref_key_all_apps);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(context.getString(R.string.status_apps, context.getString(R.string.all)));
        } else {
            key = context.getString(R.string.pref_key_apps);
            Set<String> apps = sharedPreferences.getStringSet(key, new HashSet<String>());
            if (apps == null || apps.isEmpty()) {
                sb.append(context.getString(R.string.status_apps,
                        context.getString(R.string.none)));
            } else if (apps.size() == 1) {
                sb.append(context.getString(R.string.status_one_app));
            } else {
                sb.append(context.getString(R.string.status_apps, Integer.toString(apps.size())));
            }
        }

        //And so on for other features as and when they are added

        return sb.toString();
    }
}
