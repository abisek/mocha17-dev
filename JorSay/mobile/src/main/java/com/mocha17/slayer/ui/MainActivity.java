package com.mocha17.slayer.ui;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.ActionBarActivity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.mocha17.slayer.R;
import com.mocha17.slayer.backend.NotificationListener;
import com.mocha17.slayer.etc.Constants;


public class MainActivity extends ActionBarActivity implements View.OnClickListener{
    private static final String STARTED = "Monitoring notifications...";
    private static final String STOPPED = "Stopped monitoring notifications";
    private Intent serviceIntent;
    private boolean serviceStarted;

    private LocalBroadcastManager localBroadcastManager;
    private NotificationBroadcastReceiver notificationBroadcastReceiver = new NotificationBroadcastReceiver();
    private static final IntentFilter notifIntentFilter = new IntentFilter();

    private DisplayFragment fragment;
    private final String FRAGMENT_TAG = "DisplayFragment";

    static {
        notifIntentFilter.addAction(Constants.BROADCAST_NOTIF);
    }

    private Button button;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_main);
        button = (Button) findViewById(R.id.start_button);
        button.setOnClickListener(this);

        serviceIntent = new Intent(this, NotificationListener.class);

        localBroadcastManager = LocalBroadcastManager.getInstance(this);

        if (savedInstanceState == null) {
            fragment = new DisplayFragment();
            getSupportFragmentManager().beginTransaction()
                    .add(R.id.container, fragment, FRAGMENT_TAG)
                    .commit();
        }
    }

    @Override
    protected void onPause() {
        if (serviceStarted) {
            localBroadcastManager.unregisterReceiver(notificationBroadcastReceiver);
            stopService(serviceIntent);
            Toast.makeText(this, STOPPED, Toast.LENGTH_SHORT).show();
        }
        super.onPause();
    }

    private void updateUI(Intent intent) {
        if (getSupportFragmentManager().findFragmentByTag(FRAGMENT_TAG) != null) {
            fragment.updateUI(intent);
        }
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
                .setContentText("Slayer!")
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MAX);
        Notification notification = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify((int)System.currentTimeMillis(), notification);
        //notification.notify();
    }
    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.start_button:
                localBroadcastManager.registerReceiver(notificationBroadcastReceiver, notifIntentFilter);
                startService(serviceIntent);
                Toast.makeText(this, STARTED, Toast.LENGTH_SHORT).show();
                serviceStarted = true;
                break;
        }
    }

    private class NotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Constants.BROADCAST_NOTIF.equals(intent.getAction())) {
                updateUI(intent);
            }
        }
    }
}
