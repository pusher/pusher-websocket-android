package com.pusher.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;


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
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import java.io.IOException;
import java.util.List;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.util.EntityUtils;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;



/**
 * Created by jamiepatel on 04/07/2016.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class PusherPushNotificationRegistrationTest {

    private PusherPushNotificationRegistration registration;
    private @Mock PusherAndroidFactory factory;
    private @Mock AsyncHttpClient client;
    private @Mock TokenUploadHandler tokenUploadHandler;
    private @Mock TokenUpdateHandler tokenUpdateHandler;
    private @Mock PusherPushNotificationRegistrationListener registrationListener;
    private @Mock
    PusherPushNotificationSubscriptionChangeListener subscriptionListener;
    private @Mock SubscriptionManager subscriptionManager;

    private PusherAndroidOptions options = new PusherAndroidOptions();
    private Context context = RuntimeEnvironment.application.getApplicationContext();

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(factory.newTokenUploadHandler(any(ClientIdConfirmationListener.class), any(PusherPushNotificationRegistrationListener.class))).thenReturn(tokenUploadHandler);
        when(factory.newAsyncHttpClient()).thenReturn(client);
        when(factory.newTokenUpdateHandler(
                any(Runnable.class),
                any(ClientIdConfirmationListener.class),
                any(String.class))
        ).thenReturn(tokenUpdateHandler);
        when(factory.newSubscriptionManager(
                any(String.class),
                any(Context.class),
                any(String.class),
                any(PusherAndroidOptions.class)
        )).thenReturn(subscriptionManager);
        registration = new PusherPushNotificationRegistration("superkey", options, factory);
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();
    }

    @Test
    public void testRegistrationIntentStartedOnRegister() {
        beginRegistration();
    }

    @Test
    public void testGcmFailureTriggersRegistrationFailed() {
        beginRegistration();
        Intent intent = new Intent(PusherPushNotificationRegistration.TOKEN_FAILED_INTENT_FILTER);
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
        verify(registrationListener).onFailedRegistration(0, "Failed to get registration ID from GCM");
    }

    @Test
    public void testNoIdCacheUploadsToServer() throws IOException {
        beginRegistration();
        sendGcmTokenReceivedBroadcast();
        testUpload();
    }

    @Test
    public void testIdCacheUpdatesServerToken() throws IOException {
        testUpdate();
    }

    @Test
    public void testCachedIdVerificationTriggersRegistrationSuccessCallback() throws IOException {
        UpdateCaptors captors = testUpdate();
        testClientIdConfirmation(captors.successCallback);
    }

    @Test
    public void testCachedIdNotFoundTriggersReupload() throws IOException {
        UpdateCaptors captors = testUpdate();
        captors.notFoundCallback.run();
        testUpload();
    }

    @Test
    public void testSubscriptionChangeFailsUnregistered() {
        registration.subscribe("donuts", subscriptionListener);
        registration.unsubscribe("donuts", subscriptionListener);
        verify(subscriptionListener, times(2)).onSubscriptionChangeFailed(0, "Registration still pending");
    }

    @Test
    public void testSubscriptionChangeSentWhenRegistered() throws IOException {
        beginRegistration();
        sendGcmTokenReceivedBroadcast();
        testUpload();
        registration.subscribe("donuts", subscriptionListener);
        verify(subscriptionManager).sendSubscriptionChange("donuts", InterestSubscriptionChange.SUBSCRIBE, subscriptionListener);

        registration.unsubscribe("donuts", subscriptionListener);
        verify(subscriptionManager).sendSubscriptionChange("donuts", InterestSubscriptionChange.UNSUBSCRIBE, subscriptionListener);
    }

    private void beginRegistration() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        registration.register(context, "senderId", registrationListener);
        Intent expectedIntent = new Intent(context, PusherRegistrationIntentService.class);
        Intent startedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedService();
        assertThat(startedIntent.getComponent(), equalTo(expectedIntent.getComponent()));
        Bundle extras = startedIntent.getExtras();
        assertEquals("senderId", extras.getString("gcm_defaultSenderId"));
        ShadowLocalBroadcastManager localBroadcastManager = (ShadowLocalBroadcastManager) ShadowExtractor.extract(LocalBroadcastManager.getInstance(context));
        List<ShadowLocalBroadcastManager.Wrapper> receivers = localBroadcastManager.getRegisteredBroadcastReceivers();
        assertEquals(2, receivers.size());
    }

    private void sendGcmTokenReceivedBroadcast() {
        Intent intent = new Intent(PusherPushNotificationRegistration.TOKEN_RECEIVED_INTENT_FILTER);
        intent.putExtra(PusherPushNotificationRegistration.TOKEN_EXTRA_KEY, "mysuperspecialgcmtoken");
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
    }

    private void testUpload() throws IOException {

        ArgumentCaptor clientIdListenerCaptor = ArgumentCaptor.forClass(ClientIdConfirmationListener.class);
        ArgumentCaptor registrationListenerCaptor = ArgumentCaptor.forClass(PusherPushNotificationRegistrationListener.class);

        verify(factory).newTokenUploadHandler(
                (ClientIdConfirmationListener) clientIdListenerCaptor.capture(),
                (PusherPushNotificationRegistrationListener) registrationListenerCaptor.capture());

        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(StringEntity.class);

        verify(client).post(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients"),
                (HttpEntity) paramsCaptor.capture(),
                eq("application/json"),
                eq(tokenUploadHandler)
        );

        // test proper params sent
        HttpEntity params = (HttpEntity) paramsCaptor.getValue();
        assertEquals(
                EntityUtils.toString(params),
                "{\"platform_type\":\"gcm\",\"token\":\"mysuperspecialgcmtoken\",\"app_key\":\"superkey\"}"
        );
        ClientIdConfirmationListener clientIdListener = (ClientIdConfirmationListener) clientIdListenerCaptor.getValue();
        testClientIdConfirmation(clientIdListener);
    }

    private void testClientIdConfirmation(ClientIdConfirmationListener clientIdListener) {
        // Test client id listener
        clientIdListener.onConfirmClientId("this-is-the-client-id");
        verify(factory).newSubscriptionManager(
                eq("this-is-the-client-id"),
                eq(context),
                eq("superkey"),
                eq(options)
        );

        // Test registration listener called
        verify(registrationListener).onSuccessfulRegistration();
    }

    private UpdateCaptors testUpdate() throws IOException {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        sharedPreferences.edit().putString(SubscriptionManager.PUSHER_PUSH_CLIENT_ID_KEY, "cached-id").apply();
        beginRegistration();
        sendGcmTokenReceivedBroadcast();

        ArgumentCaptor notFoundCallbackCaptor = ArgumentCaptor.forClass(Runnable.class);
        ArgumentCaptor clientIdListenerCaptor = ArgumentCaptor.forClass(ClientIdConfirmationListener.class);

        verify(factory).newTokenUpdateHandler(
                (Runnable) notFoundCallbackCaptor.capture(),
                (ClientIdConfirmationListener) clientIdListenerCaptor.capture(),
                eq("cached-id")
        );

        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(StringEntity.class);

        verify(client).put(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients/cached-id/token"),
                (HttpEntity) paramsCaptor.capture(),
                eq("application/json"),
                eq(tokenUpdateHandler)
        );

        assertEquals(
                EntityUtils.toString((HttpEntity) paramsCaptor.getValue()),
                "{\"platform_type\":\"gcm\",\"token\":\"mysuperspecialgcmtoken\",\"app_key\":\"superkey\"}"
        );
        return new UpdateCaptors((Runnable) notFoundCallbackCaptor.getValue(), (ClientIdConfirmationListener) clientIdListenerCaptor.getValue());
    }

    // so we can stack tests on one another based on the captors of the previous test
    private class UpdateCaptors {
        public final Runnable notFoundCallback;
        public final ClientIdConfirmationListener successCallback;

        private UpdateCaptors(Runnable notFoundCallback, ClientIdConfirmationListener successCallback) {
            this.notFoundCallback = notFoundCallback;
            this.successCallback = successCallback;
        }
    }
}
