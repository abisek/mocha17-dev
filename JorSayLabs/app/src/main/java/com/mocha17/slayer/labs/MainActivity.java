package com.mocha17.slayer.labs;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;

import java.util.HashSet;
import java.util.Set;

/* How are Settings displayed?
We have a FrameLayout in the Activity Layout, and the PreferenceFragment is loaded in it.
The Fragment itself has a layout - this layout contains the default ListView id used by Android for
populating preferences. The scaffolding needed comes from the preferences XML and the preferences
are added by the Fragment code.

Now, we could have extended PreferenceActivity here and called addPreferencesFromResource(), but that
is deprecated for the Activity. PreferenceFragment is the way to go, with PreferenceActivity acting as
holder of PreferenceHeaders (with Fragment displayed on tapping each of them). */
public class MainActivity extends AppCompatActivity implements SharedPreferences.OnSharedPreferenceChangeListener {
    TextView statusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);

        //Settings
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        getFragmentManager().beginTransaction().replace(R.id.settings_container, settingsFragment, "Settings").commit();

        //Status
        statusText = (TextView) findViewById(R.id.status_text);
        SharedPreferences defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
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

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
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
                sb.append(R.string.status_one_app);
            } else {
                sb.append(getString(R.string.status_apps, Integer.toString(apps.size())));
            }
        }

        //And so on for other features as and when they are added

        //Finally, add a full stop and return
        sb.append(".");
        return sb.toString();
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        statusText.setText(getStatusString(sharedPreferences));
    }
}
