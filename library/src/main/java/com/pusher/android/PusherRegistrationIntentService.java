package com.pusher.android;

import android.app.IntentService;
import android.content.Intent;

import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.io.IOException;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class PusherRegistrationIntentService extends IntentService {
    private static final String TAG = "PusherRegistrationIntentService";

    public PusherRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        InstanceID instanceID = InstanceID.getInstance(this);
        String defaultSenderId = intent.getStringExtra("gcm_defaultSenderId");
        try {
            String token = instanceID.getToken(defaultSenderId,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            System.out.println(token);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
