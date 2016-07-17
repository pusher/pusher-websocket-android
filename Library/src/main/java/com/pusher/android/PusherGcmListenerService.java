package com.pusher.android;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class PusherGcmListenerService extends GcmListenerService {
    private static final String TAG = "PGCMListenerService";
    private static PusherPushNotificationReceivedListener listener;

    static void setOnMessageReceivedListener(PusherPushNotificationReceivedListener messageReceivedListener) {
        listener = messageReceivedListener;
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "Received from GCM: " + data);
        if (listener != null) listener.onMessageReceived(from, data);
    }
}
