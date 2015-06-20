package com.mocha17.slayer.setup;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.provider.Settings;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.TextUtils;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.Wearable;
import com.mocha17.slayer.MainActivity;
import com.mocha17.slayer.R;
import com.mocha17.slayer.utils.Logger;

//https://developer.android.com/google/auth/api-client.html#Starting
public class SetupActivity extends AppCompatActivity
        implements ConnectionCallbacks, OnConnectionFailedListener {

    private TextView progressText;

    private GoogleApiClient googleApiClient;
    // Request code to use when launching the resolution activity
    private static final int GPS_REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String GPS_ERROR_DIALOG = "dialog_error";
    private static final String GPS_STATE_RESOLVING_ERROR = "resolving_error";

    private static final int TTS_DATA_CHECK_CODE = 1002;

    private AlertDialog notificationSettingsDialog;

    private enum State {
        INIT,
        CHECKING_GPS,
        GPS_RESOLVING_ERROR,
        GPS_ERROR_RESOLVED,
        GPS_USER_REJECT,
        GPS_WEAR_UNAVAILABLE,
        GPS_SUCCESS,
        GPS_SUSPENDED,
        GPS_DISCONNECTED,
        CHECKING_TTS,
        TTS_SETUP,
        TTS_USER_REJECT,
        TTS_SUCCESS,
        CHECKING_NOTIFICATION_ACCESS,
        SUCCESS;
    }
    private State state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        Toolbar toolbar = (Toolbar) findViewById(R.id.app_toolbar_setup);
        if (toolbar != null) {
            setSupportActionBar(toolbar);
        }

        progressText = (TextView) findViewById(R.id.progressText);

        updateState(State.INIT);

        if (savedInstanceState != null
                && savedInstanceState.getBoolean(GPS_STATE_RESOLVING_ERROR, false)) {
            updateState(State.GPS_RESOLVING_ERROR);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        /* Do nothing if we are GPS_RESOLVING_ERROR. Else, if we are INIT, start with CHECKING_GPS.
        If we are not INIT, restore the State.
         */
        if (state != State.GPS_RESOLVING_ERROR) {
            if (state == State.INIT) {
                Logger.d(this, "onResume updating state to " + State.CHECKING_GPS);
                updateState(State.CHECKING_GPS);
            } else {
                Logger.d(this, "onResume updating state to " + state);
                updateState(state);
            }
        }
    }

    @Override
    protected void onPause() {
        perhapsDismissNotificationSettingDialog();
        super.onPause();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == GPS_REQUEST_RESOLVE_ERROR) {
            updateState(State.GPS_ERROR_RESOLVED);
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleApiClient.isConnecting() &&
                        !googleApiClient.isConnected()) {
                    updateState(State.CHECKING_GPS);
                }
            }
        } else if (requestCode == TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                updateState(State.TTS_SUCCESS);
            } else {
                updateState(State.TTS_SETUP);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (state == State.GPS_RESOLVING_ERROR) {
            outState.putBoolean(GPS_STATE_RESOLVING_ERROR, true);
        }
    }

    //Handles UI updates and state transitions.
    private void updateState(State s) {
        state = s;
        Logger.d(this, "updateState: " + state);
        switch (state) {
            case INIT:
                break;
            case CHECKING_GPS:
                progressText.setText(getString(R.string.progress_gps));
                // Initialize GoogleApiClient instance
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApiIfAvailable(Wearable.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                //connect
                googleApiClient.connect();
                break;
            case GPS_RESOLVING_ERROR:
                break;
            case GPS_ERROR_RESOLVED:
                break;
            case GPS_USER_REJECT:
                break;
            case GPS_WEAR_UNAVAILABLE:
                break;
            case GPS_SUCCESS:
                Logger.d(this, "GPS_SUCCESS, disconnecting and starting with TTS");
                //Disconnect GPS
                updateState(State.GPS_DISCONNECTED);
                //Start with checking TTS
                updateState(State.CHECKING_TTS);
                break;
            case GPS_SUSPENDED:
                break;
            case GPS_DISCONNECTED:
                googleApiClient.unregisterConnectionCallbacks(this);
                googleApiClient.unregisterConnectionFailedListener(this);
                googleApiClient.disconnect();
                break;
            case CHECKING_TTS:
                progressText.setText(getString(R.string.progress_tts));
                Intent checkIntent = new Intent();
                checkIntent.setAction(TextToSpeech.Engine.ACTION_CHECK_TTS_DATA);
                startActivityForResult(checkIntent, TTS_DATA_CHECK_CODE);
                break;
            case TTS_SETUP:
                // missing TTS speech data, install it
                Intent installIntent = new Intent();
                installIntent.setAction(
                        TextToSpeech.Engine.ACTION_INSTALL_TTS_DATA);
                startActivity(installIntent);
                break;
            case TTS_USER_REJECT:
                break;
            case TTS_SUCCESS:
                Logger.d(this, "updateState TTS_SUCCESS starting with NOTIFICATION_ACCESS");
                updateState(State.CHECKING_NOTIFICATION_ACCESS);
                break;
            case CHECKING_NOTIFICATION_ACCESS:
                progressText.setText(getString(R.string.progress_notifications));

                /* Reference:
                http://grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/
                android/5.1.0_r1/android/provider/Settings.java#
                Settings.Secure.0ENABLED_NOTIFICATION_LISTENERS and
                http://stackoverflow.com/questions/18813321/is-there-a-way-an-app-can-check-if-it-is
                -allowed-to-access-notifications*/
                String enabledListeners = Settings.Secure.getString(getContentResolver(),
                        "enabled_notification_listeners");

                if (TextUtils.isEmpty(enabledListeners) ||
                        !enabledListeners.contains(getPackageName())) {
                    //JorSay doesn't have access to Notifications
                    perhapsShowNotificationSettingDialog();
                } else {
                    //JorSay has notifications access, go to Success
                    updateState(State.SUCCESS);
                }
                break;
            case SUCCESS:
                //start MainActivity
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
            default:
                break;
        }
    }

    private void perhapsShowNotificationSettingDialog() {
        //This check fixed the android.view.WindowLeaked problem
        if (notificationSettingsDialog != null && notificationSettingsDialog.isShowing()) {
            Logger.d(this, "perhapsShowNotificationSettingDialog - already showing, returning");
            return;
        }
       /*Reference:
        stackoverflow.com/questions/17861979/
        accessing-android-notificationlistenerservice-settings

        grepcode.com/file/repository.grepcode.com/java/ext/com.google.android/android/5.1.0_r1/
        android/provider/Settings.java#Settings.0ACTION_NOTIFICATION_LISTENER_SETTINGS*/
        Intent intent = new Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS");
        if (intent.resolveActivity(getPackageManager()) != null) {
            //intent can resolve
            notificationSettingsDialog = getNotificationSettingsDialog(intent);
        } else {
            //intent cannot resolve
            notificationSettingsDialog = getErrorDialog();
        }
        notificationSettingsDialog.show();
    }

    private void perhapsDismissNotificationSettingDialog() {
        if (notificationSettingsDialog != null && notificationSettingsDialog.isShowing()) {
            notificationSettingsDialog.dismiss();
        }
    }

    //Calling a dismiss() before finish() avoids android.view.WindowLeaked problem
    private void dismissDialogAndFinish() {
        notificationSettingsDialog.dismiss();
        finish();
    }

    private AlertDialog getNotificationSettingsDialog(final Intent notificationSettingsIntent) {
        return new AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setTitle(R.string.notification_access_dialog_title)
                .setMessage(R.string.notification_access_dialog_text)
                .setPositiveButton(R.string.notification_access_dialog_ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                startActivity(notificationSettingsIntent);
                            }
                        }).setNegativeButton(android.R.string.cancel,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismissDialogAndFinish();
                            }
                        }).create();
    }

    private AlertDialog getErrorDialog() {
        return new AlertDialog.Builder(this, R.style.AppDialogTheme)
                .setTitle(R.string.notification_access_dialog_title)
                .setMessage(R.string.notification_access_dialog_error_text)
                .setPositiveButton(android.R.string.ok,
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dismissDialogAndFinish();
                            }
                        }).create();
    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            updateState(State.GPS_WEAR_UNAVAILABLE);
            return;
        }
        if (state == State.GPS_RESOLVING_ERROR) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                updateState(State.GPS_RESOLVING_ERROR);
                result.startResolutionForResult(this, GPS_REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                updateState(State.CHECKING_GPS);
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showGPSErrorDialog(result.getErrorCode());
            updateState(State.GPS_RESOLVING_ERROR);
        }
    }

    @Override
    public void onConnected(Bundle bundle) {
        updateState(State.GPS_SUCCESS);
    }

    @Override
    public void onConnectionSuspended(int i) {
        updateState(State.GPS_SUSPENDED);
    }

    /* Creates a dialog for a GP error message */
    private void showGPSErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        GPSErrorDialogFragment gpsErrorDialogFragment = new GPSErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(GPS_ERROR_DIALOG, errorCode);
        gpsErrorDialogFragment.setArguments(args);
        gpsErrorDialogFragment.show(getSupportFragmentManager(),
                GPSErrorDialogFragment.class.getSimpleName());
    }

    /* A fragment to display GPS error dialog */
    public static class GPSErrorDialogFragment extends DialogFragment {
        public GPSErrorDialogFragment() {
        }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(GPS_ERROR_DIALOG);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), GPS_REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((SetupActivity) getActivity()).updateState(State.GPS_USER_REJECT);
        }
    }
}