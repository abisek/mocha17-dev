package com.mocha17.slayer.backend;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.os.Vibrator;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;


/**
 * Created by mocha on 5/19/15.
 */
public class TriggerMonitor extends IntentService implements SensorEventListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener{
    private static final int VIBRATE_DURATION_MILLI = 1000;

    private static final int STOP_MONITORING = 77;
    private static final int MONITORING_DURATION_MILLI = 10 * 1000; //a minute

    private static final int SEND_TRIGGER = 99;
    private static final int SEND_TRIGGER_AFTER_MILLI = 5 * 1000;

    private static final int START_MONITORING = 121;
    private static final int START_MONITORING_DELAY = 1 * 1000;

    private static final String READ_ALOUD_MESSAGE_PATH = "/read-aloud";
    private static final String READ_ALOUD_MESSAGE = "read-aloud";

    private Handler handler = new MessageHandler();

    private GoogleApiClient googleApiClient;

    private static final double MAX_ACCELERATION_THRESHOLD = 7.5;
    private static final int SHAKE_WINDOW_MILLI = 1000;
    private static final int SHAKE_COUNT = 2;

    private static final float ALPHA = 0.8f;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long startTime, elapsedTime;
    private int shakeCounter;
    private boolean triggerMessageSent;

    private TriggerMonitor thisInstance;

    public TriggerMonitor() {
        super("TriggerMonitor");
    }

    @Override
    public void onCreate() {
        super.onCreate();

        thisInstance = this;

        googleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        googleApiClient.connect();

        if(Constants.INTENT_START_TRIGGER_MONITORING.equals(intent.getAction())) {
            Logger.v("TriggerMonitor onHandleIntent got intent");
            startMonitoring();
            scheduleMonitoringStop();
            //TODO For every shake detected, vibrate quickly like 300 ms. So that user knows that shakes are being registered.
        }
    }

    private void startMonitoring() {
        shakeCounter = 0;
        startTime = 0;
        triggerMessageSent = false;
        indicateTriggerMonitoringStart();
        handler.sendMessageDelayed(handler.obtainMessage(START_MONITORING), START_MONITORING_DELAY);
        scheduleMonitoringStop();
    }

    private void indicateTriggerMonitoringStart() {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATE_DURATION_MILLI);
    }

    private void sendTriggerMessage() {
        if (!triggerMessageSent) {
            Logger.v("TriggerMonitor sending trigger message");
            handler.sendMessage(handler.obtainMessage(SEND_TRIGGER));
            triggerMessageSent = true;
            indicateTriggerMonitoringStart();
        } else {
            Logger.v("TriggerMonitor already sent trigger message");
        }
    }

    private void sendTriggerMessage(final String msg) {
        new AsyncTask<Void, Void, Void>() {
            @Override
            protected Void doInBackground(Void... params) {
                Logger.v("Activity sendMessageAsync with timestamp");
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(READ_ALOUD_MESSAGE_PATH);
                putDataMapReq.getDataMap().putString(READ_ALOUD_MESSAGE, msg);
                putDataMapReq.getDataMap().putLong("TIMESTAMP", System.currentTimeMillis());
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                /*PendingResult<DataApi.DataItemResult> pendingResult =
                        Wearable.DataApi.putDataItem(googleApiClient, putDataReq);
                pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
                    @Override
                    public void onResult(final DataApi.DataItemResult result) {
                        Logger.v("WearCommunicator sendMessage1 onResult: " + result);
                        if(result.getStatus().isSuccess()) {
                            Logger.v("WearCommunicator sendMessage1 onResult Data item set: " + result.getDataItem().getUri());
                        }
                    }
                });*/
                DataApi.DataItemResult result = Wearable.DataApi.putDataItem(googleApiClient, putDataReq).await();
                Logger.v("Activity sendMessageAsync onResult: " + result.getStatus().getStatusMessage() + ", " + result.getStatus());
                if(result.getStatus().isSuccess()) {
                    Logger.v("Activity sendMessageAsync onResult Data item set: " + result.getDataItem().getUri());
                }
                Logger.v("Activity sendMessageAsync with timestamp returning");
                return null;
            }
        }.execute();
    }

    private void scheduleMonitoringStop() {
        Logger.v("TriggerMonitor sending stopMonitoring message");
        handler.sendMessageDelayed(handler.obtainMessage(STOP_MONITORING),
                MONITORING_DURATION_MILLI);
    }

    private void stopService() {
        Logger.v("TriggerMonitor stopping self");
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.unregisterConnectionFailedListener(this);
        googleApiClient.disconnect();

        sensorManager.unregisterListener(this, accelerometer);

        stopSelf();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float [] currentAcceleration = getCurrentAcceleration(event);
        float maxAcceleration = getMaxAcceleration(currentAcceleration);
        /*Logger.v("TriggerMonitor onSensorChanged currentAcceleration: "
                + Arrays.toString(currentAcceleration) + ", maxAcceleration: " + maxAcceleration);*/
        //Logger.v("TriggerMonitor onSensorChanged maxAcceleration: " + maxAcceleration);
        if (maxAcceleration > MAX_ACCELERATION_THRESHOLD) {

            if (startTime == 0) {
                //startTime isn't initialized and this is first candidate sensorEvent
                startTime = System.currentTimeMillis();
                shakeCounter++;
                Logger.v("TriggerMonitor onSensorChanged shakeCounter init " + shakeCounter + ", max: " + maxAcceleration);
            } else {
                elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime < SHAKE_WINDOW_MILLI) {
                    Logger.v("TriggerMonitor onSensorChanged elapsedTime: " + elapsedTime + ", max: " + maxAcceleration);
                    if (++shakeCounter >= SHAKE_COUNT) {
                        Logger.v("**************** SHAKE!!! ****************");
                        shakeCounter = 0;
                        elapsedTime = 0;
                        sendTriggerMessage();
                        return;
                    }
                    Logger.v("TriggerMonitor onSensorChanged shakeCounter: " + shakeCounter);
                }
            }
        }

    }

    private float getMaxAcceleration(float [] acceleration) {
        float temp = Math.max(acceleration[0], acceleration[1]);
        return Math.max(temp, acceleration[2]);
    }

    private float[] getCurrentAcceleration(SensorEvent event) {
        float[] gravity = new float[3];
        float[] linear_acceleration = new float[3];

        // Isolate the force of gravity with the low-pass filter.
        gravity[0] = ALPHA * gravity[0] + (1 - ALPHA) * event.values[0];
        gravity[1] = ALPHA * gravity[1] + (1 - ALPHA) * event.values[1];
        gravity[2] = ALPHA * gravity[2] + (1 - ALPHA) * event.values[2];

        // Remove the gravity contribution with the high-pass filter.
        linear_acceleration[0] = event.values[0] - gravity[0];
        linear_acceleration[1] = event.values[1] - gravity[1];
        linear_acceleration[2] = event.values[2] - gravity[2];

        return linear_acceleration;

    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {

    }

    //Primarily for stopping trigger monitoring
    private class MessageHandler extends Handler {

        @Override
        public void handleMessage(Message message) {
            switch (message.what) {
                case START_MONITORING:
                    sensorManager.registerListener(thisInstance,
                            accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
                    Logger.v("TriggerMonitor Sensor listener registered");
                    break;
                case STOP_MONITORING:
                    Logger.v("TriggerMonitor handleMessage STOP_MONITORING");
                    stopService();
                    break;
                case SEND_TRIGGER:
                    Logger.v("TriggerMonitor handleMessage SEND_TRIGGER");
                    sendTriggerMessage("From TriggerMonitor on Wear!");
                    break;
                default:
                    break;
            }
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.v("TriggerMonitor onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.v("TriggerMonitor onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.v("TriggerMonitor onConnectionFailed");
    }
}
