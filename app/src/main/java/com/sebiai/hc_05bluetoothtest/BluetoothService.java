package com.sebiai.hc_05bluetoothtest;

import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.Intent;
import android.util.Log;

import androidx.annotation.RequiresPermission;
import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.UUID;

public class BluetoothService {
    private final String TAG = "BluetoothService";

    private int state;

    BluetoothDevice device;

    ConnectThread connectThread;
    ConnectedThread connectedThread;

    public BluetoothService(BluetoothDevice device) {
        state = Constants.STATE_NONE;
        this.device = device;
    }

    public BluetoothService() {
        state = Constants.STATE_NOT_INITIALIZED;
        this.device = null;
    }

    @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
    public synchronized void connect() {
        if (state == Constants.STATE_NOT_INITIALIZED)
            throw new NullPointerException("Handler or Device can't be null! => Create object with BluetoothService(Handler handler, BluetoothDevice device)");
        setState(Constants.STATE_CONNECTING);
        connectThread = new ConnectThread(device);
        connectThread.start();
    }

    public synchronized void stop() {
        cancelConnectThread();
        cancelConnectedThread();
        setState(Constants.STATE_NONE);
    }

    private synchronized void setState(int state) {
        Log.d(TAG, "setState() " + this.state + " -> " + state);
        this.state = state;
        // Broadcast the new state to the Handler so the UI Activity can update
        broadcastState(state);
    }

    private synchronized void broadcastState(int state) {
        Intent intent = new Intent(Constants.INTENT_BT_MESSAGE);
        intent.putExtra(Constants.KEY_WHAT, Constants.MESSAGE_STATE_CHANGE);
        intent.putExtra(Constants.KEY_STATE, state);
        LocalBroadcastManager.getInstance(cBaseApplication.getAppContext()).sendBroadcast(intent);
    }

    private synchronized void broadcastMessageRead(String message) {
        Intent intent = new Intent(Constants.INTENT_BT_MESSAGE);
        intent.putExtra(Constants.KEY_WHAT, Constants.MESSAGE_READ);
        intent.putExtra(Constants.KEY_MESSAGE, message);
        LocalBroadcastManager.getInstance(cBaseApplication.getAppContext()).sendBroadcast(intent);
    }

    private synchronized void broadcastMessageWrite(String message) {
        Intent intent = new Intent(Constants.INTENT_BT_MESSAGE);
        intent.putExtra(Constants.KEY_WHAT, Constants.MESSAGE_WRITE);
        intent.putExtra(Constants.KEY_MESSAGE, message);
        LocalBroadcastManager.getInstance(cBaseApplication.getAppContext()).sendBroadcast(intent);
    }

    private synchronized void broadcastInfo(String info) {
        Intent intent = new Intent(Constants.INTENT_BT_MESSAGE);
        intent.putExtra(Constants.KEY_WHAT, Constants.MESSAGE_INFO);
        intent.putExtra(Constants.KEY_MESSAGE, info);
        LocalBroadcastManager.getInstance(cBaseApplication.getAppContext()).sendBroadcast(intent);
    }

    public synchronized int getState() {
        return state;
    }


    public synchronized void connected(BluetoothSocket socket) {
        cancelConnectThread();
        // Start the thread to manage the connection and perform transmissions
        connectedThread = new ConnectedThread(socket);
        connectedThread.start();

        setState(Constants.STATE_CONNECTED);
    }

    public boolean isWorking() {
        return state >= Constants.STATE_CONNECTING;
    }

    /**
     * Indicate that the connection attempt failed and notify the UI Activity.
     */
    private void connectionFailed() {
        Log.e(TAG, "Connection Failed");
        // Send a failure item_message back to the Activity
        broadcastInfo("Unable to connect!");
        setState(Constants.STATE_ERROR);
        cancelConnectThread();
    }

    /**
     * Indicate that the connection was lost and notify the UI Activity.
     */
    private void connectionLost() {
        Log.e(TAG, "Connection Lost");
        // Send a failure item_message back to the Activity
        broadcastInfo("Connection was lost");
        setState(Constants.STATE_ERROR);
        cancelConnectedThread();
    }

    private void cancelConnectThread() {
        // Cancel the thread that completed the connection
        if (connectThread != null) {
            connectThread.cancel();
            connectThread = null;
        }
    }

    private void cancelConnectedThread() {
        // Cancel any thread currently running a connection
        if (connectedThread != null) {
            connectedThread.cancel();
            connectedThread = null;
        }
    }

    public void write(byte[] out) {
        // Create temporary object
        ConnectedThread r;
        // Synchronize a copy of the ConnectedThread
        synchronized (this) {
            if (state != Constants.STATE_CONNECTED) {
                Log.e(TAG, "Trying to send but not connected");
                return;
            }
            r = connectedThread;
        }

        // Perform the write unsynchronized
        r.write(out);
    }

    public void write(String out) {
        write(out.getBytes(StandardCharsets.UTF_8));
    }

    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        public ConnectThread(BluetoothDevice device) {
            // Use a temporary object that is later assigned to mmSocket,
            // because mmSocket is final
            BluetoothSocket tmpSocket = null;
            mmDevice = device;

            // Get a BluetoothSocket to connect with the given BluetoothDevice
            try {
                // MY_UUID is the app's UUID string, also used by the server code
                UUID uuid = Constants.myUUID;
                tmpSocket = mmDevice.createRfcommSocketToServiceRecord(uuid);
            } catch (IOException e) {
                Log.e(TAG, "Create RFcomm socket failed", e);
            }
            mmSocket = tmpSocket;
        }

        @RequiresPermission(value = "android.permission.BLUETOOTH_CONNECT")
        public void run() {
            try {
                // Connect the device through the socket. This will block
                // until it succeeds or throws an exception
                mmSocket.connect();
            } catch (IOException connectException) {
                // Unable to connect; close the socket and get out
                Log.e(TAG, "Unable to connect", connectException);
                try {
                    mmSocket.close();
                } catch (IOException closeException) {
                    Log.e(TAG, "Unable to close() socket during connection failure", closeException);
                }
                connectionFailed();
                return;
            }

            synchronized (BluetoothService.this) {
                connectThread = null;
            }

            // Do work to manage the connection (in a separate thread)
            connected(mmSocket);
        }

        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "Close() socket failed", e);
            }
        }
    }


    public class ConnectedThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;

        private boolean stop;

        public ConnectedThread(BluetoothSocket socket) {
            mmSocket = socket;
            InputStream tmpIn = null;
            OutputStream tmpOut = null;
            stop = false;

            // Get the input and output streams, using temp objects because
            // member streams are final
            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                Log.e(TAG, "Temp sockets not created", e);
            }

            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            Log.i(TAG, "Begin connectedThread");
            byte[] buffer = new byte[1024];  // buffer store for the stream
            int bytes; // bytes returned from read()

            StringBuilder readMessage = new StringBuilder();

            // Keep listening to the InputStream until an exception occurs
            while (true) {
                try {

                    bytes = mmInStream.read(buffer);
                    String read = new String(buffer, 0, bytes);
                    readMessage.append(read);

                    if (read.contains("\n")) {
                        broadcastMessageRead(readMessage.toString());
                        readMessage.setLength(0);
                    }

                } catch (IOException e) {
                    if (stop)
                        return;
                    Log.e(TAG, "Connection Lost", e);
                    connectionLost();
                    break;
                }
            }
        }

        /* Call this from the main activity to send data to the remote device */
        public void write(byte[] bytes) {
            try {
                mmOutStream.write(bytes);
                broadcastMessageWrite(new String(bytes));
            } catch (IOException e) {
                Log.e(TAG, "Exception during write", e);
            }
        }

        /* Call this from the main activity to shutdown the connection */
        /** Will cancel an in-progress connection, and close the socket */
        public void cancel() {
            // Gently stop
            stop = true;

            try {
                mmSocket.close();
            } catch (IOException e) {
                Log.e(TAG, "close() of connect socket failed", e);
            }
        }
    }
}
