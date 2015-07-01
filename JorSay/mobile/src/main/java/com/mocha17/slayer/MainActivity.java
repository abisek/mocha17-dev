package com.mocha17.slayer;

import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.mocha17.slayer.notification.NotificationListener;
import com.mocha17.slayer.settings.SettingsFragment;
import com.mocha17.slayer.utils.Utils;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    private TextView statusText;

    //For introducing change in successive notifications
    private int debug_notification_count = 1;

    SharedPreferences defaultSharedPreferences;

    //For not showing the hint on orientation changes
    private final String keyGlobalReadAloudHintShown = "keyGlobalReadAloudHintShown";
    private boolean globalReadAloudHintShown;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);

        //Settings
        String settingsFragmentName = SettingsFragment.class.getSimpleName();
        FragmentManager fragmentManager = getFragmentManager();
        if (fragmentManager.findFragmentByTag(settingsFragmentName) == null) {
            fragmentManager.beginTransaction().add(R.id.settings_container,
                    SettingsFragment.newInstance(), settingsFragmentName).commit();
        }

        //Status
        statusText = (TextView) findViewById(R.id.status_text);
        statusText.setText(Utils.getStatusText(this, defaultSharedPreferences));

        //hint shown?
        if (savedInstanceState != null) {
            globalReadAloudHintShown =
                    savedInstanceState.getBoolean(keyGlobalReadAloudHintShown);
        } else {
            globalReadAloudHintShown = false;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        if (!globalReadAloudHintShown && !defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_global_read_aloud), false)) {
            showGlobalReadAloudHint();
            globalReadAloudHintShown = true;
        }
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putBoolean(keyGlobalReadAloudHintShown, globalReadAloudHintShown);
        super.onSaveInstanceState(outState);
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
        String bigText = "A longer line of text, count is " + debug_notification_count;
        String text = "Count is " + debug_notification_count;
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle("This is how we speak!")
                .setContentText(text)
                .setTicker(text)
                .setStyle(new Notification.BigTextStyle().bigText(bigText))
                .setSmallIcon(R.mipmap.ic_notification)
                .setPriority(Notification.PRIORITY_MAX);
        Notification notification = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(10001, notification); //same ID so that the same notification is updated
        debug_notification_count++;
    }

    private void showGlobalReadAloudHint() {
        Toast toast = new Toast(this);

        LayoutInflater inflater = getLayoutInflater();
        View layout = inflater.inflate(R.layout.layout_toast,
                (ViewGroup) findViewById(R.id.toast_layout_root));

        TextView text = (TextView) layout.findViewById(R.id.toast_text);
        text.setText(getString(
                R.string.global_read_aloud_hint,
                getString(R.string.pref_global_read_aloud)));

        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.setDuration(Toast.LENGTH_LONG);
        toast.setView(layout);
        toast.show();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_global_read_aloud).equals(key)) {
            if (sharedPreferences.getBoolean(key, false)) {
                NotificationListener.start(this);
            } else {
                NotificationListener.stop(this);
            }
        }
        statusText.setText(Utils.getStatusText(this, sharedPreferences));
    }
}