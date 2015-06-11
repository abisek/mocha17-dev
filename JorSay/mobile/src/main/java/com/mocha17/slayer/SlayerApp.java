package com.mocha17.slayer;

import android.app.Application;

import com.google.android.gms.common.api.GoogleApiClient;
import com.mocha17.slayer.utils.Logger;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mocha on 5/2/15.
 */
public class SlayerApp extends Application {
    private static SlayerApp instance;

    private AtomicBoolean isTTSAvailable = new AtomicBoolean();

    private GoogleApiClient googleApiClient;

    public String getNotificationString() {
        return notificationString;
    }

    public void setNotificationString(String notificationString) {
        this.notificationString = notificationString;
    }

    private String notificationString;

    public String getConnectedNodeId() {
        return connectedNodeId;
    }

    public void setConnectedNodeId(String connectedNodeId) {
        this.connectedNodeId = connectedNodeId;
    }

    private String connectedNodeId;


    @Override
    public void onCreate() {
        super.onCreate();
        synchronized (SlayerApp.class) {
            instance = this;
        }
    }

    public static SlayerApp getInstance() {
        synchronized (SlayerApp.class) {
            return instance;
        }
    }

    public void setTTSAvailable(boolean value) {
        isTTSAvailable.compareAndSet(false, value);
        Logger.v("SlayerApp isTTSAvailable: " + getTTSAvailable());
    }

    public boolean getTTSAvailable() {
        return isTTSAvailable.get();
    }

    public GoogleApiClient getGoogleApiClient() {
        return googleApiClient;
    }

    public void setGoogleApiClient(GoogleApiClient client) {
        googleApiClient = client;
    }
}
