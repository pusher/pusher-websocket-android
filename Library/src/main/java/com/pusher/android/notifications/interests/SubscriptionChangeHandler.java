package com.pusher.android.notifications.interests;

import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 11/07/2016.
 */

public class SubscriptionChangeHandler extends AsyncHttpResponseHandler {
    private static final String TAG = "PSubHandler";
    private final String interest;
    private final InterestSubscriptionChange change;
    private final InterestSubscriptionChangeListener listener;

    public SubscriptionChangeHandler(
            String interest,
            InterestSubscriptionChange change,
            InterestSubscriptionChangeListener listener) {
        this.interest = interest;
        this.change = change;
        this.listener = listener;
    }

    @Override
    public void setUsePoolThread(boolean pool) {
        super.setUsePoolThread(true);
    }


    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        Log.d(TAG, "Successfully sent subscription change " + change + " for interest: " + interest);
        if (listener != null) listener.onSubscriptionChangeSucceeded();
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
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
