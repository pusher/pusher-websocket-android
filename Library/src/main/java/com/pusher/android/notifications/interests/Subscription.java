package com.pusher.android.notifications.interests;

import com.pusher.android.notifications.interests.InterestSubscriptionChange;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;

/**
 * Created by jamiepatel on 14/08/2016.
 */

public class Subscription {
    private final String interest;
    private final InterestSubscriptionChange change;
    private final InterestSubscriptionChangeListener listener;

    public Subscription(String interest, InterestSubscriptionChange change, InterestSubscriptionChangeListener listener) {
        this.interest = interest;
        this.change = change;
        this.listener = listener;
    }

    public String getInterest() {
        return interest;
    }

    public InterestSubscriptionChange getChange() {
        return change;
    }

    public InterestSubscriptionChangeListener getListener() {
        return listener;
    }
}
