package com.pusher.android;

import android.content.Context;
import android.os.Looper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.SyncHttpClient;
import com.pusher.android.notifications.PlatformType;
import com.pusher.android.notifications.interests.InterestSubscriptionChange;
import com.pusher.android.notifications.tokens.InternalRegistrationProgressListener;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.pusher.android.notifications.interests.SubscriptionChangeHandler;
import com.pusher.android.notifications.interests.SubscriptionManager;
import com.pusher.android.notifications.tokens.TokenUpdateHandler;
import com.pusher.android.notifications.tokens.TokenUploadHandler;

/**
 * Created by jamiepatel on 12/07/2016.
 */
public class PusherAndroidFactory {
    public SubscriptionChangeHandler newSubscriptionChangeHandler(
            String interest,
            InterestSubscriptionChange change,
            InterestSubscriptionChangeListener listener) {
        return new SubscriptionChangeHandler(interest, change, listener);
    }

    public AsyncHttpClient newHttpClient() {
        return Looper.myLooper() == Looper.getMainLooper() ? new AsyncHttpClient() : new SyncHttpClient();
    }

    public TokenUploadHandler newTokenUploadHandler(InternalRegistrationProgressListener internalRegistrationProgressListener) {
        return new TokenUploadHandler(internalRegistrationProgressListener);
    }

    public TokenUpdateHandler newTokenUpdateHandler(InternalRegistrationProgressListener listener, String cachedId) {
        return new TokenUpdateHandler(listener, cachedId);
    }

    public SubscriptionManager newSubscriptionManager(
            String clientId,
            Context context,
            String appKey,
            PusherAndroidOptions options,
            PlatformType platformType
            ) {
        return new SubscriptionManager(clientId, context, appKey, options, platformType, this);
    }
}
