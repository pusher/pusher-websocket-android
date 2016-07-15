package com.pusher.android;

import com.loopj.android.http.AsyncHttpClient;

/**
 * Created by jamiepatel on 12/07/2016.
 */
class Factory {
    private static Factory instance = new Factory();

    static Factory getInstance() {
        return instance;
    }

    private Factory() {}

    SubscriptionChangeHandler newSubscriptionChangeHandler(Outbox.Item item, Runnable successCallback) {
        return new SubscriptionChangeHandler(item, successCallback);
    }

    AsyncHttpClient newAsyncHttpClient() {
        return new AsyncHttpClient();
    }

}
