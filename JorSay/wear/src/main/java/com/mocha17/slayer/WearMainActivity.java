package com.mocha17.slayer;

import android.app.Activity;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.wearable.view.WatchViewStub;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.wearable.DataApi;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;
import com.mocha17.slayer.backend.TriggerMonitor;
import com.mocha17.slayer.etc.Constants;
import com.mocha17.slayer.etc.Logger;

public class WearMainActivity extends Activity implements View.OnClickListener,
        GoogleApiClient.ConnectionCallbacks, GoogleApiClient.OnConnectionFailedListener {


    private static final String READ_ALOUD_MESSAGE_PATH = "/read-aloud";
    private static final String READ_ALOUD_MESSAGE = "read-aloud";

    private TextView mTextView;
    private Button button;
    private GoogleApiClient googleApiClient;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_wear_main);
        final WatchViewStub stub = (WatchViewStub) findViewById(R.id.watch_view_stub);
        stub.setOnLayoutInflatedListener(new WatchViewStub.OnLayoutInflatedListener() {
            @Override
            public void onLayoutInflated(WatchViewStub stub) {
                mTextView = (TextView) stub.findViewById(R.id.text);
                button = (Button) stub.findViewById(R.id.button);
                if (button != null) {
                    button.setOnClickListener(WearMainActivity.this);
                }
            }
        });
        googleApiClient = new GoogleApiClient.Builder(this)
                .addApiIfAvailable(Wearable.API)
                .addConnectionCallbacks(this)
                .build();
    }

    @Override
    public void onResume() {
        super.onResume();
        googleApiClient.connect();
    }

    @Override
    public void onPause() {
        super.onPause();
        googleApiClient.unregisterConnectionCallbacks(this);
        googleApiClient.unregisterConnectionFailedListener(this);
        googleApiClient.disconnect();
    }
    @Override
    public void onClick(View v) {
        if (v.getId() == R.id.button) {
            //sendMessageAsync("From Wear!");
            startTriggerMonitoring();
        }
    }

    public void startTriggerMonitoring() {
        Intent intent = new Intent(this, TriggerMonitor.class);
        intent.setAction(Constants.INTENT_START_TRIGGER_MONITORING);
        startService(intent);
    }

    private void sendMessageAsync(final String msg) {
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
    @Override
    public void onConnected(Bundle bundle) {
        Logger.v("Activity GoogleApiClient connected");
    }

    @Override
    public void onConnectionSuspended(int i) {
        Logger.v("Activity GoogleApiClient onConnectionSuspended");

    }

    @Override
    public void onConnectionFailed(ConnectionResult connectionResult) {
        Logger.v("Activity GoogleApiClient onConnectionFailed");
    }
}

