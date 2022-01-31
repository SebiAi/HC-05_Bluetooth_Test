package com.sebiai.hc_05bluetoothtest;

import java.util.UUID;

public interface Constants {
    // Bluetooth states
    int STATE_NONE = 0;
    int STATE_NOT_INITIALIZED = 1;
    int STATE_ERROR = 2;
    int STATE_CONNECTING = 3;
    int STATE_CONNECTED = 4;

    // Bluetooth message types
    int MESSAGE_STATE_CHANGE = 1;
    int MESSAGE_READ = 2;
    int MESSAGE_WRITE = 3;
    int MESSAGE_INFO = 4;


    String INFO = "info";
    UUID myUUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");
}
