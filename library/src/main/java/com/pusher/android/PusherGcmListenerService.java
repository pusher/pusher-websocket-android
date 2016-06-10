package com.pusher.android;

import android.nfc.Tag;
import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class PusherGcmListenerService extends GcmListenerService {
    private static final String TAG = "PGCMListenerService";

    @Override
    public void onMessageReceived(String from, Bundle data) {
        String message = data.getString("message");
        Log.d(TAG, "PUSHER!!!");
        Log.d(TAG, "From: " + from);
        Log.d(TAG, "Message: " + message);

        if (from.startsWith("/topics/")) {
            // message received from some topic.
        } else {
            // normal downstream message.
        }

        // ...
    }
}
