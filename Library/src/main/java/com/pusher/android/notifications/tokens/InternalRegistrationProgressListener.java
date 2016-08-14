package com.pusher.android.notifications.tokens;

import android.content.Context;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public interface InternalRegistrationProgressListener {
    void onSuccessfulRegistration(String clientId, Context context);
    void onFailedRegistration(int statusCode, String reason);
}
