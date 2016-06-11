package com.pusher.android;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;

import org.json.JSONObject;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Created by jamiepatel on 11/06/2016.
 */
public class PusherPushNotificationRegistration {
    private static PusherPushNotificationRegistration instance = null;
    private String clientId;
    private Boolean isActive = false;
    private final Set<Interest> pendingInterests = Collections.synchronizedSet(new HashSet<Interest>());
    private RequestQueue queue;

    public static synchronized PusherPushNotificationRegistration getInstance() {
        if (instance == null) {
            instance = new PusherPushNotificationRegistration();
        }
        return instance;
    }

    protected PusherPushNotificationRegistration() {}

    public void activate(String clientId, Context context) {
        this.clientId = clientId;
        queue = Volley.newRequestQueue(context);
        isActive = true;

        if (pendingInterests.size() > 0) {
            flushPendingInterests();
        }
    }

    public void addInterest(String apiKey, String name) {
        Interest interest = new Interest(apiKey, name);
        if (isActive) {
            interest.register();
        } else {
            pendingInterests.add(interest);
        }
    }

    private void flushPendingInterests() {
        for (Interest interest : pendingInterests) {
            interest.register();
        }
    }

    private class Interest {
        String apiKey;
        String name;

        Interest(String apiKey, String name) {
            this.apiKey = apiKey;
            this.name = name;
        }

        public void register() {
            String url = PusherAndroid.PUSH_NOTIFICATION_URL + "/client_api/v1/clients/" + clientId + "/interests/" + this.name;
            Map<String, String> params = new HashMap<String, String>();
            params.put("app_key", apiKey);

            JsonObjectRequest request = new NoContentJSONObjectRequest(
                    Request.Method.POST,
                    url,
                    new JSONObject(params),
                    new Response.Listener<JSONObject>() {

                        @Override
                        public void onResponse(JSONObject response) {
                            pendingInterests.remove(Interest.this);
                        }
                    }, new Response.ErrorListener() {
                @Override
                public void onErrorResponse(VolleyError volleyError) {
                    System.out.println(volleyError);
                }
            });
            queue.add(request);
        }
    }
}