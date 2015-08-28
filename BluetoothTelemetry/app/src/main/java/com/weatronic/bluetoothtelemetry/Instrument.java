/**
* Contains definitions of a basic Instrument and every specific instruments.
*/
package com.weatronic.bluetoothtelemetry;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
//import android.util.Log;
import android.support.annotation.NonNull;
import android.util.Log;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Observable;
import java.util.Observer;

/**
 * A class representing dynamic graphic element, intended to monitor values of certain telemetry fields.
 * Instruments serve as GUI elements the user can add or remove at will.
 */
abstract class Instrument extends ImageView implements Observer{
    /**
     * Named constant for instrument type.
     */
    public static final int INSTRUMENT_FULL_LIST = 0;
    public static final int INSTRUMENT_NEEDLE = 1;
    public static final int INSTRUMENT_BAR = 2;
    public static final int INSTRUMENT_GPS_COORDS = 3;
    /**
     * List of instrument names to show in menus.
     * Indices correspond to constants above.
     */
    public static final String[] typeNames = {"Full list", "Needle pointer", "Vertical bar", "GPS Coordinates"};
    /**
     * Id of field to take data from
     */
    long fieldId = 0;
    /**
     * Id of instrument type. Needed to determine creation process.
     */
    public int type;
    /**
     * Named constant for x coordinate.
     */
    Context ctx;
    /**
     * Instance of Telemetry Data Container to access telemetry fields.
     */
    TelemetryDataContainer telemetry = TelemetryDataContainer.getInstance(ctx);
    /**
     * Instance of Parser to track when telemetry data is updated.
     */
    Parser parser;
    /**
     * Size of given instrument.
     */
    public int height, width;
    /**
     * Used to dynamically draw the instrument.
     */
    public Paint paint = new Paint();
    /**
     * A rectangle representing the size of text field.
     */
    public Rect textBounds = new Rect();
    /**
     * List of text field values (text mapped to identifier).
     * Every instrument will probably have text on it. All "text fields" to draw a stored here.
     */
    Map<String, String> textFieldValues = new HashMap<>();
    /**
     * List of text fields with coordinates.
     */
    Map<String, textField> textFields = new HashMap<>();
    /**
     * Class constructor.
     *
     */
    public Instrument(Context ctx, int type){
        super(ctx);
        this.ctx = ctx;
        this.type = type;
        this.parser = Parser.getInstance(ctx);
        parser.addObserver(this);
    }
    /**
     * Creates an instrument independently from class.
     * @param instrumentId Instrument type ID. Determines creation process (which class to construct).
     * @param sourceId ID of telemetry field the instrument should use data from. Can be left at 0 for instruments that do not require a source.
     */
    public static Instrument createInstrument(Context ctx, int instrumentId, long sourceId){
        switch(instrumentId){
            case INSTRUMENT_FULL_LIST:
                return new PlainTextListInstrument(ctx);
            case INSTRUMENT_NEEDLE:
                return new NeedlePointerInstrument(ctx, sourceId);
            case INSTRUMENT_BAR:
                return new BarInstrument(ctx, sourceId);
            case INSTRUMENT_GPS_COORDS:
                return new GPSCoordinatesInstrument(ctx, sourceId);
            default:
                return null;
        }
    }
    /**
     * Draws the instrument.
     * The common base for all instruments is text fields: this function draws each text field.
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        paint.setColor(Color.GRAY);
        height = this.getHeight();
        width = this.getWidth();
        paint.setTextSize(width / 15);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        //draw all text fields
        Iterator it = textFields.entrySet().iterator();
        textField nextField;
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry)it.next();
            nextField = (textField)pair.getValue();
            paint.getTextBounds(nextField.value, 0, nextField.value.length(), textBounds);
            canvas.drawText(nextField.value, (float)width * nextField.x - textBounds.width() / 2, (float)height * nextField.y, paint);
        }
    }
    /**
     * Initializes a text field.
     * @param name Name of the field
     * @param coordX - X coordinate (fraction of width)
     * @param coordY - Y coordinate (fraction of height)
     */
    public void defineTextField(String name, float coordX, float coordY){
        new textField(name, coordX, coordY);
    }
    /**
     * Fires when the {@link Parser} processes a new data message.
     * {@link #invalidate()} tells the instrument it has to be redrawn.
     */
    public void update(Observable notifier, Object data){
        this.invalidate();
    }
    /**
     * Clears instrument data to save memory.
     */
    public void destroy(){
        parser.deleteObserver(this);
    }
    /**
     * Builds a list of field names to choose from when creating an instrument.
     * @return List of names
     */
    abstract public ArrayAdapter<String> getAllowedFields();
    /**
     * Checks if the instrument can use data from a field.
     * @param name Name of the field
     * @return True if field can be used
     */
    abstract public boolean fieldUsable(String name);
    /**
     * Formats a value to 2 decimal places max.
     * @param val Unformatted value
     * @return Formatted string
     */
    protected String formatValue(Object val){
        try{
            return String.format("%.2f", (float)val);
        }catch(Exception e){
            return String.format("%d", (int)val);
        }
    }
    /**
     * A class representing a text field.
     */
    class textField{
        /**
         * Relative X position of field (center)
         */
        float x;
        /**
         * Relative Y position of field (center)
         */
        float y;
        /**
         * Text stored in field
         */
        String value;
        /**
         * Class constructor.
         */
        public textField(String name, float coordX, float coordY){
            //if a field is being redefined, delete old
            if(textFields.get(name) != null){
                textFields.remove(name);
            }
            this.value = textFieldValues.get(name);
            this.x = coordX;
            this.y = coordY;
            textFields.put(name, this);
        }
    }
}
/**
 * Sample instrument: a text list of all recieved values.
 * A rather special instrument used for testing.
 */
class PlainTextListInstrument extends Instrument{
    /**
     * Basic constructor.
     * @see Instrument#Instrument(Context, int)
     */
    public PlainTextListInstrument(Context ctx){
        super(ctx, INSTRUMENT_FULL_LIST);
    }
    /**
     * Draws the one large text field.
     * @see #drawMultiLine(Canvas, String, int, int, int)
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int textSize = width / 30;
        paint.setTextSize(textSize);
        paint.setStrokeWidth(2);
        paint.setAntiAlias(true);
        drawMultiLine(canvas, printData(), 0, 20, textSize);
    }
    /**
     * Draws text line by line.
     * @param str The big string to print
     * @see #printData()
     * @param x X-coordinate (pixels)
     * @param y Y-coordinate (pixels)
     * @param textSize Font size
     */
    private void drawMultiLine(Canvas canvas, String str, int x, int y, int textSize){
        int curY = y;
        String[] lines = str.split("\n");
        for (String line : lines) {
            canvas.drawText(line, x, curY, paint);
            curY += textSize * 1.1;
        }
    }
    /**
     * Builds a string for a simple field
     * Format: fieldname - value
     * @param in Simple Telemetry Field to print from
     * @return One line of data for simple field
     */
    private String printDataSimple(TelemetryData in){
        if (in == null) return "";
        String str = "";
        TelemetryDataSimple val = (TelemetryDataSimple)in;
        if(val.value != null){
            str = val.name + " (" + Long.toHexString(val.ID).toUpperCase() + ") = " + val.value + val.units + " (" + val.valueRaw + ")\n";
        }
        return str;
    }
    /**
     * Builds a string for a composite field
     * Format: fieldname1 - value1, fieldname2 - value2, ..., fieldnameN - valueN
     * @param in Composite Telemetry Field to print from
     * @return Several lines of data for every subfield
     */
    private String printDataComposite(TelemetryData in){
        if(in == null) return "";
        TelemetryDataComposite comp = (TelemetryDataComposite)in;
        String str = comp.name + "\n";
        TelemetryDataSimple val;
        for(int i = 0; i < comp.children.size(); i++){
            val = (TelemetryDataSimple)telemetry.getFieldById(comp.children.get(i));
            str += printDataSimple(val);
        }
        return str;
    }
    /**
     * Builds a large string representing all known data.
     * Format: fieldname1 - value1, fieldname2 - value2, ..., fieldnameN - valueN
     * @return Data to print in string form
     */
    private String printData(){
        if(parser.configFields == null) return null;
        int size = parser.configFields.length;
        TelemetryData val;
        String text = "";
        for(int i = 1; i < size; i++) {
            val = telemetry.getFieldById(parser.configFields[i]);
            if(val instanceof TelemetryDataSimple){
                text = text.concat(printDataSimple(val));
            }else{
                text = text.concat(printDataComposite(val));
            }
        }
        return text;
    }

    public ArrayAdapter<String> getAllowedFields(){
        //unused
        return null;
    }

    public boolean fieldUsable(String name){
        //unused (uses all fields)
        return true;
    }

}
/**
 * Sample instrument: needle pointer.
 * Shows a dashboard and a needle pointing at the current value.
 */
@SuppressLint("ViewConstructor")
class NeedlePointerInstrument extends Instrument{
    /**
     * Field to take data from
     */
    TelemetryDataNumber field;
    /**
     * Image of needle
     */
    public Bitmap needleBitMap = null;
    /**
     * A conversion matrix to center the needle bitmap
     */
    private Matrix centerMatrix = new Matrix();
    /**
     * A conversion matrix to rotate the needle bitmap
     */
    private Matrix rotateMatrix = new Matrix();
    /**
     * A combination of center and rotation matrices
     */
    private Matrix combinedMatrix = new Matrix();
    /**
     * Rectangle representing needle image in initial size.
     */
    RectF src;
    /**
     * Rectangle representing needle image in required size (size of instrument).
     */
    RectF trg = new RectF(0,0, width, height);
    /**
     * Needle angle corresponding to lowest possible telemetry value.
     */
    private final float minAngle = -135.0f;
    /**
     * Needle angle corresponding to highest possible telemetry value.
     */
    private final float maxAngle = 0 - minAngle;
    /**
     * @return Needle angle corresponding to current telemetry value.
     */
    private float getAngle(){
        float valRelative;
        try {
            valRelative = ((float) field.value - (int) field.limitMin);
        }catch(Exception e){
            valRelative = ((int) field.value - (int) field.limitMin);
        }
        float frac = valRelative / ((int) field.limitMax - (int) field.limitMin);
        return minAngle + (maxAngle - minAngle) * frac;
    }
    /**
     * Draws needle in addition to background and text fields.
     * @see Instrument#onDraw(Canvas)
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas){
        textFieldValues.put("value", formatValue(field.value) + field.units);
        defineTextField("value", 0.5f, 0.66f);
        super.onDraw(canvas);
        trg.set(0, 0, width, height);
        centerMatrix.setRectToRect(src, trg, Matrix.ScaleToFit.CENTER);
        rotateMatrix.setRotate(getAngle(), width / 2, height / 2);
        combinedMatrix.setConcat(rotateMatrix, centerMatrix);
        canvas.drawBitmap(needleBitMap, combinedMatrix, paint);
    }
    /**
     * Initializes each text field.
     * @see Instrument#defineTextField(String, float, float)
     */
    private void initFields(){
        defineTextField("value", 0.5f, 0.66f);
        defineTextField("name", 0.5f, 0.87f);
        defineTextField("min", 0.25f, 0.74f);
        defineTextField("max", 0.75f, 0.74f);
        defineTextField("mid1", 0.25f, 0.35f);
        defineTextField("mid2", 0.5f, 0.15f);
        defineTextField("mid3", 0.75f, 0.35f);
    }
    /**
     * Class constructor.
     * Fills text fields and initializes graphical elements
     * @param id ID of telemetry field to take data from
     * @see Instrument#Instrument(Context, int)
     */
    public NeedlePointerInstrument(Context ctx, long id){
        super(ctx, INSTRUMENT_NEEDLE);
        setImageResource(R.drawable.board);
        this.fieldId = id;
        this.field = (TelemetryDataNumber)telemetry.getFieldById(fieldId);
        textFieldValues.put("value", formatValue(field.value) + field.units);
        textFieldValues.put("name", field.name);
        int min = (int)field.limitMin;
        int max = (int)field.limitMax;
        float step = (max - min) / 4.0f;
        textFieldValues.put("min", "" + min);
        textFieldValues.put("max", "" + max);
        if(max - min > 10) {
            textFieldValues.put("mid1", "" + (int)(min + step));
            textFieldValues.put("mid2", "" + (int)(min + step * 2));
            textFieldValues.put("mid3", "" + (int)(min + step * 3));
        }else{
            textFieldValues.put("mid1", "" + (min + step));
            textFieldValues.put("mid2", "" + (min + step * 2.0f));
            textFieldValues.put("mid3", "" + (min + step * 3.0f));
        }
        initFields();
        needleBitMap = BitmapFactory.decodeResource(getResources(), R.drawable.arrow);
        src = new RectF(0,0,needleBitMap.getWidth(), needleBitMap.getHeight());
    }
    /**
     * @return Fields to be shown in the list when creating an instrument.
     */
    @Override
    public ArrayAdapter<String> getAllowedFields(){
        ArrayAdapter<String> adp = new ArrayAdapter<>(ctx, R.layout.field_name);
        TelemetryData nextField;
        for (Object o : TelemetryData.fields.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            nextField = (TelemetryData) pair.getValue();
            //numeric fields and composite fields that may contain numeric subfields
            if (nextField instanceof TelemetryDataComposite || (nextField instanceof TelemetryDataNumber && (nextField.parent == null || nextField.parent.showingChildren))) {
                adp.add(nextField.name);
            }
        }
        adp.sort(NumberAwareAlphabeticSort.stringComparator);
        return adp;
    }
    /**
     * @return True if the field can be used to create the instrument.
     * @param name Name of field to be checked
     */
    public boolean fieldUsable(String name){
        TelemetryData field = telemetry.getFieldByName(name);
        return field instanceof TelemetryDataNumber;
    }
    /**
     * Cleans up instrument resources.
     * Mainly needed to destroy reference to bitmap to avoid memory overflow.
     */
    @Override
    public void destroy(){
        super.destroy();
        //destroy bitmap reference to avoid out of memory error
        needleBitMap = null;
    }
}
/**
 * Sample instrument: vertical bar.
 * Shows a bar with height correspondint to current value.
 */
class BarInstrument extends Instrument{
    /**
     * Field to take data from
     */
    TelemetryDataNumber field;
    /**
     * Graphical element: vertical bar.
     */
    RectF bar;
    /**
     * Draws needle in addition to background and text fields.
     * @see Instrument#onDraw(Canvas)
     */
    @Override
    protected void onDraw(@NonNull Canvas canvas){
        //move value text field to be near top of bar
        textFieldValues.put("value", formatValue(field.value) + field.units);
        defineTextField("value", 0.9f, 0.9f * (1.0f - getRelativeBarSize()));
        super.onDraw(canvas);
        //calculate bar height based on telemetry value
        float maxBarHeight = height * 0.9f;
        float relativeBarSize = maxBarHeight * (1.0f - getRelativeBarSize());
        bar.set((int) (width * 0.2), (int)relativeBarSize - 2, (int) (width * 0.8), maxBarHeight);
        //calculate color
        paint.setColor(getBarColor());
        canvas.drawRect(bar, paint);
    }
    /**
     * @return Bar height corresponding to current telemetry value.
     */
    private float getRelativeBarSize(){
        float valRelative;
        try {
            valRelative = ((float) field.value - (int) field.limitMin);
        }catch(Exception e){
            valRelative = ((int) field.value - (int) field.limitMin);
        }
        return valRelative / ((int) field.limitMax - (int) field.limitMin);
    }
    /**
     * @return Bar color corresponding to current telemetry value. Red for lowest, yellow for middle, green for highest.
     */
    private int getBarColor(){
        //the lower, the more red
        int red = (int)(512 * (1.0f - getRelativeBarSize()));
        if(red > 255) red = 255;
        if(red < 0) red = 0;
        //the higher, the more green
        int green = (int)(512 * getRelativeBarSize());
        if(green > 255) green = 255;
        if(green < 0) green = 0;
        //make sure the color is coded in 2 bytes even for low values
        String redStr = Integer.toHexString(red).toUpperCase();
        if(redStr.length() < 2){
            redStr = "0"+redStr;
        }
        String greenStr = Integer.toHexString(green).toUpperCase();
        if(greenStr.length() < 2){
            greenStr = "0"+greenStr;
        }
        //Integer.parseInt won't parse 8 byte strings with 1st byte >7
        return (int)Long.parseLong("AA" + redStr + greenStr + "00", 16);
    }
    /**
     * Initializes each text field.
     * @see Instrument#defineTextField(String, float, float)
     */
    private void initFields(){
        defineTextField("name", 0.5f, 0.97f);
        defineTextField("min", 0.1f, 0.85f);
        defineTextField("max", 0.1f, 0.05f);
    }
    /**
     * Class constructor.
     * Fills text fields and initializes graphical elements
     * @param id ID of telemetry field to take data from
     * @see Instrument#Instrument(Context, int)
     */
    public BarInstrument(Context ctx, long id){
        super(ctx, INSTRUMENT_BAR);
        this.fieldId = id;
        this.field = (TelemetryDataNumber)telemetry.getFieldById(fieldId);
        bar = new RectF(0, 0, 0, 0);
        textFieldValues.put("value", formatValue(field.value) + field.units);
        textFieldValues.put("name", field.name);
        textFieldValues.put("min", "" + (int)field.limitMin);
        textFieldValues.put("max", "" + (int)field.limitMax);
        initFields();
    }
    /**
     * @return Fields to be shown in the list when creating an instrument.
     */
    @Override
    public ArrayAdapter<String> getAllowedFields(){
        ArrayAdapter<String> adp = new ArrayAdapter<>(ctx, R.layout.field_name);
        TelemetryData nextField;
        for (Object o : TelemetryData.fields.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            nextField = (TelemetryData) pair.getValue();
            //numeric fields and composite fields that may contain numeric subfields
            if (nextField instanceof TelemetryDataComposite || (nextField instanceof TelemetryDataNumber && (nextField.parent == null || nextField.parent.showingChildren))) {
                adp.add(nextField.name);
            }
        }
        adp.sort(NumberAwareAlphabeticSort.stringComparator);
        return adp;
    }
    /**
     * @return True if the field can be used to create the instrument.
     * @param name Name of field to be checked
     */
    public boolean fieldUsable(String name){
        TelemetryData field = telemetry.getFieldByName(name);
        return field instanceof TelemetryDataNumber;
    }
}
/**
 * Sample instrument: GPS coordinates.
 * Crudely shows GPS coordinates on world map.
 */
class GPSCoordinatesInstrument extends Instrument{
    /**
     * Field to take data from
     */
    TelemetryData field;
    /**
     * Draws needle in addition to background and text fields.
     * @see Instrument#onDraw(Canvas)
     */
    protected void onDraw(@NonNull Canvas canvas){
        //if not placeholder
        if(fieldId != 0) {
            textFieldValues.put("valueLong", getLong() + " long");
            textFieldValues.put("valueLat", getLat() + " lat");
            defineTextField("valueLong", 0.5f, 0.9f);
            defineTextField("valueLat", 0.5f, 0.8f);
            super.onDraw(canvas);
            //draw point
            canvas.drawCircle(getXByLong(), getYByLat(), 5, paint);
        }
    }

    /**
     * @return Latitude stored in this GPS composite field
     */
    private float getLat(){
        TelemetryDataSimple subfield;
        //dependent on the protocol
        if(field instanceof TelemetryDataGPS_DV4){
            subfield = (TelemetryDataNumber)(telemetry.getFieldById(((TelemetryDataGPS_DV4)field).children.get(0)));
        }else{
            subfield = (TelemetryDataNumber)(telemetry.getFieldById(((TelemetryDataGPS_SkyNav) field).children.get(0)));
        }
        try {
            return (float) subfield.value;
        }catch(Exception e){
            //value is empty and is considered an integer
            return 0.0f;
        }
    }
    /**
     * @return Latitude stored in this GPS composite field
     * @see #getLat()
     */
    private float getLong(){
        TelemetryDataSimple subfield;
        if(field instanceof TelemetryDataGPS_DV4){
            subfield = (TelemetryDataNumber)(telemetry.getFieldById(((TelemetryDataGPS_DV4) field).children.get(1)));
        }else{
            subfield = (TelemetryDataNumber)(telemetry.getFieldById(((TelemetryDataGPS_SkyNav)field).children.get(1)));
        }
        try {
            return (float) subfield.value;
        }catch(Exception e){
            return 0.0f;
        }
    }
    /**
     * @return X Position for marker based on longitude
     */
    private float getXByLong(){
        return (getLong() + 180.0f) / 360.0f * width * 0.85f;
    }
    /**
     * @return Y Position for marker based on latitude
     */
    private float getYByLat(){
        return (getLat() - 90.0f) / -180.0f * height * 0.55f + height * 0.2f;
    }
    /**
     * @return Fields to be shown in the list when creating an instrument.
     */
    @Override
    public ArrayAdapter<String> getAllowedFields(){
        ArrayAdapter<String> adp = new ArrayAdapter<>(ctx, R.layout.field_name);
        TelemetryData nextField;
        for (Object o : TelemetryData.fields.entrySet()) {
            Map.Entry pair = (Map.Entry) o;
            nextField = (TelemetryData) pair.getValue();
            //GPS fields only
            if (nextField instanceof TelemetryDataGPS_DV4 || nextField instanceof TelemetryDataGPS_SkyNav) {
                adp.add(nextField.name);
            }
        }
        adp.sort(NumberAwareAlphabeticSort.stringComparator);
        return adp;
    }
    /**
     * @return True if the field can be used to create the instrument.
     * @param name Name of field to be checked
     */
    public boolean fieldUsable(String name){
        TelemetryData field = telemetry.getFieldByName(name);
        return field instanceof TelemetryDataGPS_DV4 || field instanceof TelemetryDataGPS_SkyNav;
    }
    /**
     * Initializes each text field.
     * @see Instrument#defineTextField(String, float, float)
     */
    private void initFields(){
        defineTextField("name", 0.5f, 0.10f);
        defineTextField("valueLong", 0.5f, 0.9f);
        defineTextField("valueLat", 0.5f, 0.8f);
    }
    /**
     * Class constructor.
     * Fills text fields and initializes graphical elements
     * @param id ID of telemetry field to take data from
     * @see Instrument#Instrument(Context, int)
     */
    public GPSCoordinatesInstrument(Context ctx, long id){
        super(ctx, INSTRUMENT_GPS_COORDS);
        setImageResource(R.drawable.world);
        this.fieldId = id;
        //if not placeholder
        if(fieldId != 0) {
            this.field = telemetry.getFieldById(fieldId);
            textFieldValues.put("valueLong", getLong() + " long");
            textFieldValues.put("valueLat", getLat() + " lat");
            textFieldValues.put("name", field.name);
            initFields();
        }
    }

}
