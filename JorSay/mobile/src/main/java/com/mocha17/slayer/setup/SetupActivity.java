package com.mocha17.slayer.setup;

import android.app.Dialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentSender;
import android.os.Bundle;
import android.speech.tts.TextToSpeech;
import android.support.v4.app.DialogFragment;
import android.support.v7.app.AppCompatActivity;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GooglePlayServicesUtil;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.common.api.GoogleApiClient.ConnectionCallbacks;
import com.google.android.gms.common.api.GoogleApiClient.OnConnectionFailedListener;
import com.google.android.gms.wearable.Wearable;
import com.mocha17.slayer.R;
import com.mocha17.slayer.SlayerApp;
import com.mocha17.slayer.utils.Logger;
import com.mocha17.slayer.MainActivity;


//https://developer.android.com/google/auth/api-client.html#Starting
public class SetupActivity extends AppCompatActivity
        implements ConnectionCallbacks, OnConnectionFailedListener {

    // Request code to use when launching the resolution activity
    private static final int REQUEST_RESOLVE_ERROR = 1001;
    // Unique tag for the error dialog fragment
    private static final String DIALOG_ERROR = "dialog_error";

    private static final String STATE_RESOLVING_ERROR = "resolving_error";

    private GoogleApiClient googleApiClient;

    private TextView progressText;

    private static final int TTS_DATA_CHECK_CODE = 1002;

    private enum STATE {
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
        SUCCESS;
    }
    private STATE state;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_setup);
        progressText = (TextView)findViewById(R.id.progressText);

        updateState(STATE.INIT);

        if (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false)) {
            updateState(STATE.GPS_RESOLVING_ERROR);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (state != STATE.GPS_RESOLVING_ERROR) {
            updateState(STATE.CHECKING_GPS);
        }
    }

    @Override
    protected void onStop() {
        updateState(STATE.GPS_DISCONNECTED);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
            updateState(STATE.GPS_ERROR_RESOLVED);
            if (resultCode == RESULT_OK) {
                // Make sure the app is not already connected or attempting to connect
                if (!googleApiClient.isConnecting() &&
                        !googleApiClient.isConnected()) {
                    updateState(STATE.CHECKING_GPS);
                }
            }
        } else if (requestCode == TTS_DATA_CHECK_CODE) {
            if (resultCode == TextToSpeech.Engine.CHECK_VOICE_DATA_PASS) {
                updateState(STATE.TTS_SUCCESS);
            } else {
                updateState(STATE.TTS_SETUP);
            }
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        if (state == STATE.GPS_RESOLVING_ERROR) {
            outState.putBoolean(STATE_RESOLVING_ERROR, true);
        }
    }

    //Handles UI updates and state transitions.
    private void updateState(STATE s) {
        state = s;
        switch(state) {
            case INIT:
                // Initialize GoogleApiClient instance
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApiIfAvailable(Wearable.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
                //Set it in Application class so that it can be shared with the Service
                //TODO This doesn't seem necessary
                SlayerApp.getInstance().setGoogleApiClient(googleApiClient);

                break;
            case CHECKING_GPS:
                progressText.setText(getString(R.string.progress_gps));
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
                //Start with checking TTS
                updateState(STATE.CHECKING_TTS);
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
                //This is same as overall setup success as of now.
            case SUCCESS:
                SlayerApp.getInstance().setTTSAvailable(true);
                Logger.v("SetupActivity GoogleAPIClient connected? " + googleApiClient.isConnected()
                        + " " + SlayerApp.getInstance().getGoogleApiClient().isConnected());
                Logger.v("SetupActivity GoogleAPIClient equals? " + googleApiClient + " "
                        + SlayerApp.getInstance().getGoogleApiClient());
                Toast.makeText(this, "Will read notifications \"JorSay\"", Toast.LENGTH_SHORT).show();
                //start MainActivity
                startActivity(new Intent(this, MainActivity.class));
                finish();
                break;
            default:
                break;
        }

    }

    @Override
    public void onConnectionFailed(ConnectionResult result) {
        if (result.getErrorCode() == ConnectionResult.API_UNAVAILABLE) {
            updateState(STATE.GPS_WEAR_UNAVAILABLE);
            return;
        }
        if (state == STATE.GPS_RESOLVING_ERROR) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                updateState(STATE.GPS_RESOLVING_ERROR);
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                updateState(STATE.CHECKING_GPS);
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            updateState(STATE.GPS_RESOLVING_ERROR);
        }
    }

    // The rest of this code is all about building the error dialog

    /* Creates a dialog for an error message */
    private void showErrorDialog(int errorCode) {
        // Create a fragment for the error dialog
        ErrorDialogFragment dialogFragment = new ErrorDialogFragment();
        // Pass the error that should be displayed
        Bundle args = new Bundle();
        args.putInt(DIALOG_ERROR, errorCode);
        dialogFragment.setArguments(args);
        dialogFragment.show(getSupportFragmentManager(), ErrorDialogFragment.class.getSimpleName());
    }

    @Override
    public void onConnected(Bundle bundle) {
        updateState(STATE.GPS_SUCCESS);
    }

    @Override
    public void onConnectionSuspended(int i) {
        updateState(STATE.GPS_SUSPENDED);
    }

    /* A fragment to display an error dialog */
    public static class ErrorDialogFragment extends DialogFragment {
        public ErrorDialogFragment() { }

        @Override
        public Dialog onCreateDialog(Bundle savedInstanceState) {
            // Get the error code and retrieve the appropriate dialog
            int errorCode = this.getArguments().getInt(DIALOG_ERROR);
            return GooglePlayServicesUtil.getErrorDialog(errorCode,
                    this.getActivity(), REQUEST_RESOLVE_ERROR);
        }

        @Override
        public void onDismiss(DialogInterface dialog) {
            ((SetupActivity)getActivity()).updateState(STATE.GPS_USER_REJECT);
        }
    }
}
