package com.mocha17.slayer.tts;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.media.AudioManager;
import android.os.Build;
import android.os.Handler;
import android.os.HandlerThread;
import android.os.IBinder;
import android.os.Looper;
import android.os.Message;
import android.os.Process;
import android.preference.PreferenceManager;
import android.speech.tts.TextToSpeech;
import android.speech.tts.UtteranceProgressListener;
import android.support.annotation.Nullable;
import android.text.TextUtils;

import com.mocha17.slayer.R;
import com.mocha17.slayer.notification.db.NotificationDBContract.NotificationData;
import com.mocha17.slayer.notification.db.NotificationDBOps;
import com.mocha17.slayer.tts.snooze.SnoozeReadAloud;
import com.mocha17.slayer.utils.Logger;
import com.mocha17.slayer.utils.Utils;

import java.util.HashMap;
import java.util.Locale;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by Chaitanya on 5/21/15.
 */
/* JorSayReader used to be an IntentService, and was conceptually simpler - an Intent to start 'READ
 ALOUD', handled in onHandleIntent, with IntentService taking care of thread management. We have now
 made it a Service as an instance of IntentService is really about processing an intent - it stops
 when that is done. We need the ability to snooze reading aloud, which means we need more state than
 an IntentService.

 Snooze is more complicated than it looks. On a snooze, current ongoing reading aloud needs to stop,
 and no other notifications should be read aloud till snooze is active. We can no longer have TTS
 being initialized for every read-aloud, nor can we have a new UtteranceProgressListener for every
 read-aloud. These need to be service level, not task level.

 Why a started Service and not a Bound service? We do not need binding. This Service is intended to
 be one-shot from caller's perspective; it is started to read something aloud, and now the
 read-aloud can be snoozed. We don't need an ongoing session with caller and a ServiceConnection.

 In fact, onStartCommand() is a good mechanism for us. The Service is started, it will keep running
 until stopped, and can receive additional intents in onStartCommand().*/
public class JorSayReader extends Service implements TextToSpeech.OnInitListener,
        SharedPreferences.OnSharedPreferenceChangeListener {
    private static final String ACTION_MSG_START_READ_ALOUD =
            "com.mocha17.slayer.ACTION_MSG_START_READ_ALOUD";
    private static final String ACTION_SNOOZE_READ_ALOUD =
            "com.mocha17.slayer.JorSayReader.SNOOZE_READ_ALOUD";
    private static final int READ_ALOUD = 1;
    private static final int SNOOZE_READ_ALOUD = 2;

    private TextToSpeech tts;
    private AtomicBoolean ttsReady, readOnReady;

    private SharedPreferences defaultSharedPreferences;
    private boolean prefMaxVolume;
    private int originalVolume;
    private AudioManager audioManager;

    private Handler actionHandler;

    public static void startReadAloud(Context context) {
        Intent intent = new Intent(context, JorSayReader.class);
        intent.setAction(ACTION_MSG_START_READ_ALOUD);
        context.startService(intent);
    }

    public static void snoozeReadAloud(Context context) {
        Intent intent = new Intent(context, JorSayReader.class);
        intent.setAction(ACTION_SNOOZE_READ_ALOUD);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        defaultSharedPreferences.registerOnSharedPreferenceChangeListener(this);
        prefMaxVolume = defaultSharedPreferences.getBoolean(
                getString(R.string.pref_key_max_volume), false);

        audioManager = (AudioManager)getSystemService(Context.AUDIO_SERVICE);

        tts = new TextToSpeech(getApplicationContext(), JorSayReader.this);
        ttsReady = new AtomicBoolean();
        readOnReady = new AtomicBoolean();

        //Set up the Handler for processing requests on a background thread
        HandlerThread actionHandlerThread = new HandlerThread(ActionHandler.class.getSimpleName(),
                Process.THREAD_PRIORITY_BACKGROUND);
        actionHandlerThread.start();
        actionHandler = new ActionHandler(actionHandlerThread.getLooper());
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        String action = intent.getAction();
        Logger.d(this, "onStartCommand for intent " + intent.getAction());
        if (ACTION_MSG_START_READ_ALOUD.equals(action)) {
            actionHandler.sendMessage(actionHandler.obtainMessage(READ_ALOUD));
        } else if (ACTION_SNOOZE_READ_ALOUD.equals(action)) {
            actionHandler.sendMessage(actionHandler.obtainMessage(SNOOZE_READ_ALOUD));
        }
        return START_NOT_STICKY;
    }

    private final class ActionHandler extends Handler {
        public ActionHandler(Looper looper) {
            super(looper);
        }
        @Override
        public void handleMessage(Message msg) {
            if (READ_ALOUD == msg.what) {
                if (SnoozeReadAloud.get().isActive()) {
                    Logger.d(JorSayReader.this, "reading aloud is snoozed, returning");
                    return;
                }
                if (ttsReady.get()) {
                    readAloud();
                } else {
                    readOnReady.set(true);
                }
            } else if (SNOOZE_READ_ALOUD == msg.what) {
                if (tts != null) {
                    tts.stop();
                }
                //clear pending queue
                actionHandler.removeMessages(READ_ALOUD);
                actionHandler.removeMessages(SNOOZE_READ_ALOUD);
            }
        }
    }

    @Override
    public void onDestroy() {
        if (tts != null) {
            Logger.d(this, "onDestroy shutting down TTS");
            tts.shutdown();
        }
        super.onDestroy();
    }

    @Override
    public void onInit(int status) {
        if (TextToSpeech.SUCCESS == status) {
            Logger.d("onInit tts ready");
            //TODO use user's locale to find the closest match for TTS language
            tts.setLanguage(Locale.US);
            tts.setOnUtteranceProgressListener(new JorSayReaderUtteranceProgressListener());
            ttsReady.set(true);
            if (readOnReady.getAndSet(false)) {
                readAloud();
            }
        }
    }

    private void readAloud() {
        if (ttsReady.get()) {
            if (!SnoozeReadAloud.get().isActive()) {
            /*TODO
            Volume settings check needs to be done for each notification.
            If the user turns the volume off, we should abort immediately.*/
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
                Cursor notificationCursor = NotificationDBOps.get(this).getMostRecentNotification();
                if (notificationCursor != null) {
                    if (notificationCursor.moveToFirst()) {
                        Logger.d(this, "TTS reading now");
                        //Utterance ID should be unique per notification.
                        String utteranceID = Long.toString(System.currentTimeMillis());
                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                            tts.speak(getStringToRead(notificationCursor), TextToSpeech.QUEUE_ADD, null,
                                    utteranceID);
                        } else {
                            HashMap<String, String> params = new HashMap<>();
                            params.put(TextToSpeech.Engine.KEY_PARAM_UTTERANCE_ID, utteranceID);
                            //Turned off noinspection deprecation as we have a version check around this
                            tts.speak(getStringToRead(notificationCursor),
                                    TextToSpeech.QUEUE_ADD, params);
                        }
                        //Mark the notification as read
                        /*Retrieving the ID like this is a little tricky, the ID would be included
                        only when it was part of the columns in the query that generated this
                        Cursor. Else, it would be -1. Reference: stackoverflow.com/
                        questions/2848056/how-to-get-a-row-id-from-a-cursor*/
                        NotificationDBOps.get(this).markNotificationRead(notificationCursor.getLong(
                                notificationCursor.getColumnIndex(NotificationData._ID)));
                    }
                    notificationCursor.close();
                }
            }
        }
    }

    private String getStringToRead(Cursor cursor) {
        /*Why are we using Strings directly rather than defining them as static finals? Because...
        defining BY makes little sense, definition and string are not different here. The name for
        the variable isn't supposed to mean anything - it isn't a key, it isn't a path, it isn't an
        intent action etc. For the Strings used in this method, we would instead rely on GC to claim
        the memory back, rather than keeping something permanently occupied.
         */
        StringBuilder sb = new StringBuilder();

        String titleBig = cursor.getString(
                cursor.getColumnIndex(NotificationData.COLUMN_NAME_TITLE_BIG));
        String title = cursor.getString(cursor.getColumnIndex(NotificationData.COLUMN_NAME_TITLE));

        //Use TITLE_BIG if available, else TITLE
        //Also remove emoji symbols from title; we do not want them read aloud
        title = Utils.withoutEmoji((!TextUtils.isEmpty(titleBig))?titleBig:title);

        String appName = Utils.getAppName(cursor.getString(
                cursor.getColumnIndex(NotificationData.COLUMN_NAME_PACKAGE_NAME)));

        if (TextUtils.isEmpty(title) || appName.equals(title)) {
            sb.append(appName).append(".\n");
            String summary = cursor.getString(
                    cursor.getColumnIndex(NotificationData.COLUMN_NAME_SUMMARY));
            if (!TextUtils.isEmpty(summary)) {
                sb.append(summary).append(".\n");
            }
        } else {
            sb.append(appName).append(".\n").append(title).append(".\n");
        }

        String textLines = cursor.getString(
                cursor.getColumnIndex(NotificationData.COLUMN_NAME_TEXT_LINES));
        if (!TextUtils.isEmpty(textLines)) {
            sb.append(textLines).append(".");
        } else {
            //These checks are nested because we want to avoid getting data we are not going to use,
            //and we want to do isEmpty() checks too.
            String bigText = cursor.getString(
                    cursor.getColumnIndex(NotificationData.COLUMN_NAME_BIG_TEXT));
            if (!TextUtils.isEmpty(bigText)) {
                sb.append(bigText).append(".");
            } else {
                String text = cursor.getString(
                        cursor.getColumnIndex(NotificationData.COLUMN_NAME_TEXT));
                if (!TextUtils.isEmpty(text)) {
                    sb.append(text).append(".");
                } else {
                    String tickerText = cursor.getString(
                            cursor.getColumnIndex(NotificationData.COLUMN_NAME_TICKER_TEXT));
                    if (!TextUtils.isEmpty(tickerText)) {
                        sb.append(tickerText).append(".");
                    }
                }
            }
        }
        return sb.toString();
    }

    private void ttsDone() {
        //After TTS is done, set volume back to original level
        Logger.d(this, "ttsDone restoring original volume");
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, originalVolume, 0);
        SnoozeReadAloud.get().cancelNotification();
    }

    private class JorSayReaderUtteranceProgressListener extends UtteranceProgressListener {
        @Override
        public void onStart(String utteranceId) {
            //post this only when we actually start reading
            SnoozeReadAloud.get().postNotification();
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

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        if (getString(R.string.pref_key_max_volume).equals(key)) {
            prefMaxVolume = defaultSharedPreferences.getBoolean(
                    getString(R.string.pref_key_max_volume), false);
        }
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        //not needed
        return null;
    }
}