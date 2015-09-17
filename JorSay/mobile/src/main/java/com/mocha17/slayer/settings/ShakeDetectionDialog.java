package com.mocha17.slayer.settings;

import android.app.Activity;
import android.app.DialogFragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TextView;

import com.mocha17.slayer.R;
import com.mocha17.slayer.communication.WearDataSender;
import com.mocha17.slayer.utils.Constants;

/**
 * Created by Chaitanya on 7/23/15.
 */
public class ShakeDetectionDialog extends DialogFragment
        implements RadioGroup.OnCheckedChangeListener, View.OnClickListener {
    //Shake intensity
    private static final String SHAKE_INTENSITY_LOW = "SHAKE_INTENSITY_LOW";
    private static final String SHAKE_INTENSITY_MED = "SHAKE_INTENSITY_MED";
    private static final String SHAKE_INTENSITY_HIGH = "SHAKE_INTENSITY_HIGH";
    private static final String SHAKE_INTENSITY_DEFAULT = SHAKE_INTENSITY_MED;

    private SharedPreferences defaultSharedPreferences;
    private SharedPreferences.Editor editor;

    private RadioGroup shakeIntensity, shakeDuration;
    private TextView shakeIntensityTitle, shakeDurationTitle;
    private String shakeIntensityVal;
    private int shakeDurationVal;

    private Button ok, cancel;

    public static ShakeDetectionDialog newInstance() {
        return new ShakeDetectionDialog();
    }

    public ShakeDetectionDialog() {
        // Required empty public constructor
    }

    @Override
    public void onAttach(Activity activity) {
        super.onAttach(activity);
        defaultSharedPreferences = PreferenceManager.getDefaultSharedPreferences(activity);
        editor = defaultSharedPreferences.edit();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.dialog_shake_detection, container, false);

        shakeIntensity = (RadioGroup) view.findViewById(R.id.shake_intensity_choice);
        shakeIntensity.setOnCheckedChangeListener(this);
        shakeDuration = (RadioGroup) view.findViewById(R.id.shake_duration_choice);
        shakeDuration.setOnCheckedChangeListener(this);
        RadioButton shakeDurationLow = (RadioButton) view.findViewById(R.id.shake_duration_low);
        shakeDurationLow.setText(
                getString(R.string.shake_duration_value, Constants.SHAKE_DURATION_LOW));
        RadioButton shakeDurationMed = (RadioButton) view.findViewById(R.id.shake_duration_med);
        shakeDurationMed.setText(
                getString(R.string.shake_duration_value, Constants.SHAKE_DURATION_MED));
        RadioButton shakeDurationHigh = (RadioButton) view.findViewById(R.id.shake_duration_high);
        shakeDurationHigh.setText(
                getString(R.string.shake_duration_value, Constants.SHAKE_DURATION_HIGH));

        shakeIntensityTitle = (TextView) view.findViewById(R.id.shake_intensity_title);
        shakeDurationTitle = (TextView) view.findViewById(R.id.shake_duration_title);

        ok = (Button) view.findViewById(R.id.ok_button);
        ok.setOnClickListener(this);
        cancel = (Button) view.findViewById(R.id.cancel_button);
        cancel.setOnClickListener(this);

        //Set UI state
        if (savedInstanceState != null) {
            shakeIntensityVal = savedInstanceState.getString(
                    getString(R.string.pref_key_shake_intensity), SHAKE_INTENSITY_DEFAULT);
            shakeDurationVal = savedInstanceState.getInt(
                    getString(R.string.pref_key_shake_duration), Constants.SHAKE_DURATION_DEFAULT);
        } else { //obtain values from SharedPreferences
            shakeIntensityVal =
                    defaultSharedPreferences.getString(getString(R.string.pref_key_shake_intensity),
                            SHAKE_INTENSITY_DEFAULT);
            shakeDurationVal =
                    defaultSharedPreferences.getInt(getString(R.string.pref_key_shake_duration),
                            Constants.SHAKE_DURATION_DEFAULT);
        }
        //Once the required details are available, set UI State
        updateUIState();

        return view;
    }

    @Override
    public void onSaveInstanceState (Bundle outState) {
        outState.putString(getString(R.string.pref_key_shake_intensity), shakeIntensityVal);
        outState.putInt(getString(R.string.pref_key_shake_duration), shakeDurationVal);

        super.onSaveInstanceState(outState);
    }

    private void updateUIState() {
        if (SHAKE_INTENSITY_LOW.equals(shakeIntensityVal)) {
            shakeIntensity.check(R.id.shake_intensity_low);
        } else if (SHAKE_INTENSITY_MED.equals(shakeIntensityVal)) {
            shakeIntensity.check(R.id.shake_intensity_med);
        } else if (SHAKE_INTENSITY_HIGH.equals(shakeIntensityVal)) {
            shakeIntensity.check(R.id.shake_intensity_high);
        }

        if (Constants.SHAKE_DURATION_LOW == shakeDurationVal) {
            shakeDuration.check(R.id.shake_duration_low);
        } else if (Constants.SHAKE_DURATION_MED == shakeDurationVal) {
            shakeDuration.check(R.id.shake_duration_med);
        } else if (Constants.SHAKE_DURATION_HIGH == shakeDurationVal) {
            shakeDuration.check(R.id.shake_duration_high);
        }
    }

    @Override
    public void onCheckedChanged(RadioGroup group, int checkedId) {
        if (group.equals(shakeIntensity)) {
            switch (checkedId) {
                case R.id.shake_intensity_low:
                    shakeIntensityVal = SHAKE_INTENSITY_LOW;
                    break;
                case R.id.shake_intensity_med:
                    shakeIntensityVal = SHAKE_INTENSITY_MED;
                    break;
                case R.id.shake_intensity_high:
                    shakeIntensityVal = SHAKE_INTENSITY_HIGH;
                    break;
                default:
                    shakeIntensityVal = SHAKE_INTENSITY_DEFAULT;
            }
        } else if (group.equals(shakeDuration)) {
            switch (checkedId) {
                case R.id.shake_duration_low:
                    shakeDurationVal = Constants.SHAKE_DURATION_LOW;
                    break;
                case R.id.shake_duration_med:
                    shakeDurationVal = Constants.SHAKE_DURATION_MED;
                    break;
                case R.id.shake_duration_high:
                    shakeDurationVal = Constants.SHAKE_DURATION_HIGH;
                    break;
                default:
                    shakeDurationVal = Constants.SHAKE_DURATION_DEFAULT;
            }
        }
    }

    @Override
    public void onClick(View v) {
        switch (v.getId()) {
            case R.id.ok_button:
                //save values
                editor.putString(getString(R.string.pref_key_shake_intensity),
                        shakeIntensityVal).apply();
                editor.putInt(getString(R.string.pref_key_shake_duration),
                        shakeDurationVal).apply();
                //send data to Wear
                WearDataSender.setShakeIntensity(getActivity(), shakeIntensityVal);
                WearDataSender.setShakeDuration(getActivity(), shakeDurationVal);

                //fall-through to dismiss the Dialog
            case R.id.cancel_button:
                getDialog().dismiss();
                break;
        }
    }
}