package com.weatronic.bluetoothtelemetry;

import java.util.UUID;

/**
 * Application-wide constants.
 * Includes named event codes and a default UUID.
 */
public class Constants {
    public static final int INCOMING_MESSAGE = 1;
    public static final int CONN_STATUS = 2;
    public static final int CONN_ERROR = 3;
    public static final int DATA_UPDATED = 4;

    public static final int CONNECTION_SUCCESSFUL = 100;
    public static final int DISCONNECT_SUCCESSFUL = 101;
    public static final int READ_SUCCESSFUL = 103;
    public static final int WRITE_SUCCESSFUL = 104;
    public static final int CONNECTION_ERROR = 201;
    public static final int STREAM_ERROR = 202;
    public static final int READ_ERROR = 203;
    public static final int WRITE_ERROR = 204;

    public static final UUID BASIC_UUID = UUID.fromString("00001101-0000-1000-8000-00805f9b34fb");

    public static final int REQUEST_ENABLE_BT = 100;
    public static final int REQUEST_ENABLE_BT_DIRECT = 101;
    public static final int REQUEST_MAKE_DISCOVERABLE = 200;

}
