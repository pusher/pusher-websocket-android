package com.pusher.android;

import android.app.IntentService;
import android.content.Intent;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

import com.android.volley.NetworkResponse;
import com.android.volley.ParseError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.HttpHeaderParser;
import com.android.volley.toolbox.JsonObjectRequest;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;
import com.google.gson.JsonObject;


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
    private static final String TAG = "PusherRegistrationIntentService";
    private static final String SERVER_CLIENT_ID_KEY = "__pusher__client__key__";
    private static final String SERVER_URL = "https://yolo.ngrok.io";
    private static final String PLATFORM_TYPE = "gcm";

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
        String clientID = sharedPreferences.getString(SERVER_CLIENT_ID_KEY, "");
        RequestQueue queue = Volley.newRequestQueue(this.getApplicationContext());

        if (clientID.isEmpty()){
            registerOnServer(queue, token);
        } else {
            updateRegistrationToken(queue, clientID, token);
        }
    }

    private void registerOnServer(RequestQueue queue, String token) {
        String url = SERVER_URL + "/client_api/v1/clients";
        JSONObject json = createRegistrationJSON(token);
        JsonObjectRequest request = new JsonObjectRequest(url, json,
                new Response.Listener<JSONObject>() {
            @Override
            public void onResponse(JSONObject response) {
                System.out.println(response.toString());
                try {
                    String clientId = response.getString("id");
                    SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(PusherRegistrationIntentService.this);
                    sharedPreferences.edit().putString(SERVER_CLIENT_ID_KEY, clientId).apply();
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                System.out.print(volleyError);
            }
        });
        queue.add(request);
    }

    private void updateRegistrationToken(RequestQueue queue, String clientId, String token) {
        System.out.println("UPDATING TOKEN");
        String url = SERVER_URL + "/client_api/v1/clients/" + clientId + "/token";
        JSONObject json = createRegistrationJSON(token);
        JsonObjectRequest request = new NoContentJSONObjectRequest(
                Request.Method.PUT,
                url,
                json,
                new Response.Listener<JSONObject>() {

            @Override
            public void onResponse(JSONObject response) {
                System.out.println("GREAT SUCCESSS");
            }
        }, new Response.ErrorListener() {
            @Override
            public void onErrorResponse(VolleyError volleyError) {
                System.out.println(volleyError);
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

    private class NoContentJSONObjectRequest extends JsonObjectRequest {

        public NoContentJSONObjectRequest(int method, String url, JSONObject jsonRequest, Response.Listener<JSONObject> listener, Response.ErrorListener errorListener) {
            super(method, url, jsonRequest, listener, errorListener);
        }

        @Override
        protected Response<JSONObject> parseNetworkResponse(NetworkResponse response) {
            try {
                String jsonString = new String(response.data, HttpHeaderParser.parseCharset(response.headers));
                // here's the new code, if jsonString.length() == 0 don't parse
                if (jsonString.length() == 0) {
                    return Response.success(null, HttpHeaderParser.parseCacheHeaders(response));
                }
                // end of patch
                return Response.success(new JSONObject(jsonString), HttpHeaderParser.parseCacheHeaders(response));
            } catch (UnsupportedEncodingException e) {
                return Response.error(new ParseError(e));
            } catch (JSONException je) {
                return Response.error(new ParseError(je));
            }
        }
    }
}
