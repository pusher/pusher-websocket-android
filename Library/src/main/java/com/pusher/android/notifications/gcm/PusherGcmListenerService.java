package com.pusher.android.notifications.gcm;

import android.os.Bundle;
import android.util.Log;

import com.google.android.gms.gcm.GcmListenerService;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class PusherGCMListenerService extends GcmListenerService {
    private static final String TAG = "PGCMListenerService";
    private static GCMPushNotificationReceivedListener listener;

    public static void setOnMessageReceivedListener(GCMPushNotificationReceivedListener messageReceivedListener) {
        listener = messageReceivedListener;
    }

    @Override
    public void onMessageReceived(String from, Bundle data) {
        Log.d(TAG, "Received from GCM: " + data);
        if (listener != null) listener.onMessageReceived(from, data);
    }
}
