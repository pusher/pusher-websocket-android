package com.pusher.android;

import android.content.Context;
import android.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpClient;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import java.io.IOException;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.util.EntityUtils;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jamiepatel on 17/07/2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class SubscriptionManagerTest {

    private @Mock PusherAndroidFactory factory;
    private @Mock
    PusherPushNotificationSubscriptionChangeListener listener;
    private @Mock AsyncHttpClient client;
    private @Mock SubscriptionChangeHandler subscriptionChangeHandler;
    private Context context = RuntimeEnvironment.application.getApplicationContext();
    private PusherAndroidOptions options = new PusherAndroidOptions();
    private SubscriptionManager subscriptionManager;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();

        when(factory.newAsyncHttpClient()).thenReturn(client);
        when(factory.newSubscriptionChangeHandler(
                any(String.class),
                any(InterestSubscriptionChange.class),
                any(PusherPushNotificationSubscriptionChangeListener.class)
        )).thenReturn(subscriptionChangeHandler);

        String clientId = "123-456";
        String appKey = "super-cool-key";

        subscriptionManager = new SubscriptionManager(
            clientId, context, appKey, options, factory
        );
    }

    @Test
    public void testClientIDCachedOnConstruction(){
        String cachedId = PreferenceManager.getDefaultSharedPreferences(context).getString(
                SubscriptionManager.PUSHER_PUSH_CLIENT_ID_KEY,
                null
        );
        assertEquals("123-456", cachedId);
    }

    @Test
    public void testSubscribe() throws IOException {
        subscriptionManager.sendSubscriptionChange(
                "donuts",
                InterestSubscriptionChange.SUBSCRIBE,
                listener);
        verify(factory).newSubscriptionChangeHandler("donuts", InterestSubscriptionChange.SUBSCRIBE, listener);

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
    public void testUnsubscribe() throws IOException {
        subscriptionManager.sendSubscriptionChange(
                "donuts",
                InterestSubscriptionChange.UNSUBSCRIBE,
                listener);
        verify(factory).newSubscriptionChangeHandler("donuts", InterestSubscriptionChange.UNSUBSCRIBE, listener);

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