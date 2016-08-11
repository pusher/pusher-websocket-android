package com.pusher.android.notifications.fcm;

import com.google.firebase.messaging.RemoteMessage;

/**
 * Created by jamiepatel on 10/08/2016.
 */

public interface FcmPushNotificationReceivedListener {
    void onMessageReceived(RemoteMessage remoteMessage);
}
