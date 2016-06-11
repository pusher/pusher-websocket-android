package com.pusher.android;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;
import android.util.Log;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;


import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class PusherRegistrationIntentService extends IntentService {
    private static final String TAG = "PusherRegIntentService";
    private static final String PLATFORM_TYPE = "gcm";
    private static final String PUSHER_PUSH_CLIENT_ID_KEY = "__pusher__client__key__";

    public PusherRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        InstanceID instanceID = InstanceID.getInstance(this);
        String defaultSenderId = intent.getStringExtra("gcm_defaultSenderId");
        String token;

        try {
            token = instanceID.getToken(defaultSenderId,
                    GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
        } catch (IOException e) {
            // TODO: properly handle exception
            e.printStackTrace();
            return;
        }

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        String clientID = sharedPreferences.getString(PUSHER_PUSH_CLIENT_ID_KEY, "");
        RequestQueue queue = Volley.newRequestQueue(this.getApplicationContext());

        if (clientID.isEmpty()){
            registerOnServer(queue, token);
        } else {
            PusherPushNotificationRegistration.getInstance().activate(clientID, PusherRegistrationIntentService.this.getApplicationContext());
            updateRegistrationToken(queue, clientID, token);
        }
    }

    private void registerOnServer(RequestQueue queue, String token) {
        String url = PusherAndroid.PUSH_NOTIFICATION_URL + "/client_api/v1/clients";
        JSONObject json = createRegistrationJSON(token);
        JsonObjectRequest request = new JsonObjectRequest(url, json,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                try {
                    String clientId = response.getString("id");
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PusherRegistrationIntentService.this);
                    sharedPreferences.edit().putString(PUSHER_PUSH_CLIENT_ID_KEY, clientId).apply();
                    PusherPushNotificationRegistration.getInstance().activate(clientId, PusherRegistrationIntentService.this.getApplicationContext());
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                Log.e(TAG, volleyError.getMessage());
            }
        });
        queue.add(request);
    }

    private void updateRegistrationToken(RequestQueue queue, String clientId, String token) {
        String url = PusherAndroid.PUSH_NOTIFICATION_URL + "/client_api/v1/clients/" + clientId + "/token";
        JSONObject json = createRegistrationJSON(token);
        JsonObjectRequest request = new NoContentJSONObjectRequest(
                Request.Method.PUT,
                url,
                json,
                new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                Log.d(TAG, "Registration token updated");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                System.out.println(volleyError);
                Log.d(TAG, volleyError.getMessage());
            }
        });
        queue.add(request);
    }

    private JSONObject createRegistrationJSON(String token) {
        Map<String, String> params = new HashMap<String, String>();
        params.put("platform_type", PLATFORM_TYPE);
        params.put("token", token);
        return new JSONObject(params);
    }
}
