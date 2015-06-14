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
public class MobileDataSender extends IntentService implements GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener {

    private GoogleApiClient googleApiClient;

    public MobileDataSender() {
        super("MobileDataSender");
    }

    //A convenience method for callers
    public static void sendReadAloud(Context context) {
        Intent intent = new Intent(context, MobileDataSender.class);
        intent.setAction(Constants.ACTION_MSG_READ_ALOUD);
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
        if (Constants.ACTION_MSG_READ_ALOUD.equals(intent.getAction())) {
            if (!googleApiClient.isConnected()) {
                Logger.d(this, "before blocking connect");
                googleApiClient.blockingConnect(Constants.CONNECT_TIMEOUT, TimeUnit.MILLISECONDS);
            }
            if (googleApiClient.isConnected()) {
                //do work - send 'read aloud' message
                Logger.d(this, "GoogleApiClient connected, notifying mobile");
                PutDataMapRequest putDataMapReq = PutDataMapRequest.create(
                        Constants.PATH_MSG_READ_ALOUD);
                putDataMapReq.getDataMap().putLong(
                        Constants.KEY_TIMESTAMP, System.currentTimeMillis());
                PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                DataApi.DataItemResult result =
                        Wearable.DataApi.putDataItem(googleApiClient, putDataReq).await();
                Status resultStatus = result.getStatus();
                if (resultStatus.isSuccess()) {
                    Logger.d(this, "successfully sent 'read aloud' message");
                } else {
                    Logger.d(this, "failed to send 'read aloud' message," +
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
        Logger.d(this, "GoogleApliClient connection suspended " + cause);
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.d(this, "GoogleApliClient connection failed " + connectionResult);
    }
}