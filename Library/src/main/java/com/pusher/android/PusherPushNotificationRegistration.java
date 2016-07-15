package com.pusher.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
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
    private static PusherPushNotificationRegistration instance = null;
    private static final String PLATFORM_TYPE = "gcm";
    private static final String TAG = "PusherPushNotifReg";

    private String appKey; // existence guaranteed by package protection + set in Pusher initializer.
    private PusherPushNotificationRegistrationListener registrationListener;
    private PusherPushNotificationReceivedListener messageReceivedListener;
    private final Outbox outbox = new Outbox();
    private PusherPushNotificationRegistrationOptions options;
    private SubscriptionManager clientManager;

    /*
    Package-protected static method to get the PusherPushNotificationRegistration. If the developer
    desires access to this object, they will have to call #nativePusher() on a Pusher instance.
     */
    static synchronized PusherPushNotificationRegistration getInstance() {
        if (instance == null) instance = new PusherPushNotificationRegistration();
        return instance;
    }

    /*
    Starts a PusherRegistrationIntentService, which handles token receipts and updates from
    GCM
     */
    public void register(Context context, String defaultSenderId) {
        register(context, defaultSenderId, new PusherPushNotificationRegistrationOptions());
    }

    public void register(Context context, String defaultSenderId, PusherPushNotificationRegistrationOptions options) {
        Log.d(TAG, "Registering for native notifications");
        this.options = options;
        Context applicationContext = context.getApplicationContext();
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
        this.messageReceivedListener = listener;
    }

    public void setRegistrationListener(PusherPushNotificationRegistrationListener listener) {
        this.registrationListener = listener;
    }

    // Used by a Pusher constructor to set the API key of this instance.
    void setAppKey(String appKey) {
        this.appKey = appKey;
    }

    void onMessageReceived(String from, Bundle data) {
        if (this.messageReceivedListener != null) {
            this.messageReceivedListener.onMessageReceieved(from, data);
        }
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
                        new SubscriptionManager(id, context, outbox, appKey, options);
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

    // Singleton
    private PusherPushNotificationRegistration() {}

    /*
    Uploads registration token for the first time then stores it in SharedPreferences for use
    on subsequent requests
     */
    private void uploadRegistrationToken(Context context, String token, StringEntity params, ClientIdConfirmationListener onReceiveClientId) {
        String url = options.buildURL("/clients");
        AsyncHttpClient client = Factory.getInstance().newAsyncHttpClient();
        JsonHttpResponseHandler handler = new TokenUploadHandler(onReceiveClientId, registrationListener);
        client.post(context, url, params, "application/json", handler);
    }

    /*
    Updates Pusher's mapping of client id to token.
     */
    private void updateRegistrationToken(
            final Context context, final String token,
            final StringEntity params, final String cachedClientId, final ClientIdConfirmationListener onReceiveClientId) {
        String url = options.buildURL("/clients/" + cachedClientId + "/token");
        AsyncHttpClient client = new AsyncHttpClient();

        Runnable notFoundCallback = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "Client ID cannot be found. Reregistering with Pusher...");
                uploadRegistrationToken(context, token, params, onReceiveClientId);
            }
        };

        AsyncHttpResponseHandler handler = new TokenUpdateHandler(
                notFoundCallback,
                onReceiveClientId, cachedClientId);
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