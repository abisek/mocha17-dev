package com.mocha17.slayer;

import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.mocha17.slayer.communication.MobileDataSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WearMainActivity extends WearableActivity implements View.OnClickListener {
    private static final String KEY_HELP_COUNT = "key_help_count";
    private static final int HELP_COUNT_MAX = 3;

    //This FrameLayout contains a CircledImageView with a TextView at the center
    private FrameLayout buttonJorsay;

    //For supporting ambient mode
    private View appScreen;
    private CircledImageView buttonJorsayCircle;
    private TextView buttonJorsayText;
    private TextView dateText;
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);

    SharedPreferences defaultSharedPreferences;
    SharedPreferences.Editor editor;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);
        setAmbientEnabled();

        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                buttonJorsay = (FrameLayout) stub.findViewById(R.id.button_jorsay);
                if (buttonJorsay != null) {
                    buttonJorsay.setOnClickListener(WearMainActivity.this);
                }
                appScreen = stub.findViewById(R.id.wear_layout);
                buttonJorsayCircle =
                        (CircledImageView) stub.findViewById(R.id.button_jorsay_circle);
                buttonJorsayText = (TextView) stub.findViewById(R.id.button_jorsay_text);
                dateText = (TextView) stub.findViewById(R.id.ambient_date);
                dateText.setVisibility(View.GONE);
            }
        });

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = defaultSharedPreferences.edit();

        //Show the help text only a few times
        int helpTextCount = defaultSharedPreferences.getInt(KEY_HELP_COUNT, HELP_COUNT_MAX);
        if (helpTextCount-- > 0) {
            Toast.makeText(this, R.string.read_aloud_help, Toast.LENGTH_LONG).show();
            editor.putInt(KEY_HELP_COUNT, helpTextCount).apply();
        }
    }

    //set colors and dimensions for ambient mode
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Resources resources = getResources();
        appScreen.setBackgroundColor(resources.getColor(R.color.ambient_background));
        buttonJorsayCircle.setCircleColor(resources.getColor(R.color.ambient_content_background));
        buttonJorsayText.setTextColor(resources.getColor(R.color.ambient_text));

        //Update date
        dateText.setVisibility(View.VISIBLE);
        dateText.setText(sdf.format(new Date()));
    }

    //set colors and dimensions for 'on' mode
    @Override
    public void onExitAmbient() {
        super.onExitAmbient();
        dateText.setVisibility(View.GONE);
        Resources resources = getResources();
        appScreen.setBackgroundColor(resources.getColor(R.color.app_light));
        buttonJorsayCircle.setCircleColor(resources.getColor(R.color.app_dark));
        buttonJorsayText.setTextColor(resources.getColor(R.color.app_light));
    }

    //We receive this callback every minute. Update the time displayed - be nice to our users
    @Override
    public void onUpdateAmbient() {
        super.onUpdateAmbient();
        dateText.setText(sdf.format(new Date()));
    }

    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button_jorsay) {
            MobileDataSender.sendReadAloud(this);
            //take self down after sending the message
            this.finish();
        }
    }
}

