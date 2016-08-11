package com.pusher.android.notifications.fcm;

/**
 * Created by jamiepatel on 08/08/2016.
 */
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

public class FCMMessagingService extends FirebaseMessagingService {
    private static final String TAG = "PFCMListenerService";
    private static FCMPushNotificationReceivedListener listener;

    public static void setOnMessageReceivedListener(FCMPushNotificationReceivedListener messageReceivedListener) {
        listener = messageReceivedListener;
    }

    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.d(TAG, "Received from GCM: " + remoteMessage);

        if (listener != null) {
            listener.onMessageReceived(remoteMessage);
        }
    }
}
