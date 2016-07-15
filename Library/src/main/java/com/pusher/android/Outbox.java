package com.pusher.android;

import android.util.Log;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.ResponseHandlerInterface;

import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cz.msebera.android.httpclient.entity.StringEntity;

/**
 * Created by jamiepatel on 15/07/2016.
 */

class Outbox {
    private final List items = Collections.synchronizedList(new ArrayList<Item>());

    void add(Item item) {
        items.add(item);
    }

    Item remove(int i) {
        return (Item) items.remove(i);
    }

    int size() {
        return items.size();
    }

    /*
    An immutable class that represents an intention to either subscribe or unsusbscribe
    to an interest
     */
    static class Item {
        private String interest;
        private InterestSubscriptionChange change;
        private final PusherPushNotificationSubscriptionListener listener;

        Item(String interest, InterestSubscriptionChange change, PusherPushNotificationSubscriptionListener listener) {
            this.interest = interest;
            this.change = change;
            this.listener = listener;
        }

        String getInterest() {
            return this.interest;
        }

        InterestSubscriptionChange getChange() {
            return this.change;
        }

        public PusherPushNotificationSubscriptionListener getListener() {
            return listener;
        }
    }

}
