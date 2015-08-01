package com.mocha17.slayer;

import android.content.res.Resources;
import android.os.Bundle;
import android.support.wearable.activity.WearableActivity;
import android.support.wearable.view.CircledImageView;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.mocha17.slayer.communication.MobileDataSender;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class WearMainActivity extends WearableActivity implements View.OnClickListener {
    //This FrameLayout contains a CircledImageView with a TextView at the center
    private FrameLayout buttonJorsay;

    //For supporting ambient mode
    private View appScreen;
    private CircledImageView buttonJorsayCircle;
    private TextView buttonJorsayText;
    private TextView dateText;
    SimpleDateFormat sdf = new SimpleDateFormat("hh:mm a", Locale.US);

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
    }

    //set colors and dimensions for ambient mode
    @Override
    public void onEnterAmbient(Bundle ambientDetails) {
        super.onEnterAmbient(ambientDetails);
        Resources resources = getResources();
        appScreen.setBackgroundColor(resources.getColor(R.color.ambient_background));
        buttonJorsayCircle.setCircleColor(resources.getColor(R.color.ambient_content_background));
        buttonJorsayCircle.setCircleRadius(resources.getDimension(R.dimen.circle_radius_ambient));
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
        buttonJorsayCircle.setCircleRadius(resources.getDimension(R.dimen.circle_radius));
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

