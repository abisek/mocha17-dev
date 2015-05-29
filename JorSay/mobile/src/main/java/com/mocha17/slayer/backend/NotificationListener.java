package com.mocha17.slayer.backend;

import android.app.Notification;
import android.app.Service;
import android.content.Intent;
import android.os.Bundle;
import android.service.notification.NotificationListenerService;
import android.service.notification.StatusBarNotification;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.PendingResult;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.etc.Constants;
import com.mocha17.slayer.etc.Logger;

/**
 * Created by mocha on 5/2/15.
 */
public class NotificationListener extends NotificationListenerService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {
    private LocalBroadcastManager localBroadcastManager;

    GoogleApiClient googleApiClient;

    private static final String NOTIFICATION_RECEIVED_MESSAGE_PATH = "/notification-received";
    private static final String NOTIFICATION_KEY = "notification_key";

    @Override
    public void onCreate() {
        super.onCreate();
        localBroadcastManager = LocalBroadcastManager.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        super.onStartCommand(intent, flags, startId);

        googleApiClient = SlayerApp.getInstance().getGoogleApiClient();
        if (!googleApiClient.isConnected()) {
            googleApiClient.registerConnectionCallbacks(this);
            googleApiClient.registerConnectionFailedListener(this);
            googleApiClient.connect();
        }

        return Service.START_NOT_STICKY;
    }

    @Override
    public void onDestroy() {
        Logger.v("NotificationListener onDestroy");

        if (googleApiClient.isConnected()) {
            googleApiClient.unregisterConnectionCallbacks(this);
            googleApiClient.unregisterConnectionFailedListener(this);
            googleApiClient.disconnect();
        }
        super.onDestroy();
    }

    @Override
    public void onNotificationPosted(StatusBarNotification statusBarNotification) {

        //TODO remove this. This is only for displaying notification text in UI which isn't needed
        Intent notifBroadcastIntent = new Intent(Constants.BROADCAST_NOTIF);
        notifBroadcastIntent.putExtra(Constants.KEY_DETAILS, getNotifString(statusBarNotification));
        notifBroadcastIntent.putExtra(Constants.KEY_ADDED, true/*added*/);
        Logger.v("onNotificationPosted: " + statusBarNotification.toString());
        localBroadcastManager.sendBroadcast(notifBroadcastIntent);

        //TODO replace this with a DB. We are recording the last notification posted using the App instance
        SlayerApp.getInstance().setNotificationString(getNotifString(statusBarNotification));

        sendMessage(getNotifString(statusBarNotification));
    }

    @Override
    public void onNotificationRemoved(StatusBarNotification statusBarNotification) {
        Intent notifBroadcastIntent = new Intent(Constants.BROADCAST_NOTIF);
        notifBroadcastIntent.putExtra(Constants.KEY_DETAILS, getNotifString(statusBarNotification));
        notifBroadcastIntent.putExtra(Constants.KEY_ADDED, false/*removed*/);
        Logger.v("onNotificationRemoved: " + statusBarNotification.toString());
        localBroadcastManager.sendBroadcast(notifBroadcastIntent);
    }

    private static String getNotifString(StatusBarNotification sbn) {
        //String ticker = sbn.getNotification().tickerText.toString();
        Bundle extras = sbn.getNotification().extras;
        StringBuilder sb = new StringBuilder();
        sb.append("Package: ").append(sbn.getPackageName())
                .append(", Title: ").append(extras.getString(Notification.EXTRA_TITLE)).append(", Text: ").
                append(extras.getCharSequence(Notification.EXTRA_TEXT));
        String bigText = extras.getString(Notification.EXTRA_BIG_TEXT);
        if (!TextUtils.isEmpty(bigText)) {
            sb.append("Big text: ").append(bigText);
        }
        return sb.toString();
    }


    //Using the Message API
    /*public void sendMessage(String message) {
        if (SlayerApp.getInstance().getConnectedNodeId() != null) {
            Wearable.MessageApi.sendMessage(googleApiClient, SlayerApp.getInstance().getConnectedNodeId(),
                    NOTIFICATION_RECEIVED_MESSAGE_PATH, message.getBytes()).setResultCallback(
                    new ResultCallback() {
                        @Override
                        public void onResult(Result result) {
                            if (result.getStatus().isSuccess()) {
                                Logger.v("Sending message was successful");
                            } else {
                                Logger.v("Sending message failed");
                            }
                        }
                    }
            );
        } else {
            // Unable to retrieve node with transcription capability
        }

    }*/

    public void sendMessage(String msg) {
        Logger.v("NotificationListener sendMessage1");
        PutDataMapRequest putDataMapReq = PutDataMapRequest.create(NOTIFICATION_RECEIVED_MESSAGE_PATH);
        putDataMapReq.getDataMap().putString(NOTIFICATION_KEY, msg);
        PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
        PendingResult<DataApi.DataItemResult> pendingResult =
                Wearable.DataApi.putDataItem(googleApiClient, putDataReq);
        pendingResult.setResultCallback(new ResultCallback<DataApi.DataItemResult>() {
            @Override
            public void onResult(final DataApi.DataItemResult result) {
                Logger.v("NotificationListener sendMessage1 onResult: " + result);
                if (result.getStatus().isSuccess()) {
                    Logger.v("NotificationListener sendMessage1 onResult " +
                            "Data item set: " + result.getDataItem().getUri());
                }
            }
        });
    }


    @Override
    public void onConnected(Bundle bundle) {
        Logger.v("GoogleApiClient onConnected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.v("GoogleApiClient onConnectionSuspended");
    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.v("GoogleApiClient onConnectionFailed");
    }
}