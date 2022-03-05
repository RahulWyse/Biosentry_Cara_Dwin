package android.wyse.face;

import android.app.Activity;
import android.app.PendingIntent;
import android.content.Intent;
import android.util.Log;
import android.wyse.face.com.microchip.android.mcp2221comm.Mcp2221Comm;
import android.wyse.face.com.microchip.android.mcp2221comm.Mcp2221Config;
import android.wyse.face.com.microchip.android.mcp2221comm.Mcp2221Constants;
import android.wyse.face.microchipusb.Constants;
import android.wyse.face.microchipusb.MCP2221;
import android.wyse.face.microchipusb.MicrochipUsb;

public class UsbLatchConnector {

    private static UsbLatchConnector UsbLatchConnector;
    /** USB permission action for the USB broadcast receiver. */
    private final String ACTION_USB_PERMISSION = "com.microchip.android.USB_PERMISSION";
    /** public member to be used in the test project. */
    private MCP2221 mcp2221;
    /** public member to be used in the test project. */
    private Mcp2221Comm mcp2221Comm;

    /** Microchip Product ID. */
    protected  final int MCP2221_PID = 0xDD;
    /** Microchip Vendor ID. */
    protected  final int MCP2221_VID = 0x4D8;

    private  final byte FUNCTION_GPIO = 0;
    private  final byte FUNCTION_DEDICATED = 1;
    private  final byte FUNCTION_ALTERNATE_0 = 2;
    private  final byte FUNCTION_ALTERNATE_1 = 3;
    private  final byte FUNCTION_ALTERNATE_2 = 4;
    private  final byte OUTPUT = 0;
    private  final byte INPUT = 1;
    private  final byte LOW = 0;
    private  final byte HIGH = 1;


    private UsbLatchConnector(){

    }

    public static synchronized UsbLatchConnector getInstance(){
        if (UsbLatchConnector!=null){
            return UsbLatchConnector;
        }
        return UsbLatchConnector=new UsbLatchConnector();
    }

    public static void setUsbLatchConnector(android.wyse.face.UsbLatchConnector usbLatchConnector) {
        UsbLatchConnector = usbLatchConnector;
    }

    public MCP2221 getMcp2221() {
        return mcp2221;
    }

    public void setMcp2221(MCP2221 mcp2221) {
        this.mcp2221 = mcp2221;
    }

    public void setMcp2221Comm(Mcp2221Comm mcp2221Comm) {
        this.mcp2221Comm = mcp2221Comm;
    }

    public Mcp2221Comm getMcp2221Comm() {
        return mcp2221Comm;
    }

    public  int getMcp2221Pid() {
        return MCP2221_PID;
    }

    public  int getMcp2221Vid() {
        return MCP2221_VID;
    }

    public  String getActionUsbPermission() {
        return ACTION_USB_PERMISSION;
    }

    public void flushConnection(){
        if (mcp2221!=null) {
            mcp2221.close();
            mcp2221=null;
            mcp2221Comm=null;
        }
    }

    public boolean openDoor(){

        try {
            if (getMcp2221Comm()!=null){
                Mcp2221Comm.setMcp2221Comm(null);
                setMcp2221Comm(Mcp2221Comm.getInstance());
            }
            if (getMcp2221Comm()!=null) {
                if (getMcp2221Comm().setGpPinValue((byte) 0, HIGH) == Mcp2221Constants.ERROR_SUCCESSFUL) {
                    return true;
                } else {
                    CaraManager.getInstance().setGateCommand(false);
                    CaraManager.getInstance().setLatchConnected(false);
                    flushConnection();
                    return false;
                }
            }
        }catch (Exception er){
            CaraManager.getInstance().setGateCommand(false);
            return false;
        }
        return false;
    }

    public boolean closeDoor(){
        if(getMcp2221Comm().setGpPinValue((byte)0,LOW) == Mcp2221Constants.ERROR_SUCCESSFUL){
            return true;
        }else{
            CaraManager.getInstance().setLatchConnected(false);
            CaraManager.getInstance().setGateCommand(false);
            flushConnection();
            return false;
        }
    }

    private Mcp2221Config mMcp2221Config;
    public void setDefaultConfig() {

        byte[] pinDesignation = new byte[4];
        byte[] pinDirection = new byte[4];
        byte[] pinValue = new byte[4];

        if (mMcp2221Config == null) {
            mMcp2221Config = new Mcp2221Config();
        }

        pinValue[0] = LOW;
        pinDesignation[0] = FUNCTION_GPIO;
        pinDirection[0] = OUTPUT;

        if (mcp2221Comm == null) {
            //Log.d("UsbComm","No mcp connected !");
            return;
        } else {

            mMcp2221Config.setGpPinDesignations(pinDesignation);
            mMcp2221Config.setGpPinDirections(pinDirection);
            mMcp2221Config.setGpPinValues(pinValue);

            // update the GP settings
            if (mcp2221Comm.setSRamSettings(mMcp2221Config, false, false, false, false, false,
                    false, true) == Mcp2221Constants.ERROR_SUCCESSFUL) {
                //Log.d("UsbComm","Configuration updated successfully !");
                //KioskManager.getInstance().setLatchEnable(true);
                CaraManager.getInstance().setLatchConnected(true);

            } else {
                CaraManager.getInstance().setLatchConnected(false);
                //Log.d("UsbComm","Error in configuration update !");
            }

        }

    }

    public void startUsb(Activity context){
        try{
                //Log.d("usb","is latch enabled "+CaraManager.getInstance().isLatchEnable());
                if (!(CaraManager.getInstance().isLatchEnable() || CaraManager.getInstance().isThermal())){
                    return;
                }

                if(getMcp2221()==null) {
                    setMcp2221(new MCP2221(context));
                }

               //Log.d("usb",(getMcp2221()!=null)+"");

                Constants result = MicrochipUsb.getConnectedDevice(context);
                //Log.d("usb",result+"");
                if (result == Constants.MCP2221) {
                    // try to open a connection
                    result = getMcp2221().open();
                    //Log.d("usb",result+"");
                    switch (result) {

                        case SUCCESS:
                            //Toast.makeText(context,"USB Device is connected !",Toast.LENGTH_SHORT).show();
                            setMcp2221Comm(Mcp2221Comm.getInstance());
                             //Log.d("usb","usb connection successfull");
                            setDefaultConfig();
                            context.sendBroadcast(new Intent("ACTION_BLE_CONNECT"));
                            CaraManager.getInstance().setUsbError(false);
                            CaraManager.getInstance().setLatchConnected(true);
                            break;
                        case CONNECTION_FAILED:
                            //sToast.setText("Connection failed");
                             //Log.d("usb","usb connection failed");
                            CaraManager.getInstance().setUsbError(true);
                            context.sendBroadcast(new Intent("ACTION_BLE_DISCONNECT"));
                            CaraManager.getInstance().setLatchConnected(false);
                            CaraManager.getInstance().sendSignal("NO_USB_PERMISSION",context.getApplicationContext());
                            break;
                        case NO_USB_PERMISSION:
                            //Log.d("usb","no permission");
                            CaraManager.getInstance().setUsbError(true);
                            CaraManager.getInstance().setLatchConnected(false);
                            getMcp2221().requestUsbPermission(PendingIntent.getBroadcast(context, 0, new Intent(getActionUsbPermission()), 0));
                            CaraManager.getInstance().sendSignal("NO_USB_PERMISSION",context.getApplicationContext());
                            break;
                        default:
                            break;
                    }
                }

        }catch (Exception er){
            Utility.printStack(er);
            CaraManager.getInstance().sendSignal("ACTION_START_USB_FAILED",context.getApplicationContext());
        }
    }



}
