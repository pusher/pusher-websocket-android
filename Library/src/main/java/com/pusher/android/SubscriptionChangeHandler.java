package com.pusher.android;

import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 11/07/2016.
 */

class SubscriptionChangeHandler extends AsyncHttpResponseHandler {
    private static final String TAG = "PSubHandler";
    private final Runnable successCallback;
    private final Outbox.Item item;

    SubscriptionChangeHandler(
            Outbox.Item item,
            Runnable successCallback) {
        this.item = item;
        this.successCallback = successCallback;
    }

    @Override
    public void setUsePoolThread(boolean pool) {
        super.setUsePoolThread(true);
    }


    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        Log.d(TAG, "Successfully sent subscription change " + item.getChange() + " for interest: " + item.getInterest());
        if (item.getListener() != null) item.getListener().onSubscriptionSucceeded();
        if (this.successCallback != null) this.successCallback.run();
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
                "Could not " + item.getChange() + " to " + item.getInterest() + ".");
        if (item.getListener() != null) item.getListener().onSubscriptionFailed(statusCode, responseBodyString);
    }
}
