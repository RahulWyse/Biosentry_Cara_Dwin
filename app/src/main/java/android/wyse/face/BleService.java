package android.wyse.face;

import android.app.Notification;
import android.app.PendingIntent;
import android.app.Service;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothGatt;
import android.bluetooth.BluetoothGattCallback;
import android.bluetooth.BluetoothGattCharacteristic;
import android.bluetooth.BluetoothGattDescriptor;
import android.bluetooth.BluetoothGattService;
import android.bluetooth.BluetoothManager;
import android.bluetooth.BluetoothProfile;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Binder;
import android.os.Build;
import android.os.IBinder;
import android.util.Log;
import android.widget.Toast;

import androidx.localbroadcastmanager.content.LocalBroadcastManager;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Timer;
import java.util.TimerTask;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Created by cis on 22/04/18.
 */
public class BleService extends Service {


    private static int latchConnectedCnt=0;
    //bluethoot device objects
    private static final int REQUEST_ENABLE_BT = 99;                                     //Constant to identify response from Activity that enables Bluetooth
    private String mDeviceName;
    private static String mDeviceAddress="00:1E:C0:3E:30:41";                                         //Strings for the Bluetooth device name and MAC address

    private String incomingMessage;                                                     //String to hold the incoming message from the MLDP characteristic
    private boolean mConnected = false;                                                 //Indicator of an active Bluetooth connection
    private boolean writeComplete = false;                                              //Indicator that the characteristic write has completed (for reference - not used)

    private final static UUID UUID_MLDP_PRIVATE_SERVICE = UUID.fromString("00035b03-58e6-07dd-021a-08123a000300"); //Private service for Microchip MLDP
    private final static UUID UUID_MLDP_DATA_PRIVATE_CHAR = UUID.fromString("00035b03-58e6-07dd-021a-08123a000301"); //Characteristic for MLDP Data, properties - notify, write
    private final static UUID UUID_MLDP_CONTROL_PRIVATE_CHAR = UUID.fromString("00035b03-58e6-07dd-021a-08123a0003ff"); //Characteristic for MLDP Control, properties - read, write

    private final static UUID UUID_TANSPARENT_PRIVATE_SERVICE = UUID.fromString("49535343-fe7d-4ae5-8fa9-9fafd205e455"); //Private service for Microchip Transparent
    private final static UUID UUID_TRANSPARENT_TX_PRIVATE_CHAR = UUID.fromString("49535343-1e4d-4bd9-ba61-23c647249616"); //Characteristic for Transparent Data from BM module, properties - notify, write, write no response
    private final static UUID UUID_TRANSPARENT_RX_PRIVATE_CHAR = UUID.fromString("49535343-8841-43f4-a8d4-ecbe34729bb3"); //Characteristic for Transparent Data to BM module, properties - write, write no response

    private final static UUID UUID_CHAR_NOTIFICATION_DESCRIPTOR = UUID.fromString("00002902-0000-1000-8000-00805f9b34fb"); //Special descriptor needed to enable notifications
    private UUID[] uuidScanList = {UUID_MLDP_PRIVATE_SERVICE, UUID_TANSPARENT_PRIVATE_SERVICE};
    private final Queue<BluetoothGattDescriptor> descriptorWriteQueue = new LinkedList<>();
    private final Queue<BluetoothGattCharacteristic> characteristicWriteQueue = new LinkedList<>();

    private final String OPEN_PATTERN_REGEX="open";
    private final Pattern OPEN_PATTERN_MATCHER= Pattern.compile(OPEN_PATTERN_REGEX, Pattern.CASE_INSENSITIVE);

    private BluetoothAdapter bluetoothAdapter;
    private static BluetoothDevice bluetoothDevice;
    private static BluetoothGatt bluetoothGatt;
    private static BluetoothGattCharacteristic mldpDataCharacteristic;
    private static BluetoothGattCharacteristic transparentTxDataCharacteristic;
    private static BluetoothGattCharacteristic transparentRxDataCharacteristic;

    private int connectionAttemptCountdown = 0;
    private static String RXdata="";
    private boolean isCommand=false;

    private int BLUETOOTH_STATE;
  //  BluetoothDevice BTdevice;

    private final String ACTION_BLE_CONNECT="ACTION_BLE_CONNECT";
    private final String ACTION_BLE_DISCONNECT="ACTION_BLE_DISCONNECT";

    private final String TAG="bleservice";
    private boolean isFHI_RECEIVED=false;
    private  long LAST_HIGH=0;

    private final HashMap<String,String> serviceQ=new HashMap<>();

    private static boolean isPendingFlag=false;

    private final BroadcastReceiver bleBroadcast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            String action=intent.getAction();

            if (action.equals("TOGGLE_BLE")){
                if (Build.VERSION.SDK_INT<=22) {
                    toggleBluetooth();
                    serviceQ.put("DOOR_OPEN", System.currentTimeMillis()+"");
                }
            }

            //Log.d(TAG,action);
            if (action.equals("STOP_BLE")){
                try {
                    if (bluetoothAdapter!=null){
                        bluetoothAdapter.disable();
                    }
                }catch (Exception er){
                    ////new Logger(TAG+","+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),BleService.this);
                }
                BleService.this.stopSelf();
            }

            if (action.equals("ARE_YOU_ALIVE")){
                boolean lockConnected=false;
                if (latchConnectedCnt==0){
                    lockConnected=true;
                }

            }

            if (action.equals("ACTION_OPEN_DOOR")){
                isCommand=true;
                DoorOpen();
            }

            if (action.equals("ACTION_CLOSE_DOOR")){
                isCommand=true;
                DoorClose();
            }

            if(action.equals("CLOSE_BLE")){
                isCommand=true;
                closeBle();
            }

            if (action.equals("CLOSE_GATT")){
                isCommand=true;
                closeGatt();
            }
            if (action.equals("CONNECT_BLE")){
                checkConnection();
            }
        }
    };

    @Override
    public void onCreate() {
            mConnected=false;
            isPendingFlag=false;
            latchConnectedCnt=0;
            String settings= CaraManager.getInstance().getSharedPreferences().getString(getResources().getString(R.string.settings), "");

            IntentFilter intentFilter=new IntentFilter();
            intentFilter.addAction("ACTION_OPEN_DOOR");
            intentFilter.addAction("ACTION_CLOSE_DOOR");
            intentFilter.addAction("CONNECT_BLE");
            intentFilter.addAction("CLOSE_BLE");
            intentFilter.addAction("ARE_YOU_ALIVE");
            intentFilter.addAction("STOP_BLE");
            intentFilter.addAction("TOGGLE_BLE");
            intentFilter.addAction("CLOSE_GATT");
            intentFilter.addAction(Intent.ACTION_SCREEN_OFF);

            registerReceiver(bleBroadcast,intentFilter);

            if (settings!=null){
                if (!settings.equals("")) {
                    try {
                        JSONObject jdata = new JSONObject(settings);
                        String isLatch = jdata.getString(Helper.SETTING_ACCESS_CONTROL);
                        if (isLatch.equals("true")){
                            mDeviceAddress= jdata.getString(Helper.SETTING_LATCH_MAC);
                            checkBlueToothHw();
                            startForeGroundService("",BleService.this);
                            sendBroadcast(new Intent("ACCESS_CONTROL_STARTED"));
                        }
                    } catch (Exception er) { }
                }
            }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
       // Log.d("bleservice","ble service onstartcommand");
        isPendingFlag=false;

        LAST_HIGH= System.currentTimeMillis();
      //  checkConnection();
            checkBleInterval();
        return START_NOT_STICKY;
    }

    private Timer bletimer;
    private final int SCAN_INTERVAL=20000;
    private final long HEARTBIT_INTERVAL=60*1000*2;
    private boolean IS_HEARTBIT_OKAY=false;

    private void checkBleInterval(){
        try {
            if (bletimer != null) {
                  bletimer.cancel();
                  bletimer=null;
            }
            bletimer = new Timer();
            bletimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if ((System.currentTimeMillis()-LAST_HIGH)<=HEARTBIT_INTERVAL) {
                            IS_HEARTBIT_OKAY=true;
                            LAST_HIGH= System.currentTimeMillis();
                    }
                    if (bluetoothAdapter == null || bluetoothGatt == null) {  mConnected=false;  }
                    //Log.d(TAG,"Status Flags (isConnected, isFHI,IS_HEARTBIT_OKAY)"+mConnected+", "+isFHI_RECEIVED+","+IS_HEARTBIT_OKAY);

                    if (mConnected) {
                       // Log.d(TAG,"BLE connection is "+mConnected+", "+isCommand);
                        if (!isCommand) {
                            writeMLDP("HI\r\n");
                        }

                        return;
                    } else {
                        mConnected=false;
                        IS_HEARTBIT_OKAY=false;
                        checkConnection();
                    }
                }
            }, 5000, SCAN_INTERVAL);

        }catch (Exception er){
            ////new Logger(TAG+","+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),this);
        }

    }

    // ----------------------------------------------------------------------------------------------------------------
    // Disconnect an existing connection or cancel a connection that has been requested
    private void disconnect() {
        try {
            if (bluetoothAdapter == null || bluetoothGatt == null) {
                //Log.w(TAG, "BluetoothAdapter not initialized");

                return;
            }
            connectionAttemptCountdown = 0;                                                             //Stop counting connection attempts
            bluetoothGatt.disconnect();
        } catch (Exception e) {

         //   Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    private void toggleBluetooth(){
        try {
            BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE);
            bluetoothAdapter = bluetoothManager.getAdapter();
            if (bluetoothAdapter != null) {
                if (bluetoothAdapter.disable()) {
                    if(bluetoothAdapter.enable()){
                        checkConnection();
                    }
                }
            }
        }catch (Exception er){}
    }


    private boolean checkBlueToothHw(){
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
        bluetoothAdapter = bluetoothManager.getAdapter();
        if (!bluetoothAdapter.isEnabled()) {                                           //Check if BT is not enabled
            return true;
        }else{
           // Log.d(TAG,"check connection called  from oncreate");
            checkConnection();
        }

        if (bluetoothAdapter == null) {                                                //Check if we got the BluetoothAdapter
            Toast.makeText(this, "Bluetooth not supported", Toast.LENGTH_SHORT).show(); //Message that Bluetooth not supported
            this.sendBroadcast(new Intent(ACTION_BLE_DISCONNECT));
            mConnected=false;
            // finish();                                                                   //End the app
        }
        return false;
    }

    private void checkConnection(){
        try {
               // Log.d(TAG,"bluetooth flag is "+mConnected);
                if (!mConnected) {
                    connect(mDeviceAddress.toUpperCase());
                }
        }catch (Exception er){
            // er.printStackTrace();
            try {
                this.sendBroadcast(new Intent(ACTION_BLE_DISCONNECT));
            }catch (Exception eer){}if (latchConnectedCnt==0) {
                ////new Logger(TAG+","+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),this);
            }
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Connect to a Bluetooth LE device with a specific address
    private boolean connect(final String address) {

        try {
            if (bluetoothAdapter == null || address == null) {
                //Log.w(TAG, "BluetoothAdapter not initialized or unspecified address");
                mConnected=false;
                return false;
            }
            bluetoothDevice = bluetoothAdapter.getRemoteDevice(address);
            if (bluetoothDevice == null) {
                mConnected=false;
              // Log.w(TAG, "Unable to connect because device was not found");
                return false;
            }
            if (bluetoothGatt != null) {                                                                //See if an existing connection needs to be closed
                bluetoothGatt.close();                                                                  //Faster to create new connection than reconnect with existing BluetoothGatt
            }

            connectionAttemptCountdown = 3;                                                             //Try to connect three times for reliability

            bluetoothGatt = bluetoothDevice.connectGatt(this, false, mGattCallback);                           //Directly connect to the device , so set autoConnect to false

            Log.d(TAG, "Attempting to create a new Bluetooth connection");
            return true;

        } catch (Exception e) {
           // Log.e(TAG, "Oops, exception caught in " + e.getMessage());

            return false;
        }
    }

    private void closeGatt(){
        try{
            mConnected=false;
            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }
        }catch (Exception er){

        }
    }


    private void closeBle(){

        try {
            mConnected=false;
            if (bletimer!=null){
                bletimer.cancel();
            }

            if (bluetoothGatt != null) {
                bluetoothGatt.disconnect();
                bluetoothGatt.close();
            }

        }catch (Exception er){
            ////new Logger(TAG+","+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),this);
        }
    }


    private final BluetoothGattCallback mGattCallback = new BluetoothGattCallback() {
        @Override
        public void onConnectionStateChange(BluetoothGatt gatt, int status, int newState) { //Change in connection state

            try {
                if (newState == BluetoothProfile.STATE_CONNECTED) {                         //See if we are connected

                   //Log.d(TAG, "Connected to GATT server.");
                    mConnected = true;                                                      //Record the new connection state
                    BleService.this.sendBroadcast(new Intent(ACTION_BLE_CONNECT));                          //SEND BROADCAST
                    //updateConnectionState(R.string.connected);                              //Update the display to say "Connected"
                    //invalidateOptionsMenu();                                                //Force the Options menu to be regenerated to show the disconnect option
                    descriptorWriteQueue.clear();                                                   //Clear write queues in case there was something left in the queue from the previous connection
                    characteristicWriteQueue.clear();
                    bluetoothGatt.discoverServices();                                               //Discover services after successful connection
                   // checkDataMode();
                    checkServiceQuee();
                    latchConnectedCnt=0;

                    sendBroadcast(new Intent("ACTION_LATCH_CONNECTED"));
                    ////new Logger(TAG+", STATE_CONNECTED",BleService.this);

                } else if (newState == BluetoothProfile.STATE_DISCONNECTED) {                 //See if we are not connected
                   // Log.d(TAG, "Disconnected from GATT server.");
                    mConnected = false;                                                     //Record the new connection state

                    BleService.this.sendBroadcast(new Intent(ACTION_BLE_DISCONNECT));
                    checkConnection();
                    //BleService.this.stopSelf();
                    if (latchConnectedCnt==0) {
                        sendBroadcast(new Intent("ACTION_LATCH_DISCONNECTED"));

                    }
                    latchConnectedCnt++;
                }
            }catch (Exception er){

            }
        }

        //Service discovery completed
        @Override
        public void onServicesDiscovered(BluetoothGatt gatt, int status) {
            try {
                mldpDataCharacteristic = transparentTxDataCharacteristic = transparentRxDataCharacteristic = null;
                if (status == BluetoothGatt.GATT_SUCCESS) {
                    List<BluetoothGattService> gattServices = gatt.getServices();                       //Get the list of services discovered
                    if (gattServices == null) {
                     //   Log.d(TAG, "No BLE services found");
                        return;
                    }

                    UUID uuid;
                    for (BluetoothGattService gattService : gattServices) {                             //Loops through available GATT services
                        uuid = gattService.getUuid();                                                   //Get the UUID of the service
                        if (uuid.equals(UUID_MLDP_PRIVATE_SERVICE) || uuid.equals(UUID_TANSPARENT_PRIVATE_SERVICE)) { //See if it is the MLDP or Transparent private service UUID
                            List<BluetoothGattCharacteristic> gattCharacteristics = gattService.getCharacteristics();

                            for (BluetoothGattCharacteristic gattCharacteristic : gattCharacteristics) { //Loops through available characteristics
                                uuid = gattCharacteristic.getUuid();                                    //Get the UUID of the characteristic

                                if (uuid.equals(UUID_TRANSPARENT_TX_PRIVATE_CHAR)) {                    //See if it is the Transparent Tx data private characteristic UUID
                                    transparentTxDataCharacteristic = gattCharacteristic;
                                    final int characteristicProperties = gattCharacteristic.getProperties(); //Get the properties of the characteristic
                                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
                                        bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true); //If so then enable notification in the BluetoothGatt
                                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID_CHAR_NOTIFICATION_DESCRIPTOR); //Get the descriptor that enables notification on the server
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
                                        descriptorWriteQueue.add(descriptor);                           //put the descriptor into the write queue
                                        if(descriptorWriteQueue.size() == 1) {                          //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
                                            bluetoothGatt.writeDescriptor(descriptor);                 //Write the descriptor
                                        }
                                    }
                                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                                    }
                                    //Log.d(TAG, "Found Transparent service Tx characteristics");
                                }

                                if (uuid.equals(UUID_TRANSPARENT_RX_PRIVATE_CHAR)) {                    //See if it is the Transparent Rx data private characteristic UUID
                                    transparentRxDataCharacteristic = gattCharacteristic;
                                    final int characteristicProperties = gattCharacteristic.getProperties(); //Get the properties of the characteristic
                                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                                    }
                                  //  Log.d(TAG, "Found Transparent service Rx characteristics");
                                }

                                if (uuid.equals(UUID_MLDP_DATA_PRIVATE_CHAR)) {                         //See if it is the MLDP data private characteristic UUID
                                    mldpDataCharacteristic = gattCharacteristic;
                                    final int characteristicProperties = gattCharacteristic.getProperties(); //Get the properties of the characteristic

                                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_NOTIFY)) > 0) { //See if the characteristic has the Notify property
                                        bluetoothGatt.setCharacteristicNotification(gattCharacteristic, true); //If so then enable notification in the BluetoothGatt
                                        BluetoothGattDescriptor descriptor = gattCharacteristic.getDescriptor(UUID_CHAR_NOTIFICATION_DESCRIPTOR); //Get the descriptor that enables notification on the server
                                        descriptor.setValue(BluetoothGattDescriptor.ENABLE_NOTIFICATION_VALUE); //Set the value of the descriptor to enable notification
                                        descriptorWriteQueue.add(descriptor);                           //put the descriptor into the write queue
                                        if(descriptorWriteQueue.size() == 1) {                          //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
                                            bluetoothGatt.writeDescriptor(descriptor);                 //Write the descriptor
                                        }
                                    }

                                    if ((characteristicProperties & (BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE)) > 0) { //See if the characteristic has the Write (unacknowledged) property
                                        gattCharacteristic.setWriteType(BluetoothGattCharacteristic.WRITE_TYPE_NO_RESPONSE); //If so then set the write type (write with no acknowledge) in the BluetoothGatt
                                    }

                                }
                            }
                            break;
                        }
                    }
                    if(mldpDataCharacteristic == null && (transparentTxDataCharacteristic == null || transparentRxDataCharacteristic == null)) {
                       //  Log.d(TAG, "Did not find MLDP or Transparent service");
                    }

                }
                else {
                   // Log.w(TAG, "Failed service discovery with status: " + status);
                }
            }
            catch (Exception e) {
                //Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
                ////new Logger(TAG+","+ e.getStackTrace()[0].getMethodName() + ": " + e.getMessage(),BleService.this);
                //toggleBluetooth();
            }

        }

        //For information only. This application uses Indication to receive updated characteristic data, not Read
        @Override
        public void onCharacteristicRead(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) { //A request to Read has completed
        }

        //Write completed
        //Use write queue because BluetoothGatt can only do one write at a time
        @Override
        public void onCharacteristicWrite(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic, int status) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {                                             //See if the write was successful
                   // Log.w(TAG, "Error writing GATT characteristic with status: " + status);
                    ////new Logger(TAG+",write error "+status+" @onCharacteristicWrite",BleService.this);
                }
                characteristicWriteQueue.remove();                                                      //Pop the item that we just finishing writing
                if(characteristicWriteQueue.size() > 0) {                                               //See if there is more to write
                    bluetoothGatt.writeCharacteristic(characteristicWriteQueue.element());              //Write characteristic
                }
            } catch (Exception e) {
                //Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
                ////new Logger(TAG+","+ e.getStackTrace()[0].getMethodName() + ": " + e.getMessage(),BleService.this);
            }
        }

        @Override
        public void onReliableWriteCompleted(BluetoothGatt gatt, int status) {
            //Log.d(TAG,"onReliableWrite "+status);
        }

        @Override
        public void onReadRemoteRssi(BluetoothGatt gatt, int rssi, int status) {
           // Log.d(TAG,"on remote rssi -------->"+rssi+", "+status);
        }

        //Write descriptor completed
        //Use write queue because BluetoothGatt can only do one write at a time
        @Override
        public void onDescriptorWrite(BluetoothGatt gatt, BluetoothGattDescriptor descriptor, int status) {
            try {
                if (status != BluetoothGatt.GATT_SUCCESS) {
                    //Log.w(TAG, "Error writing GATT descriptor with status: " + status);
                }
                descriptorWriteQueue.remove();                                                          //Pop the item that we just finishing writing
                if(descriptorWriteQueue.size() > 0) {                                                   //See if there is more to write
                    bluetoothGatt.writeDescriptor(descriptorWriteQueue.element());                      //Write descriptor
                }
            }
            catch (Exception e) {
                //Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
                ////new Logger(TAG+","+ e.getStackTrace()[0].getMethodName() + ": " + e.getMessage(),BleService.this);
            }
        }

        //Received notification or indication with new value for a characteristic
        @Override
        public void onCharacteristicChanged(BluetoothGatt gatt, BluetoothGattCharacteristic characteristic) {
            try {
                if (UUID_MLDP_DATA_PRIVATE_CHAR.equals(characteristic.getUuid()) || UUID_TRANSPARENT_TX_PRIVATE_CHAR.equals(characteristic.getUuid())) {                     //See if it is the MLDP data characteristic
                    String dataValue = characteristic.getStringValue(0);                                //Get the data in string format
                    //byte[] dataValue = characteristic.getValue();                                     //Example of getting data in a byte array
                    ///RXdata= characteristic.getStringValue(1);
                    //Log.d(TAG, "New notification or indication "+dataValue);

                    RXdata+=dataValue;
                    //use this data to get ack
                    //Log.d(TAG, "New data Received " + RXdata);

                    if (OPEN_PATTERN_MATCHER.matcher(RXdata).find()) {
                        LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("ACTION_BLE_DATA_RECEIVED").putExtra("data", RXdata));                                          //Broadcast the intent
                       // Log.d(TAG,"Data received from device as "+RXdata);
                        RXdata="";
                        LAST_HIGH= System.currentTimeMillis();
                        serviceQ.put("DOOR_OPEN","open");
                    }
                    if(RXdata.equalsIgnoreCase("HI")){
                        //Log.d(TAG,"Heartbeat received as Hi");
                        RXdata="";
                        LAST_HIGH= System.currentTimeMillis();
                    }
                    String ISFHI_COMMAND = "FHI";
                    if (RXdata.equalsIgnoreCase(ISFHI_COMMAND)){
                        //Log.d(TAG,"isFHI_RECEIVED "+RXdata);
                        isFHI_RECEIVED=true;
                        RXdata="";
                        LAST_HIGH= System.currentTimeMillis();
                    }
                    if (RXdata.equalsIgnoreCase("close")){
                    //    Log.d(TAG,"close received  "+RXdata);
                        LAST_HIGH= System.currentTimeMillis();
                        serviceQ.put("DOOR_CLOSE","close");
                    }
                    if (RXdata.equals(" null") ||  RXdata==null){
                        RXdata="";
                    }
                }else{
                    //Log.d(TAG,"Different char received "+characteristic.getUuid());
                    ////new Logger(TAG+",Different char received "+ characteristic.getUuid(),BleService.this);
                }

            }
            catch (Exception e) {
                //Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
                ////new Logger(TAG+","+ e.getStackTrace()[0].getMethodName() + ": " + e.getMessage(),BleService.this);
            }
        }

    };

    public void checkDataMode(){
        writeMLDP("HELLO\r\n");
    }

    private void DoorOpen() {
        try{
           // Log.d(TAG,"broadcast received from AccessControl Activity");
            serviceQ.put("DOOR_OPEN", System.currentTimeMillis()+"");
            if (mConnected) {
                writeMLDP("DOOR_OPEN" + "\r\n");
                isCommand=false;
            }else{
                isCommand=false;
                ////new Logger(TAG+" latch is not connected @DoorOpen",BleService.this);
            }
        }catch (Exception er){
            isPendingFlag=true;
            isCommand=false;
            isCommand=false;
            try { this.sendBroadcast(new Intent("ERROR_BLE")); }catch (Exception eer){ }
            ////new Logger(TAG+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),BleService.this);
        }
    }

    private void checkServiceQuee(){
        try{
            if (serviceQ.size()>0){
                if(serviceQ.get("DOOR_OPEN")!=null){
                    if (!serviceQ.get("DOOR_OPEN").equals("open")){
                        String lastcommand=serviceQ.get("DOOR_OPEN");
                        if (!lastcommand.equals("")){
                            long lasttime= System.currentTimeMillis()- Long.valueOf(lastcommand);
                            if (lasttime<60*1000){
                                // Log.d(TAG,"door open from quee");
                                DoorOpen();
                            }
                        }
                    }
                }
            }
        }catch (Exception er){
            ////new Logger(TAG+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),BleService.this);
        }
    }

    private void DoorClose() {
        try{
            serviceQ.put("DOOR_OPEN", System.currentTimeMillis()+"");
            writeMLDP("DOOR_CLOSE" + "\r\n");
        }catch (Exception er){
            this.sendBroadcast(new Intent("ERROR_BLE"));
        }
    }

    private void writeCharacteristic(BluetoothGattCharacteristic characteristic) {
        try {

            if (mldpDataCharacteristic == null || mldpDataCharacteristic == null) {                      //Check that we have access to a Bluetooth radio
                return;
            }

            int test = characteristic.getProperties();                                      //Get the properties of the characteristic
            if ((test & BluetoothGattCharacteristic.PROPERTY_WRITE) == 0 && (test & BluetoothGattCharacteristic.PROPERTY_WRITE_NO_RESPONSE) == 0) { //Check that the property is writable
                return;
            }

            if (bluetoothGatt.writeCharacteristic(characteristic)) {                       //Request the BluetoothGatt to do the Write
                //Log.d(TAG, "writeCharacteristic successful");                               //The request was accepted, this does not mean the write completed
                RXdata="";
              //  Log.d("bleservice","write success "+characteristic.getUuid());
                LocalBroadcastManager.getInstance(getApplicationContext()).sendBroadcast(new Intent("SUCCESS_BLE"));
            } else {
                //Log.d("bleservice","write error from writechar");
                this.sendBroadcast(new Intent("ERROR_BLE"));
                //  Log.d(TAG, "writeCharacteristic failed");                                   //Write request was not accepted by the BluetoothGatt
            }
        }catch (Exception er){
              //er.getMessage();
            ////new Logger(TAG+ er.getStackTrace()[0].getMethodName() + ": " + er.getMessage(),BleService.this);
        }
    }

    // ----------------------------------------------------------------------------------------------------------------
    // Write to the MLDP data characteristic
    private static BluetoothGattCharacteristic writeDataCharacteristic;
    private void writeMLDP(String string) {                            //Write string (may need to add code to limit write to 20 bytes)
        try {
           // Log.d(TAG,"into write MLDP");
            if (mldpDataCharacteristic != null) {
                writeDataCharacteristic = mldpDataCharacteristic;
            } else {
                writeDataCharacteristic = transparentRxDataCharacteristic;
            }
            if (bluetoothAdapter == null || bluetoothGatt == null || writeDataCharacteristic == null) {
              //  Log.d(TAG, "Write attempted with Bluetooth uninitialized or not connected "+bluetoothAdapter+","+bluetoothGatt+","+writeDataCharacteristic);
                //Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected");
                mConnected=false;
                //disconnect();
                return;
            }

            if (!bluetoothAdapter.isEnabled()){
               // Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected");
                return;
            }

            writeDataCharacteristic.setValue(string);
            characteristicWriteQueue.add(writeDataCharacteristic);                                       //Put the characteristic into the write queue
            if(characteristicWriteQueue.size() == 1){                                                   //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
                if (!bluetoothGatt.writeCharacteristic(writeDataCharacteristic)) {                       //Request the BluetoothGatt to do the Write
                    Log.d(TAG, "Failed to write characteristic");                                       //Write request was not accepted by the BluetoothGatt
                    ////new Logger(TAG+" failed to write ",BleService.this);
                    mConnected=false;
                    try {
                        this.sendBroadcast(new Intent("ERROR_BLE"));
                        //this.stopSelf();
                        disconnect();
                        checkConnection();
                    }catch (Exception er){
                      //  er.printStackTrace();
                    }
                }else{
                    sendBroadcast(new Intent(ACTION_BLE_CONNECT));                          //SEND BROADCAST
                }
            }

        } catch (Exception e) {
            mConnected=false;
            this.sendBroadcast(new Intent("ERROR_BLE"));
            //Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
            ////new Logger(TAG+ e.getStackTrace()[0].getMethodName() + ": " + e.getMessage(),BleService.this);
        }

    }

    public void writeMLDP(byte[] byteValues) {                                                      //Write bytes (may need to add code to limit write to 20 bytes)
        try {
          //  Log.d(TAG,"into write MLDP byte");
            BluetoothGattCharacteristic writeDataCharacteristic;
            if (mldpDataCharacteristic != null) {
                writeDataCharacteristic = mldpDataCharacteristic;
            }
            else {
                writeDataCharacteristic = transparentRxDataCharacteristic;
            }
            if (bluetoothAdapter == null || bluetoothGatt == null || writeDataCharacteristic == null) {
            //    Log.w(TAG, "Write attempted with Bluetooth uninitialized or not connected");
                return;
            }
            writeDataCharacteristic.setValue(byteValues);
            characteristicWriteQueue.add(writeDataCharacteristic);                                       //Put the characteristic into the write queue
            if(characteristicWriteQueue.size() == 1){                                                   //If there is only 1 item in the queue, then write it.  If more than 1, we handle asynchronously in the callback above
                if (!bluetoothGatt.writeCharacteristic(writeDataCharacteristic)) {                       //Request the BluetoothGatt to do the Write
                  //  Log.d(TAG, "Failed to write characteristic");                                       //Write request was not accepted by the BluetoothGatt
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Oops, exception caught in " + e.getStackTrace()[0].getMethodName() + ": " + e.getMessage());
        }
    }

    // Returns instance of this MldpBluetoothService so clients of the service can access it's methods
    class LocalBinder extends Binder {
        BleService getService() {
            return BleService.this;
        }
    }


    @Override
    public IBinder onBind(Intent intent) {
        return new LocalBinder();
    }

    // ----------------------------------------------------------------------------------------------------------------
    // All activities have stopped using the service so close the Bluetooth GATT connection
    @Override
    public boolean onUnbind(Intent intent) {
        return super.onUnbind(intent);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        try {
            if (bleBroadcast != null) {
                closeBle();
                unregisterReceiver(bleBroadcast);
            }
        }catch (Exception er){}

        if (Build.VERSION.SDK_INT >=24) {
            stopForeground(STOP_FOREGROUND_REMOVE);
        }
        ////new Logger(TAG+" service destroyed",BleService.this);
    }

    private void startForeGroundService(String type, Context context) {
        try {
            Intent notificationIntent;
            String title="VisiMaster";
            String message="Access control, tap to open";

            notificationIntent = new Intent(context, MainActivity.class);
            //notificationIntent.setAction(Constants.ACTION.MAIN_ACTION);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
            notificationIntent.addFlags(Intent.FLAG_ACTIVITY_PREVIOUS_IS_TOP);
            //notificationIntent.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
            PendingIntent pendingIntent = PendingIntent.getActivity(context, 0,
                    notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

            Bitmap icon = BitmapFactory.decodeResource(context.getResources(), R.drawable.bracket);

            String channel="";
            if (Build.VERSION.SDK_INT >=26) {
                /*channel = createChannel();
                Notification notification = new Notification.Builder(this, channel)
                        .setContentTitle(title)
                        //.setTicker("Waiting for incomming visitor")
                        .setContentText(message)
                        .setSmallIcon(R.drawable.whitelogo)
                        .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true).build();
                startForeground(101, notification); */
            }else{
                Notification notification = new Notification.Builder(context)
                        .setContentTitle(title)
                        //.setTicker("Waiting for incomming visitor")
                        .setContentText(message)
                        .setSmallIcon(R.drawable.bracket)
                        .setLargeIcon(Bitmap.createScaledBitmap(icon, 128, 128, false))
                        .setContentIntent(pendingIntent)
                        .setOngoing(true).build();
                startForeground(101, notification);
            }
        }catch (Exception er){
            // er.printStackTrace();
            ////new Logger(context.getClass().getName()+","+er.getMessage(),context);
        }

    }

}
