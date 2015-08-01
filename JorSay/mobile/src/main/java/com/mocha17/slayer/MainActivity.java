package com.mocha17.slayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.FragmentManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.animation.Animation;
import android.widget.TextView;
import android.widget.Toast;

import com.mocha17.slayer.notification.NotificationListener;
import com.mocha17.slayer.settings.SettingsFragment;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Status;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    //For Status view
    private CardView statusCard;
    private TextView statusText;
    AnimatorSet statusAnimation;

    //For introducing change in successive notifications
    private int debug_notification_count = 1;

    SharedPreferences defaultSharedPreferences;

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
        statusCard = (CardView) findViewById(R.id.statusCard);
        statusText = (TextView) findViewById(R.id.status_text);
        statusAnimation = setupAndGetStatusAnimation();
    }

    @Override
    public void onResume() {
        super.onResume();

        //Update status view with a delay, so that the animation runs after the
        //Activity UI is rendered
        updateStatus(defaultSharedPreferences, true/*withDelay*/);
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_global_read_aloud).equals(key)) {
            if (sharedPreferences.getBoolean(key, false)) {
                NotificationListener.start(this);
            } else {
                NotificationListener.stop(this);
            }
        }
        updateStatus(sharedPreferences, false/*withDelay*/);
    }

    private void updateStatus(SharedPreferences sharedPreferences, boolean withDelay) {
        Status status = Status.getStatus(this, sharedPreferences);

        statusText.setText(status.getStatusText());

        if (!status.isReadAloud()) {
            //Animate to indicate 'not reading aloud' state
            if (statusAnimation.isRunning()) {
                statusAnimation.cancel();
            }
            if (withDelay) {
                statusAnimation.setStartDelay(Constants.STATUS_ANIMATION_DELAY_MILLI);
            } else {
                statusAnimation.setStartDelay(0/*no delay*/);
            }
            statusAnimation.start();
        } else { //For 'read aloud'
            if (statusAnimation.isRunning()) {
                statusAnimation.cancel();
            }
            statusText.setTextColor(getResources().getColor(R.color.text));
            statusCard.setCardBackgroundColor(getResources().getColor(R.color.content_background));
        }
    }

    private AnimatorSet setupAndGetStatusAnimation() {
        Resources r = getResources();

        //For Status Text
        ValueAnimator statusTextAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), r.getColor(R.color.text_error_from),
                r.getColor(R.color.text_error_to));
        statusTextAnimation.setRepeatCount(Constants.STATUS_ANIMATION_REPEAT_COUNT);
        statusTextAnimation.setRepeatMode(Animation.REVERSE);
        statusTextAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                statusText.setTextColor((Integer) animator.getAnimatedValue());
            }
        });
        statusTextAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                statusText.setTextColor(getResources().getColor(R.color.text));
            }
        });

        //For Status Card
        ValueAnimator statusCardAnimation = ValueAnimator.ofObject(
                new ArgbEvaluator(), r.getColor(R.color.background_error),
                r.getColor(R.color.background_error_to));
        statusCardAnimation.setRepeatCount(Constants.STATUS_ANIMATION_REPEAT_COUNT);
        statusCardAnimation.setRepeatMode(Animation.REVERSE);
        statusCardAnimation.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator animator) {
                statusCard.setCardBackgroundColor((Integer) animator.getAnimatedValue());
            }
        });
        statusCardAnimation.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                super.onAnimationEnd(animation);
                statusCard.setCardBackgroundColor(
                        getResources().getColor(R.color.background_error));
            }
        });

        AnimatorSet statusAnimations = new AnimatorSet();
        statusAnimations.playTogether(statusTextAnimation, statusCardAnimation);
        return statusAnimations;
    }
}