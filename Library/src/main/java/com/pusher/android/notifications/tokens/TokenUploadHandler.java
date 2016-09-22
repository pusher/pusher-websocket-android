package com.pusher.android.notifications.tokens;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public class TokenUploadHandler extends JsonHttpResponseHandler {
    private static final String TAG = "PUploadSuccess";
    private RegistrationListenerStack listenerStack;
    private final Context context;

    public TokenUploadHandler(Context context, RegistrationListenerStack listenerStack) {
        this.context = context;
        this.listenerStack = listenerStack;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        try {
            String clientId = response.getString("id");
            Log.d(TAG, "Uploaded registration token and received id: " + clientId);
            this.listenerStack.onSuccessfulRegistration(clientId, context);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        String responseString = null;
        if (errorResponse != null) {
            responseString = errorResponse.toString();
        }
        onFailure(statusCode, responseString);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        onFailure(statusCode, responseString);
    }

    private void onFailure(int statusCode, String responseString) {
        Log.e(TAG, "[token upload] Received status " + statusCode + " with: " + responseString);
        this.listenerStack.onFailedRegistration(statusCode, responseString);
    }
}
