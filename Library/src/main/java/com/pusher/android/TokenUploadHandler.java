package com.pusher.android;

import android.content.SharedPreferences;
import android.util.Log;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 15/07/2016.
 */

class TokenUploadHandler extends JsonHttpResponseHandler {
    private static final String TAG = "PUploadSuccess";
    private final ClientIdConfirmationListener successCallback;
    private final PusherPushNotificationRegistrationListener registrationListener;

    TokenUploadHandler(ClientIdConfirmationListener successCallback, PusherPushNotificationRegistrationListener registrationListener) {
        this.successCallback = successCallback;
        this.registrationListener = registrationListener;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
        try {
            String clientId = response.getString("id");
            Log.d(TAG, "Uploaded registration token and received id: " + clientId);
            this.successCallback.onConfirmClientId(clientId);
        } catch (JSONException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
        onFailure(statusCode, errorResponse.toString());
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, String responseString, Throwable throwable) {
        onFailure(statusCode, responseString);
    }

    private void onFailure(int statusCode, String responseString) {
        Log.e(TAG, "[token upload] Received status " + statusCode + " with: " + responseString);
        if (this.registrationListener != null)
            this.registrationListener.onFailedRegistration(statusCode, responseString);
    }
}
