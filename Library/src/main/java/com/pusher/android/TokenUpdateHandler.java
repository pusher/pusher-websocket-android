package com.pusher.android;

import android.os.Looper;
import android.util.Log;

import com.loopj.android.http.AsyncHttpResponseHandler;

import cz.msebera.android.httpclient.Header;

/**
 * Created by jamiepatel on 15/07/2016.
 */

class TokenUpdateHandler extends AsyncHttpResponseHandler {
    private static final String TAG = "PTkUpdate";
    private final InternalRegistrationProgressListener internalRegistrationProgressListener;
    private final String cachedId;

    TokenUpdateHandler(InternalRegistrationProgressListener internalListener, String cachedId) {
        super(Looper.getMainLooper());
        this.internalRegistrationProgressListener = internalListener;
        this.cachedId = cachedId;
    }

    @Override
    public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
        Log.d(TAG, "Registration token updated");
        this.internalRegistrationProgressListener.onSuccess(cachedId);
    }

    @Override
    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
        String log = "[token update] Received status " + statusCode;
        if (responseBody != null) log += " with: " + new String(responseBody);
        Log.e(TAG, log);

        // the client ID cannot be found and we need to reregister to get a fresh one.
        // If the 404 is for a different reason, it'll fall down at the next hurdle
        // (uploading the token) anyway.
        if (statusCode == 404)
            this.internalRegistrationProgressListener.onNotFound();
    }
}
