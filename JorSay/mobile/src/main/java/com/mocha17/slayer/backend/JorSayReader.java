package com.mocha17.slayer.backend;

import android.app.IntentService;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.etc.Constants;
import com.mocha17.slayer.etc.Logger;

import java.util.Locale;

/**
 * Created by Chaitanya on 5/21/15.
 */
public class JorSayReader extends IntentService implements TextToSpeech.OnInitListener {
    private TextToSpeech tts;
    private boolean isTTSReady;

    private String toReadOnceReady = "";

    public JorSayReader() {
        super("JorSayReader");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        if (SlayerApp.getInstance().getTTSAvailable()) {
            tts = new TextToSpeech(getApplicationContext(), this);
            Logger.v("TTS init");

        }
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (Constants.ACTION_READ_ALOUD.equals(intent.getAction())) {
            String toSay = SlayerApp.getInstance().getNotificationString();

            if (isTTSReady) {
                Logger.v("Speaking...");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(toSay, TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    tts.speak(toSay, TextToSpeech.QUEUE_ADD, null);
                }
            } else {
                Logger.v("TTS NOT READY");
                toReadOnceReady = toSay;
            }
        }
        Logger.v("shutting down self");
        shutdownSelf();
    }

    private void shutdownSelf() {
        if (isTTSReady) {
            Logger.v("TTS shutdown");
            tts.shutdown();
        }
        stopSelf();
    }


    //TTS
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            // Change this to match user
            // locale
            tts.setLanguage(Locale.US);
            isTTSReady = true;
            Logger.v("TTS READY!");
            if (!TextUtils.isEmpty(toReadOnceReady)) {
                Logger.v("TTS READY! READING NOW!");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(toReadOnceReady, TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    tts.speak(toReadOnceReady, TextToSpeech.QUEUE_ADD, null);
                }
            }
        } else {
            Logger.v("TTS NOT READY");
            isTTSReady = false;
        }
    }
}
