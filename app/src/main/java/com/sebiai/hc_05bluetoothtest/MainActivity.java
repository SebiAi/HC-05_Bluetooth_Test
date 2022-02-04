package com.sebiai.hc_05bluetoothtest;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.RequiresPermission;
import androidx.appcompat.app.AppCompatActivity;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Set;

// Sauce: https://www.geeksforgeeks.org/all-about-hc-05-bluetooth-module-connection-with-android/
// Sauce2: https://github.com/MEnthoven/Android-HC05-App
// TODO: Implement for multiple activities/fragments: https://stackoverflow.com/questions/17082393/handlers-and-multiple-activities

public class MainActivity extends AppCompatActivity {
    // Views
    ListView selectDeviceListView;
    EditText inputEditText;
    Button sendButton;
    Button clearButton;
    TextView connectionStatusTextView;
    public TextView outputTextView;

    // Bluetooth
    BluetoothAdapter bluetoothAdapter = null;
    Set<BluetoothDevice> devices;
    mReceiver receiver = null;

    // Other
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
        /*
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(BluetoothDevice.ACTION_UUID);
        registerReceiver(mReceiver, intentFilter);
         */

        // Find all views
            // List devices
        selectDeviceListView = findViewById(R.id.listview_deviceselect);
        pairedDevices();

            // Input text
        inputEditText = findViewById(R.id.edittext_input);
        inputEditText.setOnKeyListener(this::inputEditTextOnKey);
            // Send button
        sendButton = findViewById(R.id.button_send);
        sendButton.setOnClickListener(this::sendButtonOnClick);
        sendButton.setOnLongClickListener(this::sendButtonOnLongClick);
            // Clear button
        clearButton = findViewById(R.id.button_clear);
        clearButton.setOnClickListener(this::clearButtonOnClick);

        connectionStatusTextView = findViewById(R.id.textview_connectionstatus);
        outputTextView = findViewById(R.id.textview_output);
    }

    @Override
    protected void onResume() {
        super.onResume();

        receiver = new mReceiver(this);
        IntentFilter filter = new IntentFilter(Constants.INTENT_BT_MESSAGE);
        LocalBroadcastManager.getInstance(this).registerReceiver(receiver, filter);
    }

    @Override
    protected void onPause() {
        super.onPause();

        try {
            LocalBroadcastManager.getInstance(this).unregisterReceiver(receiver);
        } catch (IllegalArgumentException e) {
            // Should not matter
        }
        receiver = null;
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    private void pairedDevices()
    {
        devices = bluetoothAdapter.getBondedDevices();
        ArrayList<String> list = new ArrayList<>();

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
        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, list);
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
                    getBluetoothService().stop();
                    return;
                }
            }

            if (getBluetoothService().isWorking())
                return;

            currentView = view;

            // Color
            view.setBackgroundColor(Color.GREEN);

            // Get the device MAC address
            String name = textView.getText().toString();
            String address = name.substring(name.length() - 17);
            BluetoothDevice bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);

            // Let BluetoothService handle everything
            setBluetoothService(new BluetoothService(bluetoothDevice));
            getBluetoothService().connect();

            /*
            // Get UUIDs - broadcast receiver continues
                // Set status
            connectionStatusTextView.setText(R.string.status_gettingUUIDs);
            bluetoothDevice.fetchUuidsWithSdp();
             */
        }
    };

    /*
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
     */

    ActivityResultLauncher<Intent> mActivityResultLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            new ActivityResultCallback<ActivityResult>() {
                @Override
                public void onActivityResult(ActivityResult result) {
                    if (result.getResultCode() == Constants.STATE_ERROR) {
                        onError();
                    }
                }
            });

    private boolean sendButtonOnLongClick(View view) {
        Intent intent = new Intent(this, TestActivity.class);
        //startActivity(intent);
        mActivityResultLauncher.launch(intent);
        return true;
    }
    void sendButtonOnClick(View view) {
        String sendString = inputEditText.getText().toString() + "\r\n";
        addMessageToTextView("S: " + sendString);
        getBluetoothService().write(sendString);

        //inputEditText.setText("");
    }
    private boolean inputEditTextOnKey(View view, int keyCode, KeyEvent keyEvent) {
        if (keyEvent.getAction() == KeyEvent.ACTION_UP) {
            if (keyCode == KeyEvent.KEYCODE_ENTER) {
                if (sendButton.isEnabled())
                    sendButtonOnClick(view);
                return true;
            }
        }
        return false;
    }

    void clearButtonOnClick(View view) {
        outputTextView.setText("");
    }

    BluetoothService getBluetoothService() {
        return ((cBaseApplication)getApplicationContext()).bluetoothService;
    }

    void setBluetoothService(BluetoothService bluetoothService) {
        ((cBaseApplication)getApplicationContext()).bluetoothService = bluetoothService;
    }

    void addMessageToTextView(String out) {
        String text = out + outputTextView.getText().toString();
        outputTextView.setText(text);
    }

    private void onError() {
        connectionStatusTextView.setText("Error - Not Connected");
        currentView.setBackground(outputTextView.getBackground());
        sendButton.setEnabled(false);
    }

    private static class mReceiver extends BroadcastReceiver {
        private final WeakReference<MainActivity> mActivity;

        public mReceiver(MainActivity activity) {
            mActivity = new WeakReference<>(activity);
        }

        @Override
        public void onReceive(Context context, Intent intent) {
            // No need to check intent action - only one intent will trigger this
            final MainActivity activity = mActivity.get();

            Bundle extras = intent.getExtras();
            switch (extras.getInt(Constants.KEY_WHAT)) {
                case Constants.MESSAGE_STATE_CHANGE:
                    switch (extras.getInt(Constants.KEY_STATE)) {
                        case Constants.STATE_CONNECTED:
                            activity.connectionStatusTextView.setText("Connected!");
                            activity.sendButton.setEnabled(true);
                            break;
                        case Constants.STATE_CONNECTING:
                            activity.connectionStatusTextView.setText("Connecting...");
                            break;
                        case Constants.STATE_NONE:
                            activity.connectionStatusTextView.setText("Not Connected");
                            activity.currentView.setBackground(activity.outputTextView.getBackground());
                            activity.sendButton.setEnabled(false);
                            break;
                        case Constants.STATE_ERROR:
                            activity.onError();
                            break;
                    }
                    break;
                case Constants.MESSAGE_READ:
                    String message = extras.getString(Constants.KEY_MESSAGE);
                    activity.addMessageToTextView("R: " + message);
                    break;
            }
        }
    }
}