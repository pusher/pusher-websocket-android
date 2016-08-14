package com.pusher.android.notifications.tokens;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public class TokenUpdateHandler extends AsyncHttpResponseHandler {
    private static final String TAG = "PTkUpdate";
    private final String cachedId;
    private final StringEntity retryParams;
    private final InvalidClientIdHandler invalidClientIdHandler;
    private final Context context;
    private final RegistrationListenerStack listenerStack;

    public TokenUpdateHandler(String cachedId, StringEntity retryParams, Context context, RegistrationListenerStack listenerStack, InvalidClientIdHandler invalidClientIdHandler) {
        this.cachedId = cachedId;
        this.retryParams = retryParams;
        this.context = context;
        this.listenerStack = listenerStack;
        this.invalidClientIdHandler = invalidClientIdHandler;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        Log.d(TAG, "Registration token updated");
        this.listenerStack.onSuccessfulRegistration(cachedId, context);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        String log = "[token update] Received status " + statusCode;
        String responseBodyString;

        if (responseBody != null) {
            responseBodyString = new String(responseBody);
            log += " with: " + responseBodyString;
        } else {
            responseBodyString = "[no body]";
        }
        Log.e(TAG, log);

        // the client ID cannot be found and we need to re-register to get a fresh one.
        // If the 404 is for a different reason, it'll fall down at the next hurdle
        // (uploading the token) anyway.
        if (statusCode == 404) {
            this.invalidClientIdHandler.onInvalidClientId(retryParams);
        } else {
            listenerStack.onFailedRegistration(statusCode, responseBodyString);
        }
    }
}
