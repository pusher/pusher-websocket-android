package com.pusher.android;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface PusherPushNotificationRegistrationListener {
    void onSuccessfulRegistration();
    void onFailedRegistration(int statusCode, String response);
}
