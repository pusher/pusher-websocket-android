package com.pusher.android.notifications.interests;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface InterestSubscriptionChangeListener {
    void onSubscriptionChangeSucceeded();
    void onSubscriptionChangeFailed(int statusCode, String response);
}
