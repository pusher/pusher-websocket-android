package com.pusher.android.notifications.interests;

import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 11/07/2016.
 */

public class SubscriptionChangeHandler extends AsyncHttpResponseHandler {
    private static final String TAG = "PSubHandler";
    private final Subscription subscription;

    public SubscriptionChangeHandler(
            Subscription subscription) {
        this.subscription = subscription;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        InterestSubscriptionChange change = subscription.getChange();
        InterestSubscriptionChangeListener listener = subscription.getListener();
        String interest = subscription.getInterest();

        Log.d(TAG, "Successfully sent subscription change " + change + " for interest: " + interest);
        if (listener != null) listener.onSubscriptionChangeSucceeded();
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        InterestSubscriptionChange change = subscription.getChange();
        InterestSubscriptionChangeListener listener = subscription.getListener();
        String interest = subscription.getInterest();

        String log = "Received status " + statusCode;
        String responseBodyString = new String();
        if (responseBody != null) {
            responseBodyString = new String(responseBody);
            log += " with: " + responseBodyString;
        }

        Log.e("PInterestSubChange", log);
        Log.e(TAG, "[subscription change] " +
                "Could not " + change + " to " + interest + ".");
        if (listener != null) listener.onSubscriptionChangeFailed(statusCode, responseBodyString);
    }
}
