package com.mocha17.slayer;

import android.app.Activity;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.FrameLayout;

import com.mocha17.slayer.communication.MobileDataSender;

public class WearMainActivity extends Activity implements View.OnClickListener {
    //This FrameLayout contains a CircledImageView with a TextView at the center
    private FrameLayout buttonJorSay;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                buttonJorSay = (FrameLayout) stub.findViewById(R.id.button_jorsay);
                if (buttonJorSay != null) {
                    buttonJorSay.setOnClickListener(WearMainActivity.this);
                }
            }
        });
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

