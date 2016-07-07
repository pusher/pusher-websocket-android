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

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.msebera.android.httpclient.Header;
import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 11/06/2016.
 */
public class PusherPushNotificationRegistration {
    private static PusherPushNotificationRegistration instance = null;
    private static final String PLATFORM_TYPE = "gcm";
    private static final String PUSHER_PUSH_CLIENT_ID_KEY = "__pusher__client__key__";
    private static final String TAG = "PusherPushNotifReg";
    private static final String API_PREFIX = "client_api";
    private static final String API_VERSION = "v1";

    private String apiKey; // existence guaranteed by package protection + set in Pusher initializer.
    private String clientId; // existence guaranteed by package protection + set in Pusher initializer.
    private ContextActivation contextActivation;
    private PusherPushNotificationReceivedListener listener;
    private String host = "nativepushclient-cluster1.pusher.com";
    private boolean encrypted = true;

    private final List outbox = Collections.synchronizedList(new ArrayList<OutboxItem>());

    public static synchronized PusherPushNotificationRegistration getInstance() {
        if (instance == null) {
            instance = new PusherPushNotificationRegistration();
        }
        return instance;
    }

    private PusherPushNotificationRegistration() {}

    public void register(Context context, String defaultSenderId) {
        Log.d(TAG, "Registering for native notifications");
        Context applicationContext = context.getApplicationContext();
        this.contextActivation = new ContextActivation(applicationContext);
        Intent intent = new Intent(applicationContext, PusherRegistrationIntentService.class);
        intent.putExtra("gcm_defaultSenderId", defaultSenderId);
        Log.d(TAG, "Starting registration intent service");
        applicationContext.startService(intent);
    }

    public void subscribe(String interest) {
        Log.d(TAG, "Trying to subscribe to: " + interest);
        outbox.add(new OutboxItem(interest, InterestSubscriptionChange.SUBSCRIBE));
        tryFlushOutbox();
    }

    public void unsubscribe(String interest) {
        Log.d(TAG, "Trying to unsubscribe to: " + interest);
        outbox.add(new OutboxItem(interest, InterestSubscriptionChange.UNSUBSCRIBE));
        tryFlushOutbox();
    }

    public void setMessageReceivedListener(PusherPushNotificationReceivedListener listener) {
        this.listener = listener;
    }

    void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    void onMessageReceived(String from, Bundle data) {
        if (this.listener != null) {
            this.listener.onMessageReceieved(from, data);
        }
    }

    void onReceiveRegistrationToken(String token) {
        Log.d(TAG, "Received token: " + token);
        StringEntity params = null;
        try {
            params = createRegistrationJSON(token);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        if (getClientId() == null) {
            uploadRegistrationToken(token, params);
        } else {
            updateRegistrationToken(token, params);
        }
    }

    private void tryFlushOutbox() {
        Log.d(TAG, "Trying to flushing outbox");
        if (this.contextActivation != null && outbox.size() > 0 && getClientId() != null) {
            final OutboxItem item = (OutboxItem) outbox.remove(0);
            String url = buildURL("/clients/" + clientId + "/interests/" + item.getInterest());
            JSONObject json = new JSONObject();
            try {
                json.put("app_key", apiKey);
            } catch (JSONException e) {
                Log.e(TAG, e.getMessage());
            }
            StringEntity entity = new StringEntity(json.toString(), "UTF-8");

            AsyncHttpResponseHandler handler = new AsyncHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                    Log.d(TAG, "Successfully sent subscription change " + item.getChange() + " for interest: " + item.getInterest());
                    tryFlushOutbox();
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                    String log = "[subscription change] " +
                            "Could not " + item.getChange() + " to " + item.getInterest() + "." +
                            " Received status " +
                            statusCode;
                    if (responseBody != null) log += " with: " + new String(responseBody);
                    Log.e(TAG, log);
                    outbox.add(item); // readd item back to the outbox
                }
            };

            AsyncHttpClient client = new AsyncHttpClient();

            switch (item.getChange()) {
                case SUBSCRIBE:
                    client.post(contextActivation.getContext(), url, entity, "application/json", handler);
                    break;
                case UNSUBSCRIBE:
                    client.delete(contextActivation.getContext(), url, entity, "application/json", handler);
                    break;
            }
        }
    }

    private String getClientId() {
        if (clientId == null) {
            this.clientId = this.contextActivation.getSharedPreferences().getString(PUSHER_PUSH_CLIENT_ID_KEY, null);
        }
        return this.clientId;
    }

    private String buildURL(String path) {
        String scheme = encrypted ? "https://" : "http://";
        return scheme + host + "/" + API_PREFIX + "/" + API_VERSION + path;
    }

    /*
    Uploads registration token for the first time then stores it in SharedPreferences for use
    on subsequent requests
     */
    private void uploadRegistrationToken(String token, StringEntity params) {
        if (contextActivation == null) {  // Unlikely to be null as this _should_ be called after register().
            return;
        }

        String url = buildURL("/clients");
        AsyncHttpClient client = new AsyncHttpClient(); // cannot use async in an intent service

        client.post(contextActivation.getContext(), url, params, "application/json", new JsonHttpResponseHandler() {

            @Override
            public void setUsePoolThread(boolean pool) {
                super.setUsePoolThread(true);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                try {
                    Log.d(TAG, "Uploaded registration token");
                    String clientId = response.getString("id");
                    contextActivation.getSharedPreferences().edit().putString(PUSHER_PUSH_CLIENT_ID_KEY, clientId).apply();
                    tryFlushOutbox();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, String response, Throwable error) {
                Log.e(TAG, "[token upload] Received status " + statusCode + " with: " + response);
            }
        });
    }

    /*
    Updates Pusher's mapping of client id to token.
     */
    private void updateRegistrationToken(String token, StringEntity params) {
        if (contextActivation == null) { // Unlikely to be null as this _should_ be called after register().
            return;
        }

        String url = buildURL("/clients/" + clientId + "/token");

        AsyncHttpClient client = new AsyncHttpClient(); // cannot use async in an intent service

        client.put(contextActivation.getContext(), url, params, "application/json", new AsyncHttpResponseHandler() {

            @Override
            public void setUsePoolThread(boolean pool) {
                super.setUsePoolThread(true);
            }

            @Override
            public void onSuccess(int statusCode, Header[] headers, byte[] responseBody) {
                Log.d(TAG, "Registration token updated");
                tryFlushOutbox();
            }

            @Override
            public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                String log = "[token update] Received status " + statusCode;
                if (responseBody != null) log += " with: " + new String(responseBody);
                Log.e(TAG, log);
            }
        });
    }

    private StringEntity createRegistrationJSON(String token) throws JSONException {
        JSONObject params = new JSONObject();
        params.put("platform_type", PLATFORM_TYPE);
        params.put("token", token);
        return new StringEntity(params.toString(), "UTF-8");
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public void setHost(String host) {
        this.host = host;
    }

    /*
    An immutable class that represents an intention to either subscribe or unsusbscribe
    to an interest
     */
    private class OutboxItem {
        private String interest;
        private InterestSubscriptionChange change;

        public OutboxItem(String interest, InterestSubscriptionChange change) {
            this.interest = interest;
            this.change = change;
        }

        public String getInterest() {
            return this.interest;
        }

        public InterestSubscriptionChange getChange() {
            return this.change;
        }
    }

    private enum InterestSubscriptionChange {
        SUBSCRIBE, UNSUBSCRIBE
    }

    /*
    An immutable private class that wraps around objects that depend on an Android context:
        - SharedPreferences
        - ApplicationContext
     */
    private class ContextActivation {
        private Context context;

        ContextActivation(Context context) {
            this.context = context;
        }

        SharedPreferences getSharedPreferences() {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }

        Context getContext() {
            return context;
        }
    }
}