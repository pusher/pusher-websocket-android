package com.pusher.android;

import android.app.IntentService;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.util.Log;

import com.github.rholder.retry.RetryException;
import com.github.rholder.retry.Retryer;
import com.github.rholder.retry.RetryerBuilder;
import com.github.rholder.retry.StopStrategies;
import com.google.android.gms.gcm.GoogleCloudMessaging;
import com.google.android.gms.iid.InstanceID;

import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;

/**
 * Created by jamiepatel on 10/06/2016.
 */
public class PusherRegistrationIntentService extends IntentService {
    private static final String TAG = "PusherRegIntentService";
    private static final Integer INSTANCE_ID_RETRY_ATTEMPTS = 10;


    public PusherRegistrationIntentService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {

        final InstanceID instanceID = InstanceID.getInstance(this);
        final String defaultSenderId = intent.getStringExtra("gcm_defaultSenderId");
        String token;

        Callable<String> tokenRetrieval = new Callable<String>() {
            @Override
            public String call() throws Exception {
                return instanceID.getToken(defaultSenderId, GoogleCloudMessaging.INSTANCE_ID_SCOPE, null);
            }
        };

        Retryer<String> retryer = RetryerBuilder.<String>newBuilder().
                retryIfRuntimeException().
                withStopStrategy(StopStrategies.stopAfterAttempt(INSTANCE_ID_RETRY_ATTEMPTS)).
                build();

        try {
            token = retryer.call(tokenRetrieval);
        } catch (ExecutionException e) {
            Log.e(TAG, e.getMessage());
            return;
        } catch (RetryException e) {
            Log.e(TAG,
                    "Failed to get token after " +
                            INSTANCE_ID_RETRY_ATTEMPTS +
                            " :" +
                    e.getMessage());
            Intent failedToGetToken = new Intent(PusherPushNotificationRegistration.TOKEN_FAILED_INTENT_FILTER);
            LocalBroadcastManager.getInstance(this).sendBroadcast(failedToGetToken);
            return;
        }

        Intent receivedToken = new Intent(PusherPushNotificationRegistration.TOKEN_RECEIVED_INTENT_FILTER);
        receivedToken.putExtra(PusherPushNotificationRegistration.TOKEN_EXTRA_KEY, token);
        LocalBroadcastManager.getInstance(this).sendBroadcast(receivedToken);
    }

}
