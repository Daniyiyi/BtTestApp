package com.sms.spoofing;

public final class AppStateMachine {

    public enum AppState {
        Bluetooth_off,
        Bluetooth_ON,
        BT_connected
    }

    private AppState mCurrentState;

    public AppStateMachine(AppState initialState) {
        mCurrentState = initialState;
    }

    public AppState getCurrentState() {
        return mCurrentState;
    }

    public void onBluetoothTurnedOff() {
        transitionTo(AppState.Bluetooth_off);
    }

    public void onBluetoothTurnedOn() {
        if (mCurrentState == AppState.Bluetooth_off) {
            transitionTo(AppState.Bluetooth_ON);
        }
    }

    public void onBluetoothConnected() {
        if (mCurrentState == AppState.Bluetooth_ON) {
            transitionTo(AppState.BT_connected);
        }
    }

    public void onBluetoothDisconnected() {
        if (mCurrentState == AppState.BT_connected) {
            transitionTo(AppState.Bluetooth_ON);
        }
    }

    private void transitionTo(AppState newState) {
        mCurrentState = newState;
    }
}
