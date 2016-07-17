package pusher.example;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.pusher.android.PusherAndroid;
import com.pusher.android.PusherAndroidOptions;
import com.pusher.android.PusherPushNotificationReceivedListener;
import com.pusher.android.PusherPushNotificationRegistration;
import com.pusher.android.PusherPushNotificationRegistrationListener;
import com.pusher.android.PusherPushNotificationSubscriptionListener;
import com.pusher.client.connection.ConnectionEventListener;
import com.pusher.client.connection.ConnectionState;
import com.pusher.client.connection.ConnectionStateChange;

public class MainActivity extends Activity {

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        PusherAndroidOptions options = new PusherAndroidOptions();
        options.setNotificationHost("yolo.ngrok.io");

        PusherAndroid pusher = new PusherAndroid("824e27be45b4218990c9", options);
        pusher.connect(new ConnectionEventListener() {
            @Override
            public void onConnectionStateChange(ConnectionStateChange change) {
                System.out.println("State changed to " + change.getCurrentState() +
                        " from " + change.getPreviousState());
            }

            @Override
            public void onError(String message, String code, Exception e) {
                System.out.println("There was a problem connecting!");
            }
        }, ConnectionState.ALL);

        if (checkPlayServices()) {
            String defaultSenderId = getString(R.string.gcm_defaultSenderId);
            PusherPushNotificationRegistration nativePusher = pusher.nativePusher();
            nativePusher.setMessageReceivedListener(new PusherPushNotificationReceivedListener() {
                @Override
                public void onMessageReceived(String from, Bundle data) {
                    String message = data.getString("message");
                    Log.d(TAG, "PUSHER!!!");
                    Log.d(TAG, "From: " + from);
                    Log.d(TAG, "Message: " + message);
                }
            });

            nativePusher.setRegistrationListener(new PusherPushNotificationRegistrationListener() {
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

            nativePusher.register(this, defaultSenderId);
            nativePusher.subscribe("donuts", new PusherPushNotificationSubscriptionListener() {
                @Override
                public void onSubscriptionSucceeded() {
                    System.out.println("DONUT SUCCEEDED W000HOOO!!!");
                }

                @Override
                public void onSubscriptionFailed(int statusCode, String response) {
                    System.out.println("What a disgrace: received " + statusCode + " with" + response);
                }
            });
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

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
