package com.pusher.android.notifications.gcm;

import android.os.Bundle;

/**
 * Created by jamiepatel on 03/08/2016.
 */


public interface GCMPushNotificationReceivedListener {
    void onMessageReceived(String from, Bundle data);
}
