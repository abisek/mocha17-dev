package com.mocha17.slayer.labs;

import android.app.Activity;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.CheckBoxPreference;
import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.Constants;
import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.SettingsManager;

public class SettingsFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
    private CheckBoxPreference prefGlobalReadAloud, prefPersistentNotification;
    //private boolean isPersistentNotificationPreferenceAdded;
    //private int persistentNotificationPreferenceOrder;

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
        //This is essential; without this, getPreferenceScreen() returns null
        addPreferencesFromResource(R.xml.preferences);

        //Create persistent notification preference
        prefPersistentNotification = new CheckBoxPreference(getActivity(),
                Constants.ORDER_PREF_PERSISTENT_NOTIFICATION, getString(R.string.pref_key_persistent_notification),
                getString(R.string.pref_persistent_notification), getString(R.string.pref_persistent_notification_help));
        prefPersistentNotification.setOnPreferenceChangeListener(this);
        prefPersistentNotification.setChecked(
                SettingsManager.get().getPreferenceValue(R.string.pref_key_persistent_notification, false));

        //Create global 'read aloud' preference
        prefGlobalReadAloud = new CheckBoxPreference(getActivity(), Constants.ORDER_PREF_GLOBAL_READ_ALOUD,
                getString(R.string.pref_key_global_read_aloud), getString(R.string.pref_global_read_aloud));
        prefGlobalReadAloud.setOnPreferenceChangeListener(this);
        //Add this by default to the PreferenceScreen
        getPreferenceScreen().addPreference(prefGlobalReadAloud);

        //Set 'checked' state
        if (prefGlobalReadAloud != null) {
            prefGlobalReadAloud.setChecked(
                    SettingsManager.get().getPreferenceValue(R.string.pref_key_global_read_aloud, false));
            //Show persistentNotifcation preference only if global setting is checked
            if (prefGlobalReadAloud.isChecked()) {
                getPreferenceScreen().addPreference(prefPersistentNotification);
                prefPersistentNotification.added(true);
            } else {
                getPreferenceScreen().removePreference(prefPersistentNotification);
                prefPersistentNotification.added(false);
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
            if (value == true && !prefPersistentNotification.isAdded()) {
                getPreferenceScreen().addPreference(prefPersistentNotification);
                prefPersistentNotification.added(true);
            } else if (value == false && prefPersistentNotification.isAdded()) {
                getPreferenceScreen().removePreference(prefPersistentNotification);
                prefPersistentNotification.added(false);
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
