package com.pusher.android.notifications.tokens;

import org.json.JSONException;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 12/08/2016.
 */

public interface InvalidClientIdHandler {
    void onInvalidClientId(StringEntity params);
}
