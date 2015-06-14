package com.mocha17.slayer.tts;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.speech.tts.TextToSpeech;
import android.text.TextUtils;

import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

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

    public static void startReadAloud(Context context) {
        Intent intent = new Intent(context, JorSayReader.class);
        intent.setAction(Constants.ACTION_MSG_START_READ_ALOUD);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        if (Constants.ACTION_MSG_START_READ_ALOUD.equals(intent.getAction())) {
            String toSay = SlayerApp.getInstance().getNotificationString();

            if (isTTSReady) {
                Logger.d(this, "Speaking...");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(toSay, TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    tts.speak(toSay, TextToSpeech.QUEUE_ADD, null);
                }
            } else {
                Logger.d(this, "TTS NOT READY");
                toReadOnceReady = toSay;
            }
        }
        Logger.d(this, "shutting down self");
        shutdownSelf();
    }

    private void shutdownSelf() {
        if (isTTSReady) {
            Logger.d(this, "TTS shutdown");
            tts.shutdown();
        }
        stopSelf();
    }


    //TTS
    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //TODO Change this to match user locale
            tts.setLanguage(Locale.US);

            isTTSReady = true;
            Logger.d(this, "TTS READY!");
            if (!TextUtils.isEmpty(toReadOnceReady)) {
                Logger.d(this, "TTS READY! READING NOW!");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                    tts.speak(toReadOnceReady, TextToSpeech.QUEUE_ADD, null, null);
                } else {
                    tts.speak(toReadOnceReady, TextToSpeech.QUEUE_ADD, null);
                }
            }
        } else {
            Logger.d(this, "TTS NOT READY");
            isTTSReady = false;
        }
    }
}
