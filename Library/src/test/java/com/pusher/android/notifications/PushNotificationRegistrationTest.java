package com.pusher.android.notifications;

import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.support.v4.content.LocalBroadcastManager;

import com.pusher.android.PusherAndroidFactory;
import com.pusher.android.PusherAndroidOptions;
import com.pusher.android.notifications.gcm.GCMRegistrationIntentService;
import com.pusher.android.notifications.interests.InterestSubscriptionChange;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.pusher.android.notifications.interests.Subscription;
import com.pusher.android.notifications.interests.SubscriptionManager;
import com.pusher.android.notifications.tokens.InternalRegistrationProgressListener;
import com.pusher.android.notifications.tokens.PushNotificationRegistrationListener;
import com.pusher.android.notifications.tokens.RegistrationListenerStack;
import com.pusher.android.notifications.tokens.TokenRegistry;

import org.json.JSONException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.internal.ShadowExtractor;
import org.robolectric.shadows.support.v4.ShadowLocalBroadcastManager;

import java.util.List;

import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;

/**
 * Created by jamiepatel on 04/07/2016.
 */
@RunWith(RobolectricTestRunner.class)
public class PushNotificationRegistrationTest {

    private PushNotificationRegistration registration;
    private @Mock PusherAndroidFactory factory;
    private @Mock PushNotificationRegistrationListener registrationListener;
    private @Mock InterestSubscriptionChangeListener subscriptionListener;
    private @Mock SubscriptionManager subscriptionManager;
    private @Mock ManifestValidator manifestValidator;
    private @Mock TokenRegistry tokenRegistry;

    private PusherAndroidOptions options = new PusherAndroidOptions();
    private Context context = RuntimeEnvironment.application.getApplicationContext();

    @Before
    public void setUp() throws ManifestValidator.InvalidManifestException {
        MockitoAnnotations.initMocks(this);

        doNothing().when(manifestValidator).validateGCM(context);
        doNothing().when(manifestValidator).validateFCM(context);

        when(factory.newTokenRegistry(
                any(String.class), any(RegistrationListenerStack.class), any(Context.class), any(PlatformType.class), any(PusherAndroidOptions.class)
        )).thenReturn(tokenRegistry);

        when(factory.newSubscriptionManager(
                any(String.class), any(Context.class), any(String.class), any(PusherAndroidOptions.class)
        )).thenReturn(subscriptionManager);
        registration = new PushNotificationRegistration("superkey", options, factory, manifestValidator);
    }

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

    @Test
    public void testSuccessfulGCMRegistrationLeadsToTokenRegistryReceive() throws ManifestValidator.InvalidManifestException, JSONException {
        registration.registerGCM(context, "senderId", registrationListener);
        Intent intent = new Intent(PushNotificationRegistration.GCM_CALLED_INTENT_FILTER);
        intent.putExtra(PushNotificationRegistration.TOKEN_EXTRA_KEY, "mysuperspecialgcmtoken");
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);

        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(RegistrationListenerStack.class);
        verify(factory).newTokenRegistry(
                eq("superkey"), (RegistrationListenerStack) paramsCaptor.capture(),
                eq(context), eq(PlatformType.GCM), eq(options)
        );
        verify(tokenRegistry).receive("mysuperspecialgcmtoken");

        RegistrationListenerStack givenStack = (RegistrationListenerStack) paramsCaptor.getValue();

        // verify the last item in the stack is the registration's InternalRegistrationProgressListener
        assertEquals(registration, givenStack.pop());

        // verify the first item in the stack (and last out) is a wrapper around the given registration listener
        InternalRegistrationProgressListener customerListener = givenStack.pop();
        customerListener.onSuccessfulRegistration("x", context);
        verify(registrationListener).onSuccessfulRegistration();

        customerListener.onFailedRegistration(0, "y");
        verify(registrationListener).onFailedRegistration(0, "y");
    }

    @Test
    public void testFailedGCMRegistrationFiresRegistrationCallback() throws ManifestValidator.InvalidManifestException {
        registration.registerGCM(context, "senderId", registrationListener);
        Intent intent = new Intent(PushNotificationRegistration.GCM_CALLED_INTENT_FILTER);
        intent.putExtra(PushNotificationRegistration.TOKEN_EXTRA_KEY, (String) null);
        LocalBroadcastManager.getInstance(context).sendBroadcastSync(intent);

        verify(registrationListener).onFailedRegistration(0, "Failed to get registration ID from GCM");
    }

    @Test
    public void testSubscriptionManagerCreatedOnSuccessfulRegistration() {
        registration.onSuccessfulRegistration("client-id-woot", context);
        verify(factory).newSubscriptionManager("client-id-woot", context, "superkey", options);
        verify(subscriptionManager).sendSubscriptions( Matchers.anyListOf(Subscription.class) );
    }

    @Test
    public void testPendingSubscriptionChangesSentOnRegister() {
        registration.subscribe("donuts", subscriptionListener);
        registration.unsubscribe("kittens", subscriptionListener);
        registration.onSuccessfulRegistration("client-id-woot", context);

        ArgumentCaptor<List> arg = ArgumentCaptor.forClass(List.class);
        verify(subscriptionManager).sendSubscriptions(arg.capture());

        List<Subscription> sentSubscriptions = arg.getValue();
        Subscription first = sentSubscriptions.get(0);
        assertEquals(first.getChange(), InterestSubscriptionChange.SUBSCRIBE);
        assertEquals(first.getInterest(), "donuts");
        assertEquals(first.getListener(), subscriptionListener);

        Subscription second = sentSubscriptions.get(1);

        assertEquals(second.getChange(), InterestSubscriptionChange.UNSUBSCRIBE);
        assertEquals(second.getInterest(), "kittens");
        assertEquals(second.getListener(), subscriptionListener);
    }

    @Test
    public void testSubscribesAndUnsubscribesWhenRegistered() {
        registration.onSuccessfulRegistration("client-id-woot", context);
        registration.subscribe("donuts", subscriptionListener);
        registration.unsubscribe("kittens", subscriptionListener);

        ArgumentCaptor<Subscription> arg = ArgumentCaptor.forClass(Subscription.class);
        verify(subscriptionManager, times(2)).sendSubscription(arg.capture());

        List<Subscription> sentSubscriptions = arg.getAllValues();
        Subscription first = sentSubscriptions.get(0);
        assertEquals(first.getChange(), InterestSubscriptionChange.SUBSCRIBE);
        assertEquals(first.getInterest(), "donuts");
        assertEquals(first.getListener(), subscriptionListener);

        Subscription second = sentSubscriptions.get(1);

        assertEquals(second.getChange(), InterestSubscriptionChange.UNSUBSCRIBE);
        assertEquals(second.getInterest(), "kittens");
        assertEquals(second.getListener(), subscriptionListener);
    }

    @Test
    public void testOnFailedRegistrationSubscriptionCallbacksCalled() {
        registration.subscribe("donuts", subscriptionListener);
        registration.unsubscribe("kittens", subscriptionListener);
        registration.onFailedRegistration(500, "sadtimes");
        verify(subscriptionListener, times(2)).onSubscriptionChangeFailed(500, "sadtimes");
    }

}
