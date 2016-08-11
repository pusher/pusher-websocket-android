package com.pusher.android.notifications.fcm;

import android.util.Log;

import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;
import com.pusher.android.notifications.PlatformType;
import com.pusher.android.notifications.PushNotificationRegistration;

/**
 * Created by jamiepatel on 03/08/2016.
 */

public class FCMInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = "FCMIID";
    private static PushNotificationRegistration registration;

    public static void setPushRegistration(PushNotificationRegistration reg) {
        registration = reg;
    }

    @Override
    public void onTokenRefresh() {
        String token = FirebaseInstanceId.getInstance().getToken();
        Log.d(TAG, "Refreshed token: " + token);

        if (registration != null) {
            registration.onReceiveRegistrationToken(PlatformType.FCM, token, getApplicationContext(), null);
        }
    }
}
