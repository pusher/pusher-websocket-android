package com.pusher.android.notifications.tokens;

import android.content.Context;

import com.pusher.android.BuildConfig;

import org.json.JSONObject;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.robolectric.RobolectricGradleTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

/**
 * Created by jamiepatel on 14/08/2016.
 */
@RunWith(RobolectricGradleTestRunner.class)
@Config(constants = BuildConfig.class)
public class TokenUploadHandlerTest {
    private TokenUploadHandler uploadHandler;
    private @Mock RegistrationListenerStack stack;
    private Context context = RuntimeEnvironment.application.getApplicationContext();

    @Before
    public void setUp() throws Exception {
        MockitoAnnotations.initMocks(this);
        uploadHandler = new TokenUploadHandler(context, stack);
    }

    @Test
    public void onSuccess() throws Exception {
        JSONObject response = new JSONObject();
        response.put("id", "new-client-id");
        uploadHandler.onSuccess(200, null, response);
        verify(stack).onSuccessfulRegistration("new-client-id", context);
    }

    @Test
    public void onFailure() throws Exception {
        uploadHandler.onFailure(500, null, "internal server error :(", null);
        verify(stack).onFailedRegistration(500, "internal server error :(");
    }

}