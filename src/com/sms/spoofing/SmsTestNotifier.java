package com.sms.spoofing;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;

public final class SmsTestNotifier {

    public static final String CHANNEL_ID = "sms_test_messages";
    public static final String DEFAULT_SENDER = "+15551234567";
    public static final String DEFAULT_BODY = "Bluetooth car SMS test from SMS Spoofing";

    private SmsTestNotifier() {
    }

    public static boolean isDefaultSmsApp(Context context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            if (roleManager != null
                    && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                    && roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                return true;
            }
        }

        String defaultPackage = Telephony.Sms.getDefaultSmsPackage(context);
        return context.getPackageName().equals(defaultPackage);
    }

    public static String getRoleDebugStatus(Context context) {
        String roleStatus = "n/a";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = context.getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleStatus = roleManager.isRoleHeld(RoleManager.ROLE_SMS) ? "held" : "not held";
            }
        }
        return "This package: " + context.getPackageName()
                + "\nRoleManager SMS role: " + roleStatus;
    }

    public static Uri createTestMessage(Context context, String sender, String body) {
        createNotificationChannel(context);
        Uri inserted = insertInboxSms(context, sender, body, System.currentTimeMillis());
        postMessageNotification(context, sender, body, inserted);
        return inserted;
    }

    public static void createNotificationChannel(Context context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SMS test messages",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Test SMS-style notifications for Bluetooth car message access");
        NotificationManager notificationManager = context.getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private static Uri insertInboxSms(Context context, String sender, String body, long timestampMillis) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, sender);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, timestampMillis);
        values.put(Telephony.Sms.READ, 0);
        values.put(Telephony.Sms.SEEN, 0);
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
        return context.getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
    }

    private static void postMessageNotification(Context context, String sender, String body, Uri smsUri) {
        Intent openIntent = new Intent(context, MainActivity.class);
        if (smsUri != null) {
            openIntent.setData(smsUri);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(context, CHANNEL_ID)
                : new android.app.Notification.Builder(context);
        android.app.Notification notification = builder
                .setSmallIcon(android.R.drawable.sym_action_chat)
                .setContentTitle(sender)
                .setContentText(body)
                .setStyle(new android.app.Notification.BigTextStyle().bigText(body))
                .setCategory(android.app.Notification.CATEGORY_MESSAGE)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .build();
        notificationManager.notify(1, notification);
    }
}
