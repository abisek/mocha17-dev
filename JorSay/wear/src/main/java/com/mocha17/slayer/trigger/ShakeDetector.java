package com.mocha17.slayer.trigger;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.os.Handler;
import android.os.Vibrator;
import android.preference.PreferenceManager;

import com.mocha17.slayer.communication.MobileDataSender;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;


/**
 * Created by Chaitanya on 5/19/15.
 */
public class ShakeDetector extends IntentService implements SensorEventListener {
    private static final int START_STOP_VIBRATE_DURATION_MILLI = 400;
    private static final int SHAKE_VIBRATE_DURATION_MILLI = 700;

    private static final String PREF_SHAKE_THRESHOLD = "pref_shake_threshold";
    private static final String PREF_SHAKE_DURATION = "pref_shake_duration";

    private SharedPreferences defaultSharedPreferences;
    private static SharedPreferences.Editor editor;

    private SensorManager sensorManager;
    private Sensor accelerometer;

    private float currX, currY, currZ;
    private float prevX, prevY, prevZ;

    private static float shakeThreshold = Constants.SHAKE_INTENSITY_DEFAULT;
    private static int shakeDurationMilli = Constants.SHAKE_MONITORING_DURATION_MILLI;
    private int shakeCount = 0;
    private int TOTAL_SHAKES = 2;
    private boolean triggerMessageSent = false;

    public ShakeDetector() {
        super("ShakeDetector");
    }

    public static void startShakeDetection(Context context) {
        Intent intent = new Intent(context, ShakeDetector.class);
        intent.setAction(Constants.ACTION_START_SHAKE_DETECTION);
        context.startService(intent);
    }

    public static void setShakeIntensity(String shakeIntensity) {
        if (Constants.SHAKE_INTENSITY_LOW.equals(shakeIntensity)) {
            shakeThreshold = Constants.SHAKE_INTENSITY_LOW_VALUE;
        } else if (Constants.SHAKE_INTENSITY_MED.equals(shakeIntensity)) {
            shakeThreshold = Constants.SHAKE_INTENSITY_MED_VALUE;
        } else if (Constants.SHAKE_INTENSITY_HIGH.equals(shakeIntensity)) {
            shakeThreshold = Constants.SHAKE_INTENSITY_HIGH_VALUE;
        }
        editor.putFloat(PREF_SHAKE_THRESHOLD, shakeThreshold).apply();
        Logger.d("ShakeDetector shakeThreshold set to " + shakeThreshold);
    }

    public static void setShakeDuration(int shakeDuration) {
        shakeDurationMilli = shakeDuration*1000;
        editor.putInt(PREF_SHAKE_DURATION, shakeDurationMilli).apply();
        Logger.d("ShakeDetector shakeDuration set to " + shakeDurationMilli);
    }

    @Override
    public void onCreate() {
        super.onCreate();

        sensorManager = (SensorManager)getSystemService(Context.SENSOR_SERVICE);
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);

        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        editor = defaultSharedPreferences.edit();
        shakeThreshold = defaultSharedPreferences.getFloat(
                PREF_SHAKE_THRESHOLD, Constants.SHAKE_INTENSITY_DEFAULT);
        shakeDurationMilli = defaultSharedPreferences.getInt(
                PREF_SHAKE_DURATION, Constants.SHAKE_MONITORING_DURATION_MILLI);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        if(Constants.ACTION_START_SHAKE_DETECTION.equals(intent.getAction())) {
            startMonitoringForShake();
        }
    }

    private void startMonitoringForShake() {
        //Indicate to the user that shake monitoring has started and she can start triggering
        //events i.e. start shaking her wrist.
        performUserIndication(START_STOP_VIBRATE_DURATION_MILLI);

        //register listener for Accelerometer events
        sensorManager.registerListener(this,
                accelerometer, SensorManager.SENSOR_DELAY_NORMAL);
    }

    private void performUserIndication(int shakeDurationMilli) {
        Vibrator vibrator = (Vibrator) this.getSystemService(Context.VIBRATOR_SERVICE);
        vibrator.vibrate(shakeDurationMilli);
    }

    private void scheduleMonitoringStop() {
        Runnable r = new Runnable() {
            @Override
            public void run() {
                //We would like to vibrate to signal end-of-monitoring, but in practice,
                //this is just distracting and feels very out of place.
                //performUserIndication(START_STOP_VIBRATE_DURATION_MILLI);
                unregisterSensorListener();
            }
        };
        new Handler().postDelayed(r, shakeDurationMilli);
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
        updateAccelerationParameters(event);
        if (accelerationChanged()) {
            if (++shakeCount == TOTAL_SHAKES) {
                Logger.d("*********************** SHAKE!!! ***********************");
                if (!triggerMessageSent) {
                    performUserIndication(SHAKE_VIBRATE_DURATION_MILLI);
                    //Do this only once.
                    //such safeguards are needed when working with Sensors.
                    MobileDataSender.sendReadAloud(ShakeDetector.this);
                    unregisterSensorListener();
                    triggerMessageSent = true;
                    return;
                }
            }
        }
    }

    private void updateAccelerationParameters(SensorEvent event) {
        prevX = currX;
        prevY = currY;
        prevZ = currZ;

        currX = event.values[0];
        currY = event.values[1];
        currZ = event.values[2];
    }

    private boolean accelerationChanged() {
        float deltaX = Math.abs(currX - prevX);
        float deltaY = Math.abs(currY - prevY);
        float deltaZ = Math.abs(currZ - prevZ);
        if (((deltaX > shakeThreshold) && (deltaY > shakeThreshold)) ||
                ((deltaY > shakeThreshold) && (deltaZ > shakeThreshold)) ||
                ((deltaX > shakeThreshold) && (deltaZ > shakeThreshold))) {
            Logger.d("accelerationChanged "
                    + deltaX + " " + deltaY + " " + deltaZ);
            return true;
        }
        return false;
    }

    @Override
    public void onAccuracyChanged(Sensor sensor, int accuracy) {
        //not needed
    }
}