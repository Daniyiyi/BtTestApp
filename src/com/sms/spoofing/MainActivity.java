package com.sms.spoofing;

import android.Manifest;
import android.app.Activity;
import android.app.role.RoleManager;
import android.bluetooth.BluetoothAdapter;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.provider.Telephony;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";
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

        SmsTestNotifier.createNotificationChannel(this);
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

        SmsTestNotifier.createTestMessage(
                this,
                SmsTestNotifier.DEFAULT_SENDER,
                SmsTestNotifier.DEFAULT_BODY);
        Toast.makeText(this, "Test SMS notification created", Toast.LENGTH_SHORT).show();
    }

    private boolean isDefaultSmsApp() {
        return SmsTestNotifier.isDefaultSmsApp(this);
    }

    private String getDefaultSmsDebugStatus() {
        return SmsTestNotifier.getRoleDebugStatus(this);
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

    private void updateStatus() {
        String smsStatus = isDefaultSmsApp() ? "default SMS app" : "not default SMS app";
        mStatusText.setText("State: " + mAppStateMachine.getCurrentState()
                + "\nSMS role: " + smsStatus
                + "\n" + getDefaultSmsDebugStatus());
    }
}
