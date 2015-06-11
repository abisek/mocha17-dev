package com.mocha17.slayer.backend;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Toast;

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
import com.mocha17.slayer.WearMainActivity;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

import java.util.List;
import java.util.Set;

/**
 * Created by Chaitanya on 5/13/15.
 */

public class WearCommunicator extends WearableListenerService implements GoogleApiClient.ConnectionCallbacks,
        DataApi.DataListener {
    private static final String NOTIFICATION_RECEIVED_MESSAGE_PATH = "/notification-received";

    private static final String CAPABILITY_READ_ALOUD_NOTIFICATIONS = "capability_read_aloud_notification";
    private static final int NOTIFICATION_ID = 4321;

    private GoogleApiClient googleApiClient;

    private Node connectedNode;

    @Override
    public void onCreate() {
        super.onCreate();

        Logger.v("WearCommunicator onCreate");
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
    }

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        Logger.v("WearCommunicator onDataChanged: " + dataEvents);

        //Get a frozen copy of the buffer
        final List<DataEvent> events = FreezableUtils
                .freezeIterable(dataEvents);

        // Loop through the events and send a message
        // to the node that created the data item.
        for (DataEvent event : events) {
            Logger.v("WearCommunicator dataItem: " + event.getDataItem());
            Uri uri = event.getDataItem().getUri();
            Logger.v("WearCommunicator Uri lastPath is: " + uri.getLastPathSegment());
            //if (NOTIFICATION_RECEIVED_MESSAGE_PATH.equals(uri.getLastPathSegment())) {
            if (NOTIFICATION_RECEIVED_MESSAGE_PATH.contains(uri.getLastPathSegment())) {
                Logger.v("WearCommunicator got notification_received, data: "
                        + new String(event.getDataItem().getData()));
                Logger.v("WearCommunicator starting trigger monitoring");
                startTriggerMonitoring();
                //startUserInteraction();
            }

            // Get the node id from the host value of the URI
            String nodeId = uri.getHost();


        }

        Logger.v("WearCommunicator releasing data buffer");
                dataEvents.release();
    }

    public void startTriggerMonitoring() {
        Intent intent = new Intent(this, TriggerMonitor.class);
        intent.setAction(Constants.INTENT_START_TRIGGER_MONITORING);
        startService(intent);
    }

    public void startUserInteraction() {
        Intent intent = new Intent(this, WearMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        this.startActivity(intent);
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
                            Logger.v("updateCapabilityInfo1() calling updateCapabilityInfo2");
                            updateCapabilityInfo2(result.getCapability());
                        } else {
                            Logger.v("updateCapabilityInfo1 Failed to get capabilities, "
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
                    //We got a nearby node with READ_ALOUD capability.
                    Logger.v("updateCapabilityInfo2 got a node!");
                    connectedNode = node;
                }
            }
        }
    }

    private void notifyUserAction() {
        Intent intent = new Intent(this, WearMainActivity.class);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(this, 0, intent, 0);
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle("Send reply")
                .setVibrate(new long[]{0, 200})  // Vibrate for 200 milliseconds.
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MAX)
                .addAction(R.mipmap.ic_launcher, getString(R.string.send_message), pendingIntent);
        Notification card = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, card);
    }

    private void notifyLostConnectivity() {
        Notification.Builder notificationBuilder = new Notification.Builder(this)
                .setContentTitle(getString(R.string.not_connected_title))
                .setContentText(getString(R.string.not_connected_desc))
                .setVibrate(new long[]{0, 200})  // Vibrate for 200 milliseconds.
                .setSmallIcon(R.mipmap.ic_launcher)
                .setLocalOnly(true)
                .setPriority(Notification.PRIORITY_MAX);
        Notification card = notificationBuilder.build();
        ((NotificationManager) getSystemService(NOTIFICATION_SERVICE))
                .notify(NOTIFICATION_ID, card);
        //card.notify();
    }

    @Override
    public void onConnected(Bundle bundle) {
        Logger.v("WearCommunicator GoogleApiClient onConnected added new listeners");
        Wearable.DataApi.addListener(googleApiClient, this);
        Toast.makeText(this, "READY!!", Toast.LENGTH_LONG).show();
    }

    @Override
    public void onConnectionSuspended(int cause) {
        Logger.v("WearCommunicator GoogleApiClient onConnectionSuspended");
    }

    @Override
    public void onDestroy() {
        Logger.v("WearCommunicator onDestroy");
        if (googleApiClient.isConnected() || googleApiClient.isConnecting()) {
            googleApiClient.unregisterConnectionCallbacks(this);
            googleApiClient.disconnect();
        }
        Wearable.DataApi.removeListener(googleApiClient, this);
        super.onDestroy();
    }
}
