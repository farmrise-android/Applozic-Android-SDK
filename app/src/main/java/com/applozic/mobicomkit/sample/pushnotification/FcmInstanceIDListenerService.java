package com.applozic.mobicomkit.sample.pushnotification;

import android.util.Log;

import com.applozic.mobicomkit.Applozic;
import com.applozic.mobicomkit.api.account.register.RegisterUserClientService;
import com.applozic.mobicomkit.api.account.register.RegistrationResponse;
import com.applozic.mobicomkit.api.account.user.MobiComUserPreference;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;

/**
 * Created by sunil on 9/4/16.
 */
public class FcmInstanceIDListenerService extends FirebaseInstanceIdService {

    final private static String TAG = "FcmInstanceIDListener";

    @Override
    public void onTokenRefresh() {
        // Get updated InstanceID token.
        String refreshedToken = FirebaseInstanceId.getInstance().getToken();

        Applozic.getInstance(this).setDeviceRegistrationId(refreshedToken);

        if (MobiComUserPreference.getInstance(this).isRegistered()) {
            try {
                new RegisterUserClientService(this).updatePushNotificationId(refreshedToken);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}

      /*  super.onTokenRefresh();

        String registrationId = FirebaseInstanceId.getInstance().getToken();
        Log.i(TAG, "Found Registration Id:" + registrationId);
        Applozic.getInstance(this).setDeviceRegistrationId(registrationId);
        if (MobiComUserPreference.getInstance(this).isRegistered()) {
            try {
                RegistrationResponse registrationResponse = new RegisterUserClientService(this).updatePushNotificationId(registrationId);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

    }
}*/
