package com.sms.spoofing;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.widget.Toast;

public class TestSmsBroadcastReceiver extends BroadcastReceiver {

    public static final String ACTION_SEND_TEST_SMS = "com.sms.spoofing.action.SEND_TEST_SMS";
    public static final String EXTRA_SENDER = "sender";
    public static final String EXTRA_BODY = "body";

    private static final String TAG = "TestSmsBroadcastReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (intent == null || !ACTION_SEND_TEST_SMS.equals(intent.getAction())) {
            return;
        }

        if (!SmsTestNotifier.isDefaultSmsApp(context)) {
            Log.w(TAG, "Broadcast ignored because app does not hold the SMS role");
            Toast.makeText(context, "Make SMS Spoofing the default SMS app first", Toast.LENGTH_LONG).show();
            return;
        }

        String sender = intent.getStringExtra(EXTRA_SENDER);
        String body = intent.getStringExtra(EXTRA_BODY);
        if (sender == null || sender.trim().isEmpty()) {
            sender = SmsTestNotifier.DEFAULT_SENDER;
        }
        if (body == null || body.trim().isEmpty()) {
            body = SmsTestNotifier.DEFAULT_BODY;
        }

        SmsTestNotifier.createTestMessage(context, sender, body);
        Log.i(TAG, "Created test SMS message from broadcast sender=" + sender);
    }
}
