package com.sebiai.hc_05bluetoothtest;

import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Set;

// Sauce: https://www.geeksforgeeks.org/all-about-hc-05-bluetooth-module-connection-with-android/

public class MainActivity extends AppCompatActivity {
    // Views
    ListView selectDeviceListView;
    EditText inputEditText;
    Button sendButton;
    TextView connectionStatusTextView;
    public TextView outputTextView;

    // Bluetooth
    BluetoothAdapter bluetoothAdapter = null;
    Set<BluetoothDevice> devices;

    // Other
    boolean onItemClickGate = true;
    Thread thread;
    public View currentView = null;

    @Override
    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setup();
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    void setup() {
        // Bluetooth
        bluetoothAdapter = BluetoothAdapter.getDefaultAdapter();

        // Broadcast Receiver
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(mReceiver, intentFilter);

        // Find all views
            // List devices
        selectDeviceListView = findViewById(R.id.listview_deviceselect);
        pairedDevices();

            // Input text
        inputEditText = findViewById(R.id.edittext_input);
            // Send button
        sendButton = findViewById(R.id.button_send);
        sendButton.setOnClickListener(this::sendButtonOnClick);

        connectionStatusTextView = findViewById(R.id.textview_connectionstatus);
        outputTextView = findViewById(R.id.textview_output);
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void pairedDevices()
    {
        devices = bluetoothAdapter.getBondedDevices();
        ArrayList list = new ArrayList();

        if (devices.size() > 0) {
            for (BluetoothDevice bt : devices) {
                // Add all the available devices to the list
                list.add(bt.getName() + "\n" + bt.getAddress());
            }
        }
        else {
            // In case no device is found
            Toast.makeText(getApplicationContext(), "No Paired Bluetooth Devices Found.", Toast.LENGTH_LONG).show();
        }

        // Adding the devices to the list with ArrayAdapter class
        final ArrayAdapter adapter = new ArrayAdapter(this, android.R.layout.simple_list_item_1, list);
        selectDeviceListView.setAdapter(adapter);

        // Method called when the device from the list is clicked
        selectDeviceListView.setOnItemClickListener(selectDeviceListViewOnItemClick);
    }

    AdapterView.OnItemClickListener selectDeviceListViewOnItemClick = new AdapterView.OnItemClickListener() {
        @Override
        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        public void onItemClick(AdapterView parent, View view, int position, long id) {
            TextView textView = (TextView) view;
            if (textView.getBackground() instanceof ColorDrawable) {
                int color = ((ColorDrawable)textView.getBackground()).getColor();
                if (color == Color.GREEN) {
                    thread.interrupt();
                    return;
                }
            }

            if ((thread != null && thread.isAlive()) || currentView != null) return;
            currentView = view;
            onItemClickGate = false;

            // Color
            view.setBackgroundColor(Color.GREEN);

            // Get the device MAC address
            String name = textView.getText().toString();
            String address = name.substring(name.length() - 17);
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

            // Get UUIDs - broadcast receiver continues
                // Set status
            connectionStatusTextView.setText(R.string.status_gettingUUIDs);
            bluetoothDevice.fetchUuidsWithSdp();
        }
    };

    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();

            if (BluetoothDevice.ACTION_UUID.equals(action)) {
                Runnable bluetoothRunnable = new BluetoothRunnable(intent, (MainActivity) context);
                thread = new Thread(bluetoothRunnable);
                thread.start();
            }
        }
    };

    void initAgain() {
        for (int i = 0; i < selectDeviceListView.getAdapter().getCount(); i++) {
            selectDeviceListView.getAdapter().getView(i, null, null).setBackgroundColor(Color.RED);
        }
        onItemClickGate = true;
    }

    void sendButtonOnClick(View view) {
        // TODO: Do stuff on send
    }
}