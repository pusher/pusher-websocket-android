package com.pusher.android;

import android.util.Log;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public class PusherPushNotificationRegistrationOptions {
    private static final String API_PREFIX = "client_api";
    private static final String API_VERSION = "v1";

    private String host = "hedwig-staging.herokuapp.com";
    private boolean encrypted = true;


    public void setHost(String host) {
        this.host = host;
    }

    public void setEncrypted(boolean encrypted) {
        this.encrypted = encrypted;
    }

    public boolean isEncrypted() {
        return encrypted;
    }

    public String getHost() {
        return host;
    }

    String buildURL(String path) {
        String scheme = encrypted ? "https://" : "http://";
        Log.d("Poptions", "Using host " + host + " for " + path);
        return scheme + host + "/" + API_PREFIX + "/" + API_VERSION + path;
    }
}
