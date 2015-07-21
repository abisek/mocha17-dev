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
    public static final int SHAKE_MONITORING_DURATION_MILLI = 15000; //15 seconds
    public static final String PATH_MSG_START_SHAKE_DETECTION = "/start_shake_detection";
    public static final String ACTION_START_SHAKE_DETECTION =
            "com.mocha17.slayer.ACTION_START_SHAKE_DETECTION";

    //read aloud
    public static final String ACTION_MSG_READ_ALOUD =
            "com.mocha17.slayer.ACTION_MSG_READ_ALOUD";
    public static final String PATH_MSG_READ_ALOUD = "/jorsay";
}