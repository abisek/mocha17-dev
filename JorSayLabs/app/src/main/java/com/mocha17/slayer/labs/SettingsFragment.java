package com.mocha17.slayer.labs;

import android.app.Activity;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.SettingsManager;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private CheckBoxPreference prefGlobalReadAloud, prefPersistentNotification;
    private boolean isPersistentNotificationPreferenceAdded;
    private int persistentNotificationPreferenceOrder;

    public static SettingsFragment newInstance() {
        SettingsFragment fragment = new SettingsFragment();
        return fragment;
    }

    public SettingsFragment() {
        // Required empty public constructor
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        View settingsView = inflater.inflate(R.layout.fragment_settings, container, false);
        initPreferences();
        return settingsView;
    }

    void initPreferences() {
        //Add preferences
        addPreferencesFromResource(R.xml.preferences);

        //Create persistent notification preference
        prefPersistentNotification = new CheckBoxPreference(getActivity());
        prefPersistentNotification.setKey(getString(R.string.pref_key_persistent_notification));
        prefPersistentNotification.setTitle(getString(R.string.pref_persistent_notification));
        prefPersistentNotification.setSummary(getString(R.string.pref_persistent_notification_help));
        prefPersistentNotification.setOnPreferenceChangeListener(this);
        prefPersistentNotification.setChecked(
                    SettingsManager.get().getPreferenceValue(R.string.pref_key_persistent_notification, false));

        //set 'read aloud' preference checked/unchecked
        prefGlobalReadAloud = (CheckBoxPreference) findPreference(getString(R.string.pref_key_global_read_aloud));
        prefGlobalReadAloud.setOnPreferenceChangeListener(this);
        if (prefGlobalReadAloud != null) {
            prefGlobalReadAloud.setChecked(
                    SettingsManager.get().getPreferenceValue(R.string.pref_key_global_read_aloud, false));
            //Show persistentNotifcation preference only if global setting is checked
            if (prefGlobalReadAloud.isChecked()) {
                getPreferenceScreen().addPreference(prefPersistentNotification);
                persistentNotificationPreferenceOrder = prefGlobalReadAloud.getOrder() + 1;
                prefPersistentNotification.setOrder(persistentNotificationPreferenceOrder);
                isPersistentNotificationPreferenceAdded = true;
            } else {
                getPreferenceScreen().removePreference(prefPersistentNotification);
                isPersistentNotificationPreferenceAdded = false;
            }
        }
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
    }

    @Override
    public void onDetach() {
        super.onDetach();
    }

    @Override
    public boolean onPreferenceChange(Preference preference, Object newValue) {
        String key = preference.getKey();
        Log.v("CK", "onPrefernceChange key: " + key);
        if (key.equals(prefGlobalReadAloud.getKey())) {
            boolean value = (boolean) newValue;
            Log.v("CK", "onPrefernceChange key: " + key + ", value " + value);
            prefGlobalReadAloud.setChecked(value);
            SettingsManager.get().setPreferenceValue(key, value);
            if (value == true && !isPersistentNotificationPreferenceAdded) {
                getPreferenceScreen().addPreference(prefPersistentNotification);
                prefPersistentNotification.setOrder(persistentNotificationPreferenceOrder);
                isPersistentNotificationPreferenceAdded = true;
            } else if (value == false && isPersistentNotificationPreferenceAdded) {
                getPreferenceScreen().removePreference(prefPersistentNotification);
                isPersistentNotificationPreferenceAdded = false;
            }
            return true;
        }
        if (key.equals(prefPersistentNotification.getKey())) {
            boolean value = (boolean) newValue;
            Log.v("CK", "onPrefernceChange key: " + key + ", value " + value);
            prefPersistentNotification.setChecked(value);
            SettingsManager.get().setPreferenceValue(key, value);
            return true;
        }
        return false;
    }
}
