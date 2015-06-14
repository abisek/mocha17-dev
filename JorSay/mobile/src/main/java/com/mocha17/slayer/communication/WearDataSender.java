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

    //A convenience method for callers
    //This is a good pattern, but it makes more sense when this method has more to do - setting
    //intent extras for example.
    public static void startShakeDetection(Context context) {
        Intent intent = new Intent(context, WearDataSender.class);
        intent.setAction(Constants.ACTION_MSG_START_SHAKE_DETECTION);
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
        if (Constants.ACTION_MSG_START_SHAKE_DETECTION.equals(intent.getAction())) {
            if (!googleApiClient.isConnected()) {
                /* blockingConnect() allows us to park here till we have a connection, and then we
                do the actual work. It is safe to block because IntentService handles an intent
                using a worker thread. */
                Logger.d(this, "before blocking connect");
                googleApiClient.blockingConnect(Constants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
            }
            if (googleApiClient.isConnected()) {
                //do work
                Logger.d(this, "GoogleApiClient connected, notifying Wear");
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(
                        Constants.PATH_MSG_START_SHAKE_DETECTION);
                /*DataAPI isn't about message-passing in the traditional sense. We are basically
                modifying a shared, synced data store. The framework wouldn't (and shouldn't,
                under this model) invoke the 'data received' flow if there's no change. Putting
                a guaranteed unique value in the DataRequest ensures that there *is* a change.*/
                putDataMapReq.getDataMap().putLong(
                        Constants.KEY_TIMESTAMP, System.currentTimeMillis());
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                DataApi.DataItemResult result =
                        Wearable.DataApi.putDataItem(googleApiClient, putDataReq).await();
                Status resultStatus = result.getStatus();
                if (resultStatus.isSuccess()) {
                    Logger.d(this, "successfully sent 'start shake detection' message");
                } else {
                    Logger.d(this, "failed to send 'start shake detection' message," +
                            " resultStatus: " + resultStatus.getStatusMessage());
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