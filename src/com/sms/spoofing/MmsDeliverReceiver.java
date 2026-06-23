package com.sms.spoofing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

public class MmsDeliverReceiver extends BroadcastReceiver {

    private static final String TAG = "MmsDeliverReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.i(TAG, "MMS deliver intent received: " + intent.getAction());
    }
}
