package com.pusher.android;

/**
 * Created by jamiepatel on 15/07/2016.
 */

interface InternalRegistrationProgressListener {
    void onSuccess(String id);
    void onFailure(int statusCode, String reason);
    void onClientIdInvalid();
}
