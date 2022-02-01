package com.sebiai.hc_05bluetoothtest;

import android.app.Application;
import android.content.Context;

public class cBaseApplication extends Application {

    public BluetoothService bluetoothService;

    private static Context context;

    @Override
    public void onCreate() {
        super.onCreate();

        // Code
        bluetoothService = new BluetoothService();
        cBaseApplication.context = getAppContext();
    }

    public static Context getAppContext() {
        return cBaseApplication.context;
    }
}
