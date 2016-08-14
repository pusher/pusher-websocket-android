package com.pusher.android.notifications.interests;

import android.content.Context;
import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.ResponseHandlerInterface;
import com.pusher.android.PusherAndroidFactory;
import com.pusher.android.PusherAndroidOptions;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.Iterator;
import java.util.List;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 15/07/2016.
 */

public class SubscriptionManager {
    private static final String TAG = "PClientManager";
    private final String clientId;
    private final Context context;
    private final String appKey;
    private final PusherAndroidOptions options;
    private final PusherAndroidFactory factory;

    public SubscriptionManager(
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
    }

    public void sendSubscriptions(List<Subscription> pendingSubscriptions) {
        for (Iterator<Subscription> iterator = pendingSubscriptions.iterator(); iterator.hasNext();){
            Subscription subscription = iterator.next();
            sendSubscription(subscription);
            iterator.remove();
        }
    }

    public void sendSubscription(Subscription subscription) {
        String interest =  subscription.getInterest();
        InterestSubscriptionChange change = subscription.getChange();

        JSONObject json = new JSONObject();
        try {
            json.put("app_key", appKey);
        } catch (JSONException e) {
            Log.e(TAG, e.getMessage());
        }
        StringEntity entity = new StringEntity(json.toString(), "UTF-8");

        String url = options.buildNotificationURL("/clients/" + clientId + "/interests/" + interest);
        ResponseHandlerInterface handler = factory.newSubscriptionChangeHandler(subscription);
        AsyncHttpClient client = factory.newHttpClient();
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
