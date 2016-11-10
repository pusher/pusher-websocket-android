package com.pusher.android.notifications.gcm;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.pusher.android.notifications.PushNotificationRegistration;

import java.io.IOException;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class GCMRegistrationIntentService extends IntentService {
    private static final String TAG = "PusherRegIntentService";
    private static final Integer INSTANCE_ID_RETRY_ATTEMPTS = 10;

    public GCMRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final InstanceID instanceID = InstanceID.getInstance(this);
        final String defaultSenderId = intent.getStringExtra("gcm_defaultSenderId");

        Intent gcmCalled = new Intent(PushNotificationRegistration.GCM_CALLED_INTENT_FILTER);
        int retryAttempts = 0;
        String token = null;

        while (token == null && retryAttempts < INSTANCE_ID_RETRY_ATTEMPTS){
            try {
                token = instanceID.getToken(defaultSenderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            }
            catch (IOException e) {
                Log.d(TAG, "Failed to get token from InstanceID", e);
            }
            finally {
                retryAttempts++;
            }
        }

        if(token == null){
            Log.e(TAG, "Failed to get token after " + INSTANCE_ID_RETRY_ATTEMPTS + " attempts.");
        }

        gcmCalled.putExtra(PushNotificationRegistration.TOKEN_EXTRA_KEY, token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(gcmCalled);
    }
}
