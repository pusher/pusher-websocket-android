package com.pusher.android;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface PusherPushNotificationSubscriptionListener {
    void onSubscriptionSucceeded();
    void onSubscriptionFailed(int statusCode, String response);
}
