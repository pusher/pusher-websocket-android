package com.pusher.android;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;

/**
 * Created by jamiepatel on 12/07/2016.
 */
class PusherAndroidFactory {
    SubscriptionChangeHandler newSubscriptionChangeHandler(
            String interest,
            InterestSubscriptionChange change,
            PusherPushNotificationSubscriptionChangeListener listener) {
        return new SubscriptionChangeHandler(interest, change, listener);
    }

    AsyncHttpClient newAsyncHttpClient() {
        return new AsyncHttpClient();
    }

    TokenUploadHandler newTokenUploadHandler(ClientIdConfirmationListener onReceiveClientId, PusherPushNotificationRegistrationListener registrationListener) {
        return new TokenUploadHandler(onReceiveClientId, registrationListener);
    }

    TokenUpdateHandler newTokenUpdateHandler(Runnable notFoundCallback, ClientIdConfirmationListener listener, String cachedId) {
        return new TokenUpdateHandler(notFoundCallback, listener, cachedId);
    }

    SubscriptionManager newSubscriptionManager(
            String clientId,
            Context context,
            String appKey,
            PusherAndroidOptions options
    ) {
        return new SubscriptionManager(clientId, context, appKey, options, this);
    }
}
