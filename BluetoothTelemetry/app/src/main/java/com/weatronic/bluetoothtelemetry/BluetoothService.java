package com.weatronic.bluetoothtelemetry;

import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothSocket;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.os.Handler;
import android.preference.PreferenceManager;
//import android.util.Log;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.Toast;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Set;
/**
 * A class handling Bluetooth connections.
 */
public class BluetoothService{

    /**
     * Stores class instance to be returned.
     * See {@link #getInstance(Context, Handler) getInstance}
     */
    static BluetoothService instance;
    /**
     * Calling activity.
     */
    private Activity caller;
    /**
     * Context of calling activity.
     */
    private Context ctx;
    /**
     * A handler to send incoming data to.
     * Connection is handled within a thread, therefore functions from other activity cannot be called directly.
     */
    private Handler incomingHandler;

    /**
     * Bluetooth adapter.
     */
    private BluetoothAdapter btAdapter;
    /**
     * Bluetooth socket.
     */
    private BluetoothSocket btSocket;

    /**
     * List of paired bluetooth devices. Used in connection dialog.
     */
    private ArrayAdapter<String> pairedDevices;
    /**
     * List of paired device mac-addresses.
     */
    private ArrayList<String> pairedDeviceMac;
    /**
     * List of bluetooth devices found using scan. Used in connection dialog.
     */
    private ArrayAdapter<String> foundDevices;
    /**
     * List of found device mac-addresses.
     */
    private ArrayList<String> foundDeviceMac;

    /**
     * Stores current state of reconnect attempt.
     * The program reacts differently on some events depending on this state.
     */
    private boolean reconnecting = false;
    /**
     * Stores current state of connection attempt.
     * The program reacts differently on some events depending on this state.
     */
    private boolean connectInProgress = false;
    /**
     * Stores the maximum number of reconnect attempts.
     */
    private int retriesMax = 5;
    /**
     * Stores the sequence number of current retry attempt.
     */
    private int retriesCur = 0;
    /**
     * A progress dialog showing that an action is being performed.
     */
    private ProgressDialog progress;
    /**
     * Instance of {@link ConnectThread}
     */
    private ConnectThread connect;
    /**
     * Instance of {@link ConnectedThread}
     */
    private ConnectedThread connection;
    /**
     * Application preferences.
     */
    private SharedPreferences prefs;
    /**
     * Access to application preferences.
     */
    private SharedPreferences.Editor prefsEditor;
    /**
     * Reacts on Android OS events such as when a new bluetooth device is found.
     */
    private final BroadcastReceiver mReceiver = new BroadcastReceiver() {
        public void onReceive(Context context, Intent intent) {
            String action = intent.getAction();
            if (action.equals(BluetoothDevice.ACTION_FOUND)) {
                try {
                    BluetoothDevice device = intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE);
                    addFoundDevice(device);
                } catch (Exception e) {
                    printMessage(e.getMessage());
                }
            }else {
                if(action.equals(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)){
                    try {
                        printMessage("Scan finished");
                    } catch (Exception e) {
                        printMessage(e.getMessage());
                    }
                }else{
                    if(action.equals(BluetoothDevice.ACTION_ACL_DISCONNECTED )){
                        if(btSocket != null && btSocket.isConnected()){
                            try {
                                disconnect();
                                initReconnect();
                            } catch (Exception e) {
                                printMessage(e.getMessage());
                            }
                        }/*else{
                            we initiated the disconnect, ignore
                        }*/

                    }
                }
            }
        }
    };
    /**
     * Prints a toast message in calling activity.
     */
    private void printMessage(String message){
        Toast.makeText(ctx, message, Toast.LENGTH_SHORT).show();
    }
    /**
     * Returns an instance of Bluetooth Service.
     * Constructs new instance if it has not been constructed; otherwise returns existing instance.
     * Makes sure the same instance is used.
     */
    public static BluetoothService getInstance(Context ctx, Handler h){
        if(instance == null){
            instance = new BluetoothService(ctx, h);
        }
        return instance;
    }
    /**
     * Class constructor. Initializes fields, preference access and list of OS events to react to.
     * @see #mReceiver
     */
    public BluetoothService(Context ctx, Handler h){
        this.caller = (Activity)ctx;

        this.ctx = ctx;
        this.incomingHandler = h;

        this.prefs = PreferenceManager.getDefaultSharedPreferences(ctx);
        this.prefsEditor = prefs.edit();
        prefsEditor.apply();

        IntentFilter filter = new IntentFilter();
        filter.addAction(BluetoothDevice.ACTION_FOUND);
        filter.addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED);
        filter.addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED);
        ctx.registerReceiver(mReceiver, filter);
    }
    /**
     * Breaks all connections, stops all processes and destroys service instance.
     */
    public void stop(){
        disconnect();
        try {
            ctx.unregisterReceiver(mReceiver);
        }catch(Exception e){
            //not registered
        }
        instance = null;
    }
    /**
     * Returns true if Bluetooth is available.
     * Promts to turn it on if possible.
     * @param code Defines the action to take if Bluetooth is turned on.
     */
    private boolean btEnabled(int code){
        boolean on = false;
        btAdapter = BluetoothAdapter.getDefaultAdapter();
        if(btAdapter==null) {
            printMessage("No Bluetooth adapter available. Running an emulator?");
        } else {
            if (btAdapter.isEnabled()) {
                on = true;
            } else {
                Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE);
                caller.startActivityForResult(enableBtIntent, code);
            }
        }
        return on;
    }
    /**
     * Creates a connection dialog with list of devices.
     * Paired devices only first.
     */
    public void initConnectionDialog(){
        resetReconnect();
        if(!btEnabled(Constants.REQUEST_ENABLE_BT)) return;
        makePairedList();
        foundDevices = new ArrayAdapter<>(ctx, R.layout.device_name);
        foundDeviceMac = new ArrayList<>();
        showDeviceList();
    }
    /**
     * Builds a list of paired Bluetooth devices to display.
     */
    private void makePairedList(){
        Set<BluetoothDevice> pairedDevicesSet = btAdapter.getBondedDevices();
        pairedDevices = new ArrayAdapter<>(ctx, R.layout.device_name);
        pairedDeviceMac = new ArrayList<>();
        for(BluetoothDevice bt : pairedDevicesSet) {
            pairedDevices.add(bt.getName() + "\n" + bt.getAddress());
            pairedDeviceMac.add(bt.getAddress());
        }
    }
    /**
     * Connection dialog.
     */
    class deviceListDialog extends Dialog {
        /**
         * Interface element containing names of paired devices.
         */
        private ListView pairedListView;
        /**
         * Interface element containing names of discovered devices.
         */
        private ListView foundListView;
        /**
         * Class constructor.
         * Initializes interface elements and variables.
         */
        protected deviceListDialog(Context context) {
            super(context);
            final deviceListDialog dialog_instance = this;
            setContentView(R.layout.device_list_dialog);
            pairedListView = (ListView)findViewById(R.id.pairedDeviceListView);
            pairedListView.setAdapter(pairedDevices);
            foundListView = (ListView)findViewById(R.id.foundDeviceListView);
            foundListView.setAdapter(foundDevices);
            pairedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View clicked_view, int arg2, long row) {
                    if (adapter.getCount() > 0) {
                        connect(pairedDeviceMac.get((int) row));
                    }
                    dialog_instance.dismiss();
                }
            });
            foundListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View clicked_view, int arg2, long row) {
                    if(adapter.getCount() > 0) {
                        connect(foundDeviceMac.get((int) row));
                    }
                    dialog_instance.dismiss();
                }
            });

            final View scanBtn = findViewById(R.id.scanButton);

            scanBtn.setOnClickListener(new View.OnClickListener() {
                public void onClick(View v) {
                    findViewById(R.id.scanButton).setVisibility(View.GONE);
                    findViewById(R.id.foundDeviceListView).setVisibility(View.VISIBLE);
                    findViewById(R.id.foundTitle).setVisibility(View.VISIBLE);
                    btAdapter.startDiscovery();
                }
            });

            if(pairedDevices.isEmpty()) {
                findViewById(R.id.pairedDeviceListView).setVisibility(View.GONE);
                findViewById(R.id.pairedTitle).setVisibility(View.GONE);
            }

            setTitle(ctx.getString(R.string.Available_devices));
        }
    }
    /**
     * Constructs and shows a {@link deviceListDialog}
     */
    private void showDeviceList() {
        final Dialog showDevicesDialog = new deviceListDialog(ctx);
        showDevicesDialog.show();
    }
    /**
     * Adds device into list of found devices.
     * @see #mReceiver
     */
    private void addFoundDevice(BluetoothDevice device){
        foundDevices.add(device.getName() + "\n" + device.getAddress());
        foundDeviceMac.add(device.getAddress());
    }
    /**
     * Attempts to connect to the chosen Bluetooth device.
     * Checks state of current connection; if connection is possible and necessary (not already connected to the same address),
     * breaks the connection and established a new one.
     * @param btTargetMac Mac address to connect to
     */
    public void connect(String btTargetMac){

        prefsEditor.putString("lastAttemptedAddress",btTargetMac);
        prefsEditor.commit();

        if(!btEnabled(Constants.REQUEST_ENABLE_BT_DIRECT)) return;

        btAdapter.cancelDiscovery();

        if(btSocket != null){
            if(btSocket.getRemoteDevice().getAddress().equals(btTargetMac)){
                printMessage(ctx.getString(R.string.Already_connected));
                return;
            }
        }

        if(!BluetoothAdapter.checkBluetoothAddress(btTargetMac)){
            printMessage(ctx.getString(R.string.Invalid_address));
            return;
        }

        disconnect();

        BluetoothDevice device = btAdapter.getRemoteDevice(btTargetMac);

        if(!reconnecting){
            progress = ProgressDialog.show(ctx, ctx.getString(R.string.Connecting_to) + " " + device.getName() + "... ", ctx.getString(R.string.Please_wait), true, false);
        }

        connect = new ConnectThread(device);
        connectInProgress = true;
        connect.start();

    }
    /**
     * Dismisses progress dialog. Also resets state.
     */
    public void dismissProgress(){
        resetReconnect();
        try{
            progress.dismiss();
        }catch(Exception e){
            //nothing to dismiss
        }
    }
    /**
     * Attempts to send a Bluetooth message to connected device.
     * @param message Message to send
     * @see ConnectedThread
     */
    public void send(String message){
        if(connection != null) {
            try {
                connection.write(message);
            } catch (Exception e) {
                printMessage(e.getMessage());
            }
        }
    }
    /**
     * Sends a request to make the device visible to other bluetooth devices.
     */
    public void makeDiscoverable(){
        Intent makeDiscoverableIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE);
        caller.startActivityForResult(makeDiscoverableIntent, Constants.REQUEST_MAKE_DISCOVERABLE);
    }
    /**
     * Breaks current connection.
     */
    public void disconnect(){
        if(btSocket != null && btSocket.isConnected()) {
            try {
                connect.cancel();
            } catch (Exception e) {
                //thread is null
            }
            try {
                connection.cancel();
            } catch (Exception e) {
                //thread is null
            }

            btSocket = null;
        }/*else{
            no connection to break
        }*/
    }
    /**
     * Starts reconnect process.
     * Initializes popup dialog, sets state and starts the process.
     * @see #reconnect()
     */
    private void initReconnect(){
        reconnecting = true;
        progress = new ProgressDialog(ctx);
        progress.setTitle(ctx.getString(R.string.Reconnecting));
        progress.setIndeterminate(true);
        progress.setCancelable(false);
        //progress = ProgressDialog.show(this, "Connection lost. Reconnecting...", "", true, false);
        progress.setButton(DialogInterface.BUTTON_NEGATIVE, ctx.getString(R.string.Cancel), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                resetReconnect();
                if (connectInProgress) {
                    progress = new ProgressDialog(ctx);
                    progress.setTitle(ctx.getString(R.string.Finishing_connect));
                    progress.setIndeterminate(true);
                    progress.setCancelable(false);
                    progress.setMessage(ctx.getString(R.string.Please_wait));
                    progress.show();
                }
            }
        });
        progress.show();
        reconnect();
    }
    /**
     * Attempts to reconnect to the last known mac address.
     * A timer makes so that there is a growing pause between attempts.
     * Attempts until number of attempts is above maximum or manually cancelled.
     */
    private void reconnect(){
        retriesCur++;
        //0, 2, 4, 8, 16...
        int timeOutSec = 0;
        if(retriesCur > 1){
            timeOutSec = (int)Math.pow(2, retriesCur - 1);
        }
        final String progMsgBase = ctx.getString(R.string.Attempt) + retriesCur + ctx.getString(R.string.Out_of) + retriesMax;
        progress.setMessage(progMsgBase + "\n" + ctx.getString(R.string.Next_attempt_in) + " " + timeOutSec + " " + ctx.getString(R.string.Seconds) + "...");
            if(retriesCur <= retriesMax){
            //retry

            new CountDownTimer(timeOutSec * 1000, 1000){
                public void onFinish(){
                    //check if cancelled
                    if(reconnecting) {
                        progress.setMessage(R.string.Attempt + retriesCur + R.string.Out_of + retriesMax + "\n"+ctx.getString(R.string.Attempting_reconnect));
                        connect(prefs.getString("lastSuccessfulAddress", "00:00:00:00:00:00"));
                    }
                }
                public void onTick(long timeRemaining){
                    if(reconnecting) {
                        progress.setMessage(progMsgBase +  "\n" + R.string.Next_attempt_in+ " " + (int)(timeRemaining / 1000) + " " + R.string.Seconds + "...");
                    }else{
                        this.cancel();
                    }
                }
            }.start();

        }else {
            //reset
            dismissProgress();
            printMessage(ctx.getString(R.string.Reconnect_number_exceeded));
        }
    }
    /**
     * Resets reconnect state.
     */
    private void resetReconnect(){
        reconnecting = false;
        retriesCur = 0;
    }




    // ---- here be dragons



    /**
     * Attempts to connect inside a thread, so that GUI is not blocked.
     * @see #connect
     */
    private class ConnectThread extends Thread {
        private final BluetoothSocket mmSocket;
        private final BluetoothDevice mmDevice;

        public ConnectThread(BluetoothDevice device) {

            BluetoothSocket tmp = null;
            mmDevice = device;

            try{
                tmp = device.createRfcommSocketToServiceRecord(Constants.BASIC_UUID);
            }catch(Exception e){
                //device is null
            }

            mmSocket = tmp;

        }

        public void run() {
            try{
                mmSocket.connect();
            }catch(Exception connectEx){
                try{
                    mmSocket.close();
                }catch(Exception closeEx){
                    //socket is null
                }
                connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.CONNECTION_ERROR).sendToTarget();
                connectionHandler.obtainMessage(Constants.CONN_ERROR, connectEx.getMessage()).sendToTarget();
                connectInProgress = false;
                return;
            }

            prefsEditor.putString("lastSuccessfulAddress", mmDevice.getAddress());
            prefsEditor.commit();

            connection = new ConnectedThread(mmSocket);
            connection.start();

            connectInProgress = false;
            connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.CONNECTION_SUCCESSFUL).sendToTarget();

        }

        public void cancel(){
            try{
                mmSocket.close();
                connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.DISCONNECT_SUCCESSFUL).sendToTarget();
            }catch(Exception e){
                //socket is null
            }
        }

    }
    /**
     * Listens on input stream and sends messages through output stream on the connection.
     */
    private class ConnectedThread extends Thread {
        private final InputStream mmInStream;
        private final OutputStream mmOutStream;
        private final BluetoothSocket mmSocket;

        public ConnectedThread(BluetoothSocket socket) {

            InputStream tmpIn = null;
            OutputStream tmpOut = null;

            try {
                tmpIn = socket.getInputStream();
                tmpOut = socket.getOutputStream();
            } catch (IOException e) {
                connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.STREAM_ERROR).sendToTarget();
            }

            btSocket = socket;

            mmSocket = socket;
            mmInStream = tmpIn;
            mmOutStream = tmpOut;
        }

        public void run() {
            byte[] buffer = new byte[256];
            int bytes;

            while (true) {
                try {
                    bytes = mmInStream.read(buffer);
                    incomingHandler.obtainMessage(Constants.INCOMING_MESSAGE, bytes, -1, buffer).sendToTarget();
                    //h.obtainMessage(CONN_STATUS, READ_SUCCESSFUL).sendToTarget();
                } catch (IOException e) {
                    connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.READ_ERROR).sendToTarget();
                    break;
                }
            }
        }
        /**
         * Attempts to send a bluetooth message.
         * @see #send(String)
         */
        public void write(String message) {
            //printMessage("Sending" + message);
            byte[] msgBuffer = message.getBytes();
            try {
                mmOutStream.write(msgBuffer);
                connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.WRITE_SUCCESSFUL).sendToTarget();
            } catch (IOException e) {
                connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.WRITE_ERROR).sendToTarget();
            }
        }

        public void cancel(){
            try{
                mmInStream.close();
            }catch(Exception e){
                //stream is null
            }

            try{
                mmOutStream.close();
            }catch(Exception e){
                //stream is null
            }

            try{
                mmSocket.close();
                connectionHandler.obtainMessage(Constants.CONN_STATUS, Constants.DISCONNECT_SUCCESSFUL).sendToTarget();
            }catch(Exception e){
                //socket is null
            }

        }

    }

    /** Handler watching connection status and initiating reaction
     *  If reaction was directly in thread, there would be infinite nested threads
     */
    private Handler connectionHandler = new Handler(new Handler.Callback() {

        String errString = "";

        public boolean handleMessage(android.os.Message msg) {
            switch (msg.what) {
                case Constants.CONN_STATUS:
                    int status = (int) msg.obj;
                    switch(status){
                        case Constants.CONNECTION_SUCCESSFUL:
                            printMessage(ctx.getString(R.string.Connected));
                            dismissProgress();
                            if(reconnecting) resetReconnect();
                            break;
                        case Constants.CONNECTION_ERROR:
                            errString = ctx.getString(R.string.Connection_failed)+": \n";
                            if(reconnecting){
                                reconnect();
                            }else{
                                dismissProgress();
                            }
                            break;
                        case Constants.DISCONNECT_SUCCESSFUL:
                            printMessage(ctx.getString(R.string.Disconnected));
                            break;
                        default:
                            //printMessage("" + status);
                            break;
                    }
                    break;
                case Constants.CONN_ERROR:
                    printMessage(errString + msg.obj);
                    break;
            }
            return true;
        }
    });

}
