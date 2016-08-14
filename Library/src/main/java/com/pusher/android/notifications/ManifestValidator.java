package com.pusher.android.notifications;

import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;

import com.pusher.android.notifications.fcm.FCMInstanceIDService;
import com.pusher.android.notifications.fcm.FCMMessagingService;
import com.pusher.android.notifications.gcm.GCMInstanceIDListenerService;
import com.pusher.android.notifications.gcm.GCMRegistrationIntentService;
import com.pusher.android.notifications.gcm.PusherGCMListenerService;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by jamiepatel on 14/08/2016.
 */

public class ManifestValidator {
    private final ArrayList<Class<? extends Service>> gcmServices =
            new ArrayList<>(Arrays.asList(
                    PusherGCMListenerService.class,
                    GCMInstanceIDListenerService.class,
                    GCMRegistrationIntentService.class));

    private final ArrayList<Class<? extends Service>> fcmServices = new ArrayList<Class<? extends Service>>(Arrays.asList(
            FCMInstanceIDService.class, FCMMessagingService.class
    ));

    public void validateGCM(Context context) throws InvalidManifestException {
        checkServicesInManifest(gcmServices, context);
    }

    public void validateFCM(Context context) throws InvalidManifestException {
        checkServicesInManifest(fcmServices, context);
    }

    private void checkServicesInManifest(ArrayList<Class<? extends Service>> list, Context context) throws InvalidManifestException {
        for (Class<? extends Service> service : list) {
            if (!isInManifest(context, service)) {
                throw new InvalidManifestException(service.getName() +
                        " is not registered in your AndroidManifest.xml");
            }
        }
    }

    private boolean isInManifest(Context context, Class<? extends Service> service) {
        Intent intent = new Intent(context, service);
        List<ResolveInfo> info = context.getPackageManager().queryIntentServices(intent, PackageManager.MATCH_DEFAULT_ONLY);
        return info.size() > 0;
    }


    public class InvalidManifestException extends Exception {
        public InvalidManifestException(String message) {
            super(message);
        }
    }
}
