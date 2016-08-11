package com.pusher.android.notifications.tokens;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface InternalRegistrationProgressListener {
    void onSuccess(String id);
    void onFailure(int statusCode, String reason);
    void onClientIdInvalid();
}
