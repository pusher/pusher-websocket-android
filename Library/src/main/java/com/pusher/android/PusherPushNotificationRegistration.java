package com.pusher.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 11/06/2016.
 */
public class PusherPushNotificationRegistration {
    static String TOKEN_RECEIVED_INTENT_FILTER = "__pusher__token__received";
    static String TOKEN_FAILED_INTENT_FILTER = "__pusher__token__failed";
    static String TOKEN_EXTRA_KEY = "token";

    private static final String PLATFORM_TYPE = "gcm";
    private static final String TAG = "PusherPushNotifReg";

    private final String appKey;
    private final PusherAndroidOptions options;
    private final PusherAndroidFactory factory;
    private SubscriptionManager subscriptionManager; // should only exist on successful registration with Pusher
    private final List<PusherPushNotificationRegistrationListener> pendingSubscriptions =
            Collections.synchronizedList(new ArrayList<PusherPushNotificationRegistrationListener>());

    PusherPushNotificationRegistration(String appKey, PusherAndroidOptions options, PusherAndroidFactory factory) {
        this.appKey = appKey;
        this.options = options;
        this.factory = factory;
    }

    public void register(Context context, String defaultSenderId) {
        register(context, defaultSenderId, null);
    }

    /*
    Starts a PusherRegistrationIntentService, which handles token receipts and updates from
    GCM
     */
    public void register(
            Context context,
            String defaultSenderId,
            final PusherPushNotificationRegistrationListener registrationListener) {
        Log.d(TAG, "Registering for native notifications");
        Context applicationContext = context.getApplicationContext();

        BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String token = intent.getStringExtra(TOKEN_EXTRA_KEY);
                onReceiveRegistrationToken(token, context, registrationListener);
            }
        };

        BroadcastReceiver mRegistrationFailedBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                if (registrationListener != null) {
                    registrationListener.onFailedRegistration(
                            0,
                            "Failed to get registration ID from GCM"
                    );
                }
            }
        };

        LocalBroadcastManager localBroadcastManager = LocalBroadcastManager.getInstance(applicationContext);

        // Listen for successfully getting token from GCM
        localBroadcastManager.registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(TOKEN_RECEIVED_INTENT_FILTER));

        // Listen for failure to get token from GCM
        localBroadcastManager.registerReceiver(mRegistrationFailedBroadcastReceiver,
                new IntentFilter(TOKEN_FAILED_INTENT_FILTER));

        Intent intent = new Intent(applicationContext, PusherRegistrationIntentService.class);
        intent.putExtra("gcm_defaultSenderId", defaultSenderId);
        Log.d(TAG, "Starting registration intent service");
        applicationContext.startService(intent);
    }

    // Subscribes to an interest
    public void subscribe(String interest) {
        subscribe(interest, null);
    }

    public void subscribe(final String interest, final PusherPushNotificationSubscriptionChangeListener listener) {
        trySendSubscriptionChange(interest, InterestSubscriptionChange.SUBSCRIBE, listener);
    }

    // Subscribes to an interest
    public void unsubscribe(String interest) {
        unsubscribe(interest, null);
    }

    // Unsubscribes to an interest
    public void unsubscribe(final String interest, final PusherPushNotificationSubscriptionChangeListener listener) {
        trySendSubscriptionChange(interest, InterestSubscriptionChange.UNSUBSCRIBE, listener);
    }

    private void trySendSubscriptionChange(
            final String interest,
            final InterestSubscriptionChange change,
            final PusherPushNotificationSubscriptionChangeListener listener) {
        Log.d(TAG, "Trying to "+change+" to: " + interest);
        if (subscriptionManager != null) {
            subscriptionManager.sendSubscriptionChange(interest, change, listener);
        } else {
            pendingSubscriptions.add(new PusherPushNotificationRegistrationListener() {
                @Override
                public void onSuccessfulRegistration() {
                    subscriptionManager.sendSubscriptionChange(interest, change, listener);
                }

                @Override
                public void onFailedRegistration(int statusCode, String response) {
                    if (listener != null) {
                        listener.onSubscriptionChangeFailed(0, "Failed to "+change+" as registration failed");
                    }
                }
            });
        }
    }

    // Sets the listener to execute when a notification is received
    public void setMessageReceivedListener(PusherPushNotificationReceivedListener listener) {
        PusherGcmListenerService.setOnMessageReceivedListener(listener);
    }

    /*
    This checks SharedPreferences to see if the library has cached the Pusher client id.
    If so, it will try and update the token associated with that client id.
    If not it will POST a new client.
     */
    private void onReceiveRegistrationToken(
            final String token,
            final Context context,
            final PusherPushNotificationRegistrationListener registrationListener) {
        Log.d(TAG, "Received token: " + token);
        StringEntity params = null;
        try {
            params = createRegistrationJSON(token);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String cachedId = preferences.getString(SubscriptionManager.PUSHER_PUSH_CLIENT_ID_KEY, null);

        final StringEntity finalParams = params;
        final InternalRegistrationProgressListener internalRegistrationProgressListener = new InternalRegistrationProgressListener() {
            @Override
            public void onSuccess(String id) {
                PusherPushNotificationRegistration registration = PusherPushNotificationRegistration.this;
                registration.subscriptionManager =
                        factory.newSubscriptionManager(id, context, appKey, options);
                if (registrationListener != null) {
                    registrationListener.onSuccessfulRegistration();
                }

                for (Iterator<PusherPushNotificationRegistrationListener> iterator = pendingSubscriptions.iterator(); iterator.hasNext();){
                    PusherPushNotificationRegistrationListener listener = iterator.next();
                    listener.onSuccessfulRegistration();
                    iterator.remove();
                }
            }

            @Override
            public void onFailure(int statusCode, String reason) {
                if (registrationListener != null) {
                    registrationListener.onFailedRegistration(statusCode, reason);
                }
            }


            @Override
            public void onClientIdInvalid() {
                uploadRegistrationToken(context, token, finalParams, this);
            }

        };

        if (cachedId == null) {
            uploadRegistrationToken(context, token, params, internalRegistrationProgressListener);
        } else {
            updateRegistrationToken(context, token, params, cachedId, internalRegistrationProgressListener);
        }
    }

    /*
    Uploads registration token for the first time then stores it in SharedPreferences for use
    on subsequent requests
     */
    private void uploadRegistrationToken(
            Context context, String token,
            StringEntity params, InternalRegistrationProgressListener internalRegistrationProgressListener
    ) {
        String url = options.buildNotificationURL("/clients");
        AsyncHttpClient client = factory.newAsyncHttpClient();
        JsonHttpResponseHandler handler = factory.newTokenUploadHandler(internalRegistrationProgressListener);
        client.post(context, url, params, "application/json", handler);
    }

    /*
    Updates Pusher's mapping of client id to token.
     */
    private void updateRegistrationToken(
            final Context context, final String token,
            final StringEntity params, final String cachedClientId,
            final InternalRegistrationProgressListener internalRegistrationProgressListener) {
        String url = options.buildNotificationURL("/clients/" + cachedClientId + "/token");
        AsyncHttpClient client = factory.newAsyncHttpClient();

        AsyncHttpResponseHandler handler = factory.newTokenUpdateHandler(
                internalRegistrationProgressListener,
                cachedClientId
        );
        client.put(context, url, params, "application/json", handler);
    }

    private StringEntity createRegistrationJSON(String token) throws JSONException {
        JSONObject params = new JSONObject();
        params.put("platform_type", PLATFORM_TYPE);
        params.put("token", token);
        params.put("app_key", appKey);
        return new StringEntity(params.toString(), "UTF-8");
    }

}