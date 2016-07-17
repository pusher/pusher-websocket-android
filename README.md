# Pusher Android Library

pusher-websocket-android is a wrapper library around [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java). Whereas the underlying library is a purely Java library, this library has interaction with Android APIs. As a result, we can provide a better experience for mobile developers. We can also use Pusher's new BETA feature: native push notifications.

This README will only cover library-specific features. In order to get the core documentation, please visit the README of [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java).

**Please note that this library is still in beta and may not be ready in a production environment. As this library is still pre-1.0, expect breaking changes. Feel free to raise an issue about any bugs you find.**

## Installation

You can install the library via Gradle:

```groovy
repositories {
  maven { url 'http://clojars.org/repo' }
}

dependencies {
  compile 'com.pusher:pusher-websocket-android:0.1.0'
}
```

## Native Notifications

This set up will assume:

* That you have an app on the Google Developers Console, that you have an API server key, and that you have a [valid configuration file](https://developers.google.com/cloud-messaging/android/client#get-config) for Google services in your `app/` directory.
* Your target Android SDK is 23 or higher.
* You have installed version 9.x.x of `"com.google.android.gms:play-services-gcm:9.0.0"`.

Add to your `AndroidManifest.xml` the following:

```xml
  <!-- GCM permissions -->
  <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />

<!-- GCM Receiver  -->
<receiver
    android:name="com.google.android.gms.gcm.GcmReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND" >
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <category android:name="gcm.play.android.samples.com.gcmquickstart" />
    </intent-filter>
</receiver>

<!-- Pusher's GCM listeners and services -->
<receiver
    android:name="com.google.android.gms.gcm.GcmReceiver"
    android:exported="true"
    android:permission="com.google.android.c2dm.permission.SEND" >
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        <category android:name="gcm.play.android.samples.com.gcmquickstart" />
    </intent-filter>
</receiver>

<service
    android:name="com.pusher.android.PusherGcmListenerService"
    android:exported="false" >
    <intent-filter>
        <action android:name="com.google.android.c2dm.intent.RECEIVE" />
    </intent-filter>
</service>

<service
    android:name="com.pusher.android.PusherInstanceIDListenerService"
    android:exported="false">
    <intent-filter>
        <action android:name="com.google.android.gms.iid.InstanceID"/>
    </intent-filter>
</service>

<service
    android:name="com.pusher.android.PusherRegistrationIntentService"
    android:exported="false">
</service>
```

Pusher's GCM listeners and services above allow the library to handle incoming tokens and keep state synced with our servers.

### Registering Your Device With Pusher

You can start registering for push notifications in an `Activity` or any other valid `Context`. You will need to check Google Play Services availability on the device, with a function such as:

```java
private boolean checkPlayServices() {
    GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
    int resultCode = apiAvailability.isGooglePlayServicesAvailable(this);
    if (resultCode != ConnectionResult.SUCCESS) {
        if (apiAvailability.isUserResolvableError(resultCode)) {
            apiAvailability.getErrorDialog(this, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                    .show();
        } else {
            Log.i(TAG, "This device is not supported.");
            finish();
        }
        return false;
    }
    return true;
}
```

To then register for notifications:

```java
if (checkPlayServices()) {
  String defaultSenderId = getString(R.string.gcm_defaultSenderId);
  PusherPushNotificationRegistration nativePusher = pusher.nativePusher();
  nativePusher.register(this, defaultSenderId);
}
```

`this` refers to the `Context`. The `defaultSenderId` comes automatically from your configuration file.

Having called `register` this will start an `IntentService` under the hood that uploads the device token to Pusher.

To get progress updates on your registration, you can optionally pass a `PusherPushNotificationRegistrationListener`:

```java
nativePusher.register(this, defaultSenderId, new PusherPushNotificationRegistrationListener() {
    @Override
    public void onSuccessfulRegistration() {
        System.out.println("REGISTRATION SUCCESSFUL!!! YEEEEEHAWWWWW!");

    }

    @Override
    public void onFailedRegistration(int statusCode, String response) {
        System.out.println(
                "A real sad day. Registration failed with code " + statusCode +
                        " " + response
        );
    }
});
```

### Receiving Notifications

Pusher has a concept of `interests` which clients can subscribe to. Whenever your server application sends a notification to an interest, subscribed clients will receive those notifications.

Subscribing to an interest is simply a matter of calling:

```java
PusherPushNotificationRegistration nativePusher = pusher.nativePusher();
nativePusher.subscribe("kittens"); // the client is interested in kittens
```

To unsubscribe to an interest:

```java
nativePusher.unsubscribe("kittens"); // we are no longer interested in kittens
```

You can also keep track of the state of your subscriptions or un-subscriptions by passing an optional `PusherPushNotificationSubscriptionChangeListener`:


```java
nativePusher.subscribe("kittens", new PusherPushNotificationSubscriptionChangeListener() {
    @Override
    public void onSubscriptionChangeSucceeded() {
        System.out.println("Success! I love kittens!");
    }

    @Override
    public void onSubscriptionChangeFailed(int statusCode, String response) {
        System.out.println(":(: received " + statusCode + " with" + response);
    }
});
```

### Configuring the notifications client

#### Setting the host of Pusher's notifications server

```java
PusherAndroidOptions options = new PusherAndroidOptions();
options.setHost("yolo.io");

PusherAndroid pusher = new PusherAndroid("key", options);
```

#### Using SSL

The client uses SSL by default. To unset it:

```java
PusherAndroidOptions options = new PusherAndroidOptions();
options.setEncrypted(false);
PusherAndroid pusher = new PusherAndroid("key", options);
```
