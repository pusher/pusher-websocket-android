package com.pusher.android;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface PusherPushNotificationSubscriptionChangeListener {
    void onSubscriptionChangeSucceeded();
    void onSubscriptionChangeFailed(int statusCode, String response);
}
