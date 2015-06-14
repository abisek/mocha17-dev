package com.mocha17.slayer.communication;

import android.net.Uri;

import com.google.android.gms.wearable.DataEvent;
import com.google.android.gms.wearable.DataEventBuffer;
import com.google.android.gms.wearable.WearableListenerService;
import com.mocha17.slayer.tts.JorSayReader;
import com.mocha17.slayer.utils.Constants;
import com.mocha17.slayer.utils.Logger;

/**
 * Receives 'read_aloud' message and starts read_aloud.
 * <br>Created by Chaitanya on 5/13/15.
 */

public class WearDataReceiver extends WearableListenerService {
    @Override
    public void onDataChanged(DataEventBuffer dataEvents) {
        //We do not need dataEvents outside this scope, freeze() isn't needed
        for (DataEvent event : dataEvents) {
            Uri uri = event.getDataItem().getUri();
            Logger.v(this, "onDataChanged URI lastPath: " + uri.getLastPathSegment());
            if (Constants.PATH_MSG_READ_ALOUD.contains(uri.getLastPathSegment())) {
                Logger.d(this, "onDataChanged starting read aloud");
                JorSayReader.startReadAloud(this);
                break; //we do not consider multiple nodes yet (Issue #36)
            }
        }

        Logger.d(this, "releasing data buffer");
        dataEvents.release();
    }
}