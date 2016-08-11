package com.pusher.android.notifications;

/**
 * Created by jamiepatel on 03/08/2016.
 */

public enum PlatformType {
    GCM("gcm"),
    FCM("fcm");

    private final String name;

    PlatformType(String name) {
        this.name = name;
    }

    @Override
    public String toString() {
        return name;
    }
}
