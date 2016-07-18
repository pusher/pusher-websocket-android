package com.pusher.android;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONException;
import org.json.JSONObject;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 15/07/2016.
 */

class SubscriptionManager {
    static final String PUSHER_PUSH_CLIENT_ID_PREFIX = "__pusher__client__key__";
    private static final String TAG = "PClientManager";
    private final String clientId;
    private final Context context;
    private final String appKey;
    private final PusherAndroidOptions options;
    private final PusherAndroidFactory factory;

    static String sharedPreferencesKey(String appKey) {
        return PUSHER_PUSH_CLIENT_ID_PREFIX + appKey;
    }

    SubscriptionManager(
            String clientId,
            Context context,
            String appKey,
            PusherAndroidOptions options,
            PusherAndroidFactory factory
    ) {
        this.clientId = clientId;
        this.context = context;
        this.appKey = appKey;
        this.options = options;
        this.factory = factory;
        SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(context);
        preferences.edit().putString(sharedPreferencesKey(appKey), clientId).apply();
    }

    void sendSubscriptionChange(String interest, InterestSubscriptionChange change, PusherPushNotificationSubscriptionChangeListener listener) {
        JSONObject json = new JSONObject();
        try {
            json.put("app_key", appKey);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        StringEntity entity = new StringEntity(json.toString(), "UTF-8");

        String url = options.buildNotificationURL("/clients/" + clientId + "/interests/" + interest);
        ResponseHandlerInterface handler = factory.newSubscriptionChangeHandler(
                interest,
                change,
                listener);
        AsyncHttpClient client = factory.newAsyncHttpClient();
        switch (change) {
            case SUBSCRIBE:
                client.post(context, url, entity, "application/json", handler);
                break;
            case UNSUBSCRIBE:
                client.delete(context, url, entity, "application/json", handler);
                break;
        }
    }
}
