package com.weatronic.bluetoothtelemetry;

import android.content.Context;
/**
 * Instantiates all possible telemetry data fields and provides a way to access them.
 * @see TelemetryData
 */
public class TelemetryDataContainer{
    /**
     * Stores class instance to be returned.
     * See {@link #getInstance(Context)}  getInstance}
     */
    static TelemetryDataContainer instance = null;
    /**
     * Returns an instance of Telemetry Data Container.
     * Constructs new instance if it has not been constructed; otherwise returns existing instance.
     * Makes sure the same instance is used.
     */
    public static TelemetryDataContainer getInstance(Context ctx){
        if(instance == null){
            instance = new TelemetryDataContainer(ctx, TelemetryData.protocol);
        }
        return instance;
    }
    /**
     * Returns an instance of Telemetry Data Container.
     * Explicitly destroys any exisiting instance and constructs a new one.
     * Used for example when switching protocols.
     */
    public static TelemetryDataContainer getNewInstance(Context ctx, int protocolID){
        instance = null;
        instance = new TelemetryDataContainer(ctx, protocolID);
        return instance;
    }
    /**
     * Updates a field with a new value.
     * Finds a field with given id and calls the appropriate {@link TelemetryDataSimple#setValue(long) simple} or {@link TelemetryDataComposite#setValues(String) composite} function.
     * @param id ID of the field to be updated
     * @param hex New value in form of a raw hex-string
     */
    public void setDataById(long id, String hex){
        if(TelemetryData.fields.get(id) == null){
            //unknown field id
            return;
        }
        if(hex.length() < 2){
            //empty value (less than 1 complete byte)
            return;
        }
        TelemetryData field = TelemetryData.fields.get(id);

        //composite fields
        if(field instanceof TelemetryDataComposite){
            TelemetryDataComposite compField = (TelemetryDataComposite)field;
            try {
                compField.setValues(hex);
            }catch(Exception e){
                //invalid string, do not update value
            }
        }else{
            //simple fields (number)
            TelemetryDataSimple simpleField = (TelemetryDataSimple)field;
            try {
                simpleField.setValue(Long.parseLong(TelemetryData.littleToBigEndian(hex), 16));
            }catch(Exception e){
                //invalid string, do not update value
            }
        }
    }
    /**
     * Returns a reference to a field by its id.
     * @param id ID of the field to find.
     */
    public TelemetryData getFieldById(long id){
        return TelemetryData.fields.get(id);
    }
    /**
     * Returns a reference to a field by its name.
     * @param name Name of the field to find.
     */
    public TelemetryData getFieldByName(String name){
        return TelemetryData.fields.get(TelemetryData.nameToId.get(name));
    }
    /**
     * Contains constructors for each predefined telemetry data field.
     * @see TelemetryData
     * @param ctx Context of the calling activity. Needed to access <strings.xml>
     */
    public TelemetryDataContainer(Context ctx, int protocolID){
        //when created, clear field list of other protocol's subfields
        TelemetryData.fields.clear();
        TelemetryData.nameToId.clear();

        TelemetryData.protocol = protocolID;

        String Rx = ctx.getString(R.string.Rx) + " ";
        String Tx = ctx.getString(R.string.Tx) + " ";
        String GPS = ctx.getString(R.string.GPS) + " ";
        String LV = ctx.getString(R.string.LinkVario) + " ";
        String MUX = ctx.getString(R.string.MUX);
        String sensor = ctx.getString(R.string.External_sensor) + " ";
        String RxMain = Rx + ctx.getString(R.string.Main) + " ";
        String RxSub1 = Rx + ctx.getString(R.string.Sub)+ "1 ";
        String RxSub2 = Rx + ctx.getString(R.string.Sub)+ "2 ";
        String packet = " " + ctx.getString(R.string.Packet);
        String board = " " + ctx.getString(R.string.Board);
        String TxRx = " " + ctx.getString(R.string.TxRx);

        TelemetryDataSimple Placehoder = new TelemetryDataNumber(0x0000, ctx.getString(R.string.Placeholder), 0, 4, "");

        switch(protocolID) {
            case TelemetryData.PROTOCOL_DV4:
                //---LinkVario
                TelemetryDataSimple LinkVario_power_source_voltage = new TelemetryDataNumber(0x4013, LV + ctx.getString(R.string.Power_source_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple LinkVario_motor_voltage = new TelemetryDataNumber(0x4013, LV + ctx.getString(R.string.Motor_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple LinkVario_motor_current = new TelemetryDataNumber(0x4103, LV + ctx.getString(R.string.Motor_current), 0, 20, "A", 0.01f);
                TelemetryDataSimple LinkVario_used_capacity = new TelemetryDataNumber(0x4203, LV + ctx.getString(R.string.Used_capacity), 0, 100000, "mAh");
                TelemetryDataSimple Barometric_height = new TelemetryDataNumber(0x4304, ctx.getString(R.string.Barometric_height), -1000, 8000, "m", 0.1f);
                TelemetryDataSimple LinkVario_temperature = new TelemetryDataNumber(0x4402, LV + ctx.getString(R.string.Temperature), -40, 125, "°C", 0.1f, -273);
                TelemetryDataSimple Vertical_speed = new TelemetryDataNumber(0x4502, ctx.getString(R.string.Vertical_speed), -30, 30, "m/s", 0.005f);
                TelemetryDataSimple Pitot_speed = new TelemetryDataNumber(0x4603, ctx.getString(R.string.Pitot_speed), 0, 30, "m/s", 0.1f); //Pitot?

                //---MUX/VM
                TelemetryDataArrayVirtual MUX_source_voltage = new TelemetryDataArrayVirtual(0x5003, 0x0010, MUX + " " + ctx.getString(R.string.Power_source_voltage) + packet, MUX + " " + ctx.getString(R.string.Power_source_voltage) + board, 16, 0, 20, "V", 0.001f);
                TelemetryDataArrayVirtual MUX_motor_voltage = new TelemetryDataArrayVirtual(0x5103, 0x0010, MUX + " " + ctx.getString(R.string.Motor_voltage) + packet, MUX + " " + ctx.getString(R.string.Motor_voltage) + board, 16, 0, 20, "V", 0.001f);
                TelemetryDataArrayVirtual MUX_motor_current = new TelemetryDataArrayVirtual(0x5203, 0x0010, MUX + " " + ctx.getString(R.string.Motor_current) + packet, MUX + " " + ctx.getString(R.string.Motor_current) + board, 16, 0, 20, "A", 0.01f);
                TelemetryDataArrayVirtual MUX_Used_capacity = new TelemetryDataArrayVirtual(0x5303, 0x0010, MUX + " " + ctx.getString(R.string.Used_capacity) + packet, MUX + " " + ctx.getString(R.string.Used_capacity) + board, 16, 0, 100000, "mAh");

                TelemetryDataArrayVirtual MUX_A1_Voltage = new TelemetryDataArrayVirtual(0x5403, 0x0010, MUX + " A1 " + ctx.getString(R.string.Voltage) + packet, MUX + " A1 " + ctx.getString(R.string.Voltage) + board, 16, 0, 20, "V", 0.001f);
                TelemetryDataArrayVirtual MUX_A2_Voltage = new TelemetryDataArrayVirtual(0x5C03, 0x0010, MUX + " A2 " + ctx.getString(R.string.Voltage) + packet, MUX + " A2 " + ctx.getString(R.string.Voltage) + board, 16, 0, 20, "V", 0.001f);
                TelemetryDataArrayVirtual MUX_A3_Voltage = new TelemetryDataArrayVirtual(0x6403, 0x0010, MUX + " A3 " + ctx.getString(R.string.Voltage) + packet, MUX + " A3 " + ctx.getString(R.string.Voltage) + board, 16, 0, 20, "V", 0.001f);
                TelemetryDataArrayVirtual MUX_A4_Voltage = new TelemetryDataArrayVirtual(0x6C03, 0x0010, MUX + " A4 " + ctx.getString(R.string.Voltage) + packet, MUX + " A4 " + ctx.getString(R.string.Voltage) + board, 16, 0, 20, "V", 0.001f);
                TelemetryDataArrayVirtual MUX_A5_Voltage = new TelemetryDataArrayVirtual(0x7403, 0x0010, MUX + " A5 " + ctx.getString(R.string.Voltage) + packet, MUX + " A5 " + ctx.getString(R.string.Voltage) + board, 16, 0, 20, "V", 0.001f);

                TelemetryDataArrayVirtual MUX_A1_temperature = new TelemetryDataArrayVirtual(0x5502, 0x0010, MUX + " A1 " + ctx.getString(R.string.Temperature) + packet, MUX + " A1 " + ctx.getString(R.string.Temperature) + board, 16, -40, 125, "°C", 0.1f, -273);
                TelemetryDataArrayVirtual MUX_A2_temperature = new TelemetryDataArrayVirtual(0x5D02, 0x0010, MUX + " A2 " + ctx.getString(R.string.Temperature) + packet, MUX + " A2 " + ctx.getString(R.string.Temperature) + board, 16, -40, 125, "°C", 0.1f, -273);
                TelemetryDataArrayVirtual MUX_A3_temperature = new TelemetryDataArrayVirtual(0x6502, 0x0010, MUX + " A3 " + ctx.getString(R.string.Temperature) + packet, MUX + " A3 " + ctx.getString(R.string.Temperature) + board, 16, -40, 125, "°C", 0.1f, -273);
                TelemetryDataArrayVirtual MUX_A4_temperature = new TelemetryDataArrayVirtual(0x6D02, 0x0010, MUX + " A4 " + ctx.getString(R.string.Temperature) + packet, MUX + " A4 " + ctx.getString(R.string.Temperature) + board, 16, -40, 125, "°C", 0.1f, -273);
                TelemetryDataArrayVirtual MUX_PT1000_temperature = new TelemetryDataArrayVirtual(0x7E02, 0x0010, MUX + " PT1000 " + ctx.getString(R.string.Temperature) + packet, MUX + " PT1000 " + ctx.getString(R.string.Temperature) + board, 16, -40, 125, "°C", 0.1f, -273);

                TelemetryDataArrayVirtual MUX_A1_pitot_speed = new TelemetryDataArrayVirtual(0x5B03, 0x0010, MUX + " A1 " + ctx.getString(R.string.Pitot_speed) + packet, MUX + " A1 " + ctx.getString(R.string.Pitot_speed) + board, 16, 0, 30, "m/s", 0.1f);
                //may be special rules for this field
                TelemetryDataArrayVirtual MUX_A2_RPM = new TelemetryDataArrayVirtual(0x6305, 0x0010, MUX + " A2 " + ctx.getString(R.string.RPM) + packet, MUX + " A2 " + ctx.getString(R.string.RPM) + board, 16, 0, 42000, "", 1.0f/6.0f);
                TelemetryDataArrayVirtual MUX_A3_fuel_flow = new TelemetryDataArrayVirtual(0x6603, 0x0010, MUX + " A3 " + ctx.getString(R.string.Fuel_flow) + packet, MUX + " A3 " + ctx.getString(R.string.Fuel_flow) + board, 16, 0, 3000, "ml/min");
                TelemetryDataArrayVirtual MUX_A3_fuel = new TelemetryDataArrayVirtual(0x6703, 0x0010, MUX + " A3 " + ctx.getString(R.string.Fuel) + packet, MUX + " A3 " + ctx.getString(R.string.Fuel) + board, 16, 0, 6500, "ml");

                //digital input, digital output - bitmask arrays, an exception...


                //---Tx/Rx internal
                TelemetryDataSimple GPS_height = new TelemetryDataNumber(0xDF04, GPS + ctx.getString(R.string.Height), -4000, 4000, "m", 0.1f);
                TelemetryDataSimple GPS_distance_ground = new TelemetryDataNumber(0xE103, GPS + ctx.getString(R.string.Distance_ground), 0, 6553, "m", 0.1f);
                TelemetryDataSimple GPS_distance = new TelemetryDataNumber(0xE103, GPS + ctx.getString(R.string.Distance_pilot), 1, 6553, "m", 0.1f);
                TelemetryDataSimple GPS_time = new TelemetryDataMsUTC(0xE404, GPS + ctx.getString(R.string.Time));
                TelemetryDataSimple Tx_LinkVario_Status = new TelemetryDataBitmask(0xE203, Tx + ctx.getString(R.string.LinkVario_status));
                TelemetryDataSimple Rx_LinkVario_Status = new TelemetryDataBitmask(0xE303, Rx + ctx.getString(R.string.LinkVario_status));
                TelemetryDataSimple Sync_progress = new TelemetryDataNumber(0xE501, ctx.getString(R.string.Sync_progress), 0, 100, "%", 0.1f);
                TelemetryDataSimple USB_voltage_Tx = new TelemetryDataNumber(0xE603, Tx + ctx.getString(R.string.USB_voltage), 0, 20, "V", 0.1f);
                TelemetryDataSimple USB_voltage_Rx = new TelemetryDataNumber(0xE703, Rx + ctx.getString(R.string.USB_voltage), 0, 20, "V", 0.1f);
                TelemetryDataArrayDV4 gyro = new TelemetryDataArrayDV4(0xE80B, ctx.getString(R.string.Gyro_packet), ctx.getString(R.string.Gyro), 5, TelemetryData.SIGNED_SHORT, 1, -200, 200, "%");
                TelemetryDataSimple Tx_timestamp = new TelemetryDataTimestamp(0xE905, ctx.getString(R.string.Tx_Timestamp));
                TelemetryDataSimple Rx_timestamp = new TelemetryDataTimestamp(0xEA05, ctx.getString(R.string.Rx_Timestamp));
                TelemetryDataGPS_DV4 GPS_packet = new TelemetryDataGPS_DV4(0xEB0C, ctx.getString(R.string.GPS), ctx);
                TelemetryDataSimple Channels_used = new TelemetryDataNumber(0xEC01, ctx.getString(R.string.Tx_channels_used), 0, 255, "");
                TelemetryDataSimple Pultframes_per_second = new TelemetryDataNumber(0xED01, Tx + ctx.getString(R.string.Pultframes), 0, 255, ""); //can't translate
                TelemetryDataSimple Tx_temperature = new TelemetryDataNumber(0xEE00, Tx + ctx.getString(R.string.Temperature), -40, 125, "°C", 0.1f, -273);
                TelemetryDataSimple Rx_temperature = new TelemetryDataNumber(0xEF00, Rx + ctx.getString(R.string.Temperature), -40, 125, "°C", 0.1f, -273);
                TelemetryDataBitmask Tx_Status_word = new TelemetryDataBitmask(0xF005, Tx + ctx.getString(R.string.Status_word));
                TelemetryDataBitmask Tx_Status_byte = new TelemetryDataBitmask(0xF101, Tx + ctx.getString(R.string.Status_byte));
                TelemetryDataBitmask Rx_Status_word = new TelemetryDataBitmask(0xF205, Rx + ctx.getString(R.string.Status_word));
                TelemetryDataBitmask Rx_Status_byte = new TelemetryDataBitmask(0xF301, ctx.getString(R.string.Status_byte));
                TelemetryDataArrayDV4 servos1 = new TelemetryDataArrayDV4(0xF40E, ctx.getString(R.string.Servo_packet) + " 1-16", ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 1, -200, 200, "%", 0.1f);
                TelemetryDataArrayDV4 servos2 = new TelemetryDataArrayDV4(0xF50E, ctx.getString(R.string.Servo_packet) + " 17-32", ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 17, -200, 200, "%", 0.1f);
                TelemetryDataArrayDV4 multiSwitchChannel = new TelemetryDataArrayDV4(0xF60D, ctx.getString(R.string.Multi_switch_channel_data), ctx.getString(R.string.Multi_switch_channel), 16, TelemetryData.SIGNED_12BIT, 1, -100, 100, "%", 0.05f);
                TelemetryDataArrayDV4 Channel = new TelemetryDataArrayDV4(0xF70D, ctx.getString(R.string.Channel_data), ctx.getString(R.string.Channel), 16, TelemetryData.SIGNED_12BIT, 1, -100, 100, "%", 0.05f);
                TelemetryDataSimple LQI_Tx_1 = new TelemetryDataNumber(0xF800, Tx + ctx.getString(R.string.LQI_1), 0, 100, "%");
                TelemetryDataSimple LQI_Tx_2 = new TelemetryDataNumber(0xF810, Tx + ctx.getString(R.string.LQI_2), 0, 100, "%");
                TelemetryDataSimple LQI_Rx_1 = new TelemetryDataNumber(0xF900, Rx + ctx.getString(R.string.LQI_1), 0, 100, "%");
                TelemetryDataSimple LQI_Rx_2 = new TelemetryDataNumber(0xF910, Rx + ctx.getString(R.string.LQI_2), 0, 100, "%");
                //wiki says SIGNED BYTE ???
                TelemetryDataSimple RSSI_Tx_1 = new TelemetryDataNumber(0xFA01, Tx + ctx.getString(R.string.RSSI_1), -128, 20, "dBm", 0.5f);
                TelemetryDataSimple RSSI_Tx_2 = new TelemetryDataNumber(0xFA11, Tx + ctx.getString(R.string.RSSI_2), -128, 20, "dBm", 0.5f);
                TelemetryDataSimple RSSI_Rx_1 = new TelemetryDataNumber(0xFB01, Rx + ctx.getString(R.string.RSSI_1), -128, 20, "dBm", 0.5f);
                TelemetryDataSimple RSSI_Rx_2 = new TelemetryDataNumber(0xFB11, Rx +ctx.getString(R.string.RSSI_2), -128, 20, "dBm", 0.5f);
                TelemetryDataArrayDV4 servoCurrent = new TelemetryDataArrayDV4(0xFC0F, ctx.getString(R.string.Servo_bank_current_packet), ctx.getString(R.string.Servo_bank), 8, TelemetryData.UNSIGNED_SHORT, 1, 0, 40, "A", 0.01f);
                TelemetryDataSimple Rx_current = new TelemetryDataNumber(0xFD03, Rx + ctx.getString(R.string.Current), 0, 20, "A", 0.01f);
                TelemetryDataSimple Tx_voltage = new TelemetryDataNumber(0xFE03, Tx + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple Rx_voltage_1 = new TelemetryDataNumber(0xFF03, Rx + ctx.getString(R.string.Voltage) + " 1", 0, 20, "V", 0.001f);
                TelemetryDataSimple Rx_voltage_2 = new TelemetryDataNumber(0xFF13, Rx + ctx.getString(R.string.Voltage) + " 2", 0, 20, "V", 0.001f);

                break;

            case TelemetryData.PROTOCOL_SKYNAVIGATOR:

                TelemetryDataGPS_SkyNav Rx_GPS = new TelemetryDataGPS_SkyNav(0x6500, Rx + ctx.getString(R.string.GPS), ctx);
                TelemetryDataGPS_SkyNav Tx_GPS = new TelemetryDataGPS_SkyNav(0x6510, Tx + ctx.getString(R.string.GPS), ctx);

                TelemetryDataArraySkyNav tx_servos1 = new TelemetryDataArraySkyNav(0x6B08, Tx + ctx.getString(R.string.Servo_packet) + " 1-16", Tx + " " + ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 1, 0x1080, -200, 200, "%", 0.1f);
                TelemetryDataArraySkyNav tx_servos2 = new TelemetryDataArraySkyNav(0x6B09, Tx + ctx.getString(R.string.Servo_packet) + " 17-32", Tx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 17, 0x1090, -200, 200, "%", 0.1f);
                TelemetryDataArraySkyNav tx_servos3 = new TelemetryDataArraySkyNav(0x6B0A, Tx + ctx.getString(R.string.Servo_packet) + " 33-48", Tx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 33, 0x10A0, -200, 200, "%", 0.1f);
                TelemetryDataArraySkyNav tx_servos4 = new TelemetryDataArraySkyNav(0x6B0B, Tx + ctx.getString(R.string.Servo_packet) + " 49-64", Tx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 49, 0x10B0, -200, 200, "%", 0.1f);

                TelemetryDataArraySkyNav rx_servos1 = new TelemetryDataArraySkyNav(0x6B10, Rx + ctx.getString(R.string.Servo_packet) + " 1-16", Rx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 1, 0x1110, -200, 200, "%", 0.1f);
                TelemetryDataArraySkyNav rx_servos2 = new TelemetryDataArraySkyNav(0x6B11, Rx + ctx.getString(R.string.Servo_packet) + " 17-32", Rx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 17, 0x1120, -200, 200, "%", 0.1f);
                TelemetryDataArraySkyNav rx_servos3 = new TelemetryDataArraySkyNav(0x6B12, Rx + ctx.getString(R.string.Servo_packet) + " 33-48", Rx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 33, 0x1130, -200, 200, "%", 0.1f);
                TelemetryDataArraySkyNav rx_servos4 = new TelemetryDataArraySkyNav(0x6B13, Rx + ctx.getString(R.string.Servo_packet) + " 49-64", Rx + " " +  ctx.getString(R.string.Servo), 16, TelemetryData.SIGNED_12BIT, 49, 0x1140, -200, 200, "%", 0.1f);

                TelemetryDataArraySkyNav control_data1 = new TelemetryDataArraySkyNav(0x6B00, ctx.getString(R.string.Control_data_packet) + " 1-16", ctx.getString(R.string.Control_data), 16, TelemetryData.SIGNED_12BIT, 1, 0x1000, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav control_data2 = new TelemetryDataArraySkyNav(0x6B01, ctx.getString(R.string.Control_data_packet) + " 17-32", ctx.getString(R.string.Control_data), 16, TelemetryData.SIGNED_12BIT, 17, 0x1010, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav control_data3 = new TelemetryDataArraySkyNav(0x6B02, ctx.getString(R.string.Control_data_packet) + " 33-48", ctx.getString(R.string.Control_data), 16, TelemetryData.SIGNED_12BIT, 33, 0x1020, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav control_data4 = new TelemetryDataArraySkyNav(0x6B03, ctx.getString(R.string.Control_data_packet) + " 49-64", ctx.getString(R.string.Control_data), 16, TelemetryData.SIGNED_12BIT, 49, 0x1030, -100, 100, "%", 0.05f);

                TelemetryDataArraySkyNav functions1 = new TelemetryDataArraySkyNav(0x6B20, ctx.getString(R.string.Function_packet) + " 1-16", ctx.getString(R.string.Function), 16, TelemetryData.SIGNED_12BIT, 1, 0x1200, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav functions2 = new TelemetryDataArraySkyNav(0x6B21, ctx.getString(R.string.Function_packet) + " 17-32", ctx.getString(R.string.Function), 16, TelemetryData.SIGNED_12BIT, 17, 0x1210, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav functions3 = new TelemetryDataArraySkyNav(0x6B22, ctx.getString(R.string.Function_packet) + " 33-48", ctx.getString(R.string.Function), 16, TelemetryData.SIGNED_12BIT, 33, 0x1220, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav functions4 = new TelemetryDataArraySkyNav(0x6B23, ctx.getString(R.string.Function_packet) + " 49-64", ctx.getString(R.string.Function), 16, TelemetryData.SIGNED_12BIT, 49, 0x1230, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav functions5 = new TelemetryDataArraySkyNav(0x6B24, ctx.getString(R.string.Function_packet) + " 65-80", ctx.getString(R.string.Function), 16, TelemetryData.SIGNED_12BIT, 65, 0x1240, -100, 100, "%", 0.05f);
                TelemetryDataArraySkyNav functions6 = new TelemetryDataArraySkyNav(0x6B25, ctx.getString(R.string.Function_packet) + " 81-96", ctx.getString(R.string.Function), 16, TelemetryData.SIGNED_12BIT, 81, 0x1250, -100, 100, "%", 0.05f);

                TelemetryDataArraySkyNav control_IDs1 = new TelemetryDataArraySkyNav(0x8B18, ctx.getString(R.string.Control_ID_packet) + " 1-16", ctx.getString(R.string.Control_ID), 16, TelemetryData.UNSIGNED_SHORT, 1, 0x1980, 0, 65000, "%", 0.05f);
                TelemetryDataArraySkyNav control_IDs2 = new TelemetryDataArraySkyNav(0x8B19, ctx.getString(R.string.Control_ID_packet) + " 17-32", ctx.getString(R.string.Control_ID), 16, TelemetryData.UNSIGNED_SHORT, 17, 0x1990, 0, 65000, "%", 0.05f);
                TelemetryDataArraySkyNav control_IDs3 = new TelemetryDataArraySkyNav(0x8B1A, ctx.getString(R.string.Control_ID_packet) + " 33-48", ctx.getString(R.string.Control_ID), 16, TelemetryData.UNSIGNED_SHORT, 33, 0x19A0, 0, 65000, "%", 0.05f);
                TelemetryDataArraySkyNav control_IDs4 = new TelemetryDataArraySkyNav(0x8B1B, ctx.getString(R.string.Control_ID_packet) + " 49-64", ctx.getString(R.string.Control_ID), 16, TelemetryData.UNSIGNED_SHORT, 49, 0x19B0, 0, 65000, "%", 0.05f);

                TelemetryDataPowerSupply power_supply = new TelemetryDataPowerSupply(0x84B0, ctx.getString(R.string.Power_supply), ctx);

                TelemetryDataSimple angle_of_attack = new TelemetryDataNumber(0x0368, sensor + ctx.getString(R.string.Angle_of_attack), -45, 45, "°");
                TelemetryDataSimple RSSI_RxMain_1 = new TelemetryDataNumber(0x0403, RxMain + ctx.getString(R.string.RSSI_1), -40, 125, "%");
                TelemetryDataSimple RSSI_RxMain_2 = new TelemetryDataNumber(0x0404, RxMain + ctx.getString(R.string.RSSI_2), -40, 125, "%");
                TelemetryDataSimple RSSI_RxSub1_1 = new TelemetryDataNumber(0x0413, RxSub1 + ctx.getString(R.string.RSSI_1), -40, 125, "%");
                TelemetryDataSimple RSSI_RxSub1_2 = new TelemetryDataNumber(0x0414, RxSub1 + ctx.getString(R.string.RSSI_2), -40, 125, "%");
                TelemetryDataSimple RSSI_RxSub2_1 = new TelemetryDataNumber(0x0423, RxSub2 + ctx.getString(R.string.RSSI_1), -40, 125, "%");
                TelemetryDataSimple RSSI_RxSub2_2 = new TelemetryDataNumber(0x0424, RxSub2 + ctx.getString(R.string.RSSI_2), -40, 125, "%");
                TelemetryDataSimple batRSSI_Tx_1 = new TelemetryDataNumber(0x0483, Tx + ctx.getString(R.string.RSSI_1), -40, 125, "%");
                TelemetryDataSimple batRSSI_Tx_2 = new TelemetryDataNumber(0x0484, Tx + ctx.getString(R.string.RSSI_2), -40, 125, "%");
                TelemetryDataSimple Temperature_RxMain = new TelemetryDataNumber(0x0408, RxMain + ctx.getString(R.string.Temperature), -40, 125, "°C", -273);
                TelemetryDataSimple Temperature_RxSub1 = new TelemetryDataNumber(0x0418, RxSub1 + ctx.getString(R.string.Temperature), -40, 125, "°C", -273);
                TelemetryDataSimple Temperature_RxSub2 = new TelemetryDataNumber(0x0428, RxSub2 + ctx.getString(R.string.Temperature), -40, 125, "°C", -273);
                TelemetryDataSimple Temperature_Tx = new TelemetryDataNumber(0x0485, Tx + ctx.getString(R.string.Temperature), -40, 125, "°C", -273);
                TelemetryDataSimple climbrate = new TelemetryDataNumber(0x1361, sensor + ctx.getString(R.string.Climbrate), -50, 50, "m/s", 0.005f);

                TelemetryDataSimple sensor1_current = new TelemetryDataNumber(0x2381, sensor + "1 " + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor1_temperature1 = new TelemetryDataNumber(0x1388, sensor + "1 " + ctx.getString(R.string.Temperature) + " 1", -40, 125, "°C", -273);
                TelemetryDataSimple sensor1_temperature2 = new TelemetryDataNumber(0x1389, sensor + "1 " + ctx.getString(R.string.Temperature) + " 2", -40, 125, "°C", -273);
                TelemetryDataSimple sensor1_Rx_current = new TelemetryDataNumber(0x238B, sensor + "1 " + Rx + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor2_current = new TelemetryDataNumber(0x23A1, sensor + "2 " + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor2_temperature1 = new TelemetryDataNumber(0x13A8, sensor + "2 " + ctx.getString(R.string.Temperature) + " 1", -40, 125, "°C", -273);
                TelemetryDataSimple sensor2_temperature2 = new TelemetryDataNumber(0x13A9, sensor + "2 " + ctx.getString(R.string.Temperature) + " 2", -40, 125, "°C", -273);
                TelemetryDataSimple sensor2_Rx_current = new TelemetryDataNumber(0x23AB, sensor + "2 " + Rx + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor3_current = new TelemetryDataNumber(0x23C1, sensor + "3 " + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor3_temperature1 = new TelemetryDataNumber(0x13C8, sensor + "3 " + ctx.getString(R.string.Temperature) + " 1", -40, 125, "°C", -273);
                TelemetryDataSimple sensor3_temperature2 = new TelemetryDataNumber(0x13C9, sensor + "3 " + ctx.getString(R.string.Temperature) + " 2", -40, 125, "°C", -273);
                TelemetryDataSimple sensor3_Rx_current = new TelemetryDataNumber(0x23CB, sensor + "3 " + Rx + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor4_current = new TelemetryDataNumber(0x23E1, sensor + "4 " + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);
                TelemetryDataSimple sensor4_temperature1 = new TelemetryDataNumber(0x13E8, sensor + "4 " + ctx.getString(R.string.Temperature) + " 1", -40, 125, "°C", -273);
                TelemetryDataSimple sensor4_temperature2 = new TelemetryDataNumber(0x13E9, sensor + "4 " + ctx.getString(R.string.Temperature) + " 2", -40, 125, "°C", -273);
                TelemetryDataSimple sensor4_Rx_current = new TelemetryDataNumber(0x23EB, sensor + "4 " + Rx + ctx.getString(R.string.Current), 0, 20, "A", 0.1f);

                TelemetryDataSimple RxMain_voltage_1 = new TelemetryDataNumber(0x1C05, RxMain + ctx.getString(R.string.Battery_voltage) + " 1", 0, 20, "V", 0.001f);
                TelemetryDataSimple RxMain_voltage_2 = new TelemetryDataNumber(0x1C06, RxMain + ctx.getString(R.string.Battery_voltage) + " 2", 0, 20, "V", 0.001f);
                TelemetryDataSimple RxMain_total_current = new TelemetryDataNumber(0x1C07, RxMain + ctx.getString(R.string.Current) + " " + ctx.getString(R.string.Total), 0, 20, "A", 0.1f);
                TelemetryDataSimple RxSub1_voltage_1 = new TelemetryDataNumber(0x1C15, RxSub1 + ctx.getString(R.string.Battery_voltage) + " 1", 0, 20, "V", 0.001f);
                TelemetryDataSimple RxSub1_voltage_2 = new TelemetryDataNumber(0x1C16, RxSub1 + ctx.getString(R.string.Battery_voltage) + " 2", 0, 20, "V", 0.001f);
                TelemetryDataSimple RxSub1_total_current = new TelemetryDataNumber(0x1C17, RxSub1 + ctx.getString(R.string.Current) + " " + ctx.getString(R.string.Total), 0, 20, "A", 0.1f);
                TelemetryDataSimple RxSub2_voltage_1 = new TelemetryDataNumber(0x1C25, RxSub2 + ctx.getString(R.string.Battery_voltage) + " 1", 0, 20, "V", 0.001f);
                TelemetryDataSimple RxSub2_voltage_2 = new TelemetryDataNumber(0x1C26, RxSub2 + ctx.getString(R.string.Battery_voltage) + " 2", 0, 20, "V", 0.001f);
                TelemetryDataSimple RxSub2_total_current = new TelemetryDataNumber(0x1C27, RxSub2 + ctx.getString(R.string.Current) + " " + ctx.getString(R.string.Total), 0, 20, "A", 0.1f);

                TelemetryDataSimple Rx_internal_gyro1 = new TelemetryDataNumber(0x1450, Rx + ctx.getString(R.string.Internal_gyro_1), -360, 360, "%/s", 0.1f);
                TelemetryDataSimple Rx_internal_gyro2 = new TelemetryDataNumber(0x1451, Rx + ctx.getString(R.string.Internal_gyro_2), -360, 360, "%/s", 0.1f);
                TelemetryDataSimple Rx_internal_gyro3 = new TelemetryDataNumber(0x1452, Rx + ctx.getString(R.string.Internal_gyro_3), -360, 360, "%/s", 0.1f);
                TelemetryDataSimple Rx_external_gyro1 = new TelemetryDataNumber(0x1453, Rx + ctx.getString(R.string.External_gyro_1), -360, 360, "%/s", 0.1f);
                TelemetryDataSimple Rx_external_gyro2 = new TelemetryDataNumber(0x1454, Rx + ctx.getString(R.string.External_gyro_2), -360, 360, "%/s", 0.1f);

                TelemetryDataSimple Rx_acceleration1 = new TelemetryDataNumber(0x1455, Rx + ctx.getString(R.string.Acceleration_1), -10, 10, "g", 0.01f);
                TelemetryDataSimple Rx_acceleration2 = new TelemetryDataNumber(0x1456, Rx + ctx.getString(R.string.Acceleration_2), -10, 10, "g", 0.01f);
                TelemetryDataSimple Rx_acceleration3 = new TelemetryDataNumber(0x1457, Rx + ctx.getString(R.string.Acceleration_3), -10, 10, "g", 0.01f);

                TelemetryDataSimple Rx_roll = new TelemetryDataNumber(0x1459, Rx + ctx.getString(R.string.Roll), -180, 180, "°", 0.01f);
                TelemetryDataSimple Rx_pitch = new TelemetryDataNumber(0x145A, Rx + ctx.getString(R.string.Pitch), -180, 180, "°", 0.01f);
                TelemetryDataSimple Tx_roll = new TelemetryDataNumber(0x14C7, Tx + ctx.getString(R.string.Roll), -180, 180, "°", 0.01f);
                TelemetryDataSimple Tx_pitch = new TelemetryDataNumber(0x14C8, Tx + ctx.getString(R.string.Pitch), -180, 180, "°", 0.01f);

                TelemetryDataSimple batLinkVario_temperature = new TelemetryDataNumber(0x1585, LV + ctx.getString(R.string.Temperature), -40, 125, "°C", 0.1f, -273);
                TelemetryDataSimple LinkVario_vario = new TelemetryDataNumber(0x4402, LV + ctx.getString(R.string.Vario), -50, 50, "m/s", 0.005f);

                TelemetryDataSimple MUX1_temperature_p1000 = new TelemetryDataNumber(0x1606, MUX + "1 " + ctx.getString(R.string.Temperature) + " PT1000", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX1_temperature_A1 = new TelemetryDataNumber(0x1611,MUX + "1 " + ctx.getString(R.string.Temperature) + " A1", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX1_temperature_A2 = new TelemetryDataNumber(0x1619, MUX + "1 " + ctx.getString(R.string.Temperature) + " A2", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX1_temperature_A3 = new TelemetryDataNumber(0x1621, MUX + "1 " + ctx.getString(R.string.Temperature) + " A3", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX1_temperature_A4 = new TelemetryDataNumber(0x1629, MUX + "1 " + ctx.getString(R.string.Temperature) + " A4", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX1_temperature_A5 = new TelemetryDataNumber(0x1631, MUX + "1 " + ctx.getString(R.string.Temperature) + " A5", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX2_temperature_p1000 = new TelemetryDataNumber(0x1646, MUX + "2 " + ctx.getString(R.string.Temperature) + " PT1000", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX2_temperature_A1 = new TelemetryDataNumber(0x1651,MUX + "2 " + ctx.getString(R.string.Temperature) + " A1", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX2_temperature_A2 = new TelemetryDataNumber(0x1659, MUX + "2 " + ctx.getString(R.string.Temperature) + " A2", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX2_temperature_A3 = new TelemetryDataNumber(0x1661, MUX + "2 " + ctx.getString(R.string.Temperature) + " A3", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX2_temperature_A4 = new TelemetryDataNumber(0x1669, MUX + "2 " + ctx.getString(R.string.Temperature) + " A4", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX2_temperature_A5 = new TelemetryDataNumber(0x1671, MUX + "2 " + ctx.getString(R.string.Temperature) + " A5", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX3_temperature_p1000 = new TelemetryDataNumber(0x1686, MUX + "3 " + ctx.getString(R.string.Temperature) + " PT1000", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX3_temperature_A1 = new TelemetryDataNumber(0x1691,MUX + "3 " + ctx.getString(R.string.Temperature) + " A1", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX3_temperature_A2 = new TelemetryDataNumber(0x1699, MUX + "3 " + ctx.getString(R.string.Temperature) + " A2", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX3_temperature_A3 = new TelemetryDataNumber(0x16A1, MUX + "3 " + ctx.getString(R.string.Temperature) + " A3", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX3_temperature_A4 = new TelemetryDataNumber(0x16A9, MUX + "3 " + ctx.getString(R.string.Temperature) + " A4", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX3_temperature_A5 = new TelemetryDataNumber(0x16B1, MUX + "3 " + ctx.getString(R.string.Temperature) + " A5", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX4_temperature_p1000 = new TelemetryDataNumber(0x16C6, MUX + "4 " + ctx.getString(R.string.Temperature) + " PT1000", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX4_temperature_A1 = new TelemetryDataNumber(0x16D1,MUX + "4 " + ctx.getString(R.string.Temperature) + " A1", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX4_temperature_A2 = new TelemetryDataNumber(0x16D9, MUX + "4 " + ctx.getString(R.string.Temperature) + " A2", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX4_temperature_A3 = new TelemetryDataNumber(0x16E1, MUX + "4 " + ctx.getString(R.string.Temperature) + " A3", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX4_temperature_A4 = new TelemetryDataNumber(0x16E9, MUX + "4 " + ctx.getString(R.string.Temperature) + " A4", -200, 850, "°C", 0.1f, -273);
                TelemetryDataSimple MUX4_temperature_A5 = new TelemetryDataNumber(0x16F1, MUX + "4 " + ctx.getString(R.string.Temperature) + " A5", -200, 850, "°C", 0.1f, -273);

                TelemetryDataArrayVirtual timers = new TelemetryDataArrayVirtual(0x22E0, 0x0001, ctx.getString(R.string.Timer_packet), ctx.getString(R.string.Timer), 16, 0, 86400, "s", 0.1f);
                TelemetryDataArrayVirtual last_lap_timers = new TelemetryDataArrayVirtual(0x22F0, 0x0001, ctx.getString(R.string.Last_lap_timer_packet), ctx.getString(R.string.Last_lap_timer), 16, 0, 86400, "s", 0.1f);

                TelemetryDataSimple pressure_height = new TelemetryDataNumber(0x2360, sensor + ctx.getString(R.string.Pressure_height), -1000, 8000, "m", 0.1f);
                TelemetryDataSimple height_from_pilot = new TelemetryDataNumber(0x2363, sensor + ctx.getString(R.string.Height_from_pilot), -1000, 8000, "m", 0.1f);
                TelemetryDataSimple energy = new TelemetryDataNumber(0x236A, sensor + ctx.getString(R.string.Pressure_height), -3000, 3000, "mW/min");
                TelemetryDataSimple GPS_altitude_txrx = new TelemetryDataNumber(0x2522, GPS + ctx.getString(R.string.Altitude) + TxRx, -1000, 8000, "m", 0.1f);
                TelemetryDataSimple batBarometric_height = new TelemetryDataNumber(0x2584, LV + ctx.getString(R.string.Barometric_height), -1000, 8000, "m", 0.1f);
                TelemetryDataSimple Altitude_difference_abs = new TelemetryDataNumber(0x2588, LV + ctx.getString(R.string.Altitude_difference_abs), -1000, 8000, "m", 0.1f);
                TelemetryDataSimple Altitude_difference_rel = new TelemetryDataNumber(0x2589, LV + ctx.getString(R.string.Altitude_difference_rel), -300, 300, "m/s", 0.1f);

                //text message - needs completely new class, right now SIMPLE = NUMBEr in raw form

                TelemetryDataArrayVirtual time_within_sequencer = new TelemetryDataArrayVirtual(0x0A80, 0x0001, ctx.getString(R.string.Time_sequencer_packet), ctx.getString(R.string.Time_sequencer), 12, 0, 100, "%");

                TelemetryDataSimple LQI_RxMain_1 = new TelemetryDataNumber(0x0C01, RxMain + ctx.getString(R.string.LQI_1), 0, 100, "%");
                TelemetryDataSimple LQI_RxMain_2 = new TelemetryDataNumber(0x0C02, RxMain + ctx.getString(R.string.LQI_2), 0, 100, "%");
                TelemetryDataSimple LQI_RxSub1_1 = new TelemetryDataNumber(0x0C11, RxSub1 + ctx.getString(R.string.LQI_1), 0, 100, "%");
                TelemetryDataSimple LQI_RxSub1_2 = new TelemetryDataNumber(0x0C12, RxSub1 + ctx.getString(R.string.LQI_2), 0, 100, "%");
                TelemetryDataSimple LQI_RxSub2_1 = new TelemetryDataNumber(0x0C21, RxSub2 + ctx.getString(R.string.LQI_1), 0, 100, "%");
                TelemetryDataSimple LQI_RxSub2_2 = new TelemetryDataNumber(0x0C22, RxSub2 + ctx.getString(R.string.LQI_2), 0, 100, "%");
                TelemetryDataSimple batLQI_Tx_1 = new TelemetryDataNumber(0x0C81, Tx + ctx.getString(R.string.LQI_1), 0, 100, "%");
                TelemetryDataSimple batLQI_Tx_2 = new TelemetryDataNumber(0x0C82, Tx + ctx.getString(R.string.LQI_2), 0, 100, "%");

                TelemetryDataSimple RxMain_status = new TelemetryDataBitmask(0x0C0A, RxMain + ctx.getString(R.string.Status_byte));
                TelemetryDataSimple RxSub1_status = new TelemetryDataBitmask(0x0C1A, RxSub1 + ctx.getString(R.string.Status_byte));
                TelemetryDataSimple RxSub2_status = new TelemetryDataBitmask(0x0C2A, RxSub2 + ctx.getString(R.string.Status_byte));

                TelemetryDataSimple Rx_sync_progress = new TelemetryDataNumber(0x0C0D, Rx + ctx.getString(R.string.Sync_progress), 0, 100, "%");
                TelemetryDataSimple Tx_flight_mode = new TelemetryDataNumber(0x0C87, Tx + ctx.getString(R.string.Flight_mode), 0, 20, "");
                TelemetryDataSimple Tx_sequence_control_config = new TelemetryDataBitmask(0x0C89, Tx + ctx.getString(R.string.Sequence_control_config));
                TelemetryDataSimple Rx_general_progress = new TelemetryDataNumber(0x0C8F, Rx + ctx.getString(R.string.General_progress), 0, 100, "%");
                TelemetryDataSimple Tx_teacher_student_status = new TelemetryDataBitmask(0x0CCC, Tx + ctx.getString(R.string.Teacher_student) + ctx.getString(R.string.Status_byte));

                TelemetryDataSimple MUX1_digital_input = new TelemetryDataBitmask(0x0E04, MUX + "1 " + ctx.getString(R.string.Digital_input));
                TelemetryDataSimple MUX1_digital_output = new TelemetryDataBitmask(0x0E05, MUX + "1 " + ctx.getString(R.string.Digital_output));
                TelemetryDataSimple MUX2_digital_input = new TelemetryDataBitmask(0x0E44, MUX + "2 " + ctx.getString(R.string.Digital_input));
                TelemetryDataSimple MUX2_digital_output = new TelemetryDataBitmask(0x0E45, MUX + "2 " + ctx.getString(R.string.Digital_output));
                TelemetryDataSimple MUX3_digital_input = new TelemetryDataBitmask(0x0E84, MUX + "3 " + ctx.getString(R.string.Digital_input));
                TelemetryDataSimple MUX3_digital_output = new TelemetryDataBitmask(0x0E85, MUX + "3 " + ctx.getString(R.string.Digital_output));
                TelemetryDataSimple MUX4_digital_input = new TelemetryDataBitmask(0x0EC4, MUX + "4 " + ctx.getString(R.string.Digital_input));
                TelemetryDataSimple MUX4_digital_output = new TelemetryDataBitmask(0x0EC5, MUX + "4 " + ctx.getString(R.string.Digital_output));

                TelemetryDataSimple airspeed = new TelemetryDataNumber(0x1B62, sensor + ctx.getString(R.string.Airspeed), 0, 100, "km/h", 0.36f);
                TelemetryDataSimple distance_from_pilot = new TelemetryDataNumber(0x1B64, sensor + ctx.getString(R.string.Distance_pilot), 0, 3000, "m", 0.1f);
                TelemetryDataSimple ground_distance_from_pilot = new TelemetryDataNumber(0x1B65, sensor + ctx.getString(R.string.Distance_pilot_ground), 0, 3000, "m", 0.1f);
                TelemetryDataSimple bearing_from_pilot = new TelemetryDataNumber(0x1B66, sensor + ctx.getString(R.string.Distance_pilot_ground), 0, 360, "°", 0.1f);
                TelemetryDataSimple sensor_GForce = new TelemetryDataNumber(0x1B69, sensor + ctx.getString(R.string.GForce), 0, 20, "g", 0.01f);

                TelemetryDataSimple sensor1_voltage = new TelemetryDataNumber(0x1B80, sensor + "1 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.01f);
                TelemetryDataSimple sensor1_power = new TelemetryDataNumber(0x1B82, sensor + "1 " + ctx.getString(R.string.Power), 0, 100, "KW", 0.001f);
                TelemetryDataSimple sensor1_capacity = new TelemetryDataNumber(0x1B83, sensor + "1 " + ctx.getString(R.string.Capacity), 0, 100, "Ah", 0.01f);
                TelemetryDataSimple sensor1_rx_voltage = new TelemetryDataNumber(0x1B8A, sensor + "1 " + Rx + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple sensor1_rx_capacity = new TelemetryDataNumber(0x1B8C, sensor + "1 " + Rx + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple sensor1_PWM = new TelemetryDataNumber(0x1B8D, sensor + "1 " + ctx.getString(R.string.PWM), 0, 100, "°", 0.1f);
                TelemetryDataSimple sensor1_fuel_flow = new TelemetryDataNumber(0x1B8F, sensor + "1 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple sensor1_fuel = new TelemetryDataNumber(0x1B90, sensor + "1 " + ctx.getString(R.string.Fuel), 0, 6500, "ml", 10.0f); //cl -> ml
                TelemetryDataSimple sensor1_fuel_quality= new TelemetryDataNumber(0x1B91, sensor + "1 " + ctx.getString(R.string.Fuel_quality), 0, 100, "°", 0.1f);
                //sensor1 engine noise level

                TelemetryDataSimple sensor2_voltage = new TelemetryDataNumber(0x1BA0, sensor + "2 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.01f);
                TelemetryDataSimple sensor2_power = new TelemetryDataNumber(0x1BA2, sensor + "2 " + ctx.getString(R.string.Power), 0, 100, "KW", 0.001f);
                TelemetryDataSimple sensor2_capacity = new TelemetryDataNumber(0x1BA3, sensor + "2 " + ctx.getString(R.string.Capacity), 0, 100, "Ah", 0.01f);
                TelemetryDataSimple sensor2_rx_voltage = new TelemetryDataNumber(0x1BAA, sensor + "2 " + Rx + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple sensor2_rx_capacity = new TelemetryDataNumber(0x1BAC, sensor + "2 " + Rx + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple sensor2_PWM = new TelemetryDataNumber(0x1BAD, sensor + "2 " + ctx.getString(R.string.PWM), 0, 100, "°", 0.1f);
                TelemetryDataSimple sensor2_fuel_flow = new TelemetryDataNumber(0x1BAF, sensor + "2 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple sensor2_fuel = new TelemetryDataNumber(0x1BB0, sensor + "2 " + ctx.getString(R.string.Fuel), 0, 6500, "ml", 10.0f); //cl -> ml
                TelemetryDataSimple sensor2_fuel_quality= new TelemetryDataNumber(0x1BB1, sensor + "2 " + ctx.getString(R.string.Fuel_quality), 0, 100, "°", 0.1f);
                //sensor2 engine noise level

                TelemetryDataSimple sensor3_voltage = new TelemetryDataNumber(0x1BC0, sensor + "3 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.01f);
                TelemetryDataSimple sensor3_power = new TelemetryDataNumber(0x1BC2, sensor + "3 " + ctx.getString(R.string.Power), 0, 100, "KW", 0.001f);
                TelemetryDataSimple sensor3_capacity = new TelemetryDataNumber(0x1BC3, sensor + "3 " + ctx.getString(R.string.Capacity), 0, 100, "Ah", 0.01f);
                TelemetryDataSimple sensor3_rx_voltage = new TelemetryDataNumber(0x1BCA, sensor + "3 " + Rx + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple sensor3_rx_capacity = new TelemetryDataNumber(0x1BCC, sensor + "3 " + Rx + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple sensor3_PWM = new TelemetryDataNumber(0x1BCD, sensor + "3 " + ctx.getString(R.string.PWM), 0, 100, "°", 0.1f);
                TelemetryDataSimple sensor3_fuel_flow = new TelemetryDataNumber(0x1BCF, sensor + "3 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple sensor3_fuel = new TelemetryDataNumber(0x1BD0, sensor + "3 " + ctx.getString(R.string.Fuel), 0, 6500, "ml", 10.0f); //cl -> ml
                TelemetryDataSimple sensor3_fuel_quality= new TelemetryDataNumber(0x1BD1, sensor + "3 " + ctx.getString(R.string.Fuel_quality), 0, 100, "°", 0.1f);
                //sensor3 engine noise level

                TelemetryDataSimple sensor4_voltage = new TelemetryDataNumber(0x1BE0, sensor + "4 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.01f);
                TelemetryDataSimple sensor4_power = new TelemetryDataNumber(0x1BE2, sensor + "4 " + ctx.getString(R.string.Power), 0, 100, "KW", 0.001f);
                TelemetryDataSimple sensor4_capacity = new TelemetryDataNumber(0x1BE3, sensor + "4 " + ctx.getString(R.string.Capacity), 0, 100, "Ah", 0.01f);
                TelemetryDataSimple sensor4_rx_voltage = new TelemetryDataNumber(0x1BEA, sensor + "4 " + Rx + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple sensor4_rx_capacity = new TelemetryDataNumber(0x1BEC, sensor + "4 " + Rx + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple sensor4_PWM = new TelemetryDataNumber(0x1BED, sensor + "4 " + ctx.getString(R.string.PWM), 0, 100, "°", 0.1f);
                TelemetryDataSimple sensor4_fuel_flow = new TelemetryDataNumber(0x1BEF, sensor + "4 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple sensor4_fuel = new TelemetryDataNumber(0x1BF0, sensor + "4 " + ctx.getString(R.string.Fuel), 0, 6500, "ml", 10.0f); //cl -> ml
                TelemetryDataSimple sensor4_fuel_quality= new TelemetryDataNumber(0x1BF1, sensor + "4 " + ctx.getString(R.string.Fuel_quality), 0, 100, "°", 0.1f);
                //sensor4 engine noise level

                TelemetryDataSimple MUX1_source_voltage = new TelemetryDataNumber(0x1E00, MUX + "1 " + ctx.getString(R.string.Power_source_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX1_motor_voltage = new TelemetryDataNumber(0x1E01, MUX + "1 " + ctx.getString(R.string.Motor_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX1_motor_current = new TelemetryDataNumber(0x1E02, MUX + "1 " + ctx.getString(R.string.Motor_current), 0, 20, "A", 0.01f);
                TelemetryDataSimple MUX1_capacity = new TelemetryDataNumber(0x1E03, MUX + "1 " + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple MUX1_A1_voltage = new TelemetryDataNumber(0x1E10, MUX + "1 " + "A1 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX1_A1_airspeed = new TelemetryDataNumber(0x1E12, MUX + "1 " + "A1 " + ctx.getString(R.string.Airspeed), 0, 100, "km/h", 0.36f);
                TelemetryDataSimple MUX1_A2_voltage = new TelemetryDataNumber(0x1E18, MUX + "1 " + "A2 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX1_A3_voltage = new TelemetryDataNumber(0x1E20, MUX + "1 " + "A3 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX1_A3_fuel_flow = new TelemetryDataNumber(0x1E22, MUX + "1 " + "A1 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple MUX1_A3_fuel = new TelemetryDataNumber(0x1E23, MUX + "1 " + "A3 " + ctx.getString(R.string.Fuel), 0, 6500, "ml");
                TelemetryDataSimple MUX1_A4_voltage = new TelemetryDataNumber(0x1E28, MUX + "1 " + "A4 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX1_A5_voltage = new TelemetryDataNumber(0x1E30, MUX + "1 " + "A5 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);

                TelemetryDataSimple MUX2_source_voltage = new TelemetryDataNumber(0x1E40, MUX + "2 " + ctx.getString(R.string.Power_source_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX2_motor_voltage = new TelemetryDataNumber(0x1E41, MUX + "2 " + ctx.getString(R.string.Motor_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX2_motor_current = new TelemetryDataNumber(0x1E42, MUX + "2 " + ctx.getString(R.string.Motor_current), 0, 20, "A", 0.01f);
                TelemetryDataSimple MUX2_capacity = new TelemetryDataNumber(0x1E43, MUX + "2 " + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple MUX2_A1_voltage = new TelemetryDataNumber(0x1E50, MUX + "2 " + "A1 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX2_A1_airspeed = new TelemetryDataNumber(0x1E52, MUX + "2 " + "A1 " + ctx.getString(R.string.Airspeed), 0, 100, "km/h", 0.36f);
                TelemetryDataSimple MUX2_A2_voltage = new TelemetryDataNumber(0x1E58, MUX + "2 " + "A2 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX2_A3_voltage = new TelemetryDataNumber(0x1E60, MUX + "2 " + "A3 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX2_A3_fuel_flow = new TelemetryDataNumber(0x1E62, MUX + "2 " + "A1 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple MUX2_A3_fuel = new TelemetryDataNumber(0x1E63, MUX + "2 " + "A3 " + ctx.getString(R.string.Fuel), 0, 6500, "ml");
                TelemetryDataSimple MUX2_A4_voltage = new TelemetryDataNumber(0x1E68, MUX + "2 " + "A4 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX2_A5_voltage = new TelemetryDataNumber(0x1E70, MUX + "2 " + "A5 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);

                TelemetryDataSimple MUX3_source_voltage = new TelemetryDataNumber(0x1E80, MUX + "3 " + ctx.getString(R.string.Power_source_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX3_motor_voltage = new TelemetryDataNumber(0x1E81, MUX + "3 " + ctx.getString(R.string.Motor_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX3_motor_current = new TelemetryDataNumber(0x1E82, MUX + "3 " + ctx.getString(R.string.Motor_current), 0, 20, "A", 0.01f);
                TelemetryDataSimple MUX3_capacity = new TelemetryDataNumber(0x1E83, MUX + "3 " + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple MUX3_A1_voltage = new TelemetryDataNumber(0x1E90, MUX + "3 " + "A1 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX3_A1_airspeed = new TelemetryDataNumber(0x1E92, MUX + "3 " + "A1 " + ctx.getString(R.string.Airspeed), 0, 100, "km/h", 0.36f);
                TelemetryDataSimple MUX3_A2_voltage = new TelemetryDataNumber(0x1E98, MUX + "3 " + "A2 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX3_A3_voltage = new TelemetryDataNumber(0x1EA0, MUX + "3 " + "A3 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX3_A3_fuel_flow = new TelemetryDataNumber(0x1EA2, MUX + "3 " + "A1 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple MUX3_A3_fuel = new TelemetryDataNumber(0x1EA3, MUX + "3 " + "A3 " + ctx.getString(R.string.Fuel), 0, 6500, "ml");
                TelemetryDataSimple MUX3_A4_voltage = new TelemetryDataNumber(0x1EA8, MUX + "3 " + "A4 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX3_A5_voltage = new TelemetryDataNumber(0x1EB0, MUX + "3 " + "A5 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);

                TelemetryDataSimple MUX4_source_voltage = new TelemetryDataNumber(0x1EC0, MUX + "4 " + ctx.getString(R.string.Power_source_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX4_motor_voltage = new TelemetryDataNumber(0x1EC1, MUX + "4 " + ctx.getString(R.string.Motor_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX4_motor_current = new TelemetryDataNumber(0x1EC2, MUX + "4 " + ctx.getString(R.string.Motor_current), 0, 20, "A", 0.01f);
                TelemetryDataSimple MUX4_capacity = new TelemetryDataNumber(0x1EC3, MUX + "4 " + ctx.getString(R.string.Capacity), 0, 100000, "mAh");
                TelemetryDataSimple MUX4_A1_voltage = new TelemetryDataNumber(0x1ED0, MUX + "4 " + "A1 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX4_A1_airspeed = new TelemetryDataNumber(0x1ED2, MUX + "4 " + "A1 " + ctx.getString(R.string.Airspeed), 0, 100, "km/h", 0.36f);
                TelemetryDataSimple MUX4_A2_voltage = new TelemetryDataNumber(0x1ED8, MUX + "4 " + "A2 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX4_A3_voltage = new TelemetryDataNumber(0x1EE0, MUX + "4 " + "A3 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX4_A3_fuel_flow = new TelemetryDataNumber(0x1EE2, MUX + "4 " + "A1 " + ctx.getString(R.string.Fuel_flow), 0, 3000, "ml/min");
                TelemetryDataSimple MUX4_A3_fuel = new TelemetryDataNumber(0x1EE3, MUX + "4 " + "A3 " + ctx.getString(R.string.Fuel), 0, 6500, "ml");
                TelemetryDataSimple MUX4_A4_voltage = new TelemetryDataNumber(0x1EE8, MUX + "4 " + "A4 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple MUX4_A5_voltage = new TelemetryDataNumber(0x1EF0, MUX + "4 " + "A5 " + ctx.getString(R.string.Voltage), 0, 20, "V", 0.001f);

                TelemetryDataSimple Rx_GForce = new TelemetryDataNumber(0x1C58, Rx + ctx.getString(R.string.GForce), 0, 20, "g", 0.01f);
                TelemetryDataSimple Rx_compass_direction = new TelemetryDataNumber(0x1C5B, Rx + ctx.getString(R.string.Compass_direction), 0, 360, "°", 0.01f);
                TelemetryDataSimple Tx_number_controls = new TelemetryDataNumber(0x1C88, Tx + ctx.getString(R.string.Number_controls), 0, 2048, "");
                TelemetryDataSimple Tx_number_functions = new TelemetryDataNumber(0x1C8A, Tx + ctx.getString(R.string.Number_functions), 0, 2048, "");
                TelemetryDataSimple Tx_battery_voltage = new TelemetryDataNumber(0x1C8B, Tx + ctx.getString(R.string.Battery_voltage), 0, 20, "V", 0.01f);
                TelemetryDataSimple Tx_number_functions2 = new TelemetryDataNumber(0x1C8D, Tx + ctx.getString(R.string.Number_functions) + " [1]", 0, 2048, "");
                TelemetryDataSimple Tx_compass_direction = new TelemetryDataNumber(0x1CC9, Tx + ctx.getString(R.string.Compass_direction), 0, 360, "°", 0.01f);
                TelemetryDataSimple Tx_startup_warning = new TelemetryDataBitmask(0x1CCD, Tx + ctx.getString(R.string.Startup_warning));

                TelemetryDataSimple GPS_bearing_rx = new TelemetryDataNumber(0x1D23, GPS + ctx.getString(R.string.Bearing_rx), 0, 360, "°", 0.01f);
                TelemetryDataSimple GPS_bearing_rx_compass = new TelemetryDataNumber(0x1D23, GPS + ctx.getString(R.string.Bearing_rx_compass), 0, 360, "°", 0.01f);

                TelemetryDataSimple batLinkVario_power_source_voltage = new TelemetryDataNumber(0x1D80, LV + ctx.getString(R.string.Power_source_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple batLinkVario_motor_voltage = new TelemetryDataNumber(0x1D81, LV + ctx.getString(R.string.Motor_voltage), 0, 20, "V", 0.001f);
                TelemetryDataSimple batLinkVario_motor_current = new TelemetryDataNumber(0x1D82, LV + ctx.getString(R.string.Motor_current), 0, 20, "A", 0.1f);
                TelemetryDataSimple batLinkVario_capacity = new TelemetryDataNumber(0x1D83, LV + ctx.getString(R.string.Used_capacity), 0, 100000, "mAh");
                TelemetryDataSimple batLinkVario_airspeed = new TelemetryDataNumber(0x1D87, LV + ctx.getString(R.string.Airspeed), 0, 100, "km/h", 0.36f);

                TelemetryDataSimple RxMain_UTC = new TelemetryDataTimestamp(0x2C00, RxMain + ctx.getString(R.string.UTC));
                TelemetryDataSimple RxMain_status_word = new TelemetryDataBitmask(0x2C09, RxMain + ctx.getString(R.string.Status_word));
                TelemetryDataSimple RxSub1_UTC = new TelemetryDataTimestamp(0x2C10, RxSub1 + ctx.getString(R.string.UTC));
                TelemetryDataSimple RxSub1_status_word = new TelemetryDataBitmask(0x2C19, RxSub1 + ctx.getString(R.string.Status_word));
                TelemetryDataSimple RxSub2_UTC = new TelemetryDataTimestamp(0x2C20, RxSub2 + ctx.getString(R.string.UTC));
                TelemetryDataSimple RxSub2_status_word = new TelemetryDataBitmask(0x2C29, RxSub2 + ctx.getString(R.string.Status_word));
                TelemetryDataSimple Tx_UTC = new TelemetryDataTimestamp(0x2C80, Tx + ctx.getString(R.string.UTC));
                TelemetryDataSimple TX_status_word_TRX = new TelemetryDataBitmask(0x2C86, Tx + ctx.getString(R.string.Status_word) + " TRX");
                TelemetryDataSimple TX_status_word_HK = new TelemetryDataBitmask(0x2C86, Tx + ctx.getString(R.string.Status_word) + " HK");
                TelemetryDataSimple Sequencer_control_bits = new TelemetryDataBitmask(0x2C8E, Tx + ctx.getString(R.string.Sequencer_control));
                
                //operating hours

                TelemetryDataSimple GPS_ground_distance_TxRx = new TelemetryDataNumber(0x2D20, GPS + ctx.getString(R.string.Distance_ground) + TxRx, 0, 3000, "m", 0.1f);
                TelemetryDataSimple GPS_distance_TxRx = new TelemetryDataNumber(0x2D21, GPS + ctx.getString(R.string.Distance_ground) + TxRx, 0, 3000, "m", 0.1f);
                TelemetryDataSimple RTC_UTC = new TelemetryDataTimestamp(0x2B67, sensor + "RTC " + ctx.getString(R.string.UTC));
                TelemetryDataSimple air_pressure = new TelemetryDataNumber(0x2B6B, sensor + ctx.getString(R.string.Air_pressire), 0, 500000, "Pa");

                TelemetryDataSimple MUX1_A2_rpm = new TelemetryDataNumber(0x2E1A, MUX + "1 " + ctx.getString(R.string.RPM), 0, 42000, "rpm");
                TelemetryDataSimple MUX2_A2_rpm = new TelemetryDataNumber(0x2E5A, MUX + "2 " + ctx.getString(R.string.RPM), 0, 42000, "rpm");
                TelemetryDataSimple MUX3_A2_rpm = new TelemetryDataNumber(0x2E9A, MUX + "3 " + ctx.getString(R.string.RPM), 0, 42000, "rpm");
                TelemetryDataSimple MUX4_A2_rpm = new TelemetryDataNumber(0x2EDA, MUX + "4 " + ctx.getString(R.string.RPM), 0, 42000, "rpm");

                TelemetryDataSimple sensor1_rpm1 = new TelemetryDataNumber(0x2B84, sensor + "1 " + ctx.getString(R.string.RPM) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor1_rpm2 = new TelemetryDataNumber(0x2B85, sensor + "1 " + ctx.getString(R.string.RPM) + " 2", 0, 42000, "rpm");
                TelemetryDataSimple sensor1_rpm_soll1 = new TelemetryDataNumber(0x2B86, sensor + "1 " + ctx.getString(R.string.RPM_Soll) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor1_rpm_soll2 = new TelemetryDataNumber(0x2B87, sensor + "1 " + ctx.getString(R.string.RPM_Soll) + " 2", 0, 42000, "rpm");

                TelemetryDataSimple sensor2_rpm1 = new TelemetryDataNumber(0x2BA4, sensor + "2 " + ctx.getString(R.string.RPM) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor2_rpm2 = new TelemetryDataNumber(0x2BA5, sensor + "2 " + ctx.getString(R.string.RPM) + " 2", 0, 42000, "rpm");
                TelemetryDataSimple sensor2_rpm_soll1 = new TelemetryDataNumber(0x2BA6, sensor + "2 " + ctx.getString(R.string.RPM_Soll) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor2_rpm_soll2 = new TelemetryDataNumber(0x2BA7, sensor + "2 " + ctx.getString(R.string.RPM_Soll) + " 2", 0, 42000, "rpm");

                TelemetryDataSimple sensor3_rpm1 = new TelemetryDataNumber(0x2BC4, sensor + "3 " + ctx.getString(R.string.RPM) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor3_rpm2 = new TelemetryDataNumber(0x2BC5, sensor + "3 " + ctx.getString(R.string.RPM) + " 2", 0, 42000, "rpm");
                TelemetryDataSimple sensor3_rpm_soll1 = new TelemetryDataNumber(0x2BC6, sensor + "3 " + ctx.getString(R.string.RPM_Soll) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor3_rpm_soll2 = new TelemetryDataNumber(0x2BC7, sensor + "3 " + ctx.getString(R.string.RPM_Soll) + " 2", 0, 42000, "rpm");

                TelemetryDataSimple sensor4_rpm1 = new TelemetryDataNumber(0x2BE4, sensor + "4 " + ctx.getString(R.string.RPM) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor4_rpm2 = new TelemetryDataNumber(0x2BE5, sensor + "4 " + ctx.getString(R.string.RPM) + " 2", 0, 42000, "rpm");
                TelemetryDataSimple sensor4_rpm_soll1 = new TelemetryDataNumber(0x2BE6, sensor + "4 " + ctx.getString(R.string.RPM_Soll) + " 1", 0, 42000, "rpm");
                TelemetryDataSimple sensor4_rpm_soll2 = new TelemetryDataNumber(0x2BE7, sensor + "4 " + ctx.getString(R.string.RPM_Soll) + " 2", 0, 42000, "rpm");

                break;

        }

    }

}


