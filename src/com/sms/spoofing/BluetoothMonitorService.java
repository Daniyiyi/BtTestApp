package com.sms.spoofing;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;

public class BluetoothMonitorService extends Service {

    private static final String TAG = "BluetoothMonitorService";

    @Override
    public void onCreate() {
        super.onCreate();
        Log.i(TAG, "Service created");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        Log.i(TAG, "Service started");
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        Log.i(TAG, "Service destroyed");
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}
