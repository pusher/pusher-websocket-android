package com.pusher.android;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;

import com.android.volley.RequestQueue;

import org.junit.Assert.*;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Matchers;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowPreference;

import java.io.IOException;

import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.core.IsEqual.equalTo;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.robolectric.Shadows.shadowOf;


/**
 * Created by jamiepatel on 04/07/2016.
 */

@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class PusherPushNotificationRegistrationTest {

    private @Mock RequestQueue mRequestQueue;

    @Test
    public void testRegistrationIntentStartedOnRegister() {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        PusherPushNotificationRegistration.getInstance().register(context, "senderId");
        Intent expectedIntent = new Intent(context, PusherRegistrationIntentService.class);
        Intent startedIntent = shadowOf(RuntimeEnvironment.application).getNextStartedService();
        assertThat(startedIntent.getComponent(), equalTo(expectedIntent.getComponent()));
        Bundle extras = startedIntent.getExtras();
        assertEquals(extras.getString("gcm_defaultSenderId"), "senderId");
    }

    @Test
    public void testOnFirstReceivesRegistrationTokenUploadsToServer() throws IOException, InterruptedException {
        Context context = RuntimeEnvironment.application.getApplicationContext();
        PusherPushNotificationRegistration nativePusher = PusherPushNotificationRegistration.getInstance();

        MockWebServer server = new MockWebServer();
        MockResponse mockResponse = new MockResponse();
        mockResponse.setResponseCode(200);
        mockResponse.setBody("{\"id\": \"42\"}");
        server.enqueue(mockResponse);
        server.start();

        PusherPushNotificationRegistration.setPushNotificationEndpoint(server.url("").toString());
        nativePusher.register(context, "senderId");
        nativePusher.onReceiveRegistrationToken("token-blabla");

        RecordedRequest request = server.takeRequest();
        assertEquals("{\"platform_type\":\"gcm\",\"token\":\"token-blabla\"}", request.getBody().readUtf8());
        assertEquals("//client_api/v1/clients", request.getPath());
        assertEquals("POST", request.getMethod());

        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        String savedId = sharedPreferences.getString("__pusher__client__key__", null);
        assertEquals(savedId, "42");
        server.shutdown();

    }


}
