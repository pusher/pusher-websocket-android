# Pusher Android Library

**Looking for Push Notifications? Check out [Pusher Beams](https://pusher.com/beams)**

pusher-websocket-android is a wrapper library around [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java). Whereas the underlying library is a purely Java library, this library has interaction with Android APIs. As a result, we can provide a better experience for mobile developers. 

This README will only cover library-specific features. In order to get the core documentation, please visit the README of [pusher-websocket-java](https://github.com/pusher/pusher-websocket-java).

**Please note that this library is still in beta and may not be ready in a production environment. As this library is still pre-1.0, expect breaking changes. Feel free to raise an issue about any bugs you find.**

## Installation

You can install the library via Gradle. First add these dependencies to your `$PROJECT_ROOT/app/build.gradle`:

```groovy
dependencies {
  // for GCM
  compile 'com.google.android.gms:play-services-gcm:9.8.0' // This version if often updated by Google Play Services. 

  // for FCM
  compile 'com.google.firebase:firebase-messaging:9.8.0'
  compile 'com.google.firebase:firebase-core:9.8.0'

  compile 'com.pusher:pusher-websocket-android:0.7.0'
}

// for GCM and FCM
apply plugin: 'com.google.gms.google-services'
```

In your project-level `build.gradle` add:

```groovy
buildscript {
  dependencies {
    classpath 'com.google.gms:google-services:3.0.0'
  }
}
```

## Push Notifications (Replaced by [Pusher Beams](https://pusher.com/beams))
**This documentation is left here for users of our legacy Push Notifications product. It is no longer possible to register new accounts using this system. If you're looking for a Push Notification solution, check out [Pusher Beams](https://pusher.com/beams), our new and improved Push Notifications product!**

### GCM

This feature requires some set up on your behalf. See [our guide to setting up push notifications for Android](https://pusher.com/docs/push_notifications/android) for a friendly introduction.

* That you have an app (a.k.a. project) on the Google Developers Console
* That you have a Server API Key which you have uploaded to the Pusher dashboard
* That you have a [valid configuration file](https://developers.google.com/cloud-messaging/android/client#get-config) for Google services in your `app/` directory

Add to your `AndroidManifest.xml` the following:

```xml
<manifest>
  <!-- ... -->

  <!-- GCM permissions -->
  <uses-permission android:name="com.google.android.c2dm.permission.RECEIVE" />
  <uses-permission android:name="android.permission.WAKE_LOCK" />

  <application>
    <!-- ... -->

    <!-- <application> -->
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
        android:name="com.pusher.android.notifications.gcm.PusherGCMListenerService"
        android:exported="false" >
        <intent-filter>
            <action android:name="com.google.android.c2dm.intent.RECEIVE" />
        </intent-filter>
    </service>

    <service
        android:name="com.pusher.android.notifications.gcm.GCMInstanceIDListenerService"
        android:exported="false">
        <intent-filter>
            <action android:name="com.google.android.gms.iid.InstanceID"/>
        </intent-filter>
    </service>

    <service
        android:name="com.pusher.android.notifications.gcm.GCMRegistrationIntentService"
        android:exported="false">
    </service>

    <!-- ... -->
  </application>

  <!-- ... -->
</manifest>
```

Pusher's GCM listeners and services above allow the library to handle incoming tokens and keep state synced with our servers.

### FCM

To start with, you will need to [add Firebase to your project](https://firebase.google.com/docs/android/setup). Then, in your application manifest, you need to register these services:

```xml
<application>
  <service
      android:name="com.pusher.android.notifications.fcm.FCMMessagingService">
      <intent-filter>
          <action android:name="com.google.firebase.MESSAGING_EVENT"/>
      </intent-filter>
  </service>

  <service
      android:name="com.pusher.android.notifications.fcm.FCMInstanceIDService">
      <intent-filter>
          <action android:name="com.google.firebase.INSTANCE_ID_EVENT"/>
      </intent-filter>
  </service>
</application>
```

### Registering Your Device With Pusher

You can start registering for push notifications in an `Activity` or any other valid `Context`. You will need to check Google Play Services availability on the device, with a function such as:

```java
public class MainActivity extends AppCompatActivity {
    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "yourtag";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
      if (playServicesAvailable()) {
        // ... set up Pusher push notifications
      } else {
        // ... log error, or handle gracefully
      }
    }

    private boolean playServicesAvailable() {
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

    // ...
}
```


Assuming that Google Play services are available, you can then register for push notifications.

#### GCM

Expand your `onCreate` handler to instantiate a `PusherAndroid`, get the native push notification object from it, and register using the sender ID fetched from your `google-services.json` file:

```java
public class MainActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
      if (playServicesAvailable()) {
        PusherAndroidOptions options = new PusherAndroidOptions().setCluster(<pusher_app_cluster>);
        PusherAndroid pusher = new PusherAndroid(<pusher_api_key>);
        PushNotificationRegistration nativePusher = pusher.nativePusher();
        String defaultSenderId = getString(R.string.gcm_defaultSenderId); // fetched from your google-services.json
        nativePusher.registerGCM(this, defaultSenderId);
      } else {
        // ... log error, or handle gracefully
      }
    }
    // ...
}
```

Having called `register` this will start an `IntentService` under the hood that uploads the device token to Pusher.

#### FCM

For Firebase Cloud Messaging, instead of `registerGCM` we call `registerFCM`, passing in the context:

```java
nativePusher.registerFCM(this);
```

#### Listening For Registration Progress

To get progress updates on your registration to GCM or FCM, you can optionally pass a `PushNotificationRegistrationListener`:

```java
PushNotificationRegistrationListener listener = new PushNotificationRegistrationListener() {
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
}

// GCM
nativePusher.registerGCM(this, defaultSenderId, listener);

// FCM
nativePusher.registerFCM(this, listener);
```

### Receiving Notifications

Pusher has a concept of `interests` which clients can subscribe to. Whenever your server application sends a notification to an interest, subscribed clients will receive those notifications.

Subscribing to an interest is simply a matter of calling:

```java
PushNotificationRegistration nativePusher = pusher.nativePusher();
nativePusher.subscribe("kittens"); // the client is interested in kittens
```

To unsubscribe to an interest:

```java
nativePusher.unsubscribe("kittens"); // we are no longer interested in kittens
```

You can also keep track of the state of your subscriptions or un-subscriptions by passing an optional `InterestSubscriptionChangeListener`:


```java
nativePusher.subscribe("kittens", new InterestSubscriptionChangeListener() {
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

If you wish to set a custom callback for when GCM notifications come in:

```java
nativePusher.setGCMListener(new GCMPushNotificationReceivedListener() {
    @Override
    public void onMessageReceived(String from, Bundle data) {
      // do something magical 🔮
    }
});
```

For FCM:

```java
nativePusher.setFCMListener(new FCMPushNotificationReceivedListener() {
    @Override
    public void onMessageReceived(RemoteMessage remoteMessage) {
      // do something magical 🔮
    }
});
```

### Configuring the notifications client

#### Setting the host of Pusher's notifications server

```java
PusherAndroidOptions options = new PusherAndroidOptions();
options.setCluster(<pusher_app_cluster>);
options.setNotificationHost("yolo.io");

PusherAndroid pusher = new PusherAndroid("key", options);
```

#### Using SSL

The client uses SSL by default. To unset it:

```java
PusherAndroidOptions options = new PusherAndroidOptions();
options.setCluster(<pusher_app_cluster>);
options.setNotificationEncrypted(false);
PusherAndroid pusher = new PusherAndroid("key", options);
```
