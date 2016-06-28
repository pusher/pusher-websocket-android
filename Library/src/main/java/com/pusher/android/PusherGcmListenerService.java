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
        PusherPushNotificationRegistration.getInstance().onMessageReceived(from, data);
    }
}
