package com.sms.spoofing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class SmsDeliverReceiver extends BroadcastReceiver {

    private static final String TAG = "SmsDeliverReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "SMS deliver intent received: " + intent.getAction());
    }
}
