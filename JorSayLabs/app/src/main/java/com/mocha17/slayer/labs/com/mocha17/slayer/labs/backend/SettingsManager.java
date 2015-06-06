package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.content.Context;
import android.content.SharedPreferences;

import com.mocha17.slayer.labs.SlayerLabsApp;

import java.util.Set;

/**
 * Created by mocha on 5/31/15.
 */
public class SettingsManager {
    private static final String PREF_FILE = "com.mocha17.slayer.preferences";

    private Context context;
    private SharedPreferences sharedPreferences;
    private SharedPreferences.Editor editor;

    private static final SettingsManager instance = new SettingsManager();
    public static SettingsManager get() {
        return instance;
    }

    private SettingsManager() {
        context = SlayerLabsApp.getInstance().getApplicationContext();
        sharedPreferences = context.getSharedPreferences(PREF_FILE, Context.MODE_PRIVATE);
        editor = sharedPreferences.edit();
    }

    /**
     *
     * @param keyId - resource id for the key
     * @param defaultValue - defaultValue to return
     * @return a boolean indicating whether the value is true or false
     */
    public boolean getPreferenceValue(int keyId, boolean defaultValue) {
        return sharedPreferences.getBoolean(context.getString(keyId), defaultValue);
    }

    public void setPreferenceValue(int keyId, boolean value) {
        editor.putBoolean(context.getString(keyId), value).apply();
    }

    /**
     * @param defaultValue - defaultValue to return
     * @return a boolean indicating whether the value is true or false
     */
    public boolean getPreferenceValue(String key, boolean defaultValue) {
        return sharedPreferences.getBoolean(key, defaultValue);
    }

    public void setPreferenceValue(String key, boolean value) {
        editor.putBoolean(key, value).apply();
    }


    public Set<String> getPreferenceValue(int keyId, Set<String> defValues) {
        return sharedPreferences.getStringSet(context.getString(keyId), defValues);
    }

    public void setPreferenceValue(int keyId, Set<String> values) {
        editor.putStringSet(context.getString(keyId), values).apply();
    }
}