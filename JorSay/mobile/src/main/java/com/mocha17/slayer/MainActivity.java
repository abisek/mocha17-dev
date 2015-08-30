package com.mocha17.slayer;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.FragmentManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.CardView;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.Animation;
import android.widget.Button;
import android.widget.TextView;

import com.mocha17.slayer.notification.NotificationListener;
import com.mocha17.slayer.settings.SettingsFragment;
import com.mocha17.slayer.tts.snooze.SnoozeReadAloud;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Status;

public class MainActivity extends AppCompatActivity
        implements SharedPreferences.OnSharedPreferenceChangeListener {

    //For Status view
    private CardView statusCard;
    private TextView statusText;
    private AnimatorSet statusAnimation;

    //For Snooze
    private Button cancelSnooze;
    private BroadcastReceiver snoozeUpdateReceiver;

    private SharedPreferences defaultSharedPreferences;

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

        //Snooze
        cancelSnooze = (Button) findViewById(R.id.cancel_snooze);
        cancelSnooze.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                SnoozeReadAloud.get().cancelIfActive();
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        //Update status view with a delay, so that the animation
        //runs after the Activity UI is rendered
        updateStatus(defaultSharedPreferences, true/*shouldAnimate*/, true/*withDelay*/);

        snoozeUpdateReceiver = new SnoozeUpdateReceiver();
        IntentFilter snoozeUpdateIntentFilter = new IntentFilter();
        snoozeUpdateIntentFilter.addAction(SnoozeReadAloud.ACTION_SNOOZE_TIME_LEFT);
        snoozeUpdateIntentFilter.addAction(SnoozeReadAloud.ACTION_SNOOZE_FINISHED);
        LocalBroadcastManager.getInstance(this).registerReceiver(
                snoozeUpdateReceiver, snoozeUpdateIntentFilter);
    }

    @Override
    public void onPause() {
        if(snoozeUpdateReceiver != null) {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(snoozeUpdateReceiver);
        }
        super.onPause();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_about) {
            startActivity(new Intent(this, AboutActivity.class));
        }
        return super.onOptionsItemSelected(item);
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
        updateStatus(sharedPreferences, true/*shouldAnimate*/, false/*withDelay*/);
    }

    private void updateStatus(SharedPreferences sharedPreferences,
                              boolean shouldAnimate, boolean withDelay) {
        Status status = Status.getStatus(this, sharedPreferences);

        statusText.setText(status.getStatusText());

        if (!status.isReadAloud()) {
            //Animate to indicate 'not reading aloud' state
            if (statusAnimation.isRunning()) {
                statusAnimation.cancel();
            }
            if (shouldAnimate) {
                if (withDelay) {
                    statusAnimation.setStartDelay(Constants.STATUS_ANIMATION_DELAY_MILLI);
                } else {
                    statusAnimation.setStartDelay(0/*no delay*/);
                }
                statusAnimation.start();
            } else { //no animation for snooze updates, but we still need to set state
                statusText.setTextColor(getResources().getColor(R.color.text));
                statusCard.setCardBackgroundColor(
                        getResources().getColor(R.color.background_error));
            }
        } else { //For 'read aloud'
            if (statusAnimation.isRunning()) {
                statusAnimation.cancel();
            }
            statusText.setTextColor(getResources().getColor(R.color.text));
            statusCard.setCardBackgroundColor(getResources().getColor(R.color.content_background));
            cancelSnooze.setVisibility(View.GONE);
        }
        setSnoozeVisibility();
    }

    private void setSnoozeVisibility() {
        if (!SnoozeReadAloud.get().isActive()) {
            cancelSnooze.setVisibility(View.GONE);
        } else {
            cancelSnooze.setVisibility(View.VISIBLE);
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

    private class SnoozeUpdateReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (SnoozeReadAloud.ACTION_SNOOZE_TIME_LEFT.equals(action) ||
                    SnoozeReadAloud.ACTION_SNOOZE_FINISHED.equals(action)) {
                updateStatus(defaultSharedPreferences, false/*shouldAnimate*/, false/*withDelay*/);
            }
        }
    }
}