package com.mocha17.slayer.labs;

import android.app.Activity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;

/*
How are Settings displayed?
We have a Card in the Activity layout with a FrameLayout inside it.
We load the Fragment in this FrameLayout. The Fragment itself has a layout - this layout contains the
default ListView id used by Android for populating preferences. The scaffolding needed comes from the
preferences XML and the preferences are added by the Fragment code.
This is a little complicated, but gets us the preferences displayed inside a Card.

Now, we could have extended PreferenceActivity here and called addPreferencesFromResource(), but that
is deprecated for the Activity. PreferenceFragment is the way to go, with PreferenceActivity acting as
holder of PreferenceHeaders (with Fragment displayed on tapping each of them).
 */
public class MainActivity extends Activity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        SettingsFragment settingsFragment = SettingsFragment.newInstance();
        getFragmentManager().beginTransaction().add(R.id.settings_container, settingsFragment, "Settings").commit();
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
