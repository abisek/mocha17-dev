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
    public static final int SHAKE_MONITORING_DURATION_MILLI = 20000; //20 seconds
    public static final String ACTION_START_SHAKE_DETECTION =
            "com.mocha17.slayer.ACTION_START_SHAKE_DETECTION";
    public static final String ACTION_SET_SHAKE_INTENSITY =
            "com.mocha17.slayer.ACTION_SHAKE_INTENSITY";
    public static final String ACTION_SET_SHAKE_DURATION =
            "com.mocha17.slayer.ACTION_SHAKE_DURATION";
    public static final String PATH_MSG_START_SHAKE_DETECTION = "/start_shake_detection";
    public static final String PATH_MSG_SET_SHAKE_INTENSITY = "/set_shake_intensity";
    public static final String KEY_SHAKE_INTENSITY_VALUE = "shake_intensity_value";
    public static final String PATH_MSG_SET_SHAKE_DURATION = "/set_shake_duration";
    public static final String KEY_SHAKE_DURATION_VALUE = "shake_duration_value";
    public static final String SHAKE_INTENSITY_LOW = "SHAKE_INTENSITY_LOW";
    public static final String SHAKE_INTENSITY_MED = "SHAKE_INTENSITY_MED";
    public static final String SHAKE_INTENSITY_HIGH = "SHAKE_INTENSITY_HIGH";
    public static final float SHAKE_INTENSITY_LOW_VALUE = 4f;
    public static final float SHAKE_INTENSITY_MED_VALUE = 7f;
    public static final float SHAKE_INTENSITY_HIGH_VALUE = 11f;
    public static final float SHAKE_INTENSITY_DEFAULT = SHAKE_INTENSITY_MED_VALUE;

    //read aloud
    public static final String ACTION_MSG_READ_ALOUD =
            "com.mocha17.slayer.ACTION_MSG_READ_ALOUD";
    public static final String PATH_MSG_READ_ALOUD = "/jorsay";
}