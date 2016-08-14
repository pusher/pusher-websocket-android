package com.pusher.android;

import android.content.Context;
import android.os.Looper;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.SyncHttpClient;
import com.pusher.android.notifications.PlatformType;
import com.pusher.android.notifications.interests.InterestSubscriptionChange;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.pusher.android.notifications.interests.Subscription;
import com.pusher.android.notifications.interests.SubscriptionChangeHandler;
import com.pusher.android.notifications.interests.SubscriptionManager;
import com.pusher.android.notifications.tokens.InvalidClientIdHandler;
import com.pusher.android.notifications.tokens.RegistrationListenerStack;
import com.pusher.android.notifications.tokens.TokenRegistry;
import com.pusher.android.notifications.tokens.TokenUpdateHandler;
import com.pusher.android.notifications.tokens.TokenUploadHandler;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 12/07/2016.
 */
public class PusherAndroidFactory {
    public SubscriptionChangeHandler newSubscriptionChangeHandler(Subscription subscription) {
        return new SubscriptionChangeHandler(subscription);
    }

    public AsyncHttpClient newHttpClient() {
        return Looper.myLooper() == Looper.getMainLooper() ? new AsyncHttpClient() : new SyncHttpClient();
    }

    public TokenUploadHandler newTokenUploadHandler(Context context, RegistrationListenerStack listenerStack) {
        return new TokenUploadHandler(context, listenerStack);
    }

    public TokenUpdateHandler newTokenUpdateHandler(String cachedId, StringEntity retryParams, Context context, RegistrationListenerStack listenerStack, InvalidClientIdHandler invalidClientIdHandler) {
        return new TokenUpdateHandler(cachedId, retryParams, context, listenerStack, invalidClientIdHandler);
    }

    public SubscriptionManager newSubscriptionManager(
            String clientId,
            Context context,
            String appKey,
            PusherAndroidOptions options
            ) {
        return new SubscriptionManager(clientId, context, appKey, options, this);
    }

    public TokenRegistry newTokenRegistry(
            String appKey, RegistrationListenerStack listenerStack,
            Context context, PlatformType platformType,
            PusherAndroidOptions options) {
        return new TokenRegistry(appKey, listenerStack, context, platformType, options, this);
    }
}
