package com.mocha17.slayer.labs;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;

/* How are Settings displayed?
We have a FrameLayout in the Activity Layout, and the PreferenceFragment is loaded in it.
The Fragment itself has a layout - this layout contains the default ListView id used by Android for
populating preferences. The scaffolding needed comes from the preferences XML and the preferences
are added by the Fragment code.

Now, we could have extended PreferenceActivity here and called addPreferencesFromResource(), but that
is deprecated for the Activity. PreferenceFragment is the way to go, with PreferenceActivity acting as
holder of PreferenceHeaders (with Fragment displayed on tapping each of them). */
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        getFragmentManager().beginTransaction().add(R.id.settings_container, settingsFragment, "Settings").commit();

        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar);
        setSupportActionBar(toolbar);
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
}
