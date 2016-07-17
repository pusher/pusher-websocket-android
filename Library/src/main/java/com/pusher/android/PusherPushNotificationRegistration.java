package com.pusher.android;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 11/06/2016.
 */
public class PusherPushNotificationRegistration {
    static String TOKEN_RECEIVED_INTENT_FILTER = "__pusher__token__received";
    static String TOKEN_EXTRA_KEY = "token";

    private static final String PLATFORM_TYPE = "gcm";
    private static final String TAG = "PusherPushNotifReg";

    private final String appKey;
    private final Outbox outbox = new Outbox();
    private final PusherAndroidOptions options;
    private final PusherAndroidFactory factory;

    private PusherPushNotificationRegistrationListener registrationListener;
    private SubscriptionManager clientManager;

    PusherPushNotificationRegistration(String appKey, PusherAndroidOptions options, PusherAndroidFactory factory) {
        this.appKey = appKey;
        this.options = options;
        this.factory = factory;
    }

    /*
    Starts a PusherRegistrationIntentService, which handles token receipts and updates from
    GCM
     */
    public void register(Context context, String defaultSenderId) {
        Log.d(TAG, "Registering for native notifications");
        Context applicationContext = context.getApplicationContext();

        BroadcastReceiver mRegistrationBroadcastReceiver = new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {
                String token = intent.getStringExtra(TOKEN_EXTRA_KEY);
                onReceiveRegistrationToken(token, context);
            }
        };

        LocalBroadcastManager.getInstance(applicationContext).registerReceiver(mRegistrationBroadcastReceiver,
                new IntentFilter(TOKEN_RECEIVED_INTENT_FILTER));

        Intent intent = new Intent(applicationContext, PusherRegistrationIntentService.class);
        intent.putExtra("gcm_defaultSenderId", defaultSenderId);
        Log.d(TAG, "Starting registration intent service");
        applicationContext.startService(intent);
    }

    // Subscribes to an interest
    public void subscribe(String interest) {
        subscribe(interest, null);
    }

    public void subscribe(String interest, PusherPushNotificationSubscriptionListener listener) {
        Log.d(TAG, "Trying to subscribe to: " + interest);
        outbox.add(new Outbox.Item(interest, InterestSubscriptionChange.SUBSCRIBE, listener));
        if (clientManager != null) clientManager.flushOutbox();
    }

    // Subscribes to an interest
    public void unsubscribe(String interest) {
        unsubscribe(interest, null);
    }

    // Unsubscribes to an interest
    public void unsubscribe(String interest, PusherPushNotificationSubscriptionListener listener) {
        Log.d(TAG, "Trying to unsubscribe to: " + interest);
        outbox.add(new Outbox.Item(interest, InterestSubscriptionChange.UNSUBSCRIBE, listener));
        if (clientManager != null) clientManager.flushOutbox();
    }

    // Sets the listener to execute when a notification is received
    public void setMessageReceivedListener(PusherPushNotificationReceivedListener listener) {
        PusherGcmListenerService.setOnMessageReceivedListener(listener);
    }

    public void setRegistrationListener(PusherPushNotificationRegistrationListener listener) {
        this.registrationListener = listener;
    }

    void onReceiveRegistrationToken(String token, final Context context) {
        Log.d(TAG, "Received token: " + token);
        StringEntity params = null;
        try {
            params = createRegistrationJSON(token);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }

        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        String cachedId = preferences.getString(SubscriptionManager.PUSHER_PUSH_CLIENT_ID_KEY, null);

        ClientIdConfirmationListener onReceiveClientID = new ClientIdConfirmationListener() {
            @Override
            public void onConfirmClientId(String id) {
                PusherPushNotificationRegistration registration = PusherPushNotificationRegistration.this;
                registration.clientManager =
                        factory.newSubscriptionManager(id, context, outbox, appKey, options);
                if (registration.registrationListener != null)
                    registration.registrationListener.onSuccessfulRegistration();
            }
        };

        if (cachedId == null) {
            uploadRegistrationToken(context, token, params, onReceiveClientID);
        } else {
            updateRegistrationToken(context, token, params, cachedId, onReceiveClientID);
        }
    }

    /*
    Uploads registration token for the first time then stores it in SharedPreferences for use
    on subsequent requests
     */
    private void uploadRegistrationToken(Context context, String token, StringEntity params, ClientIdConfirmationListener onReceiveClientId) {
        String url = options.buildNotificationURL("/clients");
        AsyncHttpClient client = factory.newAsyncHttpClient();
        JsonHttpResponseHandler handler = factory.newTokenUploadHandler(onReceiveClientId, registrationListener);
        client.post(context, url, params, "application/json", handler);
    }

    /*
    Updates Pusher's mapping of client id to token.
     */
    private void updateRegistrationToken(
            final Context context, final String token,
            final StringEntity params, final String cachedClientId, final ClientIdConfirmationListener onReceiveClientId) {
        String url = options.buildNotificationURL("/clients/" + cachedClientId + "/token");
        AsyncHttpClient client = factory.newAsyncHttpClient();

        Runnable notFoundCallback = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Client ID cannot be found. Reregistering with Pusher...");
                uploadRegistrationToken(context, token, params, onReceiveClientId);
            }
        };

        AsyncHttpResponseHandler handler = factory.newTokenUpdateHandler(
                notFoundCallback,
                onReceiveClientId,
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