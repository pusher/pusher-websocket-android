package com.pusher.android;

import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import java.util.concurrent.Callable;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 15/07/2016.
 */

class TokenUpdateHandler extends AsyncHttpResponseHandler {
    private static final String TAG = "PTkUpdate";
    private final Runnable notFoundCallback;
    private final ClientIdConfirmationListener successCallback;
    private final String cachedId;

    TokenUpdateHandler(Runnable notFoundCallback, ClientIdConfirmationListener successCallback, String cachedId) {
        super(Looper.getMainLooper());
        this.notFoundCallback = notFoundCallback;
        this.successCallback = successCallback;
        this.cachedId = cachedId;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        Log.d(TAG, "Registration token updated");
        this.successCallback.onConfirmClientId(cachedId);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        String log = "[token update] Received status " + statusCode;
        if (responseBody != null) log += " with: " + new String(responseBody);
        Log.e(TAG, log);

        // the client ID cannot be found and we need to reregister to get a fresh one.
        if (statusCode == 404) this.notFoundCallback.run();
    }
}
