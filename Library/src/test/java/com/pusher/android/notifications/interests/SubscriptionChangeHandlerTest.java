package com.pusher.android.notifications.interests;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.Assert.*;
import static org.mockito.Mockito.verify;

/**
 * Created by jamiepatel on 14/08/2016.
 */
public class SubscriptionChangeHandlerTest {
    private @Mock InterestSubscriptionChangeListener listener;
    private SubscriptionChangeHandler handler;

    @Before
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        Subscription subscription = new Subscription("x", InterestSubscriptionChange.UNSUBSCRIBE, listener);
        handler = new SubscriptionChangeHandler(subscription);
    }

    @Test
    public void onSuccess() throws Exception {
        handler.onSuccess(200, null, null);
        verify(listener).onSubscriptionChangeSucceeded();
    }

    @Test
    public void onFailure() throws Exception {
        handler.onFailure(500, null, new String("hello").getBytes(), null);
        verify(listener).onSubscriptionChangeFailed(500, "hello");
    }

}