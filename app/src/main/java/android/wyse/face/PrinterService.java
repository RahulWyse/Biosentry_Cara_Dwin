package android.wyse.face;

import android.app.Service;
import android.bluetooth.BluetoothDevice;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Binder;
import android.os.Handler;
import android.os.IBinder;
import android.os.Message;

import com.hoin.btsdk.BluetoothService;

import org.json.JSONObject;

import java.util.HashMap;
import java.util.Timer;
import java.util.TimerTask;


/**
 * Created by cis on 23/05/18.
 * PrinterSerivce - responsible for badge printing
 * Written by Sonu Auti at cis
 */

public class PrinterService extends Service {

    private BluetoothService mService = null;
    private BluetoothDevice con_dev = null;
    private final String PRINT_DATA_ACTION="PRINT_DATA_ACTION";
    private final String PRINTER_CONNECTED="PRINTER_CONNECTED";
    private final String PRINTER_STATUS_LIVE="PRINTER_STATUS_LIVE";
    private final String PRINTER_DISCONNECTED="PRINTER_DISCONNECTED";

    private static String mDeviceAddress="DC:0D:30:03:B2:ED";
    String TAG="PrinterService";
    private static boolean isConnected=false;
    private static int connectionState=-1;
    private HashMap<String,String> printQue;
    private int sensorDisconnCnt=0;
    private int disconnectedCnt=0;

    private final BroadcastReceiver printBroadCast = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            //Log.d(TAG,intent.getAction());

            if (intent.getAction().equals("ARE_YOU_ALIVE")){
                boolean value=false;
                if (sensorDisconnCnt==0){
                    value=true;
                }

            }

            if (intent.getAction().equals(PRINT_DATA_ACTION)){
                String[] data=intent.getStringArrayExtra("data");
                String qrcodeLink=intent.getStringExtra("link");

                /*Log.d(TAG,qrcodeLink);

                for (int d=0;d<data.length;d++){
                    Log.d(TAG,"data "+data[d]);
                }*/

                if (isConnected) {
                    setCenter();
                    if (!qrcodeLink.equals("")) {
                         setLargeFont();
                         sendData("VISI PASS");
                         printQr(qrcodeLink);
                        // setCenter();
                         sendData("\n");
                    }

                    if (data.length>0){
                       // setCenter();
                        String kioskName=data[data.length-1];
                        if (data.length>4) {
                            setLargeFont();
                            if (data[0]!=null)
                                sendData(data[0]);
                            setBigFont();
                            if (data[1]!=null)
                                sendData(data[1]);
                            if (data[2]!=null)
                               // sendData(data[2]);
                            setSmallFont();
                            if (data[3]!=null)
                                sendData("Allowed by " + data[3]);
                            if (data[4]!=null)
                                sendData(data[4]);
                        }else{
                            setBigFont();
                            kioskName="";
                            for (String aData : data) {
                                if (aData != null) {
                                    sendData(aData);
                                }
                            }
                        }
                        if (!kioskName.equals("")){
                            sendData(kioskName);
                        }
                        sendData("visit visiapp.in for more info\n----------------------\n");
                    }
                }
            }

            if (intent.getAction().equals(PRINTER_CONNECTED)){
                //Log.d(TAG,"Broadcast "+PRINTER_CONNECTED);
                sensorDisconnCnt=0;
                setSmallFont();
                mService.sendMessage("ok","GBK");
                isConnected=true;
                sendBroadcast(new Intent("ACTION_PRINTER_CONNECTED"));
                //new Logger(TAG+", PRINTER_CONNECTED",PrinterService.this);
            }

            if (intent.getAction().equals(PRINTER_DISCONNECTED)){
                //Log.d(TAG,"Broadcast "+PRINTER_DISCONNECTED);
                isConnected=false;
                    if (sensorDisconnCnt==0) {
                        sendBroadcast(new Intent("ACTION_PRINTER_DISCONNECTED"));
                        //new Logger(TAG + ", PRINTER_DISCONNECTED", PrinterService.this);
                    }
                    sensorDisconnCnt++;
            }

        }
    };

    private BluetoothManager bluetoothManager;
    @Override
    public void onCreate() {

        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(PRINT_DATA_ACTION);
        intentFilter.addAction(PRINTER_CONNECTED);
        intentFilter.addAction(PRINTER_DISCONNECTED);
        intentFilter.addAction("ARE_YOU_ALIVE");
        registerReceiver(printBroadCast, intentFilter);

        printQue=new HashMap<>();
        mService = new BluetoothService(this, mHandler);
        bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager

        sensorDisconnCnt=0;  //used to limit the disconnection log

        if(!mService.isAvailable()){
            //Toast.makeText(this, "Bluetooth is not available", Toast.LENGTH_LONG).show();
        }else{
           // SharedPreferences sharedPreferences=Utility.getSharedPref(getApplicationContext());
            String settings=CaraManager.getInstance().getSharedPreferences().getString("settings","");

            if (settings!=null){
                if (!settings.equals("")) {
                    try {
                        JSONObject jdata = new JSONObject(settings);
                        String is_printer = jdata.getString(Helper.SETTING_IS_PRINTER);
                        if (is_printer.equals("true") && bluetoothManager.getAdapter().isEnabled()){
                            mDeviceAddress= jdata.getString(Helper.SETTING_PRINTER_MAC_ID).toUpperCase();
                            con_dev = mService.getDevByMac(mDeviceAddress);
                            mService.connect(con_dev);
                           // Log.d(TAG,mDeviceAddress);
                        }
                    } catch (Exception er) {
                        //Log.d(TAG,er.getMessage());
                        //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
                        isConnected=false;
                    }
                }
            }
        }

        //new Logger(TAG+", print service started",PrinterService.this);
    }

    private void checkConnection(){
        try {
            con_dev = mService.getDevByMac(mDeviceAddress);
            mService.connect(con_dev);
        }catch (Exception er){
            isConnected=false;
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        checkBleInterval();
        return START_NOT_STICKY;
    }

    private Timer bletimer;
    private final int SCAN_INTERVAL=20000;
    private void checkBleInterval(){
        try {
            if (bletimer != null) {
                bletimer.cancel();
            }
            bletimer = new Timer();
            bletimer.schedule(new TimerTask() {
                @Override
                public void run() {
                    if (isConnected) {
                        // Log.d(TAG,"BLE connection is "+mConnected);
                        //added here to show print icon on new visitor page, do not mess with it
                        sendBroadcast(new Intent(PRINTER_STATUS_LIVE));
                        return;
                    } else {
                        if (bluetoothManager!=null) {
                            if (bluetoothManager.getAdapter().isEnabled() && connectionState != 0) {                                           //Check if BT is not enabled
                                checkConnection();
                            }
                        }
                    }
                }
            }, 60000, SCAN_INTERVAL);

        }catch (Exception er){
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }

    }

    private final Handler mHandler = new Handler() {
        @Override
        public void handleMessage(Message msg) {
            switch (msg.what) {
                case BluetoothService.MESSAGE_STATE_CHANGE:
                    switch (msg.arg1) {
                        case BluetoothService.STATE_CONNECTED:
                            //Toast.makeText(PrinterService.this, "Connect successful", Toast.LENGTH_SHORT).show();
                            isConnected=true;
                            connectionState=1;
                            sendBroadcast(new Intent(PRINTER_CONNECTED));
                            break;
                        case BluetoothService.STATE_CONNECTING:
                            connectionState=0;
                            // Log.d(TAG,"CONNECTING");
                            break;
                        case BluetoothService.STATE_LISTEN:
                        case BluetoothService.STATE_NONE:
                          //  Log.d(TAG,"ERROR");
                            connectionState=-1;
                            break;
                    }
                    break;
                case BluetoothService.MESSAGE_CONNECTION_LOST:
                    //Toast.makeText(PrinterService.this, "Device connection was lost", Toast.LENGTH_SHORT).show();
                    isConnected=false;
                    connectionState=-1;
                    sendBroadcast(new Intent(PRINTER_DISCONNECTED));

                    break;
                case BluetoothService.MESSAGE_UNABLE_CONNECT:
                    //Toast.makeText(PrinterService.this, "Unable to connect device",Toast.LENGTH_SHORT).show();
                    isConnected=false;
                    connectionState=-1;
                    sendBroadcast(new Intent(PRINTER_DISCONNECTED));
                    break;
            }
        }

    };

    private void setLargeFont(){
        try {
            byte[] cmd = new byte[3];
            cmd[0] = 0x1b;
            cmd[1] = 0x21;
            cmd[2] = 0x10;
            mService.write(cmd);
        }catch (Exception er){
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }
    }

    private void setBigFont(){
        try {
            byte[] cmd = new byte[3];
            cmd[0] = 0x1b;
            cmd[1] = 0x21;
            cmd[2] |= 0x00;
            mService.write(cmd);
        }catch (Exception er){
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }
    }

    private void setCenter(){
        try {
            byte[] cmd = new byte[3];
            cmd[0] = 0x1b;
            cmd[1] = 0x61;
            cmd[2] = 0x01;
            mService.write(cmd);
        } catch (Exception er){
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }
    }
    private void setSmallFont(){
        try {
            byte[] cmd = new byte[3];
            cmd[0] = 0x1b;
            cmd[1] = 0x21;
            cmd[2] = 0x01;
            mService.write(cmd);
        }catch (Exception er){
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }
    }

    private void sendData(final String msg) {
        if (msg.length() > 0){
            try {
                mService.sendMessage(msg, "GBK");
            }catch (Exception er){
                //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
            }
        }
    }

    private void printQr(final String data){
        byte[] cmd = new byte[7];
        cmd[0] = 0x1B; //print qr code
        cmd[1] = 0x5A; //qr code command
        cmd[2] = 0x00;
        cmd[3] = 0x02;
        cmd[4] = 0x07;
        cmd[5] = 0x20; //data size of qr code
        cmd[6] = 0x00;

        try {
            if (data.length() > 0) {
                mService.write(cmd);
                mService.sendMessage(data, "GBK");
               // Log.d(TAG, "IN printQr");
            }
        }catch (Exception er){
            //new Logger(TAG+", "+er.getMessage(),PrinterService.this);
        }

    }

    @Override
    public void onDestroy(){
        super.onDestroy();
        //sendBroadcast(new Intent("IM_DEAD").putExtra("type",KycManager.PRINTER));

        if (mService != null) { mService.stop(); }
        mService = null;
        try{
            if (printBroadCast!=null){
                unregisterReceiver(printBroadCast);
            }
        }catch (Exception er){}
        if (bletimer != null) {
            bletimer.cancel();
        }
        connectionState=-1;
        isConnected=false;
        //new Logger(TAG+", print service destroyed",PrinterService.this);
    }

    private final IBinder binder=new LocalBinder();

    @Override
    public IBinder onBind(Intent intent) {
        //TODO for communication return IBinder implementation
        return binder;
    }

    class LocalBinder extends Binder {
        PrinterService getService(){
            return PrinterService.this;
        }
    }

}
