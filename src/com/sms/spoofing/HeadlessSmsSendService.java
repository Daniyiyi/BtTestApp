package com.sms.spoofing;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class HeadlessSmsSendService extends Service {

    private static final String TAG = "HeadlessSmsSendService";

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Respond-via-message intent received: " + (intent != null ? intent.getAction() : null));
        stopSelf(startId);
        return START_NOT_STICKY;
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
