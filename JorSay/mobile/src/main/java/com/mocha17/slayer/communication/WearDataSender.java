package com.mocha17.slayer.communication;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.Status;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

import java.util.concurrent.TimeUnit;

/**
 * Created by Chaitanya on 6/11/15.
 */
public class WearDataSender extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;

    public WearDataSender() {
        super("WearDataSender");
    }

    //Convenience methods for callers
    public static void startShakeDetection(Context context) {
        Intent intent = new Intent(context, WearDataSender.class);
        intent.setAction(Constants.ACTION_MSG_START_SHAKE_DETECTION);
        context.startService(intent);
    }

    public static void setShakeIntensity(Context context, String shakeIntensity) {
        Intent intent = new Intent(context, WearDataSender.class);
        intent.setAction(Constants.ACTION_MSG_SET_SHAKE_INTENSITY);
        intent.putExtra(Constants.KEY_SHAKE_INTENSITY_VALUE, shakeIntensity);
        context.startService(intent);
    }

    public static void setShakeDuration(Context context, int shakeDuration) {
        Intent intent = new Intent(context, WearDataSender.class);
        intent.setAction(Constants.ACTION_MSG_SET_SHAKE_DURATION);
        intent.putExtra(Constants.KEY_SHAKE_DURATION_VALUE, shakeDuration);
        context.startService(intent);
    }

    @Override
    public void onCreate() {
        super.onCreate();
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .build();
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        String action = intent.getAction();
        if (Constants.ACTION_MSG_START_SHAKE_DETECTION.equals(action) ||
                Constants.ACTION_MSG_SET_SHAKE_INTENSITY.equals(action) ||
                Constants.ACTION_MSG_SET_SHAKE_DURATION.equals(action)) {
            if (!googleApiClient.isConnected()) {
                /* blockingConnect() allows us to park here till we have a connection, and then we
                do the actual work. It is safe to block because IntentService handles an intent
                using a worker thread. */
                Logger.d(this, "before blocking connect");
                googleApiClient.blockingConnect(Constants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
            }
            if (googleApiClient.isConnected()) {
                //do work
                PutDataMapRequest putDataMapReq = null;
                Logger.d(this, "GoogleApiClient connected, notifying Wear");
                if (Constants.ACTION_MSG_START_SHAKE_DETECTION.equals(action)) {
                    putDataMapReq = PutDataMapRequest.create(
                            Constants.PATH_MSG_START_SHAKE_DETECTION);
                } else if (Constants.ACTION_MSG_SET_SHAKE_INTENSITY.equals(action)) {
                    putDataMapReq = PutDataMapRequest.create(
                            Constants.PATH_MSG_SET_SHAKE_INTENSITY);
                    putDataMapReq.getDataMap().putString(Constants.KEY_SHAKE_INTENSITY_VALUE,
                            intent.getStringExtra(Constants.KEY_SHAKE_INTENSITY_VALUE));
                } else if (Constants.ACTION_MSG_SET_SHAKE_DURATION.equals(action)) {
                    putDataMapReq = PutDataMapRequest.create(Constants.PATH_MSG_SET_SHAKE_DURATION);
                    putDataMapReq.getDataMap().putInt(Constants.KEY_SHAKE_DURATION_VALUE,
                            intent.getIntExtra(Constants.KEY_SHAKE_DURATION_VALUE,
                                    Constants.SHAKE_DURATION_DEFAULT));
                }
                /*DataAPI isn't about message-passing in the traditional sense. We are basically
                modifying a shared, synced data store. The framework wouldn't (and shouldn't,
                under this model) invoke the 'data received' flow if there's no change. Putting
                a guaranteed unique value in the DataRequest ensures that there *is* a change.*/
                if (putDataMapReq != null) {
                    putDataMapReq.getDataMap().putLong(
                            Constants.KEY_TIMESTAMP, System.currentTimeMillis());
                    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                    DataApi.DataItemResult result =
                            Wearable.DataApi.putDataItem(googleApiClient, putDataReq).await();
                    Status resultStatus = result.getStatus();
                    if (resultStatus.isSuccess()) {
                        Logger.d(this, "successfully performed " + action);
                    } else {
                        Logger.d(this, "failed to perform " + action + "," +
                                " resultStatus: " + resultStatus.getStatusMessage());
                    }
                }
            }
        }

        //disconnect GoogleApiClient
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.unregisterConnectionFailedListener(this);
        googleApiClient.disconnect();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.d(this, "GoogleApliClient connected");
    }

    @Override
    public void onConnectionSuspended(int cause) {
        //1: CAUSE_SERVICE_DISCONNECTED
        //2: CAUSE_NETWORK_LOST - peer device connection is lost
        Logger.d(this, "GoogleApliClient connection suspended cause: " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.d(this, "GoogleApliClient connection failed: " + connectionResult);
    }
}