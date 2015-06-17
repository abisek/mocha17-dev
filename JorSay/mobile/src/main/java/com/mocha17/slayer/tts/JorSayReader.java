package com.mocha17.slayer.tts;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.AudioManager;
import android.os.Build;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;

import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

import java.util.HashMap;
import java.util.Locale;

/**
 * Created by Chaitanya on 5/21/15.
 */
public class JorSayReader extends IntentService implements TextToSpeech.OnInitListener,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private TextToSpeech tts;
    int originalVolume;

    private SharedPreferences defaultSharedPreferences;
    private boolean prefMaxVolume;
    private AudioManager audioManager;

    public JorSayReader() {
        super("JorSayReader");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        prefMaxVolume = defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_max_volume), false);
        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);
    }

    public static void startReadAloud(Context context) {
        Intent intent = new Intent(context, JorSayReader.class);
        intent.setAction(Constants.ACTION_MSG_START_READ_ALOUD);
        context.startService(intent);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if (Constants.ACTION_MSG_START_READ_ALOUD.equals(intent.getAction())) {
            Logger.d(this, "onHandleIntent TTS init");
            tts = new TextToSpeech(getApplicationContext(), this);
        }
    }

    @Override
    public void onInit(int status) {
        if (status == TextToSpeech.SUCCESS) {
            //TODO Change this to match user locale
            tts.setLanguage(Locale.US);
            Logger.d(this, "TTS ready");
            /*TODO
            Volume settings check needs to be done for each notification.
            If the user turns the volume off, we should abort immediately.
             */
            originalVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC);
            if (prefMaxVolume) {
                Logger.d(this, "setting volume to max");
                audioManager.setStreamVolume(AudioManager.STREAM_MUSIC,
                        audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC), 0);
            } else {
                if (originalVolume == 0) {
                    Logger.d(this, "Max volume isn't selected and device volume is at 0," +
                            "not reading aloud");
                    return;
                }
            }
            String toRead = SlayerApp.getInstance().getNotificationString();
            Logger.d(this, "TTS reading now");
            //Utterance ID should be unique per notification. Could be stored in the DB, or,
            //alternatively, could be DB row ID.
            String utteranceID = Long.toString(System.currentTimeMillis());
            tts.setOnUtteranceProgressListener(new JorSayReaderUtteranceProgressListener());
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                tts.speak(toRead,TextToSpeech.QUEUE_ADD, null, utteranceID);
            } else {
                HashMap<String, String> params = new HashMap<>();
                params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);
                tts.speak(toRead, TextToSpeech.QUEUE_ADD, params);
            }
        }
    }

    private void ttsDone() {
        Logger.d(this, "ttsDone shutting down");
        tts.shutdown();
        //After TTS is done, set volume back to original level
        Logger.d(this, "ttsDone restoring original volume");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_max_volume).equals(key)) {
            prefMaxVolume = defaultSharedPreferences.getBoolean(
                    getString(R.string.pref_key_max_volume), false);
        }

    }

    private class JorSayReaderUtteranceProgressListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
            Logger.d(this, "onStart utteranceId: " + utteranceId);
        }

        @Override
        public void onDone(String utteranceId) {
            ttsDone();
        }

        @Override
        public void onError(String utteranceId) {
            ttsDone();
        }
    }
}
