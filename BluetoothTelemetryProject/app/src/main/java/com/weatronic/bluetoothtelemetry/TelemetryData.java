/**
 * Contains definitions of a basic Telemetry Data field and every specific type of field.
 */
package com.weatronic.bluetoothtelemetry;

import android.content.Context;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Locale;
import java.util.Vector;

/**
 * A class representing a generic telemetry data field.
 * The fields and functions in this class are common for every field.
 * Also contains high-level static variables.
 * Further separated into {@link TelemetryDataSimple simple} and {@link TelemetryDataComposite composite} fields.
 */
abstract class TelemetryData{
    static final int PROTOCOL_DV4 = 1;
    static final int PROTOCOL_SKYNAVIGATOR = 2;
    /**
     * Currently used protocol.
     * Some actions are done differently based on protocol.
     */
    static int protocol = PROTOCOL_DV4;
    /**
     * Integer constant for data type IDs. The list goes on...
     */
    final static int SIGNED_BYTE = 0x00;
    final static int UNSIGNED_BYTE = 0x01;
    final static int SIGNED_SHORT = 0x02;
    final static int UNSIGNED_SHORT = 0x03;
    final static int SIGNED_WORD =0x04;
    final static int UNSIGNED_WORD = 0x05;
    final static int FLOAT = 0x06;
    final static int SIGNED_12BIT = 0x09;
    final static int GPS_PACKET_HIGH_RES = 0x0A;
    final static int GYRO_PACKET = 0x0B;
    final static int GPS_PACKET = 0x0C;
    final static int CONTROL_DATA_PACKET = 0x0D;
    final static int POWER_SUPPLY_PACKET = 0x10;
    final static int CONTROL_ID_PACKET = 0x11;
    final static int TEXT_MESSAGE = 0x12;
    final static int VM_PACKET = 0x13;
    /**
     * Unique numeric identifier of the field.
     */
    long ID;
    /**
     * Name of the field to be displayed.
     */
    String name = "";
    /**
     * ID of the datatype this field contains.
     */
    int type;
    /**
     * Stores parent field if the field is a subfield.
     */
    TelemetryDataComposite parent = null;
    /**
     * List of all fields mapped to IDs.
     */
    protected static HashMap<Long, TelemetryData> fields = new HashMap<>();
    /**
     * List of all field IDs mapped to field names for reverse searching.
     */
    protected static HashMap<String, Long> nameToId = new HashMap<>();


    //for simulation, to be able to get random IDs
    public static Vector<Long> idList = new Vector<>();


    /**
     * Field type is usually contained in field ID.
     * @param ID field ID
     * @return Type ID
     */
    public static int idToType(long ID){
        if(ID == 0){
            return 0;
        }
        if(protocol == PROTOCOL_DV4){
            String hexStr = Long.toHexString(ID);
            String typeChar = hexStr.substring(hexStr.length() - 1);
            int type;
            try {
                type = Integer.parseInt(typeChar, 16);
            }catch(Exception e){
                //assume unsigned word - use value as is
                type = UNSIGNED_WORD;
            }
            return type;
        }else{
            //SkyNavigator
            String binStr = Long.toBinaryString(ID);
            String hexStr = Long.toHexString(ID);
            while(binStr.length() < 4 * hexStr.length()){
                binStr = "0" + binStr;
            }
            String typeBits = binStr.substring(0, 5);
            int type;
            try {
                type = Integer.parseInt(typeBits, 2);
            }catch(Exception e){
                //assume unsigned word - use value as is
                type = UNSIGNED_WORD;
            }
            return type;
        }
    }
    /**
     * Simplest possible constructor.
     * The most basic field must have an identifier and a name.
     */
    public TelemetryData(long ID, String name){
        this.ID = ID;
        this.name = name;
        this.type = idToType(ID);
        fields.put(ID, this);
        nameToId.put(name, ID);
        //for simulation
        idList.add(ID);
    }
    /**
     * Converts unsigned numbers to signed.
     * In signed numbers, first bit is used to store the sign; in unsigned numbers, first bit is a part of the number.
     * An unsigned variable can therefore contain a 2x larger number, but only positive.
     * This function gets the maximum possible value of a signed number of given length by removing the first bit (FF -> 7F).
     * If given unsigned number is larger than that, the value "above limit" is substracted from 0.
     * Note: hex-strings of course have no sign and always return positive numbers on parse.
     * @param value unsigned value to be converted
     * @param sizeBytes Based on type, e.g. 2 bytes for Signed Short
     * @return Signed number
     */
    public static long unsignedToSigned(long value, int sizeBytes){
        String maxPositiveStr = "7F";
        for(int i = 1; i < sizeBytes; i++){
            maxPositiveStr += "FF";
        }
        long maxPositive = Long.parseLong(maxPositiveStr, 16);
        if(value > maxPositive){
            value = 0 - (value - maxPositive);
        }
        return value;
    }
    /**
     * Swaps the order of bytes from LE to BE.
     * In Bluetooth messages, the hex numbers are sent as little-endian. To properly convert them to Java integers, the byte order must be changed.
     * The string is split into bytes (two hex symbols) and built again in reverse order.
     * @param little Little-endian hex string
     * @return Big-endian hex string
     */
    public static String littleToBigEndian(String little){
        String big = "";
        String curByte;
        for(int i = 0; i < little.length(); i += 2){
            curByte = little.substring(i, i+2);
            big = curByte.concat(big);
        }
        //Log.i("", little + " <-> " + big);
        return big;
    }
    /**
     * Returns size in bytes for each data type. Sizes are fixed.
     * @param type ID of datatype
     * @return Size in bytes
     */
    public static int sizeByType(int type){
        switch(type) {
            case UNSIGNED_BYTE:
            case SIGNED_BYTE:
                return 1;
            case UNSIGNED_SHORT:
            case SIGNED_SHORT:
                return 2;
            case UNSIGNED_WORD:
            case SIGNED_WORD:
                return 4;
            case FLOAT:
                return 4;
            case SIGNED_12BIT:
                return 3;
            case GPS_PACKET_HIGH_RES:
                return 14;
            case GPS_PACKET:
                return 18;
            case GYRO_PACKET:
                return 10;
            case CONTROL_DATA_PACKET:
                return 24;
            case POWER_SUPPLY_PACKET:
                return 64;
            case TEXT_MESSAGE:
                return 16;
            case VM_PACKET:
                return 24;
        }
        return 0;
    }
    /**
     * Returns true if a type is a signed number.
     * @param type ID of datatype
     */
    public static boolean isSigned(int type) {
        switch(type){
            case SIGNED_BYTE:
            case SIGNED_SHORT:
            case SIGNED_WORD:
            case SIGNED_12BIT:
                return true;
            default: return false;
        }
    }
}

/**
 * A class representing a basic telemetry data field that contains one value.
 */
abstract class TelemetryDataSimple extends TelemetryData{
    /**
     * Value of the field before field-specific conversion.
     * All simple data is transmitted as one integer number.
     * long (8 byte) because Java only has signed types: unsigned int (4 byte) may cause integer overflow.
     */
    public long valueRaw;
    /**
     * {@link #adaptValue(long) "Adapted"} value, actual value to be displayed (int, float, String...)
     */
    Object value = 0;
    /**
     * Measuring units. Can me empty.
     */
    String units = "";
    /**
     * Sets the field's value using field-specific conversion.
     * @param value Raw numeric value after protocol-specific conversion
     */
    public void setValue(long value){
        if(isSigned(type)){
            value = unsignedToSigned(value, sizeByType(type));
        }
        this.valueRaw = value;
        this.value = adaptValue(value);
    }
    /**
     * Converts raw numeric value into actual value to be displayed.
     * Unique for each child class.
     */
    protected abstract Object adaptValue(long value);
    /**
     * Basic constructor.
     * @see TelemetryData#TelemetryData(long, String)
     */
    public TelemetryDataSimple(long ID, String name){
        super(ID, name);
    }
}

/**
 * A class representing a simple telemetry data field that contains date/time.
 */
class TelemetryDataTimestamp extends TelemetryDataSimple{
    /**
     * Converts raw value (timestamp) into readable date/time string.
     * @param value Raw value
     * @return Date/time string
     */
    @Override
    protected String adaptValue(long value){
        long tsMilliseconds = value * 1000;
        try{
            Locale here = Locale.getDefault();
            DateFormat sdf = new SimpleDateFormat("dd.MM.yyyy, HH:mm:ss", here);
            Date netDate = (new Date(tsMilliseconds));
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "error";
        }
    }
    /**
     * Basic constructor.
     * @see TelemetryData#TelemetryData(long, String)
     */
    public TelemetryDataTimestamp(long ID, String name){
        super(ID, name);
    }

}

/**
 * A class representing a simple telemetry data field that contains precise time.
 */
class TelemetryDataMsUTC extends TelemetryDataSimple{
    /**
     * Converts raw value (timestamp) into readable time string.
     * @param value Raw value
     * @return Time string
     */
    @Override
    protected String adaptValue(long value){
        try{
            Locale here = Locale.getDefault();
            DateFormat sdf = new SimpleDateFormat("HH:mm:ss.SSS", here);
            Date netDate = (new Date(value));
            return sdf.format(netDate);
        }
        catch(Exception ex){
            return "error";
        }
    }
    /**
     * Basic constructor.
     * @see TelemetryData#TelemetryData(long, String)
     */
    public TelemetryDataMsUTC(long ID, String name){
        super(ID, name);
    }
}

/**
 * A class representing a simple telemetry data field that contains a numeric value.
 */
class TelemetryDataNumber extends TelemetryDataSimple{
    /**
     * Minimal allowed/"sensible" value for this field.
     */
    Number limitMin;
    /**
     * Maximal allowed/"sensible" value for this field.
     */
    Number limitMax;
    /**
     * conversion factors for linear conversion (k * x + a)
     * e.g. voltage is transmitted in mV, but must be displayed in V; then k = 0.001, a = 0.
     */
    float factorK;
    /**
     * See {@link #factorK}
     */
    int factorA;
    /**
     * Converts raw number into value to be displayed using conversion factors.
     * @param value Raw value
     * @return Converted value
     */
    @Override
    protected Object adaptValue(long value){
        if(factorK % 1.0f == 0.0f){
            return Math.round(factorK * value) + factorA;
        }else{
            return factorK * value + factorA;
        }
    }

    public TelemetryDataNumber(long ID, String name, Number limitMin, Number limitMax, String units){
        this(ID, name, limitMin, limitMax, units, 1, 0);
    }

    public TelemetryDataNumber(long ID, String name, Number limitMin, Number limitMax, String units, float factorK){
        this(ID, name, limitMin, limitMax, units, factorK, 0);
    }
    /**
     * Class constructor, including number-specific fields.
     * @see TelemetryData#TelemetryData(long, String)
     */
    public TelemetryDataNumber(long ID, String name, Number limitMin, Number limitMax, String units, float factorK, int factorA){
        super(ID, name);
        this.units = units;
        this.factorK = factorK;
        this.factorA = factorA;
        this.limitMin = limitMin;
        this.limitMax = limitMax;
    }
}
/**
 * A class representing a simple telemetry data field that contains list of bit flags.
 */
class TelemetryDataBitmask extends TelemetryDataNumber{
    /**
     * Length of bitmask in bits.
     */
    int sizeBits = 8;
    /**
     * Converts numeric value into bitmask string.
     * @return Bitmask string
     */
    protected String getBits() {
        String bits = Integer.toBinaryString((int)value);
        while(bits.length() < sizeBits){
            bits = "0" + bits;
        }
        return bits;
    }
    /**
     * Basic constructor.
     * @see TelemetryData#TelemetryData(long, String)
     */
    public TelemetryDataBitmask(long ID, String name){
        super(ID, name, 0, Math.pow(2, sizeByType(idToType(ID)) * 8) - 1, "");
        this.sizeBits = sizeByType(idToType(ID)) * 8;
    }

}
/**
 * A class representing a simple telemetry data field that contains a numeric value, where one bit is an "is valid" flag.
 */
class TelemetryDataNumberValidFlag extends TelemetryDataNumber{
    /**
     * Position of bit that serves as flag
     */
    int flagPos;

    public TelemetryDataNumberValidFlag(long ID, String name, int flagPos, Number limitMin, Number limitMax, String units){
        this(ID, name, flagPos, limitMin, limitMax, units, 1, 0);
    }

    public TelemetryDataNumberValidFlag(long ID, String name, int flagPos, Number limitMin, Number limitMax, String units, float factorK){
        this(ID, name, flagPos, limitMin, limitMax, units, factorK, 0);
    }
    /**
     * Class constructor, including number-specific fields and flag position
     * @see TelemetryDataNumber#TelemetryDataNumber(long, String, Number, Number, String, float, int)
     */
    public TelemetryDataNumberValidFlag(long ID, String name, int flagPos, Number limitMin, Number limitMax, String units, float factorK, int factorA){
        super(ID, name, limitMin, limitMax, units, factorK, factorA);
        this.flagPos = flagPos;
    }
    /**
     * Returns true if the bit at specified position is 1 and the number is therefore valid.
     * @param valueRaw Raw value
     */
    private boolean isValid(long valueRaw){
        String bits = Long.toBinaryString(valueRaw);
        char flag = bits.charAt(this.flagPos);
        return flag == '1';
    }
    /**
     * Removes the bit that is a flag from the number.
     * The flag is not part of the number proper.
     */
    private long removeFlag(long raw){
        String bits = Long.toBinaryString(raw);
        StringBuilder sb = new StringBuilder(bits);
        sb.deleteCharAt(this.flagPos);
        bits = sb.toString();
        return Long.parseLong(bits, 2);
    }
    /**
     * Checks the flag and converts raw value into value to be displated.
     * @param value Raw value
     * @return Converted value
     * @see TelemetryDataNumber#adaptValue(long)
     */
    @Override
    public Object adaptValue(long value){
        if(isValid(value)){
            value = removeFlag(valueRaw);
        }else{
            //return old value/ignore invalid message
            return this.value;
        }
        if(factorK % 1.0f == 0.0f){
            return Math.round(factorK * value) + factorA;
        }else{
            return factorK * value + factorA;
        }
    }
}
/**
 * A class representing a telemetry data field that is composed of several subfields.
 * Each of subfields is usually a {@link TelemetryDataSimple simple field}.
 */
abstract class TelemetryDataComposite extends TelemetryData{
    /**
     * List of subfields of this composite field.
     */
    Vector<TelemetryDataSimple> subfields = new Vector<>();
    /**
     * List of IDs of subfields of this composite field.
     */
    Vector<Long> children = new Vector<>();
    /**
     * Used in GUI to expand/contract the list of subfields.
     */
    boolean showingChildren = false;
    /**
     * Basic constructor.
     * @see TelemetryData#TelemetryData(long, String)
     */
    public TelemetryDataComposite(long ID, String name){
        super(ID, name);
    }
    /**
     * Creates a subfield for this composite field.
     * @param ID ID of parent field
     * @param type Type of subfield. Cannot be devised from field ID, because that type belongs to composite field.
     * @return ID of newly created field
     */
    abstract protected long newSubField(long ID, int type);
    /**
     * Sets the the values of each subfield.
     * The input string is separated into blocks of length correspoding to subfield size;
     * Each block is converted by each subfield's {@link TelemetryDataSimple#setValue(long) setter function}.
     * @param inputString Raw string containing all values for this composite field
     */
    public void setValues(String inputString){
        int curPos = 0, endPos;
        TelemetryDataSimple field;
        String piece;
        for(int i = 0; i < subfields.size(); i++){
            try {
                field = subfields.get(i);
                endPos = curPos + sizeByType(field.type) * 2;
                piece = inputString.substring(curPos, endPos);
                curPos = endPos;
                field.setValue(Long.parseLong(TelemetryData.littleToBigEndian(piece), 16));
            }catch(Exception e){
                //invalid input string
            }
        }
    }
    /**
     * After all subfields are created, this method sets the {@link TelemetryData#parent} field for each subfield.
     * A tradeoff to keep the number of parameters for {@link #newSubField(long, int)} minimal.
     */
    protected void updateParentForSubfields(){
        long id;
        TelemetryDataSimple child;
        for(int i = 0; i < children.size(); i++){
            id = children.get(i);
            child = (TelemetryDataSimple)TelemetryData.fields.get(id);
            if(child.parent == null)
                child.parent = this;
        }
    }
}
/**
 * A class representing a telemetry data field that is composed of several subfields.
 * In DV4 protocol, a field ID consists of following: Actual ID - 2 bytes, Index - 1 byte, Type - 1 byte.
 * It is therefore possible to build the IDs of subfields by keeping the ID and changing Index and Type. See {@link TelemetryDataCompositeDV4#newSubField(long, int)}
 * Each of subfields is usually a {@link TelemetryDataSimple simple field}.
 */
class TelemetryDataCompositeDV4 extends TelemetryDataComposite{
    public TelemetryDataCompositeDV4(long ID, String name){
        super(ID, name);
    }
    /**
     * Creates a subfield for this composite field.
     * In this case, and ID can be built by changing the index (3rd byte) and type (4th byte).
     * @param ID ID of parent field
     * @param type Type of subfield. Cannot be devised from field ID, because that type belongs to composite field.
     * @return ID of newly created field
     */
    @Override
    protected long newSubField(long ID, int type){
        String baseId = Long.toHexString(ID).substring(0, 2);
        //virtual type, not used in IDs
        int subType;
        if(type == SIGNED_12BIT)
            subType = SIGNED_SHORT;
        else
            subType = type;
        String finalIdStr = baseId + Integer.toHexString(subfields.size() + 1) + Integer.toHexString(subType);
        long finalId = Integer.parseInt(finalIdStr, 16);
        this.children.add(finalId);
        return finalId;
    }
}
/**
 * A class representing a telemetry data field that contains GPS data.
 * @see TelemetryDataCompositeDV4
 */
class TelemetryDataGPS_DV4 extends TelemetryDataCompositeDV4{
    /**
     * A class constructor.
     * Also constructs all GPS subfields for this field.
     */
    public TelemetryDataGPS_DV4(int ID, String name, Context ctx){
        super(ID, name);
        subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_WORD), this.name + " " + ctx.getResources().getString(R.string.Latitude), -90, 90, "°", (1.0f / 6000000.0f)));
        subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_WORD), this.name + " " + ctx.getResources().getString(R.string.Longitude), -180, 180, "°", (1.0f / 6000000.0f)));
        subfields.add(new TelemetryDataNumberValidFlag(newSubField(ID, UNSIGNED_SHORT), this.name + " " + ctx.getResources().getString(R.string.Speed), 0, 0, 60, "kn", 0.1f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_SHORT), this.name + " " + ctx.getResources().getString(R.string.Altitude), -1000, 8000, "m", 0.1f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), this.name + " " + ctx.getResources().getString(R.string.Course), 0, 360, "°", 0.01f));
        subfields.add(new TelemetryDataMsUTC(newSubField(ID, UNSIGNED_WORD), this.name + " " + ctx.getResources().getString(R.string.UTC)));
        updateParentForSubfields();
    }
}
/**
 * A class representing a telemetry data field that is contains an array of data.
 * The array has a type and the elements are numbered - e.g. values for Servos.
 * @see TelemetryDataCompositeDV4
 */
class TelemetryDataArrayDV4 extends TelemetryDataCompositeDV4{

    public TelemetryDataArrayDV4(long ID, String name, String elementName, int size, int type, int firstIndex, Number limitMin, Number limitMax, String units){
        this(ID, name, elementName, size, type, firstIndex, limitMin, limitMax, units, 1, 0);
    }

    public TelemetryDataArrayDV4(long ID, String name, String elementName, int size, int type, int firstIndex, Number limitMin, Number limitMax, String units, float factorK){
        this(ID, name, elementName, size, type, firstIndex, limitMin, limitMax, units, factorK, 0);
    }
    /**
     * A class constructor, including array-specific parameters.
     * @param elementName Name of each subfield. For example: name = Servos, element name = Servo.
     * @param size Size of array
     * @param firstIndex Index of first subfield. For example, if firstIndex = 17, size = 16: Servo17, Servo18, ..., Servo32.
     * @see TelemetryDataNumber#TelemetryDataNumber(long, String, Number, Number, String, float, int)
     */
    public TelemetryDataArrayDV4(long ID, String name, String elementName, int size, int type, int firstIndex, Number limitMin, Number limitMax, String units, float factorK, int factorA){
        super(ID, name);
        for(int i = 0; i < size; i++){
            //make sub fields
            subfields.add(new TelemetryDataNumber(newSubField(ID, type), elementName + " " + (firstIndex + i), limitMin, limitMax, units, factorK, factorA));
        }
        updateParentForSubfields();
    }

}
/**
 * A class representing a telemetry data field that is composed of several subfields.
 * For this type of field, every subfield is an independent field and can be transmitted both one by one and as a packet.
 * The "Composite"/packet class therefore contains references to those fields.
 * The other type is {@link TelemetryDataCompositeSkyNavSolid}
 * Each of subfields is usually a {@link TelemetryDataSimple simple field}.
 */
class TelemetryDataCompositeSkyNavReferential extends TelemetryDataComposite{
    public TelemetryDataCompositeSkyNavReferential(long ID, String name){
        super(ID, name);
    }
    /**
     * Creates a subfield for this composite field.
     * SkyNavigator protocol does not use Indices. Therefore, a base ID (minus type) must be supplied.
     * @param ID Base ID for new field (all bytes except 1st)
     * @param type Type of subfield. Cannot be devised from field ID, because that type belongs to composite field.
     * @return ID of newly created field
     */
    @Override
    protected long newSubField(long ID, int type){
        String baseID = Long.toBinaryString(ID);
        baseID = baseID.substring(1, baseID.length());
        //virtual type, not used in IDs
        int subType;
        if(type == SIGNED_12BIT)
            subType = SIGNED_SHORT;
        else
            subType = type;
        String finalIdStr = Long.toBinaryString(subType) + baseID;
        long finalId = Long.parseLong(finalIdStr, 2);
        this.children.add(finalId);
        return finalId;
    }
    /**
     * Creates a subfield for this composite field.
     * For cases when there is only one type and ID can be given directly
     * @param ID Base ID for new field
     * @return ID of newly created field
     */
    protected long newSubField(long ID){
        this.children.add(ID);
        return ID;
    }
}
/**
 * A class representing a telemetry data field that contains GPS data.
 * @see TelemetryDataCompositeSkyNavReferential
 */
class TelemetryDataGPS_SkyNav extends TelemetryDataCompositeSkyNavReferential{
    /**
     * A class constructor.
     * Also constructs all GPS subfields for this field.
     */
    public TelemetryDataGPS_SkyNav(int ID, String name, Context ctx){
        super(ID, name);
        int midBytes = Integer.parseInt(Long.toBinaryString(ID).substring(5, 12));
        String midBytesStr = Integer.toHexString(midBytes);
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "1", 16), SIGNED_WORD), this.name + " " + ctx.getResources().getString(R.string.Latitude), -90, 90, "°", (1.0f / 6000000.0f)));
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "2", 16), SIGNED_WORD), this.name + " " + ctx.getResources().getString(R.string.Longitude), -180, 180, "°", (1.0f / 6000000.0f)));
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "3", 16), UNSIGNED_BYTE), this.name + " " + ctx.getResources().getString(R.string.Speed), 0, 60, "kn", 0.1f));
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "4", 16), SIGNED_SHORT), this.name + " " + ctx.getResources().getString(R.string.Altitude), -1000, 8000, "m", 0.1f));
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "5", 16), UNSIGNED_SHORT), this.name + " " + ctx.getResources().getString(R.string.Course), 0, 360, "°", 0.01f));
        subfields.add(new TelemetryDataMsUTC(newSubField(Long.parseLong(midBytesStr + "8", 16), UNSIGNED_WORD), this.name + " " + ctx.getResources().getString(R.string.UTC)));
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "9", 16), UNSIGNED_BYTE), this.name + " " + ctx.getResources().getString(R.string.FracUTC), 0, 996, "ms", 4.0f));
        subfields.add(new TelemetryDataNumber(newSubField(Long.parseLong(midBytesStr + "A", 16), SIGNED_SHORT), this.name + " " + ctx.getResources().getString(R.string.AltitudeRelative), -1000, 8000, "m", 0.1f));
        updateParentForSubfields();
    }
}
/**
 * A class representing a telemetry data field that is contains an array of data.
 * The array has a type and the elements are numbered - e.g. values for Servos.
 * @see TelemetryDataCompositeSkyNavReferential
 */
class TelemetryDataArraySkyNav extends TelemetryDataCompositeSkyNavReferential{

    public TelemetryDataArraySkyNav(long ID, String name, String elementName, int size, int type, int firstIndex, long firstID, Number limitMin, Number limitMax, String units){
        this(ID, name, elementName, size, type, firstIndex, firstID, limitMin, limitMax, units, 1, 0);
    }

    public TelemetryDataArraySkyNav(long ID, String name, String elementName, int size, int type, int firstIndex, long firstID, Number limitMin, Number limitMax, String units, float factorK){
        this(ID, name, elementName, size, type, firstIndex, firstID, limitMin, limitMax, units, factorK, 0);
    }
    /**
     * A class constructor, including array-specific parameters.
     * @param elementName Name of each subfield. For example: name = Servos, element name = Servo.
     * @param size Size of array
     * @param firstIndex Index of first subfield. For example, if firstIndex = 17, size = 16: Servo17, Servo18, ..., Servo32.
     * @see TelemetryDataNumber#TelemetryDataNumber(long, String, Number, Number, String, float, int)
     */
    public TelemetryDataArraySkyNav(long ID, String name, String elementName, int size, int type, int firstIndex, long firstID, Number limitMin, Number limitMax, String units, float factorK, int factorA){
        super(ID, name);
        for(int i = 0; i < size; i++){
            //make sub fields
            subfields.add(new TelemetryDataNumber(newSubField(firstID + i), elementName + " " + (firstIndex + i), limitMin, limitMax, units, factorK, factorA));
        }
        updateParentForSubfields();
    }

}
/**
 * A class representing a telemetry data field that is composed of several subfields.
 * This type of field exists only as a whole, i.e. subfields cannot be transmitted separately.
 * Still, it should be possible to access these subfields separately. Thus, a virtual field with a virtual ID is created.
 * See {@link @TelemetryDataCompositeSkyNavSolid#newSubField} for how and ID is built.
 * The other type is {@link TelemetryDataCompositeSkyNavReferential}
 * Each of subfields is usually a {@link TelemetryDataSimple simple field}.
 */
class TelemetryDataCompositeSkyNavSolid extends TelemetryDataComposite{
    public TelemetryDataCompositeSkyNavSolid(long ID, String name){
        super(ID, name);
    }
    /**
     * Creates a subfield for this composite field.
     * SkyNavigator protocol does not use Indices and there are no separate fields to refer to.
     * Therefore, a new ID is built by adding a 2-byte "index" at the end (see {@link TelemetryDataCompositeDV4#newSubField(long, int)}.
     * This way, it is guaranteed that the ID is not already taken.
     * @param ID Parent field ID
     * @param type Type of subfield. Cannot be devised from field ID, because that type belongs to composite field.
     * @return ID of newly created field
     */
    @Override
    protected long newSubField(long ID, int type){
        String baseID = Long.toBinaryString(ID);
        baseID = baseID.substring(1, baseID.length());
        //virtual type, not used in IDs
        int subType;
        if(type == SIGNED_12BIT)
            subType = SIGNED_SHORT;
        else
            subType = type;
        String indexStr = Integer.toBinaryString(subfields.size() + 1);
        while(indexStr.length() < 8){
            indexStr = "0" + indexStr;
        }
        String finalIdStr = Integer.toBinaryString(subType) + baseID + indexStr;
        long finalId = Long.parseLong(finalIdStr, 2);
        this.children.add(finalId);
        return finalId;
    }
}
/**
 * A class representing a telemetry data field that contains Power Supply data.
 * @see TelemetryDataCompositeSkyNavSolid
 */
class TelemetryDataPowerSupply extends TelemetryDataCompositeSkyNavSolid{
    /**
     * A class constructor.
     * Also constructs all PowerSupply subfields for this field.
     */
    public TelemetryDataPowerSupply(int ID, String name, Context ctx){
        super(ID, name);
        String PS = ctx.getResources().getString(R.string.Power_supply);
        String cellStr = ctx.getResources().getString(R.string.Cell) + " ";
        subfields.add(new TelemetryDataBitmask(newSubField(ID, UNSIGNED_WORD), PS + " " + ctx.getResources().getString(R.string.Status)));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " +  ctx.getResources().getString(R.string.Voltage), 0, 20, "V", 0.001f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " +  ctx.getResources().getString(R.string.Current), -20, 20, "A", 0.001f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " +  ctx.getResources().getString(R.string.Input_voltage), 0, 20, "V", 0.001f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " +  ctx.getResources().getString(R.string.Input_current), -20, 20, "A", 0.001f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " +  ctx.getResources().getString(R.string.Main_voltage), 0, 20, "V", 0.001f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " +  ctx.getResources().getString(R.string.Reserve_voltage), 0, 20, "V", 0.001f));
        subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_BYTE), PS + " " +  ctx.getResources().getString(R.string.Input_temperature), -40, 40, "°С"));
        subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_12BIT), PS + " " +  ctx.getResources().getString(R.string.Reserved), 0, 0, ""));
        for(int cell = 1; cell <= 4; cell++){
            subfields.add(new TelemetryDataBitmask(newSubField(ID, UNSIGNED_WORD), PS + " " + cellStr + cell + " " + ctx.getResources().getString(R.string.Status)));
            subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " + cellStr + cell + " " + ctx.getResources().getString(R.string.Voltage), 0, 20, "V", 0.001f));
            subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_SHORT), PS + " " + cellStr + cell + " " + ctx.getResources().getString(R.string.Current), -20, 20, "A", 0.001f));
            subfields.add(new TelemetryDataNumber(newSubField(ID, UNSIGNED_SHORT), PS + " " + cellStr + cell + " " + ctx.getResources().getString(R.string.Capacity), 0, 20, "mAh"));
            subfields.add(new TelemetryDataNumber(newSubField(ID, SIGNED_BYTE), PS + " " + cellStr + cell + " " + ctx.getResources().getString(R.string.Temperature), -40, 40, "°С"));
        }
        updateParentForSubfields();
    }
}

/**
 * A class representing a virtual composite field (array).
 * Functionally similar to any other Array field type, but the field does not really exist and does not most of parent class functionality.
 * Used simply to save space in both source code and program menus by grouping very similar fields into "categories" that can be expanded and contracted.
 * The "category" itself is a field, but it is assigned an impossible ID and serves mostly to hide large groups of similar fields (e.g. Timer1-Timer16 under Timers)
 */
class TelemetryDataArrayVirtual extends TelemetryDataComposite{

    /**
     * Creates a subfield for this composite field.
     * Simplified to the very basics; adds the ID to list of children and returns it back. Ignores type.
     * @param ID ID of the field to add.
     * @param type Type of subfield. Only added because the parent class needs it.
     * @return
     */
    @Override
    protected long newSubField(long ID, int type){
        this.children.add(ID);
        return ID;
    }

    public TelemetryDataArrayVirtual(long startID, long diffID, String name, String elementName, int size, Number limitMin, Number limitMax, String units){
        this(startID, diffID, name, elementName, size, limitMin, limitMax, units, 0, 1);
    }

    public TelemetryDataArrayVirtual(long startID, long diffID, String name, String elementName, int size, Number limitMin, Number limitMax, String units, float factorK){
        this(startID, diffID, name, elementName, size, limitMin, limitMax, units, factorK, 0);
    }
    /**
     * A class constructor, including virtual array-specific parameters.
     * @param startID ID of the first element of the array (the group of similar fields)
     * @param diffID Difference in IDs between subsequent elements. Can be different for different protocols, e.g. in DV4 it's always 3rd byte (diff = 0x10) and in SkyNav it's usually 4th byte (diff = 0x01)
     * @param elementName Name of each subfield. For example: name = Timers, element name = Timer.
     * @param size Size of array
     * @see TelemetryDataNumber#TelemetryDataNumber(long, String, Number, Number, String, float, int)
     */
    public TelemetryDataArrayVirtual(long startID, long diffID, String name, String elementName, int size, Number limitMin, Number limitMax, String units, float factorK, int factorA){
        //add fifth byte at the end for ID
        //no real field (or field generated by TelememetryDataCompositeSkyNavSolid) should overlap
        super(Long.parseLong(Long.toHexString(startID) + "A", 16), name);
        long nextID;
        //build an ID and add it as child to the virtual field
        for(int i = 0; i < size; i++){
            nextID = startID + diffID * i;
            subfields.add(new TelemetryDataNumber(newSubField(nextID, 0), elementName + " " + (i + 1), limitMin, limitMax, units, factorK, factorA));
        }
        updateParentForSubfields();
    }


}
