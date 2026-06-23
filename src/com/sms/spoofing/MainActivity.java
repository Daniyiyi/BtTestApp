package com.sms.spoofing;

import android.Manifest;
import android.app.Activity;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.role.RoleManager;
import android.bluetooth.BluetoothAdapter;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
    private static final String CHANNEL_ID = "sms_test_messages";
    private static final int REQUEST_DEFAULT_SMS = 1001;
    private static final int REQUEST_RUNTIME_PERMISSIONS = 1002;

    private AppStateMachine mAppStateMachine;
    private TextView mStatusText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        AppStateMachine.AppState initialState = getInitialBluetoothState();

        mAppStateMachine = new AppStateMachine(initialState);
        Log.i(TAG, "Initial app state: " + mAppStateMachine.getCurrentState());

        mStatusText = findViewById(R.id.statusText);
        Button defaultSmsButton = findViewById(R.id.defaultSmsButton);
        Button sendTestButton = findViewById(R.id.sendTestButton);

        createNotificationChannel();
        requestRuntimePermissions();

        defaultSmsButton.setOnClickListener(view -> requestDefaultSmsRole());
        sendTestButton.setOnClickListener(view -> sendTestSmsNotification());
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStatus();
    }

    public AppStateMachine.AppState getCurrentAppState() {
        return mAppStateMachine.getCurrentState();
    }

    private AppStateMachine.AppState getInitialBluetoothState() {
        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        if (adapter == null) {
            return AppStateMachine.AppState.Bluetooth_off;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
                && checkSelfPermission(Manifest.permission.BLUETOOTH_CONNECT)
                != PackageManager.PERMISSION_GRANTED) {
            return AppStateMachine.AppState.Bluetooth_off;
        }

        return adapter.isEnabled()
                ? AppStateMachine.AppState.Bluetooth_ON
                : AppStateMachine.AppState.Bluetooth_off;
    }

    private void requestDefaultSmsRole() {
        if (isDefaultSmsApp()) {
            Toast.makeText(this, "Already default SMS app", Toast.LENGTH_SHORT).show();
            updateStatus();
            return;
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                startActivityForResult(roleManager.createRequestRoleIntent(RoleManager.ROLE_SMS), REQUEST_DEFAULT_SMS);
                return;
            }
        }

        Intent intent = new Intent(Telephony.Sms.Intents.ACTION_CHANGE_DEFAULT);
        intent.putExtra(Telephony.Sms.Intents.EXTRA_PACKAGE_NAME, getPackageName());
        startActivityForResult(intent, REQUEST_DEFAULT_SMS);
    }

    private void sendTestSmsNotification() {
        if (!isDefaultSmsApp()) {
            Toast.makeText(this, "Make this app the default SMS app first", Toast.LENGTH_LONG).show();
            requestDefaultSmsRole();
            return;
        }

        String sender = "+15551234567";
        String body = "Bluetooth car SMS test from SMS Spoofing";
        long now = System.currentTimeMillis();

        Uri inserted = insertInboxSms(sender, body, now);
        postMessageNotification(sender, body, inserted);
        Toast.makeText(this, "Test SMS notification created", Toast.LENGTH_SHORT).show();
    }

    private Uri insertInboxSms(String sender, String body, long timestampMillis) {
        ContentValues values = new ContentValues();
        values.put(Telephony.Sms.ADDRESS, sender);
        values.put(Telephony.Sms.BODY, body);
        values.put(Telephony.Sms.DATE, timestampMillis);
        values.put(Telephony.Sms.READ, 0);
        values.put(Telephony.Sms.SEEN, 0);
        values.put(Telephony.Sms.TYPE, Telephony.Sms.MESSAGE_TYPE_INBOX);
        return getContentResolver().insert(Telephony.Sms.Inbox.CONTENT_URI, values);
    }

    private void postMessageNotification(String sender, String body, Uri smsUri) {
        Intent openIntent = new Intent(this, MainActivity.class);
        if (smsUri != null) {
            openIntent.setData(smsUri);
        }
        PendingIntent pendingIntent = PendingIntent.getActivity(
                this,
                0,
                openIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        NotificationManager notificationManager =
                (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
        android.app.Notification.Builder builder = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O
                ? new android.app.Notification.Builder(this, CHANNEL_ID)
                : new android.app.Notification.Builder(this);
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

    private boolean isDefaultSmsApp() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null
                    && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)
                    && roleManager.isRoleHeld(RoleManager.ROLE_SMS)) {
                return true;
            }
        }

        String defaultPackage = Telephony.Sms.getDefaultSmsPackage(this);
        return getPackageName().equals(defaultPackage);
    }

    private String getDefaultSmsDebugStatus() {
        String roleStatus = "n/a";
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            RoleManager roleManager = getSystemService(RoleManager.class);
            if (roleManager != null && roleManager.isRoleAvailable(RoleManager.ROLE_SMS)) {
                roleStatus = roleManager.isRoleHeld(RoleManager.ROLE_SMS) ? "held" : "not held";
            }
        }
        return "This package: " + getPackageName()
                + "\nRoleManager SMS role: " + roleStatus;
    }

    private void requestRuntimePermissions() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            return;
        }

        java.util.ArrayList<String> permissions = new java.util.ArrayList<>();
        addPermissionIfMissing(permissions, Manifest.permission.READ_SMS);
        addPermissionIfMissing(permissions, Manifest.permission.RECEIVE_SMS);
        addPermissionIfMissing(permissions, Manifest.permission.SEND_SMS);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            addPermissionIfMissing(permissions, Manifest.permission.POST_NOTIFICATIONS);
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            addPermissionIfMissing(permissions, Manifest.permission.BLUETOOTH_CONNECT);
        }

        if (!permissions.isEmpty()) {
            requestPermissions(permissions.toArray(new String[0]), REQUEST_RUNTIME_PERMISSIONS);
        }
    }

    private void addPermissionIfMissing(java.util.ArrayList<String> permissions, String permission) {
        if (checkSelfPermission(permission) != PackageManager.PERMISSION_GRANTED) {
            permissions.add(permission);
        }
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) {
            return;
        }
        NotificationChannel channel = new NotificationChannel(
                CHANNEL_ID,
                "SMS test messages",
                NotificationManager.IMPORTANCE_HIGH);
        channel.setDescription("Test SMS-style notifications for Bluetooth car message access");
        NotificationManager notificationManager = getSystemService(NotificationManager.class);
        notificationManager.createNotificationChannel(channel);
    }

    private void updateStatus() {
        String smsStatus = isDefaultSmsApp() ? "default SMS app" : "not default SMS app";
        mStatusText.setText("State: " + mAppStateMachine.getCurrentState()
                + "\nSMS role: " + smsStatus
                + "\n" + getDefaultSmsDebugStatus());
    }
}
