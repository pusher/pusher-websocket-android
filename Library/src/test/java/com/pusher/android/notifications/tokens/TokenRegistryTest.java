package com.pusher.android.notifications.tokens;

import android.content.Context;
import android.preference.PreferenceManager;

import com.loopj.android.http.AsyncHttpClient;
import com.pusher.android.PusherAndroidFactory;
import com.pusher.android.PusherAndroidOptions;
import com.pusher.android.notifications.PlatformType;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.skyscreamer.jsonassert.JSONAssert;

import cz.msebera.android.httpclient.HttpEntity;
import cz.msebera.android.httpclient.entity.StringEntity;
import cz.msebera.android.httpclient.util.EntityUtils;

import static org.mockito.Matchers.any;
import static org.junit.Assert.*;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Created by jamiepatel on 12/08/2016.
 */
@RunWith(RobolectricTestRunner.class)
public class TokenRegistryTest {

    private TokenRegistry tokenRegistry;
    private @Mock PusherAndroidFactory factory;
    private PusherAndroidOptions options = new PusherAndroidOptions();
    private Context context = RuntimeEnvironment.application.getApplicationContext();
    private @Mock AsyncHttpClient client;
    private RegistrationListenerStack stack;
    private @Mock TokenUploadHandler tokenUploadHandler;
    private @Mock TokenUpdateHandler tokenUpdateHandler;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        PreferenceManager.getDefaultSharedPreferences(context).edit().clear().apply();

        when(factory.newHttpClient()).thenReturn(client);
        when(factory.newTokenUploadHandler(any(Context.class), any(RegistrationListenerStack.class))).thenReturn(tokenUploadHandler);
        when(factory.newTokenUpdateHandler(any(String.class), any(StringEntity.class), any(Context.class), any(RegistrationListenerStack.class), any(InvalidClientIdHandler.class))).thenReturn(tokenUpdateHandler);

        stack = new RegistrationListenerStack();
        tokenRegistry = new TokenRegistry("superkey", stack, context, PlatformType.FCM, options, factory);
    }


    @Test
    public void receiveUploadsWhenNoCachedId() throws Exception {
        tokenRegistry.receive("mysuperspecialfcmtoken");
        verify(factory).newTokenUploadHandler(context, stack);

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
        JSONAssert.assertEquals(
                EntityUtils.toString(params),
                "{\"platform_type\":\"fcm\",\"token\":\"mysuperspecialfcmtoken\",\"app_key\":\"superkey\"}",
                true
        );
    }

    @Test
    public void receivesUpdatesWhenCachedId() throws Exception {
        PreferenceManager.
                getDefaultSharedPreferences(context).
                edit().
                putString("__pusher__client__key__fcm__superkey", "this-is-the-client-id")
                .apply();

        tokenRegistry.receive("woot-token-woot");
        ArgumentCaptor paramsCaptor = ArgumentCaptor.forClass(StringEntity.class);

        verify(factory).newTokenUpdateHandler(
                eq("this-is-the-client-id"),
                (StringEntity) paramsCaptor.capture(),
                eq(context),
                eq(stack),
                eq(tokenRegistry)
        );


        StringEntity sentParams = (StringEntity) paramsCaptor.getValue();

        verify(client).put(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients/this-is-the-client-id/token"),
                eq(sentParams),
                eq("application/json"),
                eq(tokenUpdateHandler)
        );

        JSONAssert.assertEquals(EntityUtils.toString((HttpEntity) paramsCaptor.getValue()),
                "{\"platform_type\":\"fcm\",\"token\":\"woot-token-woot\",\"app_key\":\"superkey\"}",
                true
        );
    }

    @Test
    public void uploadsOnInvalidClientId() throws Exception {
        JSONObject json = new JSONObject();
        json.put("platform_type", PlatformType.GCM.toString());
        json.put("token", "token-woot");
        json.put("app_key", "app-key-lala");
        StringEntity params = new StringEntity(json.toString(), "UTF-8");
        tokenRegistry.onInvalidClientId(params);
        verify(client).post(
                eq(context),
                eq("https://nativepushclient-cluster1.pusher.com/client_api/v1/clients"),
                eq(params),
                eq("application/json"),
                eq(tokenUploadHandler)
        );
    }

    @Test
    public void onSuccessfulRegistrationSavesClientId() throws Exception {
        tokenRegistry.onSuccessfulRegistration("new-client-id", context);
        String cachedId = PreferenceManager.
                getDefaultSharedPreferences(context)
                .getString("__pusher__client__key__fcm__superkey", null);
        assertEquals("new-client-id", cachedId);
    }

}