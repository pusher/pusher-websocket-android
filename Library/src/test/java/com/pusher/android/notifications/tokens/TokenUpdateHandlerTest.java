package com.pusher.android.notifications.tokens;

import android.content.Context;

import com.pusher.android.BuildConfig;
import com.pusher.android.notifications.PlatformType;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import cz.msebera.android.httpclient.entity.StringEntity;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

/**
 * Created by jamiepatel on 14/08/2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class TokenUpdateHandlerTest {
    private TokenUpdateHandler tokenUpdateHandler;
    private Context context = RuntimeEnvironment.application.getApplicationContext();
    private @Mock RegistrationListenerStack listenerStack;
    private @Mock InvalidClientIdHandler invalidClientIdHandler;
    private StringEntity params;

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        JSONObject json = new JSONObject();
        json.put("platform_type", PlatformType.GCM.toString());
        json.put("token", "token-woot");
        json.put("app_key", "app-key-lala");
        params = new StringEntity(json.toString(), "UTF-8");

        tokenUpdateHandler = new TokenUpdateHandler(
                "cached-id", params, context,listenerStack, invalidClientIdHandler
        );
    }

    @Test
    public void testListenerStackSuccessOnSuccess(){
        tokenUpdateHandler.onSuccess(200, null, null);
        verify(listenerStack).onSuccessfulRegistration("cached-id", context);
    }

    @Test
    public void testListenerStackFailureOnNon404(){
        tokenUpdateHandler.onFailure(500, null, null, null);
        verify(listenerStack).onFailedRegistration(500, "[no body]");
    }

    @Test
    public void testCallsOnInvalidClientIdOn404() {
        tokenUpdateHandler.onFailure(404, null, null, null);
        verify(invalidClientIdHandler).onInvalidClientId(params);
    }

}