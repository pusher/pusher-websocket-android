package com.pusher.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Created by jamiepatel on 11/06/2016.
 */
public class PusherPushNotificationRegistration {
    private static PusherPushNotificationRegistration instance = null;
    private static String PUSH_NOTIFICATION_URL = "https://yolo.ngrok.io";
    private static final String PLATFORM_TYPE = "gcm";
    private static final String PUSHER_PUSH_CLIENT_ID_KEY = "__pusher__client__key__";
    private static final String TAG = "PusherPushNotifReg";

    static void setPushNotificationEndpoint(String url) {
        PUSH_NOTIFICATION_URL = url;
    }

    private String apiKey; // existence guaranteed by package protection + set in Pusher initializer.
    private String clientId; // existence guaranteed by package protection + set in Pusher initializer.
    private ContextActivation contextActivation;
    private PusherPushNotificationReceivedListener listener;

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
        RequestQueue requestQueue = Volley.newRequestQueue(context);
        this.contextActivation = new ContextActivation(applicationContext, requestQueue);
        Intent intent = new Intent(applicationContext, PusherRegistrationIntentService.class);
        intent.putExtra("gcm_defaultSenderId", defaultSenderId);
        applicationContext.startService(intent);
    }

    public void subscribe(String interest) {
        outbox.add(new OutboxItem(interest, InterestSubscriptionChange.SUBSCRIBE));
        tryFlushOutbox();
    }

    public void unsubscribe(String interest) {
        for (Iterator<OutboxItem> iter = outbox.iterator(); iter.hasNext(); ){
            OutboxItem item = iter.next();
            if (item.interest.equals(interest)) {
                iter.remove();
            }
        }
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
        Log.d(TAG, "Registering received token");
        if (getClientId() == null) {
            uploadRegistrationToken(token);
        } else {
            updateRegistrationToken(token);
        }
    }

    private void tryFlushOutbox() {
        if (this.contextActivation != null && outbox.size() > 0 && getClientId() != null) {
            OutboxItem item = (OutboxItem) outbox.remove(0);
            modifySubscription(item, new Runnable() {
                @Override
                public void run() {
                    tryFlushOutbox();
                }
            });
        }
    }

    private String getClientId() {
        if (clientId == null) {
            this.clientId = this.contextActivation.getSharedPreferences().getString(PUSHER_PUSH_CLIENT_ID_KEY, null);
        }
        return this.clientId;
    }

    private void modifySubscription(OutboxItem item, final Runnable callback) {
            String url = PUSH_NOTIFICATION_URL + "/client_api/v1/clients/" + clientId + "/interests/" + item.getInterest();
            Map<String, String> params = new HashMap<String, String>();
            params.put("app_key", apiKey);

            int method = Request.Method.POST;

            if (item.getChange() == InterestSubscriptionChange.UNSUBSCRIBE) {
                method = Request.Method.DELETE;
            }

            JsonObjectRequest request = new NoContentJSONObjectRequest(
                    method,
                    url,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            callback.run();
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    Log.e(TAG, "Received status " + volleyError.networkResponse.statusCode  +" with:" + volleyError.networkResponse.data.toString());
                }
            });
            this.contextActivation.getRequestQueue().add(request);
    }

    private void uploadRegistrationToken(String token) {
        if (contextActivation == null) {  // Unlikely to be null as this _should_ be called after register().
            return;
        }

        String url = PUSH_NOTIFICATION_URL + "/client_api/v1/clients";
        JSONObject json = createRegistrationJSON(token);
        JsonObjectRequest request = new JsonObjectRequest(url, json,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        try {
                            String clientId = response.getString("id");
                            contextActivation.getSharedPreferences().edit().putString(PUSHER_PUSH_CLIENT_ID_KEY, clientId).apply();
                            tryFlushOutbox();
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "Received status " + volleyError.networkResponse.statusCode  +" with:" + volleyError.networkResponse.data.toString());
            }
        });
        contextActivation.getRequestQueue().add(request);
    }

    private void updateRegistrationToken(String token) {
        if (contextActivation == null) { // Unlikely to be null as this _should_ be called after register().
            return;
        }

        String url = PUSH_NOTIFICATION_URL + "/client_api/v1/clients/" + clientId + "/token";
        JSONObject json = createRegistrationJSON(token);
        JsonObjectRequest request = new NoContentJSONObjectRequest(
                Request.Method.PUT,
                url,
                json,
                new Response.Listener<JSONObject>() {

                    @Override
                    public void onResponse(JSONObject response) {
                        Log.d(TAG, "Registration token updated");
                        tryFlushOutbox();
                    }
                }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, "Received status " + volleyError.networkResponse.statusCode  +" with: " + volleyError.networkResponse.data.toString());
            }
        });
        contextActivation.getRequestQueue().add(request);
    }

    private JSONObject createRegistrationJSON(String token) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("platform_type", PLATFORM_TYPE);
        params.put("token", token);
        return new JSONObject(params);
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
        - Volley Request queue
        - ApplicationContext
     */
    private class ContextActivation {
        private Context context;
        private RequestQueue requestQueue;

        ContextActivation(Context context, RequestQueue requestQueue) {
            this.context = context;
            this.requestQueue = requestQueue;
        }

        RequestQueue getRequestQueue() {
            return requestQueue;
        }

        SharedPreferences getSharedPreferences() {
            return PreferenceManager.getDefaultSharedPreferences(context);
        }
    }
}