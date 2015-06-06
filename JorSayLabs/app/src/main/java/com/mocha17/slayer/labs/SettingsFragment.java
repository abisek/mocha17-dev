package com.mocha17.slayer.labs;

import android.app.Activity;
import android.app.Dialog;
import android.content.DialogInterface;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceScreen;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.Constants;
import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.SelectAppsDialog;
import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.SettingsManager;
import com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend.SwitchPreference;

import java.util.HashSet;
import java.util.Set;

public class SettingsFragment extends PreferenceFragment
        implements Preference.OnPreferenceChangeListener, Preference.OnPreferenceClickListener, Dialog.OnDismissListener {
    PreferenceScreen preferenceScreen;
    private SwitchPreference prefGlobalReadAloud, prefPersistentNotification, prefAndroidWear;
    private MultiSelectListPreference prefAppsList;
    private Preference prefApps;

    private ListPreference l;

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
        preferenceScreen = getPreferenceScreen();

        //Create Android Wear requirement preference
        prefAndroidWear = new SwitchPreference(getActivity(), Constants.ORDER_PREF_ANDROID_WEAR,
                getString(R.string.pref_key_android_wear), getString(R.string.pref_android_wear));
        prefAndroidWear.setOnPreferenceChangeListener(this);

        //Create persistent notification preference
        prefPersistentNotification = new SwitchPreference(getActivity(),
                Constants.ORDER_PREF_PERSISTENT_NOTIFICATION, getString(R.string.pref_key_persistent_notification),
                getString(R.string.pref_persistent_notification), getString(R.string.pref_persistent_notification_help));
        prefPersistentNotification.setOnPreferenceChangeListener(this);

        //Create 'apps' preference
        prefApps = new SwitchPreference(getActivity(),
                Constants.ORDER_PREF_APPS, getString(R.string.pref_key_apps),
                getString(R.string.pref_apps_title));
        prefApps = new Preference(getActivity());
        prefApps.setTitle(R.string.pref_apps_title);
        prefApps.setKey(getString(R.string.pref_key_apps));
        prefApps.setOrder(Constants.ORDER_PREF_APPS);
        prefApps.setOnPreferenceClickListener(this);

        //Create the apps list
        prefAppsList = new MultiSelectListPreference(getActivity());
        prefAppsList.setTitle(R.string.select_apps_title);
        prefAppsList.setEntries(R.array.listSelectAllAppsOrSome);
        prefAppsList.setEntryValues(R.array.valuesSelectAllAppsOrSome);

        //Create global 'read aloud' preference
        prefGlobalReadAloud = new SwitchPreference(getActivity(), Constants.ORDER_PREF_GLOBAL_READ_ALOUD,
                getString(R.string.pref_key_global_read_aloud), getString(R.string.pref_global_read_aloud));
        prefGlobalReadAloud.setOnPreferenceChangeListener(this);
        //Add this by default to the PreferenceScreen
        preferenceScreen.addPreference(prefGlobalReadAloud);
        //Set 'checked' state
        prefGlobalReadAloud.setChecked(
                    SettingsManager.get().getPreferenceValue(R.string.pref_key_global_read_aloud, false));

        updatePreferenceVisibility();

    }

    /** conditionally shows or hides preferences */
    private void updatePreferenceVisibility() {
        if (prefGlobalReadAloud != null) {
            //Show persistent notifcation preference only if global setting is checked
            if (prefGlobalReadAloud.isChecked()) {
                preferenceScreen.addPreference(prefPersistentNotification);
                prefPersistentNotification.added(true);
                prefPersistentNotification.setChecked(
                        SettingsManager.get().getPreferenceValue(R.string.pref_key_persistent_notification, true));

                preferenceScreen.addPreference(prefApps);
                setPrefAppsSummary();

                preferenceScreen.addPreference(prefAndroidWear);
                prefAndroidWear.added(true);
                prefAndroidWear.setChecked(SettingsManager.get().getPreferenceValue(R.string.pref_key_android_wear, true));
                setPrefAndroidWearSummary();
            } else {
                preferenceScreen.removePreference(prefPersistentNotification);
                prefPersistentNotification.added(false);

                preferenceScreen.removePreference(prefApps);

                preferenceScreen.removePreference(prefAndroidWear);
                prefAndroidWear.added(false);
            }
        }
    }

    private void setPrefAndroidWearSummary() {
        if (!prefAndroidWear.isChecked()) {
            prefAndroidWear.setSummary(R.string.pref_android_wear_read_all);
        } else {
            prefAndroidWear.setSummary(null);
        }
    }

    private void setPrefAppsSummary() {
        if (SettingsManager.get().getPreferenceValue(R.string.pref_key_all_apps, false)) {
            prefApps.setSummary(getString(R.string.pref_apps_summary, getString(R.string.all)));
        } else {
            Set<String> appsList = SettingsManager.get().getPreferenceValue(R.string.pref_key_apps,
                    new HashSet<String>());
            if (appsList == null || appsList.isEmpty()) {
                prefApps.setSummary(getString(R.string.pref_apps_summary, getString(R.string.none)));
            } else {
                prefApps.setSummary(getString(R.string.pref_apps_summary, getString(R.string.selected)));
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
        if (key.equals(prefGlobalReadAloud.getKey())) {
            boolean value = (boolean) newValue;
            prefGlobalReadAloud.setChecked(value);
            SettingsManager.get().setPreferenceValue(key, value);
            updatePreferenceVisibility();
            return true;
        }
        if (key.equals(prefPersistentNotification.getKey())) {
            boolean value = (boolean) newValue;
            prefPersistentNotification.setChecked(value);
            SettingsManager.get().setPreferenceValue(key, value);
            return true;
        }
        if (key.equals(prefAndroidWear.getKey())) {
            boolean value = (boolean) newValue;
            prefAndroidWear.setChecked(value);
            SettingsManager.get().setPreferenceValue(key, value);
            setPrefAndroidWearSummary();
            return true;
        }
        return false;
    }

    @Override
    public boolean onPreferenceClick(Preference preference) {
        String key = preference.getKey();
        if (key.equals(prefApps.getKey())) {
            if (getFragmentManager().findFragmentByTag("SelectAppsDialog") == null) {
                SelectAppsDialog selectAppsDialog = SelectAppsDialog.newInstance();
                selectAppsDialog.setOnDismissListener(this);
                getFragmentManager().beginTransaction().add(selectAppsDialog, "SelectAppsDialog").commit();
            }
            return true;
        }
        return false;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        //Update summary text when the 'select apps' dialog goes away
        setPrefAppsSummary();
    }
}
