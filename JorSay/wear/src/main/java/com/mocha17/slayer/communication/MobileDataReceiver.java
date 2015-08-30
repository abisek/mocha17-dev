package com.mocha17.slayer.communication;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.mocha17.slayer.trigger.ShakeDetector;
import com.mocha17.slayer.utils.Logger;

/**
 * Receives 'start_trigger_detection' message and starts trigger detection.
 * <br>Created by Chaitanya on 5/13/15.
 */

public class MobileDataReceiver extends WearableListenerService {
    private static final String PATH_MSG_START_SHAKE_DETECTION = "/start_shake_detection";
    private static final String PATH_MSG_SET_SHAKE_INTENSITY = "/set_shake_intensity";
    private static final String KEY_SHAKE_INTENSITY_VALUE = "shake_intensity_value";
    private static final String PATH_MSG_SET_SHAKE_DURATION = "/set_shake_duration";
    private static final String KEY_SHAKE_DURATION_VALUE = "shake_duration_value";

    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //We do not need dataEvents outside this scope, freeze() isn't needed
        for (DataEvent event : dataEvents) {
            String path = event.getDataItem().getUri().getLastPathSegment();
            Logger.v(this, "onDataChanged URI lastPath: " + path);
            if (PATH_MSG_START_SHAKE_DETECTION.contains(path)) {
                Logger.d(this, "onDataChanged starting shake detection");
                ShakeDetector.startShakeDetection(this);
                break; //we do not consider multiple nodes yet (Issue #36)
            } else if (PATH_MSG_SET_SHAKE_INTENSITY.contains(path)) {
                Logger.d(this, "onDataChanged setting shake intensity");
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                ShakeDetector.setShakeIntensity(this,
                        dataMap.getString(KEY_SHAKE_INTENSITY_VALUE));
                break;
            } else if (PATH_MSG_SET_SHAKE_DURATION.contains(path)) {
                Logger.d(this, "onDataChanged setting shake duration");
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                ShakeDetector.setShakeDuration(this,
                        dataMap.getInt(KEY_SHAKE_DURATION_VALUE));
                break;
            }
        }

        Logger.d(this, "releasing data buffer");
        dataEvents.release();
    }
}