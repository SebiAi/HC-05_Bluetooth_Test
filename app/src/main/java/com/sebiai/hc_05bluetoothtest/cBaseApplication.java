package com.sebiai.hc_05bluetoothtest;

import android.app.Application;

public class cBaseApplication extends Application {

    public BluetoothService bluetoothService;

    @Override
    public void onCreate() {
        super.onCreate();

        // Code
        bluetoothService = new BluetoothService();
    }
}
