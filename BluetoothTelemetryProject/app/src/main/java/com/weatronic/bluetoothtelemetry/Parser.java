package com.weatronic.bluetoothtelemetry;

import android.content.Context;
import java.util.Observable;

/**
 * Parses Bluetooth message strings: extracts the list of field IDs and data values.
 * Stores the IDs, sets the values of corresponding fields in {@link TelemetryDataContainer telemetry container}
 */
public class Parser extends Observable {
    /**
     * Stores class instance to be returned.
     * See {@link #getInstance(Context) getInstance}
     */
    public static Parser instance = null;
    /**
     * Context of the calling activity.
     * See {@link Parser(Context) constructor}
     */
    private Context ctx;
    /**
     * See {@link TelemetryDataContainer}
     */
    private TelemetryDataContainer telemetry = TelemetryDataContainer.getInstance(ctx);
    /**
     * List of field IDs from latest configuration message.
     */
    public long[] configFields = new long[0];
    /**
     * Latest config message as string. Used in {@link MainActivity.fieldListDialog#useLastConfig()}
     */
    public String lastConfigMessage = "";
    /**
     * Returns an instance of Parser.
     * Constructs new instance if it has not been constructed; otherwise returns existing instance.
     * Makes sure the same instance is used.
     */
    public static Parser getInstance(Context ctx){
        if(instance == null){
            instance = new Parser(ctx);
        }
        return instance;
    }
    /**
     * A simple constructor.
     * @param ctx Context of the calling activity
     */
    public Parser(Context ctx){
        this.ctx = ctx;
    }
    /**
     * Converts field IDs from hex-strings to integers and stores for later use.
     * @param values List of field IDs to be converted and stored
     */
    public void processConfig(String[] values){
        int size = values.length;
        configFields = new long[size];
        for(int i = 1; i < size; i++){
            try {
                configFields[i] = Long.parseLong(values[i], 16);
            }catch(Exception e){
                //bad string, ignoring
                configFields[i] = 0;
            }
        }
    }
    /**
     * Sets data values to telemetry data fields, if possible.
     * The list of field IDs is stored in {@link #configFields}.
     * Conversion is handled by the {@link TelemetryDataContainer#setDataById(long, String) setter function}.
     * @param values List of values to be set
     */
    public void processData(String[] values){
        int size = values.length;
        for(int i = 1; i < size; i++){
            //if somehow config array is smaller
            if(i >= configFields.length) break;
            //if id is correct
            if(configFields[i] != 0) {
                telemetry.setDataById(configFields[i], values[i]);
            }
        }
        setChanged();
        notifyObservers();
    }
    /**
     * Reads a Bluetooth message and separates it into meaningful values.
     * Process values as {@link #processConfig(String[]) config} or {@link #processData(String[]) data} based on message prefix.
     * @param message Bluetooth message to be processed
     */
    public void processMessage(String message){
        try {
            message = message.substring(0, message.lastIndexOf('*'));
        }catch(Exception e){
            //invalid message
            return;
        }
        String[] values = message.split(",");
        if(values == null){
            return;
        }
        String msgType = values[0];

        switch(msgType){
            case "$PWEAC":
                if(!message.equals(lastConfigMessage)){
                    processConfig(values);
                    lastConfigMessage = message;
                }
                break;
            case "$PWEAD0":
            case "$PWEAD1":
            case "$PWEAD2":
            case "$PWEAD3":
                if(configFields != null){
                    processData(values);
                    //printData();
                }
                break;
        }
    }

}
