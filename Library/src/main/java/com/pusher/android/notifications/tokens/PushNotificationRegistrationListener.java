package com.pusher.android.notifications.tokens;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface PushNotificationRegistrationListener {
    void onSuccessfulRegistration();
    void onFailedRegistration(int statusCode, String response);
}
