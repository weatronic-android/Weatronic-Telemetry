package com.weatronic.bluetoothtelemetry;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Rect;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v7.app.AppCompatActivity;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.Window;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.Toast;

import java.util.Iterator;
import java.util.Map;
import java.util.Vector;

/**
 * An activity for application workspace.
 * Handles reaction to user actions: options menu clicks and popup dialogs.
 * Handles instrument grid: allows adding/removing instruments, builds a layout.
 */
public class MainActivity extends AppCompatActivity {
    /**
     * Application preferences.
     * Stores data shared between activities and application sessions.
     */
    private SharedPreferences prefs;
    /**
     * Access to application preferences.
     */
    private SharedPreferences.Editor prefsEditor;
    /**
     * Instance of bluetooth service to access its functions.
     */
    private BluetoothService bluetooth;
    /**
     * Instance of parser to access its fields and functions.
     */
    private Parser parser;
    /**
     * Instance of telemetry data container to access telemetry data.
     */
    private TelemetryDataContainer telemetry;
    /**
     * @see DisplayMetrics
     */
    private DisplayMetrics displaymetrics = new DisplayMetrics();
    private int screenWidth;
    private int screenHeight;
    /**
     * Stores all instruments.
     * @see Instrument
     */
    private Vector<Instrument> instrumentList = new Vector<>();
    /**
     * Stores references to table row so that they can be emptied.
     * Not emptying rows will result in "view already has parent" error for instruments.
     */
    private Vector<TableRow> tableRows = new Vector<>();
    /**
     * List of fields selected for new config.
     * Global to avoid emptying on dialog initialization.
     * @see fieldListDialog
     */
    private ArrayAdapter<String> selectedFieldList;
    /**
     * Number of instrments per row in layour grid.
     */
    private int rowSize;
    /**
     * Number of rows in layout grid.
     */
    private int rowCount;
    /**
     * Stores layout mode.
     */
    private boolean fullScreen = false;
    /**
     * "New instrument" button.
     * Handled as an instrument for layout purposes, but is always present.
     */
    private ImageView newInstrument;
    /**
     * Temporary storage for newly created instruments.
     */
    private Instrument instrumentInWork;
    /**
     * State of layout restoration process.
     * By default, the {@link #addInstrument(Instrument)} function stores the layout. While layout is being restored, storing it will break the restoration process.
     * True on launch and false after restoration is finished.
     */
    private boolean restoring = true;
    /**
     * Used to fit an instrument into a screen.
     * @return largest possible instrument size that still fits into screen
     */
    private int getSmallerDimension(){
        if(screenWidth < screenHeight){
            return screenWidth;
        }else{
            return screenHeight;
        }
    }
    /**
     * Calculates best size for an instrument.
     * Best means largest possible with least possible free space left.
     * All instruments are square and equal in size.
     * @return Size
     */
    private int getInstrumentSize(){
        //start at 1x1 grid
        rowSize = 1;
        rowCount = 1;
        int spaceRight, spaceBottom;
        int sizeByHeight, sizeByWidth;
        int curSize = getSmallerDimension();
        //iteratively add instruments to grid and resize when needed
        // *   **    **    **      ***       ***       ***       ***       ***     ****     ****
        //           *     **      **        ***       ***       ***       ***     ****     ****
        //                                             *         **        ***     **       ***
        for(int i = 1; i < instrumentList.size() + 1; i++){
            //if element fits into current grid
            if(i < rowSize * rowCount){
                //size stays the same
            }else{
                //recalculate size
                spaceRight = screenWidth - curSize * rowSize;
                spaceBottom = screenHeight - curSize * rowCount;
                if(spaceBottom > spaceRight){
                    //more space at the bottom, add row
                    rowCount++;
                }else{
                    //more space at the right side, increase row size
                    rowSize++;
                }
                sizeByHeight = Math.round(screenHeight / rowCount);
                sizeByWidth = Math.round(screenWidth / rowSize);
                if(sizeByHeight > sizeByWidth){
                    curSize = sizeByWidth;
                }else{
                    curSize = sizeByHeight;
                }
                //if space has appeared
                spaceRight = screenWidth - curSize * rowSize;
                if(spaceRight > curSize) rowSize++;
            }
        }
        return curSize;
    }
    /**
     * Sets all instrument to invisible.
     * Used to switch to full screen without deleting any instruments.
     */
    private void hideAllInstruments(){
        for(int i = 0; i < instrumentList.size(); i++){
            instrumentList.get(i).setVisibility(View.GONE);
        }
        newInstrument.setVisibility(View.GONE);
    }
    /**
     * Sets all instruments to visible.
     * Used when switching off full screen mode.
     */
    private void showAllInstruments(){
        for(int i = 0; i < instrumentList.size(); i++){
            instrumentList.get(i).setVisibility(View.VISIBLE);
        }
        newInstrument.setVisibility(View.VISIBLE);
    }
    /**
     * Builds a grid of instruments (any number).
     */
    private void makeLayout() {
        //get available screen space
        updateDimensions();
        //calculate dimensions for one instrument (same size for all)
        int instrSize = getInstrumentSize();
        int instrIndex;

        TableRow.LayoutParams rowParams = new TableRow.LayoutParams(TableRow.LayoutParams.WRAP_CONTENT, TableRow.LayoutParams.WRAP_CONTENT, (float) 1.0);
        TableRow.LayoutParams viewParams;
        //if showing one instrument full screen
        if(fullScreen){
            //fit into screen
            viewParams = new TableRow.LayoutParams(getSmallerDimension(), getSmallerDimension(), (float) 1.0);
        }else{
            //fit into grid
            viewParams = new TableRow.LayoutParams(instrSize, instrSize, (float) 1.0);
        }
        viewParams.setMargins(0, 0, 0, 0);
        TableLayout layout = (TableLayout)findViewById(R.id.instrumentTable);
        //destroy old layout
        for(int i = 0; i < tableRows.size(); i++){
            tableRows.get(i).removeAllViews();
        }
        tableRows.clear();
        //iterate through rows
        for(int row = 0; row < rowCount; row++){
            TableRow tr = new TableRow(getApplicationContext());
            tr.setLayoutParams(rowParams);
            tableRows.add(tr);
            for(int col = 0; col < rowSize; col++){
                instrIndex = row * rowSize + col;
                //if already added all instruments
                if(instrIndex >= instrumentList.size()){
                    //add + button
                    try {
                        newInstrument.setLayoutParams(viewParams);
                        tr.addView(newInstrument);
                    }catch(Exception e){
                        //already added
                    }
                    break;
                }
                instrumentList.get(instrIndex).setLayoutParams(viewParams);
                tr.addView(instrumentList.get(instrIndex));
            }
            //if last row
            if(row + 1 == rowCount && !fullScreen){
                //add + button
                try {
                    newInstrument.setLayoutParams(viewParams);
                    tr.addView(newInstrument);
                }catch(Exception e){
                    //already added
                }
            }
            layout.addView(tr);
        }
    }
    /**
     * Saves instrument parameters in preferences to be restored on app restart.
     * The number of instruments is stored as "instrumentCount".
     * Each instrument is stored as two values: "instrumentTypeN" (where N is instrument index) and "instrumentSourceN".
     */
    private void storeLayout(){
        int newInstrCount = instrumentList.size();
        boolean instrRemoved = newInstrCount < prefs.getInt("instrumentCount", 0);
        Instrument nextInstrument;
        //clear prefs key
        if(instrRemoved){
            prefsEditor.remove("instrumentType"+newInstrCount);
            prefsEditor.remove("instrumentSource" + newInstrCount);
        }
        prefsEditor.putInt("instrumentCount", newInstrCount);
        for(int i = 0; i < newInstrCount; i++){
            nextInstrument = instrumentList.get(i);
            prefsEditor.putInt("instrumentType" + i, nextInstrument.type);
            prefsEditor.putLong("instrumentSource" + i, nextInstrument.fieldId);
        }
        prefsEditor.commit();
    }
    /**
     * Creates and adds instruments from parameters stored in preferences.
     * Should restore the layout to what it was when the app was closed.
     * It is assumed that every instrument has a maximum of one source field. May need to be redesigned to support more complicated instruments.
     */
    private void restoreLayout(){
        int instrCount = prefs.getInt("instrumentCount", 0);
        int instrType;
        long instrSource;
        Instrument instr;
        for(int i = 0; i < instrCount; i++){
            instrType = prefs.getInt("instrumentType" + i, -1);
            instrSource = prefs.getLong("instrumentSource" + i, 0);
            instr = Instrument.createInstrument(this, instrType, instrSource);
            addInstrument(instr);
        }
        restoring = false;
        makeLayout();
    }
    /**
     * Used to execute restoration with a short delay.
     * If executed immediately in onCreate, even after setContentView, the screen dimensions are measured incorrectly.
     */
    final Runnable restoreLayout = new Runnable() {
        public void run() {
            restoreLayout();
        }
    };
    /**
     * Triggers on screen rotation.
     */
    @Override
    public void onConfigurationChanged(Configuration newConfig) {
        super.onConfigurationChanged(newConfig);
        //build layout with new dimensions (height and width switched)
        makeLayout();
    }
    /**
     * Initializes and adds an instrument to the list.
     * @param instr Instrument to add
     */
    private void addInstrument(Instrument instr){
        //add to list
        instrumentList.add(instr);
        //on click - full screen
        instr.setOnClickListener(new View.OnClickListener(){
            @Override
            public void onClick(View v){
                if(fullScreen){
                    //go back to normal
                    showAllInstruments();
                    fullScreen = false;
                    makeLayout();
                }else{
                    //go into fullscreen
                    hideAllInstruments();
                    v.setVisibility(View.VISIBLE);
                    fullScreen = true;
                    makeLayout();
                }
            }
        });
        instr.setLongClickable(true);
        //on long click - delete instrment
        instr.setOnLongClickListener(new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                Instrument instr = (Instrument) v;
                removeInstrument(instr);
                showAllInstruments();
                fullScreen = false;
                makeLayout();
                return true;
            }
        });
        //build new grid
        makeLayout();
        if(!restoring) storeLayout();
    }

    private void removeInstrument(Instrument instr){
        instr.destroy();
        instrumentList.remove(instr);
        storeLayout();
    }
    /**
     * Gets width and height of the screen.
     */
    private void updateDimensions(){
        getWindowManager().getDefaultDisplay().getMetrics(displaymetrics);
        //size with extra bars
        screenHeight = displaymetrics.heightPixels;
        screenWidth = displaymetrics.widthPixels;
        Rect rect = new Rect();
        Window win = getWindow();
        win.getDecorView().getWindowVisibleDisplayFrame(rect);
        int statusBarHeight = rect.top;
        int contentViewTop = win.findViewById(Window.ID_ANDROID_CONTENT).getTop();
        int titleBarHeight = contentViewTop - statusBarHeight;
        //substracts size of other elements to make sure instruments stay in screen bounds
        screenHeight -= statusBarHeight + titleBarHeight + screenHeight * 0.07;
    }
    /**
     * Initializes the "instrument" used to create instruments.
     */
    private void initNewInstrumentButton(){
        newInstrument = new ImageView(this);
        newInstrument.setImageResource(R.drawable.new_instrument);
        newInstrument.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                try {
                    showInstrumentList();
                } catch (Exception e) {
                    printMessage(e.getMessage());
                }
            }
        });
    }
    /**
     * Triggers when the app is stated.
     * Initializes global variables.
     */
    @Override
    protected void onCreate(Bundle savedInstanceState){
        super.onCreate(savedInstanceState);
        this.setContentView(R.layout.activity_main_grid);

        this.prefs = PreferenceManager.getDefaultSharedPreferences(this);
        this.prefsEditor = prefs.edit();
        prefsEditor.apply();

        telemetry = TelemetryDataContainer.getNewInstance(this, prefs.getInt("protocol", TelemetryData.PROTOCOL_SKYNAVIGATOR));
        bluetooth = BluetoothService.getInstance(this, incomingHandler);
        parser = Parser.getInstance(this);

        initNewInstrumentButton();

        selectedFieldList = new ArrayAdapter<>(this, R.layout.field_name);
        //a moment to let the title bar appear, otherwise screen space is calculated wrong
        incomingHandler.postDelayed(restoreLayout, 200);
    }
    /**
     * Triggers when the activity is opened again from background.
     */
    @Override
    protected void onResume() {

        super.onResume();

        //this.setContentView(R.layout.activity_main_grid);

        //send fake message
        //simulate
        //startSimulation();
        //.processMessage("$PWEAC,E905,F101,FE03,FA01,FA11,F800,F810,EC01,ED01,F301,FF03,FF13,FD03,FB01,FB11,F900,F910,DF04,E103,EB0C*39");
        //parser.processMessage("$PWEAD0,6C536A4E,48,0000,54,50," + 64 + ",62,51,00,08,A21E,,,55,5B,64,63,00000000,0000,28BFBA125CC4D5040080370287395EF1D903*7A");
    }


    //test
    private void startSimulation(){
        //build config (random 10 fields)
        long nextFieldId;
        String message = "$PWEAC";
        for(int i = 0; i < 10; i++){
            nextFieldId = TelemetryData.idList.get((int)Math.floor(Math.random() * TelemetryData.fields.size()));
            message += "," + Long.toHexString(nextFieldId).toUpperCase();
        }
        message += "*39";
        parser.processMessage(message);

        new CountDownTimer(1000000, 1000){
            String hexData;
            int fieldSize;
            TelemetryData nextField;

            public void onFinish(){

            }
            public void onTick(long timeRemaining){
                String message = "$PWEAD0";
                //build data message
                for(int i = 0; i < 10; i++){
                    nextField = TelemetryData.fields.get(parser.configFields[i]);
                    fieldSize = TelemetryData.sizeByType(nextField.type);
                    hexData = "";
                    for(int j = 0; j < fieldSize; j++){
                        hexData += Integer.toHexString((int)Math.floor(Math.random() * 16));
                    }
                    message += "," + hexData;
                }
                message += "*39";
                parser.processMessage(message);
            }
        }.start();
    }


    /**
     * Triggers when options menu is created.
     * Standard function.
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }
    /**
     * Options menu actions.
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        //clicked option
        switch(id){
            //connect - show device list
            case R.id.action_connect:
                bluetooth.initConnectionDialog();
                return true;
            //disconnect
            case R.id.action_disconnect:
                bluetooth.disconnect();
                return true;
            //make discoverable by other devices
            case R.id.action_discoverable:
                bluetooth.makeDiscoverable();
                return true;
            //pick fields - build and transmit new config
            case R.id.action_pick_fields:
                showFieldList();
                return true;
            //toggle protocol
            case R.id.action_switch_protocol:
                telemetry = switchProtocol();
                return true;
        }

        return super.onOptionsItemSelected(item);
    }
    /**
     * Reacts on Android OS events.
     * Used with "enable bluetooth" request.
     */
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        switch (requestCode) {
            //trying to open device list with bluetooth disabled
            case Constants.REQUEST_ENABLE_BT:
                if (resultCode == Activity.RESULT_OK) {
                    //user allowed to turn on bluetooth - open dialog
                    bluetooth.initConnectionDialog();
                } else {
                    printMessage(getString(R.string.Bluetooth_needed));
                }
                break;
            //trying to connect/reconnect with bluetooth disabled
            case Constants.REQUEST_ENABLE_BT_DIRECT:
                if(resultCode == Activity.RESULT_OK){
                    //user allowed to turn on bluetooth - try to connect again
                    bluetooth.connect(prefs.getString("lastAttemptedAddress", ""));
                }else{
                    //user declined - cancel attempt
                    bluetooth.dismissProgress();
                }
                break;
        }
    }
    /**
     * Changes currently used protocol.
     * Only two protocols are used, therefore the button simply toggles them without additional menus.
     */
    private TelemetryDataContainer switchProtocol(){
        int curProtocol = TelemetryData.protocol;
        int newProtocol;
        if(curProtocol == TelemetryData.PROTOCOL_DV4){
            newProtocol = TelemetryData.PROTOCOL_SKYNAVIGATOR;
            printMessage(getString(R.string.Switched_to) + " DV4");
        }else{
            newProtocol = TelemetryData.PROTOCOL_DV4;
            printMessage(getString(R.string.Switched_to) + " SkyNavigator");
        }
        prefsEditor.putInt("protocol", newProtocol);
        prefsEditor.commit();
        //remove instruments because they are now "pointing" at wrong or non-existent fields
        instrumentList.clear();
        storeLayout();
        makeLayout();
        return TelemetryDataContainer.getNewInstance(this, newProtocol);
    }
    /**
     * Shortcut funftion to output a Toast message.
     * @param message String to print
     */
    private void printMessage(String message){
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }
    /**
     * Triggers on exit.
     * Used to make sure bluetooth is stopped. Doesn't trigger on "back" from main activity.
     */
    @Override
    protected void onDestroy(){
        bluetooth.stop();
        super.onDestroy();
    }
    /**
     * Triggers on key press.
     * Used to forcefully remove App from RAM on back key from main menu.
     */
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event)
    {
        if ((keyCode == KeyEvent.KEYCODE_BACK))
        {
            System.runFinalization();
            bluetooth.stop();
            System.exit(0);
        }
        return super.onKeyDown(keyCode, event);
    }
    /**
     * A class defining the popup {@link Dialog} that appears when choosing the fields to be transmitted.
     * The dialog consists of two lists - selected field and all other known fields.
     * Clicking on a field moves it to the other list. Long click expands/contracts composite fields.
     */
    class fieldListDialog extends Dialog {
        /**
         * List of instruments to show in the menu.
         * {@link #selectedFieldList} is global for persistency
         */
        private ArrayAdapter<String> knownFieldList;
        /**
         * Interface element containing values from {@link #knownFieldList}
         */
        private ListView knownListView;
        /**
         * Interface element containing values from {@link #selectedFieldList}
         */
        private ListView selectedListView;
        /**
         * Button to fill the Selected list with fields according to last received config.
         * Can be useful to quickly correct existing config.
         * @see #useLastConfig()
         */
        private Button getLastConfigButton;
        /**
         * Button to send the config line to the transmitting device.
         * The transmitting device will start transmitting according to this new config.
         */
        private Button sendConfigButton;
        /**
         * Gets the {@link Parser#configFields current config} and fills dialog lists accordingly.
         */
        private void useLastConfig(){
            if(parser.configFields == null){
                printMessage(getString(R.string.No_messages));
                return;
            }
            TelemetryData nextField;
            refreshFieldList();
            for(int i = 1; i < parser.configFields.length; i++){
                nextField = (telemetry.getFieldById(parser.configFields[i]));
                if(nextField.parent != null && !nextField.parent.showingChildren){
                    nextField.parent.showingChildren = true;
                }
                try {
                    selectedFieldList.add(nextField.name);
                    knownFieldList.remove(nextField.name);
                }catch(Exception e){
                    //field is null
                }
            }
        }
        /**
         * Gets the contents of {@link #selectedFieldList} and builds a String to transmit via Bluetooth.
         * @return Config string
         */
        private String makeConfigLine(ArrayAdapter<String> list){
            //message tag
            String config = "$PWEAXX";
            //add comma and hex ID of each field -> $PWEAXX,FE03,3302,3403,3502
            for(int i = 0; i < list.getCount(); i++){
                config +=  "," + Long.toHexString(telemetry.getFieldByName(list.getItem(i)).ID).toUpperCase();
            }
            config += "\r\n";
            return config;
        }
        /**
         * Gets all field name from {@link TelemetryData#fields} and stores them in {@link #knownFieldList}
         */
        private void initFieldList(){
            knownFieldList = new ArrayAdapter<>(getContext(), R.layout.field_name);

            Iterator it = TelemetryData.fields.entrySet().iterator();
            TelemetryData nextField;
            while (it.hasNext()) {
                Map.Entry pair = (Map.Entry)it.next();
                nextField = (TelemetryData)pair.getValue();
                if(nextField.parent == null || nextField instanceof TelemetryDataComposite)
                    knownFieldList.add(nextField.name);
            }

            refreshFieldList();
        }
        /**
         * Builds the two field lists.
         * Checks if any composite field has been expanded/contracted and shows/hides the child fields.
         * Also handles cases such as when the children are spread among both lists and then contracted.
         */
        private void refreshFieldList(){
            TelemetryData nextField;
            long nextFieldID;
            String toAdd;
            //check each field
            for(int i = 0; i < knownFieldList.getCount(); i++){
                //list contains name, fetch field by name
                nextField = telemetry.getFieldByName(knownFieldList.getItem(i));
                //if the field is composite
                if(nextField instanceof TelemetryDataComposite){
                    //check if it should show children
                    if(((TelemetryDataComposite) nextField).showingChildren){
                        //if yes
                        for(int child = 0; child < ((TelemetryDataComposite) nextField).children.size(); child++){
                            //get child field
                            toAdd = telemetry.getFieldById(((TelemetryDataComposite) nextField).children.get(child)).name;
                            //if not already in ANY of the two lists
                            if(knownFieldList.getPosition(toAdd) == -1 && selectedFieldList.getPosition(toAdd) == -1)
                                //add to this list
                                knownFieldList.add(toAdd);
                        }
                    }else{
                        //remove each child field from BOTH fields
                        for(int child = 0; child < ((TelemetryDataComposite) nextField).children.size(); child++){
                            knownFieldList.remove(telemetry.getFieldById(((TelemetryDataComposite) nextField).children.get(child)).name);
                            selectedFieldList.remove(telemetry.getFieldById(((TelemetryDataComposite) nextField).children.get(child)).name);
                        }
                    }
                }
            }
            //same as above but for the second list
            for(int i = 0; i < selectedFieldList.getCount(); i++){
                nextField = telemetry.getFieldByName(selectedFieldList.getItem(i));
                if(nextField instanceof TelemetryDataComposite){
                    if(((TelemetryDataComposite) nextField).showingChildren){
                        for(int child = 0; child < ((TelemetryDataComposite) nextField).children.size(); child++){
                            toAdd = telemetry.getFieldById(((TelemetryDataComposite) nextField).children.get(child)).name;
                            if(knownFieldList.getPosition(toAdd) == -1 && selectedFieldList.getPosition(toAdd) == -1)
                                selectedFieldList.add(toAdd);
                        }
                    }else{
                        for(int child = 0; child < ((TelemetryDataComposite) nextField).children.size(); child++){
                            selectedFieldList.remove(telemetry.getFieldById(((TelemetryDataComposite) nextField).children.get(child)).name);
                            knownFieldList.remove(telemetry.getFieldById(((TelemetryDataComposite) nextField).children.get(child)).name);
                        }
                    }
                }
            }
            //Sorts both lists in ascending order
            knownFieldList.sort(NumberAwareAlphabeticSort.stringComparator);
            selectedFieldList.sort(NumberAwareAlphabeticSort.stringComparator);
        }
        /**
         * Class constructor.
         * Defines interface elements and initializes variables.
         */
        protected fieldListDialog(Context context) {
            super(context);

            initFieldList();

            final fieldListDialog dialog_instance = this;
            setContentView(R.layout.field_list_dialog);

            knownListView = (ListView)findViewById(R.id.knownFieldsListView);
            knownListView.setAdapter(knownFieldList);

            selectedListView = (ListView)findViewById(R.id.selectedFieldsListView);
            selectedListView.setAdapter(selectedFieldList);

            getLastConfigButton = (Button)findViewById(R.id.getLastConfigButton);
            sendConfigButton = (Button)findViewById(R.id.sendConfigButton);
            //click in known list - move to selected list
            knownListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View clicked_view, int pos, long row) {
                    if (adapter.getCount() > 0) {
                        String clicked = (String) knownListView.getItemAtPosition(pos);
                        selectedFieldList.add(clicked);
                        knownFieldList.remove(clicked);
                        refreshFieldList();
                    }
                    //dialog_instance.dismiss();
                }
            });
            ////click in selected list - move to known list (deselect)
            selectedListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View clicked_view, int pos, long row) {
                    if (adapter.getCount() > 0) {
                        String clicked = (String) selectedListView.getItemAtPosition(pos);
                        knownFieldList.add(clicked);
                        selectedFieldList.remove(clicked);
                        refreshFieldList();
                    }
                    //dialog_instance.dismiss();
                }
            });
            knownListView.setLongClickable(true);
            selectedListView.setLongClickable(true);
            //long click in known list - expand/contract
            knownListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapter, View clicked_view, int pos, long row) {
                    String clicked = (String) knownListView.getItemAtPosition(pos);
                    TelemetryData field = telemetry.getFieldByName(clicked);
                    TelemetryDataComposite container;
                    if (field instanceof TelemetryDataComposite) {
                        container = (TelemetryDataComposite) field;
                    } else {
                        if (field.parent != null) {
                            container = field.parent;
                        } else {
                            return true;
                        }
                    }

                    container.showingChildren = !container.showingChildren;

                    refreshFieldList();

                    return true;
                }
            });
            //long click in known list - expand/contract
            selectedListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener(){
                @Override
                public boolean onItemLongClick(AdapterView<?> adapter, View clicked_view, int pos, long row){
                    String clicked = (String)selectedListView.getItemAtPosition(pos);
                    TelemetryData field = telemetry.getFieldByName(clicked);
                    TelemetryDataComposite container;
                    if (field instanceof TelemetryDataComposite) {
                        container = (TelemetryDataComposite)field;
                    } else {
                        if (field.parent != null) {
                            container = field.parent;
                        }else{
                            return true;
                        }
                    }

                    container.showingChildren = !container.showingChildren;

                    refreshFieldList();

                    return true;
                }
            });

            getLastConfigButton.setOnClickListener(new Button.OnClickListener() {
                @Override
                public void onClick(View v) {
                    useLastConfig();
                    //dialog_instance.dismiss();
                }
            });

            sendConfigButton.setOnClickListener(new Button.OnClickListener(){
                @Override
                public void onClick(View v){
                    try{
                        bluetooth.send(makeConfigLine(selectedFieldList));
                    }catch(Exception e){
                        //bluetooth is null
                    }
                    dialog_instance.dismiss();
                }
            });
        }
    }
    /**
     * A function to construct and show the {@link fieldListDialog}
     */
    private void showFieldList() {
        final Dialog showDevicesDialog = new fieldListDialog(this);
        showDevicesDialog.show();
    }
    /**
     * A class defining the popup {@link Dialog} that appears when choosing the type of instrument to be created.
     * The dialog consists of a list of instrument types. Clicking on an instrument creates it and calls {@link allowedFieldListDialog} if the instrument requires a source.
     */
    class instrumentListDialog extends Dialog {
        /**
         * List of instruments to show in the menu.
         */
        private ArrayAdapter<String> knownInstrumentList;
        /**
         * Interface element containing values from {@link #knownInstrumentList}
         */
        private ListView instrumentListView;
        /**
         * List of instruments that can be created.
         */
        private void initInstrumentList(){
            knownInstrumentList = new ArrayAdapter<>(getContext(), R.layout.field_name);
            //get from static list in Instrument class
            for(int i = 0; i < Instrument.typeNames.length; i++){
                knownInstrumentList.add(Instrument.typeNames[i]);
            }
        }
        /**
         * Class constructor.
         * Defines interface elements and initializes variables.
         */
        protected instrumentListDialog(Context context) {
            super(context);
            final instrumentListDialog dialog_instance = this;
            setContentView(R.layout.basic_list_dialog);

            initInstrumentList();

            instrumentListView = (ListView)findViewById(R.id.basicListView);
            instrumentListView.setAdapter(knownInstrumentList);
            //click - create instrument
            instrumentListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View clicked_view, int pos, long row) {
                    //construct (and do additional work for) each instrument type
                    //create a dummy instrument so that a proper list of allowed fields can be fetched
                    instrumentInWork = Instrument.createInstrument(getContext(), pos, 0);
                    addInstrument(instrumentInWork);
                    //do extra
                    showAllowedFieldList();
                    dialog_instance.dismiss();
                }
            });
            setTitle(getString(R.string.Available_instruments));
        }
    }
    /**
     * A function to construct and show the {@link instrumentListDialog}
     */
    private void showInstrumentList() {
        final Dialog showInstrumentsDialog = new instrumentListDialog(this);
        showInstrumentsDialog.show();
    }
    /**
     * A class defining the popup {@link Dialog} that appears when choosing the telemetry field for an instrument.
     * The dialog consists of a list of telemetry fields. Clicking on a field picks it as source for data. Long click expands/contracts composite fields.
     */
    class allowedFieldListDialog extends Dialog {
        /**
         * List of fields to show in the menu.
         */
        private ArrayAdapter<String> allowedFieldList;
        /**
         * A temporary list used to dynamically change {@link #allowedFieldList}
         */
        private ArrayAdapter<String> updatedFieldList;
        /**
         * Interface element containing values from {@link #allowedFieldList}
         */
        private ListView allowedFieldListView;
        /**
         * Class constructor.
         * Defines interface elements and initializes variables.
         */
        protected allowedFieldListDialog(Context context) {
            super(context);
            allowedFieldList = instrumentInWork.getAllowedFields();
            //do not show dialog if no selection needed
            if(allowedFieldList == null) return;
            final allowedFieldListDialog dialog_instance = this;
            setContentView(R.layout.basic_list_dialog);
            allowedFieldListView = (ListView)findViewById(R.id.basicListView);
            allowedFieldListView.setAdapter(allowedFieldList);
            //click - choose as source
            allowedFieldListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
                @Override
                public void onItemClick(AdapterView<?> adapter, View clicked_view, int pos, long row) {
                    String clicked = (String) allowedFieldListView.getItemAtPosition(pos);
                    //check if field can be used
                    if(!instrumentInWork.fieldUsable(clicked)) {
                        printMessage(getString(R.string.Invalid_instrument_field));
                        return;
                    }
                    //construct new instrument
                    Instrument finalInstrument = Instrument.createInstrument(getContext(), instrumentInWork.type, telemetry.getFieldByName(clicked).ID);
                    //delete last (placeholder)
                    removeInstrument(instrumentInWork);
                    //add new instrument
                    addInstrument(finalInstrument);
                    dialog_instance.dismiss();
                }
            });
            allowedFieldListView.setLongClickable(true);
            //long click - expand/contract
            //can be used on both parents and children, e.g.
            //long click on "Functions 1-16" will show the fields (Function 1, ..., Function 16)
            //long click on any of the fields (Function 1, ..., Function 16) fields will hide the fields (Function 1, ..., Function 16)
            //long click on "Functions 1-16" will also hide the fields (Function 1, ..., Function 16) if they are showing
            allowedFieldListView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> adapter, View clicked_view, int pos, long row) {
                    String clicked = (String) allowedFieldListView.getItemAtPosition(pos);
                    TelemetryData field = telemetry.getFieldByName(clicked);
                    //the parent field
                    TelemetryDataComposite container;
                    //if the clicked field is composite, then it is the container;
                    //if the clicked field is simple and has a parent, that parent is the container
                    if (field instanceof TelemetryDataComposite) {
                        container = (TelemetryDataComposite) field;
                    } else {
                        if (field.parent != null) {
                            container = field.parent;
                        } else {
                            //field is simple and has no parent
                            return true;
                        }
                    }
                    //toggle "showing"
                    container.showingChildren = !container.showingChildren;
                    allowedFieldList.clear();
                    //the new adapter will now contain the expanded fields and vice versa if a field was contracted
                    updatedFieldList = instrumentInWork.getAllowedFields();
                    //copy new adapter to base adapter
                    for (int i = 0; i < updatedFieldList.getCount(); i++) {
                        allowedFieldList.add(updatedFieldList.getItem(i));
                    }
                    return true;
                }
            });
            setTitle(getString(R.string.Available_instruments));
            //on dismiss delete placeholder istrument
            setOnDismissListener(new Dialog.OnDismissListener(){
                @Override
                public void onDismiss(DialogInterface d){
                    removeInstrument(instrumentInWork);
                    makeLayout();
                }
            });
            this.show();
        }
    }
    /**
     * A function to construct and show the {@link allowedFieldListDialog}.
     * Shows directly from constructor in order to be able to cancel construction.
     */
    private void showAllowedFieldList() {
        new allowedFieldListDialog(this);
    }

    /**
     * A handler to process messages from {@link BluetoothService.ConnectedThread Bluetooth input stream}.
     * @see Handler
     */
    private Handler incomingHandler = new Handler(new Handler.Callback() {
        public boolean handleMessage(android.os.Message msg) {
            StringBuilder sb = new StringBuilder();
            switch (msg.what) {
                case Constants.INCOMING_MESSAGE:
                    byte[] readBuf = (byte[]) msg.obj;
                    String strIncom = new String(readBuf, 0, msg.arg1);
                    sb.append(strIncom);
                    int endOfLineIndex = sb.lastIndexOf("\r\n");
                    if (endOfLineIndex > 0) {
                        String sbPrint = sb.substring(0, endOfLineIndex);
                        sb.delete(0, sb.length());
                        parser.processMessage(sbPrint);
                    }
                    break;
                case Constants.DATA_UPDATED:
                    //
                    break;
            }
            return true;
        }
    });

}
