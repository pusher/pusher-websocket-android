package com.pusher.android.notifications.tokens;

import android.content.Context;

import java.util.Stack;

/**
 * Created by jamiepatel on 12/08/2016.
 */

public class RegistrationListenerStack extends Stack<InternalRegistrationProgressListener> implements InternalRegistrationProgressListener {

    public void push(final PushNotificationRegistrationListener customerListener) {
        push(new InternalRegistrationProgressListener() {
            @Override
            public void onSuccessfulRegistration(String clientId, Context context) {
                customerListener.onSuccessfulRegistration();
            }

            @Override
            public void onFailedRegistration(int statusCode, String reason) {
                customerListener.onFailedRegistration(statusCode, reason);
            }
        });
    }

    @Override
    public void onSuccessfulRegistration(String clientId, Context context) {
        while (!empty()) {
            InternalRegistrationProgressListener listener = pop();
            listener.onSuccessfulRegistration(clientId, context);
        }
    }

    @Override
    public void onFailedRegistration(int statusCode, String reason) {
        while (!empty()) {
            InternalRegistrationProgressListener listener = pop();
            listener.onFailedRegistration(statusCode, reason);
        }
    }
}
