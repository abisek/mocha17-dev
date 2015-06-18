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
import com.mocha17.slayer.MainActivity;
import com.mocha17.slayer.R;


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
        progressText = (TextView)findViewById(R.id.progressText);

        updateState(State.INIT);

        if (savedInstanceState != null
                && savedInstanceState.getBoolean(STATE_RESOLVING_ERROR, false)) {
            updateState(State.GPS_RESOLVING_ERROR);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        if (state != State.GPS_RESOLVING_ERROR) {
            updateState(State.CHECKING_GPS);
        }
    }

    @Override
    protected void onStop() {
        updateState(State.GPS_DISCONNECTED);
        super.onStop();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == REQUEST_RESOLVE_ERROR) {
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
            outState.putBoolean(STATE_RESOLVING_ERROR, true);
        }
    }

    //Handles UI updates and state transitions.
    private void updateState(State s) {
        state = s;
        switch(state) {
            case INIT:
                // Initialize GoogleApiClient instance
                googleApiClient = new GoogleApiClient.Builder(this)
                        .addApiIfAvailable(Wearable.API)
                        .addConnectionCallbacks(this)
                        .addOnConnectionFailedListener(this)
                        .build();
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
                updateState(State.CHECKING_NOTIFICATION_ACCESS);
                break;
            case CHECKING_NOTIFICATION_ACCESS:

            case SUCCESS:
                Toast.makeText(
                        this, "Will read notifications \"JorSay\"", Toast.LENGTH_SHORT).show();
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
            updateState(State.GPS_WEAR_UNAVAILABLE);
            return;
        }
        if (state == State.GPS_RESOLVING_ERROR) {
            // Already attempting to resolve an error.
            return;
        } else if (result.hasResolution()) {
            try {
                updateState(State.GPS_RESOLVING_ERROR);
                result.startResolutionForResult(this, REQUEST_RESOLVE_ERROR);
            } catch (IntentSender.SendIntentException e) {
                // There was an error with the resolution intent. Try again.
                updateState(State.CHECKING_GPS);
            }
        } else {
            // Show dialog using GooglePlayServicesUtil.getErrorDialog()
            showErrorDialog(result.getErrorCode());
            updateState(State.GPS_RESOLVING_ERROR);
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
        updateState(State.GPS_SUCCESS);
    }

    @Override
    public void onConnectionSuspended(int i) {
        updateState(State.GPS_SUSPENDED);
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
            ((SetupActivity)getActivity()).updateState(State.GPS_USER_REJECT);
        }
    }
}
