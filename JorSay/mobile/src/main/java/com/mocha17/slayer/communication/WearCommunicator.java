package com.mocha17.slayer.communication;

import android.app.Notification;
import android.app.NotificationManager;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.ResultCallback;
import com.google.android.gms.common.data.FreezableUtils;
import com.google.android.gms.wearable.CapabilityApi;
import com.google.android.gms.wearable.CapabilityInfo;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.Node;
import com.google.android.gms.wearable.Wearable;
import com.google.android.gms.wearable.WearableListenerService;
import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.tts.JorSayReader;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

import java.util.List;
import java.util.Set;

/**
 * Created by mocha on 5/13/15.
 */

public class WearCommunicator extends WearableListenerService implements
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener,
        DataApi.DataListener {
    private static final String READ_ALOUD_MESSAGE_PATH = "/read-aloud";
    private static final String CAPABILITY_READ_ALOUD_NOTIFICATIONS = "capability_read_aloud_notification";

    private static final int NOTIFICATION_ID = 4321;

    private GoogleApiClient googleApiClient;

    private Node connectedNode;

    private boolean readAloudStarted;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.v("WearCommunicatorHandheld onCreate");
        if (!googleApiClient.isConnected()) {
            googleApiClient.registerConnectionCallbacks(this);
            googleApiClient.registerConnectionFailedListener(this);
            googleApiClient.connect();
        }
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Logger.v("WearCommunicatorHandheld onDataChanged: " + dataEvents);

        //Get a frozen copy of the buffer
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : events) {
            Uri uri = event.getDataItem().getUri();
            Logger.v("WearCommunicatorHandheld uri: " + uri.getLastPathSegment());
            if (READ_ALOUD_MESSAGE_PATH.contains(uri.getLastPathSegment())) {
                Logger.v("WearCommunicatorHandheld got read_aloud, data: "
                        + new String(event.getDataItem().getData()));
                readAloud();
            }
            /*Uri uri = event.getDataItem().getUri();
            // Get the node id from the host value of the URI
            String nodeId = uri.getHost();*/
        }

        Logger.v("WearCommunicatorHandheld releasing data buffer");
        dataEvents.release();
    }

    private void readAloud() {
        if (!readAloudStarted) {
            Logger.v("WearCommunicator reading aloud");
            Intent intent = new Intent(this, JorSayReader.class);
            intent.setAction(Constants.ACTION_READ_ALOUD);
            startService(intent);
            readAloudStarted = true;
        }
    }

    @Override
    public void onConnectedNodes(List<Node> connectedNodes) {
        Logger.v("WearCommunicator onConnectedNodes");
        // After we are notified by this callback, we need to query for the nodes that provide the
        // "capability_read_aloud_notification" and are directly connected.
        if (googleApiClient.isConnected()) {
            updateCapabilityInfo1();
        } else if (!googleApiClient.isConnecting()) {
            Logger.v("WearCommunicator onConnectedNodes calling googleApiClient.connect");
            googleApiClient.connect();
        }
    }

    private void updateCapabilityInfo1() {

        Wearable.CapabilityApi.getCapability(
                googleApiClient, CAPABILITY_READ_ALOUD_NOTIFICATIONS,
                CapabilityApi.FILTER_REACHABLE).setResultCallback(
                new ResultCallback<CapabilityApi.GetCapabilityResult>() {
                    @Override
                    public void onResult(CapabilityApi.GetCapabilityResult result) {
                        if (result.getStatus().isSuccess()) {
                            Logger.v("updateCapabilityInfo1() calling updateReadAloudCapability");
                            updateCapabilityInfo2(result.getCapability());
                        } else {
                            Logger.v("updateCapabilityInfo1() Failed to get capabilities, "
                                    + "status: "
                                    + result.getStatus().getStatusMessage());
                        }
                    }
                });
    }

    private void updateCapabilityInfo2(CapabilityInfo capabilityInfo) {
        Set<Node> connectedNodes = capabilityInfo.getNodes();
        if (connectedNodes.isEmpty()) {
            Logger.v("updateCapabilityInfo2 lost connectivity");
            notifyLostConnectivity();
        } else {
            for (Node node : connectedNodes) {
                // we are only considering those nodes that are directly connected
                if (node.isNearby()) {
                    //We got a nearby node with READ_ALOUD capability. Start monitoring for shake
                    connectedNode = node;
                    Logger.v("updateCapabilityInfo2 got a node!");
                    SlayerApp.getInstance().setConnectedNodeId(connectedNode.getId());
                }
            }
        }
    }

    private void notifyLostConnectivity() {
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.not_connected_title))
                .setContentText(getString(R.string.not_connected_desc))
                .setVibrate(new long[]{0, 200})  // Vibrate for 200 milliseconds.
                .setSmallIcon(R.mipmap.ic_launcher)
                .setPriority(Notification.PRIORITY_MAX);
        Notification notification = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, notification);
    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.v("WearCommunicatorHandheld GoogleApiClient onConnected");
        Wearable.DataApi.addListener(googleApiClient, this);
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Logger.v("WearCommunicatorHandheld GoogleApiClient onConnectionSuspended");
    }

    @Override
    public void onDestroy() {
        Logger.v("WearCommunicatorHandheld onDestroy");
        if (googleApiClient.isConnected() || googleApiClient.isConnecting()) {
            googleApiClient.unregisterConnectionCallbacks(this);
            googleApiClient.disconnect();
        }
        Wearable.DataApi.removeListener(googleApiClient, this);
        super.onDestroy();

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {

    }
}
