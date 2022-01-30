package com.sebiai.hc_05bluetoothtest;

import android.app.Application;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Context;
import android.content.Intent;
import android.os.Parcelable;
import android.util.Log;
import android.widget.TextView;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import java.io.IOException;
import java.util.UUID;

public class BluetoothRunnable implements Runnable {
    private final MainActivity mainActivity;
    private final Intent intent;
    private final TextView connectionStatusTextView;
    private final BluetoothAdapter bluetoothAdapter;

    private BluetoothSocket btSocket = null;

    BluetoothRunnable(Intent intent, MainActivity mainActivity) {
        this.intent = intent;
        this.mainActivity = mainActivity;
        this.connectionStatusTextView = mainActivity.findViewById(R.id.textview_connectionstatus);
        this.bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();
    }

    @Override
    @RequiresPermission(allOf = {"android.permission.BLUETOOTH_CONNECT", "android.permission.BLUETOOTH_SCAN"})
    public void run() {
        BluetoothDevice bluetoothDevice = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
        Parcelable[] uuids = intent.getParcelableArrayExtra(BluetoothDevice.EXTRA_UUID);

        // Connect
            // Set status
        mainActivity.runOnUiThread(() -> connectionStatusTextView.setText(R.string.status_connecting));
        final UUID deviceUUID = UUID.fromString(uuids[0].toString());
        try {
            if (btSocket == null) {
                // Connect
                btSocket = bluetoothDevice.createInsecureRfcommSocketToServiceRecord(deviceUUID);
                bluetoothAdapter.cancelDiscovery();
                btSocket.connect();
                mainActivity.runOnUiThread(() -> Log.d("Test", "Works!"));
                // Set status
                mainActivity.runOnUiThread(() -> connectionStatusTextView.setText(R.string.status_connected));

                try {
                    btSocket.getOutputStream().write("Jo Mama".getBytes());
                }
                catch (IOException e) {
                    e.printStackTrace();
                }

                while (!Thread.interrupted()) { }

                try {
                    btSocket.close();
                } catch (IOException e) { }

                mainActivity.runOnUiThread(() -> connectionStatusTextView.setText(R.string.status_notConnected));
            }
        } catch (IOException e) {
            e.printStackTrace();
            mainActivity.runOnUiThread(() -> {
                Log.d("BluetoothRunnable", "No valid device!");
                connectionStatusTextView.setText(R.string.status_noValidDevice);
            });
            try {
                btSocket.close();
            } catch (IOException ee) { }
        }

        mainActivity.runOnUiThread(() -> {
            mainActivity.currentView.setBackground(mainActivity.outputTextView.getBackground());
            mainActivity.currentView = null;
        });
    }
}
