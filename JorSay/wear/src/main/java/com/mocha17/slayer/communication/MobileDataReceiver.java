package com.mocha17.slayer.communication;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.DataMap;
import com.google.android.gms.wearable.DataMapItem;
import com.google.android.gms.wearable.WearableListenerService;
import com.mocha17.slayer.trigger.ShakeDetector;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

/**
 * Receives 'start_trigger_detection' message and starts trigger detection.
 * <br>Created by Chaitanya on 5/13/15.
 */

public class MobileDataReceiver extends WearableListenerService {
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //We do not need dataEvents outside this scope, freeze() isn't needed
        for (DataEvent event : dataEvents) {
            String path = event.getDataItem().getUri().getLastPathSegment();
            Logger.v(this, "onDataChanged URI lastPath: " + path);
            if (Constants.PATH_MSG_START_SHAKE_DETECTION.contains(path)) {
                Logger.d(this, "onDataChanged starting shake detection");
                ShakeDetector.startShakeDetection(this);
                break; //we do not consider multiple nodes yet (Issue #36)
            } else if (Constants.PATH_MSG_SET_SHAKE_INTENSITY.contains(path)) {
                Logger.d(this, "onDataChanged setting shake intensity");
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                ShakeDetector.setShakeIntensity(
                        dataMap.getString(Constants.KEY_SHAKE_INTENSITY_VALUE));
                break;
            } else if (Constants.PATH_MSG_SET_SHAKE_DURATION.contains(path)) {
                Logger.d(this, "onDataChanged setting shake duration");
                DataMap dataMap = DataMapItem.fromDataItem(event.getDataItem()).getDataMap();
                ShakeDetector.setShakeDuration(
                        dataMap.getInt(Constants.KEY_SHAKE_DURATION_VALUE));
                break;
            }
        }

        Logger.d(this, "releasing data buffer");
        dataEvents.release();
    }
}