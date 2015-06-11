package com.mocha17.slayer;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import com.mocha17.slayer.backend.NotificationListener;
import com.mocha17.slayer.settings.SettingsFragment;

import java.util.HashSet;
import java.util.Set;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {
    private Intent notificationListenerIntent;
    private TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        //Intent for NotificationListener
        notificationListenerIntent = new Intent(this, NotificationListener.class);

        SharedPreferences defaultSharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //Settings
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        getFragmentManager().beginTransaction().replace(R.id.settings_container, settingsFragment,
                settingsFragment.getClass().getSimpleName()).commit();

        //Status
        statusText = (TextView) findViewById(R.id.status_text);
        statusText.setText(getStatusString(defaultSharedPreferences));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            Toast.makeText(this, "No Settings!", Toast.LENGTH_SHORT).show();
            return true;
        } else if (id == R.id.action_notification) {
            postNotification();
        }

        return super.onOptionsItemSelected(item);
    }

    private void postNotification() {
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.app_name))
                .setContentText(statusText.getText())
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MAX);
        Notification notification = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify((int) System.currentTimeMillis(), notification);
    }

    private String getStatusString(SharedPreferences sharedPreferences) {
        StringBuilder sb = new StringBuilder();
        String key;

        //1. Start with 'global read aloud'
        key = getString(R.string.pref_key_global_read_aloud);
        if (!sharedPreferences.getBoolean(key, false)) {
            //Not reading notifications aloud, return the appropriate String
            sb.append(getString(R.string.status_not_reading_aloud));
            return sb.toString();
        }
        //'global read aloud' is true from now on
        sb.append(getString(R.string.status_reading_aloud));

        //2. Add Android Wear info
        sb.append(" ");
        key = getString(R.string.pref_key_android_wear);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(getString(R.string.status_android_wear_on));
        } else {
            sb.append(getString(R.string.status_android_wear_off));
        }

        //3. Add 'apps' info
        sb.append(", ");
        key = getString(R.string.pref_key_all_apps);
        if (sharedPreferences.getBoolean(key, false)) {
            sb.append(getString(R.string.status_apps, getString(R.string.all)));
        } else {
            key = getString(R.string.pref_key_apps);
            Set<String> apps = sharedPreferences.getStringSet(key, new HashSet<String>());
            if (apps == null || apps.isEmpty()) {
                sb.append(getString(R.string.status_apps, getString(R.string.none)));
            } else if (apps.size() == 1) {
                sb.append(getString(R.string.status_one_app));
            } else {
                sb.append(getString(R.string.status_apps, Integer.toString(apps.size())));
            }
        }

        //And so on for other features as and when they are added

        return sb.toString();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_global_read_aloud).equals(key)) {
            if (sharedPreferences.getBoolean(key, false)) {
                startService(notificationListenerIntent);
            } else {
                stopService(notificationListenerIntent);
            }
        }
        statusText.setText(getStatusString(sharedPreferences));
    }
}