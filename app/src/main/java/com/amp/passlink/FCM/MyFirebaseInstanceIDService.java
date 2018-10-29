package com.amp.passlink.FCM;

/**
 * Created by jayakumar on 16/02/17.
 */


import android.util.Log;

import com.amp.passlink.Utils.SharedPreferencesUtils;
import com.google.firebase.iid.FirebaseInstanceId;
import com.google.firebase.iid.FirebaseInstanceIdService;


public class MyFirebaseInstanceIDService extends FirebaseInstanceIdService {

    private static final String TAG = MyFirebaseInstanceIDService.class.getSimpleName();

    @Override
    public void onTokenRefresh() {

        String refreshedToken = FirebaseInstanceId.getInstance().getToken();
        SharedPreferencesUtils.setParam(this, SharedPreferencesUtils.SHARED_PREF_REGID, refreshedToken);
    }
}