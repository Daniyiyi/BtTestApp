package com.brcm.bttestapp;

import android.app.Activity;
import android.bluetooth.BluetoothAdapter;
import android.os.Bundle;
import android.util.Log;

public class MainActivity extends Activity {

    private static final String TAG = "MainActivity";

    private AppStateMachine mAppStateMachine;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        BluetoothAdapter adapter = BluetoothAdapter.getDefaultAdapter();
        AppStateMachine.AppState initialState =
                (adapter != null && adapter.isEnabled())
                        ? AppStateMachine.AppState.Bluetooth_ON
                        : AppStateMachine.AppState.Bluetooth_off;

        mAppStateMachine = new AppStateMachine(initialState);
        Log.i(TAG, "Initial app state: " + mAppStateMachine.getCurrentState());
    }

    public AppStateMachine.AppState getCurrentAppState() {
        return mAppStateMachine.getCurrentState();
    }
}
