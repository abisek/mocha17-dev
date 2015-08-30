package com.mocha17.slayer.utils;

/**
 * Created by Chaitanya on 5/2/15.
 */
public class Constants {
    //timeout for GoogleApiClient blocking connect() calls
    /*We are working near-realtime on the notifications received. Waiting longer than this wouldn't
    make much sense */
    public static final int CONNECT_TIMEOUT = 5000; //5 seconds

    public static final String KEY_TIMESTAMP = "key_timestamp";

    //shake
    public static final String ACTION_MSG_START_SHAKE_DETECTION =
            "com.mocha17.slayer.ACTION_MSG_START_SHAKE_DETECTION";
    public static final String PATH_MSG_START_SHAKE_DETECTION = "/start_shake_detection";

    //shake intensity
    public static final String ACTION_MSG_SET_SHAKE_INTENSITY =
            "com.mocha17.slayer.ACTION_MSG_SET_SHAKE_INTENSITY";
    public static final String PATH_MSG_SET_SHAKE_INTENSITY = "/set_shake_intensity";
    public static final String KEY_SHAKE_INTENSITY_VALUE = "shake_intensity_value";
    public static final String SHAKE_INTENSITY_LOW = "SHAKE_INTENSITY_LOW";
    public static final String SHAKE_INTENSITY_MED = "SHAKE_INTENSITY_MED";
    public static final String SHAKE_INTENSITY_HIGH = "SHAKE_INTENSITY_HIGH";
    public static final String SHAKE_INTENSITY_DEFAULT = SHAKE_INTENSITY_MED;

    //shake duration
    public static final String ACTION_MSG_SET_SHAKE_DURATION =
            "com.mocha17.slayer.ACTION_MSG_SET_SHAKE_DURATION";
    public static final String PATH_MSG_SET_SHAKE_DURATION = "/set_shake_duration";
    public static final String KEY_SHAKE_DURATION_VALUE = "shake_duration_value";
    public static final int SHAKE_DURATION_LOW = 10;
    public static final int SHAKE_DURATION_MED = 20;
    public static final int SHAKE_DURATION_HIGH = 30;
    public static final int SHAKE_DURATION_DEFAULT = SHAKE_DURATION_MED;

    //read aloud
    public static final String ACTION_MSG_START_READ_ALOUD =
            "com.mocha17.slayer.ACTION_MSG_START_READ_ALOUD";
    public static final String PATH_MSG_READ_ALOUD = "/jorsay";
    public static final int REQUEST_CODE_SNOOZE_READ_ALOUD = 1005;

    public static final int REQUEST_CODE_SHOW_MAIN_SCREEN = 1002;
    //Persistent notification
    public static final int PERSISTENT_NOTIFICATION_ID = 1001;
    //Other notifications
    public static final int NOTIFICATION_ID_VOLUME = 1003;
    public static final int NOTIFICATION_ID_READING_ALOUD = 1004;

    //For Status animation
    public static final int STATUS_ANIMATION_REPEAT_COUNT = 3;
    public static final long STATUS_ANIMATION_DELAY_MILLI = 500;
}