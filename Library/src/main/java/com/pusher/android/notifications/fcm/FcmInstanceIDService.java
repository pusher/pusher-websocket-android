package com.pusher.android.notifications.fcm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.pusher.android.notifications.PlatformType;
import com.pusher.android.notifications.PushNotificationRegistration;
import com.pusher.android.notifications.tokens.TokenRegistry;

import org.json.JSONException;

/**
 * Created by jamiepatel on 03/08/2016.
 */

public class FCMInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "FCMIID";
    private static TokenRegistry registry;

    public static void setTokenRegistry(TokenRegistry reg) {
        registry = reg;
    }

    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + token);

        if (registry != null) {
            try {
                registry.receive(token);
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    }
}
