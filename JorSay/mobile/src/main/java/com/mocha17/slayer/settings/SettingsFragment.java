package com.mocha17.slayer.settings;

import android.app.Activity;
import android.app.FragmentManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.SwitchPreference;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mocha17.slayer.R;

import java.util.HashSet;
import java.util.Set;

public class SettingsFragment extends PreferenceFragment implements
        Preference.OnPreferenceChangeListener,
        Preference.OnPreferenceClickListener, SharedPreferences.OnSharedPreferenceChangeListener {

    private SharedPreferences defaultSharedPreferences;
    private SharedPreferences.Editor editor;

    private SwitchPreference prefGlobalReadAloud, prefMaxVolume,
            prefPersistentNotification,prefAndroidWear;
    private Preference prefApps;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        editor = defaultSharedPreferences.edit();
    }

    @Override
    public void onDetach() {
        defaultSharedPreferences.unregisterOnSharedPreferenceChangeListener(this);
        super.onDetach();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View settingsView = inflater.inflate(R.layout.fragment_settings, container, false);
        initPreferences();
        return settingsView;
    }

    private void initPreferences() {
        String key;

        addPreferencesFromResource(R.xml.preferences);

        //Global read aloud
        /*findPreference() is to be used with PreferenceFragment.
        It is PreferenceActivity that is deprecated in API 11.
        http://stackoverflow.com/questions/11272839/non-deprecated-findpreference-method-android */
        key = getString(R.string.pref_key_global_read_aloud);
        prefGlobalReadAloud = (SwitchPreference)findPreference(key);
        if (prefGlobalReadAloud != null) {
            prefGlobalReadAloud.setOnPreferenceChangeListener(this);
            //Set 'checked' state
            prefGlobalReadAloud.setChecked(defaultSharedPreferences.getBoolean(key, false));
        }

        //Select apps
        key = getString(R.string.pref_key_apps);
        prefApps = findPreference(key);
        if (prefApps != null) {
            prefApps.setOnPreferenceClickListener(this);
        }

        //Volume
        key = getString(R.string.pref_key_max_volume);
        prefMaxVolume = (SwitchPreference)findPreference(key);
        if (prefMaxVolume != null) {
            prefMaxVolume.setOnPreferenceChangeListener(this);
            prefMaxVolume.setChecked(defaultSharedPreferences.getBoolean(key, false));
        }

        //Persistent notification
        key = getString(R.string.pref_key_persistent_notification);
        prefPersistentNotification = (SwitchPreference)findPreference(key);
        if (prefPersistentNotification != null) {
            prefPersistentNotification.setOnPreferenceChangeListener(this);
            prefPersistentNotification.setChecked(defaultSharedPreferences.getBoolean(key, false));
        }

        //Android Wear
        key = getString(R.string.pref_key_android_wear);
        prefAndroidWear = (SwitchPreference)findPreference(key);
        if (prefAndroidWear != null) {
            prefAndroidWear.setOnPreferenceChangeListener(this);
            prefAndroidWear.setChecked(defaultSharedPreferences.getBoolean(key, false));
        }

        updatePreferenceUIState();
    }

    /** conditionally enables or disables preferences */
    private void updatePreferenceUIState() {
        if (prefGlobalReadAloud != null) {
            //Enable other settings only if global read aloud is checked
            if (prefGlobalReadAloud.isChecked()) {
                prefPersistentNotification.setEnabled(true);
                prefPersistentNotification.setChecked(defaultSharedPreferences.getBoolean(
                        getString(R.string.pref_key_persistent_notification), true));

                prefApps.setEnabled(true);
                setPrefAppsSummary();

                prefMaxVolume.setEnabled(true);
                prefMaxVolume.setChecked(defaultSharedPreferences.getBoolean(
                        getString(R.string.pref_key_max_volume), true));

                prefAndroidWear.setEnabled(true);
                prefAndroidWear.setChecked(defaultSharedPreferences.getBoolean(
                        getString(R.string.pref_key_android_wear), true));
                setPrefAndroidWearSummary();
            } else {
                prefPersistentNotification.setEnabled(false);
                prefApps.setEnabled(false);
                prefMaxVolume.setEnabled(false);
                prefAndroidWear.setEnabled(false);
            }
        }
    }

    private void setPrefAndroidWearSummary() {
        if (!prefAndroidWear.isChecked()) {
            prefAndroidWear.setSummary(R.string.pref_android_wear_summary_off);
        } else {
            prefAndroidWear.setSummary(R.string.pref_android_wear_summary_on);
        }
    }

    private void setPrefAppsSummary() {
        if (defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_all_apps), false)) {
            prefApps.setSummary(
                    getString(R.string.pref_apps_summary, getString(R.string.all)));
        } else {
            Set<String> appsList = defaultSharedPreferences.getStringSet(
                    getString(R.string.pref_key_apps), new HashSet<String>());

            if (appsList == null || appsList.isEmpty()) {
                prefApps.setSummary(getString(R.string.pref_apps_summary_none));
            } else {
                if (appsList.size() == 1) {
                    prefApps.setSummary(getString(R.string.pref_apps_summary_one));
                } else {
                    prefApps.setSummary(getString(R.string.pref_apps_summary, appsList.size()));
                }
            }
        }
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        if (getString(R.string.pref_key_global_read_aloud).equals(key)) {
            boolean value = (boolean) newValue;
            prefGlobalReadAloud.setChecked(value);
            editor.putBoolean(key, value).apply();
            updatePreferenceUIState();
            return true;
        }
        if (getString(R.string.pref_key_persistent_notification).equals(key)) {
            boolean value = (boolean) newValue;
            prefPersistentNotification.setChecked(value);
            editor.putBoolean(key, value).apply();
            return true;
        }
        if (getString(R.string.pref_key_max_volume).equals(key)) {
            boolean value = (boolean) newValue;
            prefMaxVolume.setChecked(value);
            editor.putBoolean(key, value).apply();
            return true;
        }
        if (getString(R.string.pref_key_android_wear).equals(key)) {
            boolean value = (boolean) newValue;
            prefAndroidWear.setChecked(value);
            editor.putBoolean(key, value).apply();
            setPrefAndroidWearSummary();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key.equals(prefApps.getKey())) {
            FragmentManager fragmentManager = getFragmentManager();
            String selectAppsDialogName = SelectAppsDialog.class.getSimpleName();
            if (fragmentManager.findFragmentByTag(selectAppsDialogName) == null) {
                SelectAppsDialog selectAppsDialog = SelectAppsDialog.newInstance();
                getFragmentManager().beginTransaction().add(selectAppsDialog,
                        selectAppsDialogName).commit();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_all_apps).equals(key) ||
                getString(R.string.pref_key_apps).equals(key)) {
            //Update summary text when apps selection data changes
            setPrefAppsSummary();
        }
    }
}