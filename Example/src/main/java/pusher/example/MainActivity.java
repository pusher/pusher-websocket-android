package pusher.example;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.messaging.RemoteMessage;
import com.pusher.android.PusherAndroid;
import com.pusher.android.PusherAndroidOptions;
import com.pusher.android.notifications.ManifestValidator;
import com.pusher.android.notifications.PushNotificationRegistration;
import com.pusher.android.notifications.fcm.FCMPushNotificationReceivedListener;
import com.pusher.android.notifications.gcm.GCMPushNotificationReceivedListener;
import com.pusher.android.notifications.tokens.PushNotificationRegistrationListener;
import com.pusher.android.notifications.interests.InterestSubscriptionChangeListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

import java.util.List;

public class MainActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PusherAndroidOptions options = new PusherAndroidOptions();

        PusherAndroid pusher = new PusherAndroid("YOUR_APP_KEY", options);


        if (checkPlayServices()) {
            String defaultSenderId = getString(R.string.gcm_defaultSenderId);
            PushNotificationRegistration nativePusher = pusher.nativePusher();
            nativePusher.setFCMListener(new FCMPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(RemoteMessage remoteMessage) {

                }
            });


            nativePusher.subscribe("donuts", new InterestSubscriptionChangeListener() {
                @Override
                public void onSubscriptionChangeSucceeded() {
                    System.out.println("DONUT SUCCEEDED W000HOOO!!!");
                }

                @Override
                public void onSubscriptionChangeFailed(int statusCode, String response) {
                    System.out.println("What a disgrace: received " + statusCode + " with" + response);
                }
            });


            PushNotificationRegistrationListener regListener = new PushNotificationRegistrationListener() {
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
            };

            //FCM example:
            //Use this if using Pusher's FCMMessagingService - this is not used if you have your custom service to receive notifications
            nativePusher.setFCMListener(new FCMPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(RemoteMessage remoteMessage) {
                    String message = remoteMessage.getNotification().getBody();
                    Log.d(TAG, "PUSHER!!!");
                    Log.d(TAG, "From: " + remoteMessage.getFrom());
                    Log.d(TAG, "Message: " + message);                }
            });

            try {
                nativePusher.registerFCM(getApplicationContext(), regListener);
            } catch (ManifestValidator.InvalidManifestException e) {
                e.printStackTrace();
            }


            //GCM example:
            nativePusher.setGCMListener(new GCMPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(String from, Bundle data) {
                    String message = data.getString("message");
                    Log.d(TAG, "PUSHER!!!");
                    Log.d(TAG, "From: " + from);
                    Log.d(TAG, "Message: " + message);
                }
            });
            
            try {
                nativePusher.registerGCM(this, defaultSenderId, regListener);

            } catch (ManifestValidator.InvalidManifestException e) {
                e.printStackTrace();
            }
        }
    }

    private boolean checkPlayServices() {
    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
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
}
