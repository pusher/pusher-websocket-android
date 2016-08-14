package com.pusher.android.notifications;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.content.LocalBroadcastManager;


import com.loopj.android.http.AsyncHttpClient;
import com.pusher.android.BuildConfig;
import com.pusher.android.PusherAndroidFactory;
import com.pusher.android.PusherAndroidOptions;
import com.pusher.android.notifications.gcm.GCMRegistrationIntentService;
import com.pusher.android.notifications.interests.InterestSubscriptionChange;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.pusher.android.notifications.interests.SubscriptionManager;
import com.pusher.android.notifications.tokens.InternalRegistrationProgressListener;
import com.pusher.android.notifications.tokens.PushNotificationRegistrationListener;
import com.pusher.android.notifications.tokens.TokenUpdateHandler;
import com.pusher.android.notifications.tokens.TokenUploadHandler;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RobolectricTestRunner;
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
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;



/**
 * Created by jamiepatel on 04/07/2016.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class PushNotificationRegistrationTest {

    private PushNotificationRegistration registration;
    private @Mock PusherAndroidFactory factory;
    private @Mock PushNotificationRegistrationListener registrationListener;
    private @Mock InterestSubscriptionChangeListener subscriptionListener;
    private @Mock SubscriptionManager subscriptionManager;
    private @Mock ManifestValidator manifestValidator;

    private PusherAndroidOptions options = new PusherAndroidOptions();
    private Context context = RuntimeEnvironment.application.getApplicationContext();

    @Before
    public void setUp() throws ManifestValidator.InvalidManifestException {
        MockitoAnnotations.initMocks(this);

        doNothing().when(manifestValidator).validateGCM(context);
        doNothing().when(manifestValidator).validateFCM(context);

        registration = new PushNotificationRegistration("superkey", options, factory, manifestValidator);
    }



    //    @Test
//    public void testRegistrationIntentStartedOnRegister() throws PushNotificationRegistration.InvalidManifestException {
//        beginRegistration();
//    }
//
//    @Test
//    public void testGcmFailureTriggersRegistrationFailed() throws PushNotificationRegistration.InvalidManifestException {
//        beginRegistration();
//        Intent intent = new Intent(PushNotificationRegistration.TOKEN_FAILED_INTENT_FILTER);
//        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
//        verify(registrationListener).onFailedRegistration(0, "Failed to get registration ID from GCM");
//    }
//
//
//    @Test
//    public void testSubscriptionChangeSentWhenRegistered() throws IOException, PushNotificationRegistration.InvalidManifestException {
//        beginRegistration();
//        sendGcmTokenReceivedBroadcast();
//        testUpload();
//        registration.subscribe("donuts", subscriptionListener);
//        verify(subscriptionManager).sendSubscriptionChange("donuts", InterestSubscriptionChange.SUBSCRIBE, subscriptionListener);
//
//        registration.unsubscribe("donuts", subscriptionListener);
//        verify(subscriptionManager).sendSubscriptionChange("donuts", InterestSubscriptionChange.UNSUBSCRIBE, subscriptionListener);
//    }
//
//    @Test
//    public void testPendingSubscriptionChangesSentOnRegister() throws IOException, PushNotificationRegistration.InvalidManifestException {
//        registration.subscribe("donuts", subscriptionListener);
//        registration.unsubscribe("donuts", subscriptionListener);
//        beginRegistration();
//        sendGcmTokenReceivedBroadcast();
//        testUpload();
//
//        InOrder inOrder = inOrder(subscriptionManager);
//        inOrder.verify(subscriptionManager, times(1)).sendSubscriptionChange("donuts", InterestSubscriptionChange.SUBSCRIBE, subscriptionListener);
//        inOrder.verify(subscriptionManager, times(1)).sendSubscriptionChange("donuts", InterestSubscriptionChange.UNSUBSCRIBE, subscriptionListener);
//    }
//
//    private void beginRegistration() throws PushNotificationRegistration.InvalidManifestException {
//        Context context = RuntimeEnvironment.application.getApplicationContext();
//        registration.registerGCM(context, "senderId", registrationListener);
//        Intent expectedIntent = new Intent(context, GCMRegistrationIntentService.class);
//        Intent startedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedService();
//        assertThat(startedIntent.getComponent(), equalTo(expectedIntent.getComponent()));
//        Bundle extras = startedIntent.getExtras();
//        assertEquals("senderId", extras.getString("gcm_defaultSenderId"));
//        ShadowLocalBroadcastManager localBroadcastManager = (ShadowLocalBroadcastManager) ShadowExtractor.extract(LocalBroadcastManager.getInstance(context));
//        List<ShadowLocalBroadcastManager.Wrapper> receivers = localBroadcastManager.getRegisteredBroadcastReceivers();
//        assertEquals(2, receivers.size());
//    }
//
    @Test
    public void testRegisterGCMStartsBroadcastManagerAndIntentService() throws ManifestValidator.InvalidManifestException {
        registration.registerGCM(context, "senderId", registrationListener);
        Intent expectedIntent = new Intent(context, GCMRegistrationIntentService.class);
        Intent startedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedService();
        assertThat(startedIntent.getComponent(), equalTo(expectedIntent.getComponent()));
        Bundle extras = startedIntent.getExtras();
        assertEquals("senderId", extras.getString("gcm_defaultSenderId"));
        ShadowLocalBroadcastManager localBroadcastManager = (ShadowLocalBroadcastManager) ShadowExtractor.extract(LocalBroadcastManager.getInstance(context));
        List<ShadowLocalBroadcastManager.Wrapper> receivers = localBroadcastManager.getRegisteredBroadcastReceivers();
        assertEquals(1, receivers.size());
    }

//    private void sendGcmTokenReceivedBroadcast() {
//        Intent intent = new Intent(PushNotificationRegistration.TOKEN_RECEIVED_INTENT_FILTER);
//        intent.putExtra(PushNotificationRegistration.TOKEN_EXTRA_KEY, "mysuperspecialgcmtoken");
//        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);
//    }
//




}
