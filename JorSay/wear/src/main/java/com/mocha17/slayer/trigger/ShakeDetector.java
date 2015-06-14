package com.mocha17.slayer.trigger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Vibrator;

import com.mocha17.slayer.communication.MobileDataSender;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;


/**
 * Created by Chaitanya on 5/19/15.
 */
public class ShakeDetector extends IntentService implements SensorEventListener {
    private static final int VIBRATE_DURATION_MILLI = 1000;

    private static final double MAX_ACCELERATION_THRESHOLD = 7.5;
    private static final int SHAKE_WINDOW_MILLI = 1000;
    private static final int SHAKE_COUNT = 2;

    private static final float ALPHA = 0.8f;
    private SensorManager sensorManager;
    private Sensor accelerometer;
    private long startTime, elapsedTime;
    private int shakeCounter;
    private boolean triggerMessageSent;

    public ShakeDetector() {
        super("ShakeDetector");
    }

    public static void startShakeDetection(Context context) {
        Intent intent = new Intent(context, ShakeDetector.class);
        intent.setAction(Constants.ACTION_START_SHAKE_DETECTION);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(Constants.ACTION_START_SHAKE_DETECTION.equals(intent.getAction())) {
            startMonitoringForShake();
            /* TODO
            For every shake detected, vibrate quickly like 300 ms.
            So that user knows that shakes are being registered. */
        }
    }

    private void startMonitoringForShake() {
        //Indicate to the user that shake monitoring has started and she can start triggering
        //events i.e. start shaking her wrist.
        indicateTriggerMonitoringStart();

        //register listener for Accelerometer events
        sensorManager.registerListener(this,
                accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void indicateTriggerMonitoringStart() {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(VIBRATE_DURATION_MILLI);
    }

    private void scheduleMonitoringStop() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                unregisterSensorListener();
            }
        };
        new Handler().postDelayed(r, Constants.SHAKE_MONITORING_DURATION_MILLI);
    }

    private void unregisterSensorListener() {
        Logger.d(this, "unregistering sensor listener");
        sensorManager.unregisterListener(this, accelerometer);
    }

    @Override
    public void onDestroy() {
        Logger.d(this, "onDestroy scheduling monitoring stop");
        scheduleMonitoringStop();
        super.onDestroy();
    }

    @Override
    public void onSensorChanged(SensorEvent event) {
        float [] currentAcceleration = getCurrentAcceleration(event);
        float maxAcceleration = getMaxAcceleration(currentAcceleration);
        if (maxAcceleration > MAX_ACCELERATION_THRESHOLD) {
            if (startTime == 0) {
                //startTime isn't initialized and this is first candidate sensorEvent
                startTime = System.currentTimeMillis();
                shakeCounter++;
                Logger.d(this, "onSensorChanged shakeCounter init "
                        + shakeCounter + ", max: " + maxAcceleration);
            } else {
                elapsedTime = System.currentTimeMillis() - startTime;
                if (elapsedTime < SHAKE_WINDOW_MILLI) {
                    Logger.d(this, "onSensorChanged elapsedTime: " +
                            elapsedTime + ", max: " + maxAcceleration);
                    if (++shakeCounter >= SHAKE_COUNT) {
                        Logger.d("**************** SHAKE!!! ****************");
                        shakeCounter = 0;
                        elapsedTime = 0;
                        if (!triggerMessageSent) {
                            //Do this only once.
                            //such safeguards are needed when working with Sensors.
                            MobileDataSender.sendReadAloud(ShakeDetector.this);
                            unregisterSensorListener();
                            triggerMessageSent = true;
                        }
                        return;
                    }
                    Logger.d(this, "onSensorChanged shakeCounter: " + shakeCounter);
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
        //not needed
    }
}