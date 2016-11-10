package com.pusher.android.notifications;

import android.content.Context;
import android.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpClient;
import com.pusher.android.PusherAndroidFactory;
import com.pusher.android.PusherAndroidOptions;
import com.pusher.android.notifications.interests.InterestSubscriptionChange;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.pusher.android.notifications.interests.Subscription;
import com.pusher.android.notifications.interests.SubscriptionChangeHandler;
import com.pusher.android.notifications.interests.SubscriptionManager;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.util.EntityUtils;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jamiepatel on 17/07/2016.
 */
@RunWith(RobolectricTestRunner.class)
public class SubscriptionManagerTest {

    private @Mock
    PusherAndroidFactory factory;
    private @Mock
    InterestSubscriptionChangeListener listener;
    private @Mock AsyncHttpClient client;
    private @Mock
    SubscriptionChangeHandler subscriptionChangeHandler;
    private Context context = RuntimeEnvironment.application.getApplicationContext();
    private PusherAndroidOptions options = new PusherAndroidOptions();
    private SubscriptionManager subscriptionManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();

        when(factory.newHttpClient()).thenReturn(client);
        when(factory.newSubscriptionChangeHandler(
                any(Subscription.class)
        )).thenReturn(subscriptionChangeHandler);

        String clientId = "123-456";
        String appKey = "super-cool-key";

        subscriptionManager = new SubscriptionManager(
            clientId, context, appKey, options, factory
        );
    }

    @Test
    public void testSubscribe() throws IOException {
        Subscription sub = new Subscription("donuts",
                InterestSubscriptionChange.SUBSCRIBE,
                listener);
        subscriptionManager.sendSubscription(sub);

        ArgumentCaptor<Subscription> subCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(factory).newSubscriptionChangeHandler(sub);

        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(StringEntity.class);
        verify(client).post(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients/123-456/interests/donuts"),
                (HttpEntity) paramsCaptor.capture(),
                eq("application/json"),
                eq(subscriptionChangeHandler)
        );
        StringEntity params = (StringEntity) paramsCaptor.getValue();
        assertEquals(
                "{\"app_key\":\"super-cool-key\"}",
                EntityUtils.toString(params)
        );
    }

    @Test
    public void testSendingListOfSubscriptions() throws IOException {
        Subscription sub1 = new Subscription("kittens", InterestSubscriptionChange.SUBSCRIBE, listener);
        Subscription sub2 = new Subscription("donuts", InterestSubscriptionChange.UNSUBSCRIBE, listener);
        List<Subscription> list = new ArrayList<>(Arrays.asList(sub1, sub2));
        subscriptionManager.sendSubscriptions(list);

        ArgumentCaptor<Subscription> subCaptor = ArgumentCaptor.forClass(Subscription.class);
        verify(factory).newSubscriptionChangeHandler(sub1);

        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(StringEntity.class);

        InOrder inOrder = inOrder(client);
        inOrder.verify(client).post(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients/123-456/interests/kittens"),
                (HttpEntity) paramsCaptor.capture(),
                eq("application/json"),
                eq(subscriptionChangeHandler)
        );
        StringEntity params = (StringEntity) paramsCaptor.getValue();
        assertEquals(
                "{\"app_key\":\"super-cool-key\"}",
                EntityUtils.toString(params)
        );

        ArgumentCaptor<Subscription> subCaptor2 = ArgumentCaptor.forClass(Subscription.class);
        verify(factory).newSubscriptionChangeHandler(sub2);

        ArgumentCaptor deleteCaptor = ArgumentCaptor.forClass(StringEntity.class);

        inOrder.verify(client).delete(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients/123-456/interests/donuts"),
                (HttpEntity) deleteCaptor.capture(),
                eq("application/json"),
                eq(subscriptionChangeHandler)
        );

        StringEntity deleteParams = (StringEntity) deleteCaptor.getValue();
        assertEquals(
                "{\"app_key\":\"super-cool-key\"}",
                EntityUtils.toString(deleteParams)
        );
    }

    @Test
    public void testUnsubscribe() throws IOException {
        Subscription subscription = new Subscription(
                "donuts",
                InterestSubscriptionChange.UNSUBSCRIBE,
                listener);
        subscriptionManager.sendSubscription(subscription);

        ArgumentCaptor<Subscription> subCaptor = ArgumentCaptor.forClass(Subscription.class);

        verify(factory).newSubscriptionChangeHandler(
                subscription
        );

        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(StringEntity.class);
        verify(client).delete(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients/123-456/interests/donuts"),
                (HttpEntity) paramsCaptor.capture(),
                eq("application/json"),
                eq(subscriptionChangeHandler)
        );
        StringEntity params = (StringEntity) paramsCaptor.getValue();
        assertEquals(
                "{\"app_key\":\"super-cool-key\"}",
                EntityUtils.toString(params)
        );
    }
}