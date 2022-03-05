package android.wyse.face;

import android.animation.ObjectAnimator;
import android.app.Activity;
import android.app.ActivityManager;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.camera2.CameraCharacteristics;
import android.media.Ringtone;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.printservice.PrintService;
import android.util.Base64;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.wyse.face.com.microchip.android.mcp2221comm.Mcp2221Constants;
import android.wyse.face.models.DnsModel;
import android.wyse.face.models.SelectorTypes;
import android.wyse.face.models.ThermalModel;
import android.wyse.face.models.UsersModel;
import android.wyse.face.tech5.db.FaceRecord;
import android.wyse.face.tech5.db.LocalCacheManager;
import android.wyse.face.tech5.utilities.Listener;
import android.wyse.face.tech5.utilities.Utilities;

import com.dwin.dwinpio.GpioControlUtil;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.ml.SVM;

import java.io.File;
import java.io.IOException;
import java.math.RoundingMode;
import java.net.NetworkInterface;
import java.nio.ByteBuffer;
import java.security.KeyFactory;
import java.security.MessageDigest;
import java.security.spec.X509EncodedKeySpec;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import javax.crypto.Cipher;


public class CaraManager {

    public static final String PREF_FILE_NAME = "cara_config";
    public static String RESET_TIME = "23:59";
    public static long miliSecondsInDay = (60 * 60 * 24 * 1000);
    public static int successCnt = 0;
    private static CaraManager caraManager;
    private static Timer timer = new Timer();
    private final int bytesToRead = 525;
    private String finalDns="";
    String TAGS="cara_logs";
    private Ringtone timerTone;
    private Ringtone shutterTone;
    private boolean simulateOnly;
    private HashMap<String, String> caraUsers;
    private Database caraDb;
    private String API_KEY = "460ad6f3-8216-469f-9b1c-52cffa5d812c";
    private String MARK_ONLINE = "1";
    private String MARK_OFFLINE = "0";
    private String DEVICE_MACID = "02:00:00:00:00:00";
    private boolean isTaj;
    private boolean isLatchEnable;
    private boolean isAccessControl;
    private boolean isLatchConnected;
    private boolean isUpload;
    private int uploadCount;
    private SharedPreferences sharedPreferences;
    private boolean isCommand;
    private long MINIMUM_ALLOWED_TIME = (60 * 1000);
    private int DOOR_TIMEOUT = 1200;
    private String ACTION_OPEN_DOOR = "ACTION_OPEN_DOOR";
    private String latchComType = "2";
    private ProgressDialog progressDialog;
    private long inout_resetTime;
    private boolean isProd = false;
    private boolean isThermal;
    private String inoutMode = "-1";
    private boolean isLicVersion;
    private long lastReboot;
    private long lastUpdate;
    private boolean isReboot;
    private String rebootTime;
    private boolean isQuality;
    private boolean isAutoFlash;
    private String tempUnit;
    private boolean isAlarm;
    private float scaleTemp;
    private float maxTemp;
    private SVM svm;
    private boolean isLiveness;
    private boolean isUsbError;
    private boolean isReadCenter;
    private double refHeat;
    private boolean comeCloser;
    private ArrayList<DnsModel> whiteListDns = new ArrayList<>();
    private boolean isNetworkError;
    private String DNS_EXT="";
    private BitmapDrawable mainTheme;
    private boolean isGateCommand;
    private float defaultFaceThr=6.0f;
    private String themeDir = ".themes";
    private String[] WEEKDAY = {"", "SUN", "MON", "TUE", "WED", "THU", "FRI", "SAT"};
    private double convergePoint = 8.50;
    private boolean isConfig = false;
    private int maxAvg = 32;
    private float minTempVal=31.0f;
    private int DEFAULT_TEMP =0;
    private String shifttime="20";  //HH only
    private float enrol_quality=20.0f;
    private float liveness_thr=0.80f;
    private boolean isReadyForCapture;
    private ObjectAnimator animation;
    private boolean isImgLog;
    private Cipher decodeCp;
    private String decodeKey="MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCxw7e09u9d5NxM43oqh2pf4jXh6CLH7pauObkg6UwNBCs6D0IR75T+ZpUYPIc4vuNRV3neoIqaZoQj9JXC6d+tDRkEdNFF9oG/fCCW7M12+lCruWOr7li80Llg5EiaVzIllnN8hNjWrLRxJTn3WWZboZy5xY541y4PR3pIeX8dHQIDAQAB";
    /**
     * auto reset login for in and out
     */
    private int amount=4;
    private int maxCommands=4;
    private ArrayList<String> pendingCommands=new ArrayList<>();

    private CaraManager() { }

    public static CaraManager getInstance() {
        if (caraManager == null) {
            return caraManager = new CaraManager();
        }
        return caraManager;
    }

    public static void setSharedPref(SharedPreferences sharedPref, String key, String value) {
        try {
            SharedPreferences.Editor editor = sharedPref.edit();
            editor.putString(key, value);
            editor.apply();
        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

    public static String getResetTime() {
        return RESET_TIME;
    }

    public static void setResetTime(String resetTime) {
        RESET_TIME = resetTime;
    }

    public static String createMd5(byte[] key){
        StringBuffer sb = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key);
            byte byteData[] = md.digest();
            //convert the byte to hex format method 1
            sb = new StringBuffer();
            for (byte aByteData : byteData) {
                sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
            }
        }catch (Exception er){}
        return sb.toString();
    }

    public boolean isImgLog() {
        return isImgLog;
    }

    public void setImgLog(boolean imgLog) {
        isImgLog = imgLog;
    }

    public float getDefaultFaceThr() {
        return defaultFaceThr;
    }

    public File getThemeDir() {
        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + themeDir;
        File root = new File(rootPath);
        if (!root.exists()) {
            root.mkdirs();
        }
        return root;
    }

    public float getLiveness_thr() {
        return liveness_thr;
    }

    public void setLiveness_thr(float liveness_thr) {
        this.liveness_thr = liveness_thr;
    }

    public float getEnrol_quality() {
        return enrol_quality;
    }

    public void setEnrol_quality(float enrol_quality) {
        this.enrol_quality = enrol_quality;
    }

    public String getShifttime() {
        return shifttime;
    }

    public void setShifttime(String shifttime) {
        this.shifttime = shifttime;
    }

    public float getMinTempVal() {
        return minTempVal;
    }

    public void setMinTempVal(float minTempVal) {
        if (minTempVal<0) {
            minTempVal = 31.0f;
        }

        this.minTempVal = minTempVal;
    }

    public boolean isGateCommand() {
        return isGateCommand;
    }

    public void setGateCommand(boolean gateCommand) {
        isGateCommand = gateCommand;
    }

    public BitmapDrawable getMainTheme() {
        return mainTheme;
    }

    public void setMainTheme(BitmapDrawable mainTheme) {
        this.mainTheme = mainTheme;
    }

    public String getDNS_EXT() {
        return DNS_EXT;
    }

    public void setDNS_EXT(String DNS_EXT) {
        this.DNS_EXT = DNS_EXT;
    }

    public float convertHeat(String unit,double newHeat) {
        DecimalFormat df = new DecimalFormat("#.#");
        if (unit.equals("F")) {
            float fahrenheitHeat = (float) (newHeat * 1.8) + 32;
            if (fahrenheitHeat > 0) {
                df.setRoundingMode(RoundingMode.UP);
                return Float.parseFloat(df.format(fahrenheitHeat));
            }
        }

        return Float.parseFloat(df.format(newHeat));
    }

    /**
     * white list dns by OEM
     */
    public void initWhiteDns(){
        whiteListDns.add(new DnsModel("ws.attendanceportal.com","oem"));
        whiteListDns.add(new DnsModel("ws.tajhotels.com","oem"));
        whiteListDns.add(new DnsModel("wyse.co.in","oem"));
        whiteListDns.add(new DnsModel("biosentry.co.in","oem"));
        whiteListDns.add(new DnsModel("smartmuster.in","oem"));
        whiteListDns.add(new DnsModel("sentry.co.in","oem"));
        whiteListDns.add(new DnsModel("pintu.biosentry.co.in","oem"));
        whiteListDns.add(new DnsModel("wss.attendanceportal.com","oem"));
    }

    /**
     *
     * @param dns - DNS name to whitelist
     */
    public void addWhiteDns(String dns){
        boolean isPresent=false;
        for (DnsModel eachDns :whiteListDns) {
            if (eachDns.getDns().equalsIgnoreCase(dns)){
                isPresent=true;
            }
        }
        if (!isPresent) {
            whiteListDns.add(new DnsModel(dns,"vendor"));
        }
    }

    /**
     *
     * @return - list of all whitelisted dns
     */
    public ArrayList<DnsModel> getWhiteListDns() {
        return whiteListDns;
    }

    public void checkForExpiry(Context context){
        boolean isValidDomain=false;

        String dnsvalue= Helper.getBaseUri("").build().toString();

        if (!dnsvalue.equals("")){
            dnsvalue=dnsvalue.replaceAll("https://","");
            dnsvalue=dnsvalue.replaceAll("http://","");
            //Helper.SERVER_DNS = dnsvalue;
        }

        //Log.d("SERVER_DNS","input value ->"+dnsvalue);

        if (!dnsvalue.equals("")){
            for (DnsModel wt_dns:CaraManager.getInstance().getWhiteListDns()) {
                if (wt_dns.getDns().equalsIgnoreCase(dnsvalue)){
                    if (!wt_dns.getDnstype().equalsIgnoreCase("vendor")) {
                        isValidDomain = true;
                    }
                }
            }
        }

        if (!isValidDomain){
            //Log.d("SERVER_DNS","Unverifed domain name detected");
            //Log.d("SERVER_DNS","Verification started");
            // progressDialog.setMessage("Domain verification begin...");
            //progressDialog.show();

            finalDns=dnsvalue;
            CaraManager.getInstance().executeAction(Helper.COMMAND_VERIFY_DNS, context.getApplicationContext(), new HelperCallback() {

                @Override
                public void onResult(String result) { Log.d("DNS","into callback "+result); }

                @Override
                public void onError(String result) {
                    //removeDns(finalDns,context);
                }

            });
        }

    }

    private void removeDns(String dns,Context context){

        //Log.d("DNS","into removeDns");
        for (int i=0;i<whiteListDns.size();i++){
            if (whiteListDns.get(i).getDns().equalsIgnoreCase(dns)){
                whiteListDns.remove(i);
                //break;
            }
        }

        String settings=getSharedPreferences().getString("settings","");
        //Log.d("DNS",settings);
        if (!settings.equals("")) {
            try {
                JSONObject jdata = new JSONObject(settings);
                jdata.put(Helper.SETTING_DNS,"ws.attendanceportal.com");

                if(Utility.writeSharedPref(getSharedPreferences(),"settings",jdata.toString())) {
                    //Log.d("DNS","update success");
                }
            } catch (Exception er) {
                Utility.printStack(er);
            }
        }

    }

    public boolean isNetworkError() {
        return isNetworkError;
    }

    public void setNetworkError(boolean networkError) {
        isNetworkError = networkError;
    }

    public boolean isComeCloser() {
        return comeCloser;
    }

    public void setComeCloser(boolean comeCloser) {
        this.comeCloser = comeCloser;
    }

    public double getRefHeat() {
        return refHeat;
    }

    public void setRefHeat(double refHeat) {
        this.refHeat = refHeat;
    }

    public boolean isReadCenter() {
        return isReadCenter;
    }

    public void setReadCenter(boolean readCenter) {
        isReadCenter = readCenter;
    }

    public boolean isUsbError() {
        return isUsbError;
    }

    public void setUsbError(boolean usbError) {
        isUsbError = usbError;
    }

    public boolean isLiveness() {
        return isLiveness;
    }

    public void setLiveness(boolean liveness) {
        isLiveness = liveness;
    }

    public SVM getSvm() {
        return svm;
    }

    public void setSvm(SVM svm) {
        this.svm = svm;
    }

    public synchronized void checkForHalt(Context context) {
        if (isReboot()) {
            //String hhmm=getSharedPreferences().getString("reboot_hhmm","03:00");
            if (getRebootTime().equals(getCurrentDateTime("hh:mm"))) {
                context.sendBroadcast(new Intent("CMD_REBOOT").putExtra("access", context.getPackageName()));
            }
        }
    }

    public void checkForReset() {
        //Log.d("ShiftTime",amount+","+CaraManager.getInstance().getShifttime());
        if (CaraManager.getInstance().getCurrentDateTime("hh:mm").equals(CaraManager.RESET_TIME)) {

            try {
                if (!CaraManager.getInstance().getShifttime().equals("")) {
                    amount = (24 - Integer.valueOf(CaraManager.getInstance().getShifttime()));
                }
            }catch (Exception er){}

            Calendar cal = Calendar.getInstance();
            //Log.d("TAG","Current time " +cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE));
            //logic to reset in/out at 20 hours every day
            cal.add(Calendar.HOUR_OF_DAY, -amount);   //substract 12 hours
            //Log.d("TAG","After substract time " +cal.get(Calendar.HOUR_OF_DAY)+":"+cal.get(Calendar.MINUTE));
            if (CaraManager.getInstance().getCaraDb().resetStatus(cal.getTimeInMillis() + "")) {
                Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "inout_reset", System.currentTimeMillis() + "");
                CaraManager.getInstance().setLastInout_resetTime(System.currentTimeMillis());
            }

        } else if ((System.currentTimeMillis() - CaraManager.getInstance().getLastInout_resetTime()) > CaraManager.miliSecondsInDay) {

            try {
                if (!CaraManager.getInstance().getShifttime().equals("")) {
                    amount = (24 - Integer.valueOf(CaraManager.getInstance().getShifttime()));
                }
            }catch (Exception er){}

            //if last reset time exceed more than 1 day
            /*Calendar cal = Calendar.getInstance();
            int goback = -(cal.get(Calendar.HOUR_OF_DAY) + amount);   //substract hours
            cal.add(Calendar.HOUR_OF_DAY, goback);
            if (CaraManager.getInstance().getCaraDb().resetStatus(cal.getTimeInMillis() + "")) {
                Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "inout_reset", System.currentTimeMillis() + "");
                CaraManager.getInstance().setLastInout_resetTime(System.currentTimeMillis());
            }*/
        }
    }

    public float getMaxTemp() {
        return maxTemp;
    }

    public void setMaxTemp(float maxTemp) {
        this.maxTemp = maxTemp;
    }

    public float getScaleTemp() {
        return scaleTemp;
    }

    public void setScaleTemp(float scaleTemp) {
        this.scaleTemp = scaleTemp;
    }

    public boolean isAlarm() {
        return isAlarm;
    }

    public void setAlarm(boolean alarm) {
        isAlarm = alarm;
    }

    public String getTempUnit() {
        if (tempUnit == null)
            return "F";
        return tempUnit;
    }

    public void setTempUnit(String tempUnit) {
        this.tempUnit = tempUnit;
    }

    public boolean isAutoFlash() {
        return isAutoFlash;
    }

    public void setAutoFlash(boolean autoFlash) {
        isAutoFlash = autoFlash;
    }

    public boolean isQuality() {
        return isQuality;
    }

    public void setQuality(boolean quality) {
        isQuality = quality;
    }

    public String getRebootTime() {
        return rebootTime;
    }

    public void setRebootTime(String rebootTime) {
        this.rebootTime = rebootTime;
    }

    public boolean isReboot() {
        return isReboot;
    }

    public void setReboot(boolean reboot) {
        isReboot = reboot;
    }

    public boolean isLicVersion() {
        return isLicVersion;
    }

    public void setLicVersion(boolean licVersion) {
        isLicVersion = licVersion;
    }

    public String getInoutMode() {
        return inoutMode;
    }

    public void setInoutMode(String inoutMode) {
        this.inoutMode = inoutMode;
    }

    public boolean isThermal() {
        return isThermal;
    }

    public void setThermal(boolean thermal) {
        isThermal = thermal;
    }

    public boolean isProd() {
        return isProd;
    }

    public void setProd(boolean prod) {
        isProd = prod;
    }

    public long getLastInout_resetTime() {
        return inout_resetTime;
    }

    public void setLastInout_resetTime(long inout_resetTime) {
        this.inout_resetTime = inout_resetTime;
    }

    public boolean isCommand() {
        return isCommand;
    }

    public void setCommand(boolean command) {
        isCommand = command;
    }

    public SharedPreferences getSharedPreferences() {
        return sharedPreferences;
    }

    public void setSharedPreferences(SharedPreferences sharedPreferences) {
        this.sharedPreferences = sharedPreferences;
    }

    public long getMINIMUM_ALLOWED_TIME() {
        return MINIMUM_ALLOWED_TIME;
    }

    public void setMINIMUM_ALLOWED_TIME(long MINIMUM_ALLOWED_TIME) {
        this.MINIMUM_ALLOWED_TIME = MINIMUM_ALLOWED_TIME;
    }

    public boolean isUpload() {
        return isUpload;
    }

    public void setUpload(boolean upload) {
        isUpload = upload;
    }

    public int getUploadCount() {
        return uploadCount;
    }

    public void setUploadCount(int uploadCount) {
        this.uploadCount = uploadCount;
    }

    public boolean isAccessControl() {
        return isAccessControl;
    }

    public void setAccessControl(boolean accessControl) {
        isAccessControl = accessControl;
    }

    public boolean isLatchConnected() {
        return isLatchConnected;
    }

    public void setLatchConnected(boolean latchConnected) {
        isLatchConnected = latchConnected;
    }

    public boolean isLatchEnable() {
        return isLatchEnable;
    }

    public void setLatchEnable(boolean latchEnable) {
        isLatchEnable = latchEnable;
    }

    public boolean isTaj() {
        return isTaj;
    }

    public void setTaj(boolean taj) {
        isTaj = taj;
    }

    public Database getCaraDb() {
        return caraDb;
    }

    public void setCaraDb(Database caraDb) {
        this.caraDb = caraDb;
    }

    public HashMap<String, String> getCaraUsers() {
        return caraUsers;
    }

    public void setCaraUsers(HashMap<String, String> caraUsers) {
        this.caraUsers = caraUsers;
    }

    public String getAPI_KEY() {
        return API_KEY;
    }

    public String getLastReboot() {
        if (lastReboot == 0) {
            return getSharedPreferences().getString(Helper.LAST_REBOOT, "0");
        }
        return lastReboot + "";
    }

    public void setLastReboot(long lastReboot) {
        this.lastReboot = lastReboot;
    }

    public long getLastUpdate() {
        lastUpdate = System.currentTimeMillis();
        return lastUpdate;
    }

    public void setLastUpdate(long lastUpdate) {
        this.lastUpdate = lastUpdate;
    }

    public String getCaraUserById(String userId) {
        if (caraUsers != null) {
            return caraUsers.get(userId);
        }
        return "NO_USER_FOUND";
    }

    private boolean validateDateTime(int hr, int min, int sec) {
        return hr == 0 && min == 0 && sec == 0;
    }

    public void initUsers(Context context) {
        try {
            if (CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS) > 0) {
                //Log.d("enroll", "Users found !");
                HashMap<String, String> users = new HashMap<>();
                Cursor crs = CaraManager.getInstance().getCaraDb().getAllUsers();
                if (crs != null) {
                    //Log.d("enroll", "Users found !");
                    while (crs.moveToNext()) {
                        //Log.d("enroll", crs.getString(crs.getColumnIndex(Database.USER_NAME)));
                        users.put(crs.getString(crs.getColumnIndex(Database.USER_ID)),
                                crs.getString(crs.getColumnIndex(Database.USER_NAME)).toUpperCase());
                    }
                    crs.close();
                }
                setCaraUsers(users);
            } else {

                if (Listener.isSDKInitialized) {
                    new Thread(new Runnable() {
                        @Override
                        public void run() {
                            if (FaceManager.getInstance().getLocalCacheManager() == null)
                                FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(context));

                            List<FaceRecord> list = FaceManager.getInstance().getLocalCacheManager().getAllRecords();
                            long size = Listener.t5TemplateMatcher.Size();
                           // Log.d("Users","Total "+size+" users found !");
                            if (list != null && list.size() > 0) {
                                if (list.size() > size) {
                                    for (FaceRecord record : list) {
                                        try {
                                            //Log.d("users",record.id);
                                            Listener.t5TemplateMatcher.RemoveFace(record.id);
                                        } catch (Exception e) {
                                            Utility.printStack(e);
                                        }
                                    }
                                }
                            }
                            FaceManager.getInstance().getLocalCacheManager().deleteAllFaceRecords();
                        }
                    }).start();

                }

            }
        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

    public String getCurrentDateTime(String type) {
        Calendar cal = Calendar.getInstance();

        int second = cal.get(Calendar.SECOND);
        int minute = cal.get(Calendar.MINUTE);
        int hourofday = cal.get(Calendar.HOUR_OF_DAY);

        if (validateDateTime(hourofday, minute, second)) {
            cal.setTimeInMillis(System.currentTimeMillis() - (1000 * 60));
            second = cal.get(Calendar.SECOND);
            minute = cal.get(Calendar.MINUTE);
            hourofday = cal.get(Calendar.HOUR_OF_DAY);
        }

        int year_new = cal.get(Calendar.YEAR);
        int dayofmonth = cal.get(Calendar.DAY_OF_MONTH);
        int month_new = cal.get(Calendar.MONTH) + 1;
        int weekDay = cal.get(Calendar.DAY_OF_WEEK);

        DecimalFormat mFormat = new DecimalFormat("00");
        mFormat.setRoundingMode(RoundingMode.DOWN);

        if (type.equals("date")) {
            return mFormat.format(Double.valueOf(dayofmonth)) + "/" + mFormat.format(Double.valueOf(month_new)) + "/" + mFormat.format(Double.valueOf(year_new));
        } else if (type.equals("time")) {
            return mFormat.format(Double.valueOf(hourofday)) + ":" + mFormat.format(Double.valueOf(minute)) + ":" + mFormat.format(Double.valueOf(second));
        } else if (type.equals("hh:mm")) {
            return mFormat.format(Double.valueOf(hourofday)) + ":" + mFormat.format(Double.valueOf(minute));
        } else if (type.equals("W:D")) {
            return WEEKDAY[weekDay] + " " + dayofmonth;
        }

        //cal=null;
        return mFormat.format(Double.valueOf(dayofmonth)) + "," + mFormat.format(Double.valueOf(month_new)) + "," + mFormat.format(Double.valueOf(year_new)) + "," + mFormat.format(Double.valueOf(hourofday)) + "," + mFormat.format(Double.valueOf(minute)) + "," + mFormat.format(Double.valueOf(second));
    }

    public void initMac() {
        this.DEVICE_MACID = getMacAddr().replaceAll(":", "-");
    }

    public String getDEVICE_MACID() {
        if (DEVICE_MACID==null) {
            initMac();
        }

        if (DEVICE_MACID.equals("02:00:00:00:00:00") || DEVICE_MACID.equals("")){
            initMac();
        }
        return DEVICE_MACID;
    }

    public int getDOOR_TIMEOUT() {
        return DOOR_TIMEOUT;
    }

    /**
     * @param DOOR_TIMEOUT in sec
     */
    public void setDOOR_TIMEOUT(int DOOR_TIMEOUT) {
        this.DOOR_TIMEOUT = DOOR_TIMEOUT * 1000;
    }

    public String getLatchComType() {
        return latchComType;
    }

    public void setLatchComType(String latchComType) {
        this.latchComType = latchComType;
    }

    public ArrayList<SelectorTypes> getLatchCom() {
        ArrayList<SelectorTypes> availableTypes = new ArrayList<>();
        availableTypes.add(new SelectorTypes("0", "Select Method"));
        availableTypes.add(new SelectorTypes("1", "Bluetooth"));
        availableTypes.add(new SelectorTypes("2", "USB"));
        return availableTypes;
    }

    public void hideUi(Window window, Activity activity) {
        try {
            activity.requestWindowFeature(Window.FEATURE_NO_TITLE);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON |
                    WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD |
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

            // clear FLAG_TRANSLUCENT_STATUS flag:
            window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
            // add FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS flag to the window
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
            Helper.secureScreen(window);
        } catch (Exception er) { }
    }

    public void fullScreen(Window window) {
        try {
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
        } catch (Exception er) {
        }
    }

    public void hideHomeBar(Window window) {
        try {
            View decorView = window.getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            //getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
            Helper.secureScreen(window);
        } catch (Exception er) {
        }
    }

    public boolean openGate(Activity context) {

        try {
           if (CaraManager.getInstance().isLatchEnable()) {
               if (Build.MANUFACTURER.equalsIgnoreCase("dwin")) {
                   return openDoor(context);
               }
                if (CaraManager.getInstance().isLatchConnected()) {
                    // If Latch Connected then open door

                        if (!getLatchComType().equals("2")) {
                            context.sendBroadcast(new Intent(ACTION_OPEN_DOOR));
                            return true;
                        } else {
                            //open usb door
                            return openUsbDoor();
                        }

                }
//                else{
//                        if (Build.MANUFACTURER.equalsIgnoreCase("dwin")) {
//                            CaraManager.getInstance().connectAccessControlDwin(context.getApplicationContext(),50);
//
//                            if (CaraManager.getInstance().isLatchConnected())
//                            {
//                                return openDoor(context);
//                            }
//                            //Toast.makeText(context.getApplicationContext(),"Latch is not connected",Toast.LENGTH_LONG).show();
//                        }
//                }

            }
        } catch (Exception er) { }
        finally {
            
        }
        return false;
    }

    public boolean connectAccessControlDwin(Context context,long time) throws IOException, InterruptedException {

        if (GpioControlUtil.getInstance().startGpio(166)) {

            boolean out = GpioControlUtil.getInstance().setGpioDirection(166, "out");

            GpioControlUtil.getInstance().getGpioIntputValue(166);

            if (GpioControlUtil.nRet == -1)
            {
                Thread.sleep(1000);
                if (GpioControlUtil.getInstance().startGpio(166))
                {
                    out = GpioControlUtil.getInstance().setGpioDirection(166, "out");
                }
                else
                {
                    CaraManager.getInstance().setLatchConnected(false);
                    Toast.makeText(context, "Latch Not Connected", Toast.LENGTH_LONG).show();
                    return CaraManager.getInstance().isLatchConnected();
                }
            }
            CaraManager.getInstance().setLatchConnected(true);
//            if (out) {
//
//                try {
//                    if (time > 0) {
//                        Thread.sleep(time);
//                    }
//                } catch (InterruptedException e) {
//                    e.printStackTrace();
//
//                    boolean d = GpioControlUtil.getInstance().setGpioOutputValue(166, 1);
//                    CaraManager.getInstance().setLatchConnected(false);
//                }
//                boolean d = GpioControlUtil.getInstance().setGpioOutputValue(166, 1);
//
//                if (d && out) {
//                    CaraManager.getInstance().setLatchConnected(true);
//                } else {
//                    CaraManager.getInstance().setLatchConnected(false);
//                    Toast.makeText(context, "Latch Not Connected", Toast.LENGTH_LONG).show();
//                }
//
//            }
//            else {
//                CaraManager.getInstance().setLatchConnected(false);
//                Toast.makeText(context, "Latch Not Connected", Toast.LENGTH_LONG).show();
//
//            }

        } else {

            GpioControlUtil.getInstance().getGpioIntputValue(166);

            CaraManager.getInstance().setLatchConnected(false);
            Toast.makeText(context, "Latch Not Connected", Toast.LENGTH_LONG).show();
            // Toast.makeText(getApplicationContext(), "Latch Connected", Toast.LENGTH_SHORT).show();
        }

        return CaraManager.getInstance().isLatchConnected();
    }
    // Access Control Dwin
    private boolean openDoor(Activity context) throws IOException, InterruptedException {


        CaraManager.getInstance().connectAccessControlDwin(context.getApplicationContext(),0);
        if (GpioControlUtil.getInstance().setGpioOutputValue(166, 0)) {
            CaraManager.getInstance().setLatchConnected(true);
            //Log.d("door","door open successfully !");

            getTimer(getDOOR_TIMEOUT(), new HelperCallback() {
                @Override
                public void onResult(String data) {
                    GpioControlUtil.getInstance().setGpioOutputValue(166, 1);

                    // Log.d("door","door closed successfully !");
                }

                @Override
                public void onError(String data) {
                    GpioControlUtil.getInstance().setGpioOutputValue(166, 1);

                    //Log.d("door","door closed error !");
                }
            });

            return true;
        } else {
            Toast.makeText(context.getApplicationContext(),"Latch Not Connected",Toast.LENGTH_LONG).show();
            CaraManager.getInstance().setLatchConnected(false);
            GpioControlUtil.getInstance().setGpioOutputValue(166, 1);
        }


        return false;
    }

    private boolean openUsbDoor() {
        CaraManager.getInstance().setGateCommand(true);
        if (UsbLatchConnector.getInstance().openDoor()) {
            CaraManager.getInstance().setLatchConnected(true);
            CaraManager.getInstance().setLatchEnable(true);
            //Log.d("door","door open successfully !");

            getTimer(getDOOR_TIMEOUT(), new HelperCallback() {
                @Override
                public void onResult(String data) {
                    UsbLatchConnector.getInstance().closeDoor();
                    CaraManager.getInstance().setGateCommand(false);
                    // Log.d("door","door closed successfully !");
                }

                @Override
                public void onError(String data) {
                    CaraManager.getInstance().setGateCommand(false);
                    //Log.d("door","door closed error !");
                }
            });

            return true;
        }
        CaraManager.getInstance().setGateCommand(false);
        return false;
    }

    public synchronized void getTimer(final int timeout, final HelperCallback helperCallback) {
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
        timer = new Timer();
        timer.schedule(new TimerTask() {
            @Override
            public void run() {
                helperCallback.onResult("");
                timer.cancel();
            }
        }, timeout);
    }

    public boolean isConfig() {
        return isConfig;
    }

    public void setConfig(boolean config) {
        isConfig = config;
    }

    private ThermalModel readTemperature() {

        try {
            int result;
            ByteBuffer readData;
            ByteBuffer txData;
            byte address;

            //final StringBuilder dataString = new StringBuilder("");

            // check that we have a connection open to a device
            if (UsbLatchConnector.getInstance().getMcp2221() == null || UsbLatchConnector.getInstance().getMcp2221Comm() == null) {
                Log.d("TAG", "No MCP2221 Connected");
                CaraManager.getInstance().setUsbError(true);
                return new ThermalModel(DEFAULT_TEMP, DEFAULT_TEMP, DEFAULT_TEMP);
            }
            //  case "i2c write":

            /*address = (byte) Integer.parseInt("14", 16);

            //address=0x14;
            List<String> dataBytes = new ArrayList<String>();
            dataBytes.clear();
            String temp = "4C";
            String[] tempStringArray;
            // remove any line breaks
            temp = temp.replace("\n", "");
            // split if one or more commas are encountered
            tempStringArray = temp.split(",+");

            //remove any null entries
            for (String string : tempStringArray) {
                if (string.equals("") == false) {
                    dataBytes.add(string);
                }
            }

            txData = ByteBuffer.allocate(dataBytes.size());

            for (int i = 0; i < dataBytes.size(); i++) {
                txData.put((byte) Integer.parseInt(dataBytes.get(i), 16));
            }


            // make sure the address is even
            address &= 0xFE;
            result = UsbLatchConnector.getInstance().getMcp2221Comm().writeI2cdata(address, txData, txData.limit(), 100000); */

            byte[] configCommand = {(byte) 0x2f, (byte) 0x04, (byte) 00};
            txData = ByteBuffer.allocate(configCommand.length);
            for (int i = 0; i < configCommand.length; i++) {
                txData.put(configCommand[i]);
            }

            if (!isConfig) {
                // make sure the address is even
                address = (byte) 0xD0;
                address &= 0xFE;
                result = UsbLatchConnector.getInstance().getMcp2221Comm().writeI2cdata(address, txData, txData.limit(), 100000);
                if (result != Mcp2221Constants.ERROR_SUCCESSFUL) {
                    return new ThermalModel(DEFAULT_TEMP, DEFAULT_TEMP, DEFAULT_TEMP);
                }
                isConfig = true;
            }

            //  case "i2c write for Read command configuration "
            address = (byte) Integer.parseInt("D0", 16);

            byte[] dataBytes1 = {(byte) 0x4e, (byte) 0x00};
            txData = ByteBuffer.allocate(dataBytes1.length);

            for (int i = 0; i < dataBytes1.length; i++) {
                txData.put(dataBytes1[i]);
            }

            // make sure the address is even
            address &= 0xFE;
            result = UsbLatchConnector.getInstance().getMcp2221Comm().writeI2cdata(address, txData, txData.limit(), 100000);

            if (result == Mcp2221Constants.ERROR_SUCCESSFUL) {

                readData = ByteBuffer.allocate(bytesToRead);
                address = (byte) 0xD1;
                // make sure the address is odd
                result = UsbLatchConnector.getInstance().getMcp2221Comm().readI2cData(address, readData, bytesToRead, 100000);

                double amb_temp, actual_temp;
                if (result == Mcp2221Constants.ERROR_SUCCESSFUL) {

                    // mTxtOutput.append("< " + Output.formatText(readData, Output.DARK_GREEN));

                    /*ptat_temp = 256 * readData.get(1) + (readData.get(0) & 0x00ff);
                    actual_temp = 256 * readData.get(3) + (readData.get(2) & 0x00ff);

                    Log.d("TAG ", "Ptat temp" + ptat_temp / 10 + "°C Actual_temp" + actual_temp / 10 + "°C");
                    ptat_temp = ptat_temp / 10;
                    actual_temp = actual_temp / 10;

                    float diff = actual_temp - ptat_temp;
                    return new ThermalModel((actual_temp), (ptat_temp), diff); */

                    amb_temp = (((256 * readData.get(9)) + readData.get(10)) - 27315) / 100.00;

                    //int dcnt=0;
                    //String thermalarray="";
                    int i = 0, j = 0;
                    ArrayList<Double> mat = new ArrayList<>();
                    for (int t = 13; t < bytesToRead; t = t + 2) {
                        if (t < bytesToRead - 1) {
                            //mat.add(actual_temp);
                            //thermalarray+=(dcnt++)+"px ->"+(actual_temp/100)+",";
                            if (CaraManager.getInstance().isReadCenter()) {
                                if (j < 16) {
                                    if (i >= 4 && i <= 12 && j >= 4 && j <= 12) {
                                        double highByte = (readData.get(t) * 256);
                                        double lowByte = readData.get(t + 1) & 0x00ff;
                                        actual_temp = (((highByte + lowByte) - 27315) / 100.00); //Double.valueOf(decimalFormat.format((((256 * readData.get(t)) + readData.get(t + 1)) - 27315)/100.00));
                                        //Log.d("OTP16",highByte+ "("+String.format("%x",readData.get(t))+")"+", "+lowByte + "("+String.format("%x",readData.get(t + 1))+") "+", "+readData.get(t)+", "+readData.get(t+1) +"-> "+(actual_temp));
                                        mat.add(actual_temp);
                                    }
                                    j++;
                                }
                                if (j == 16) {
                                    j = 0;
                                    i++;
                                }
                            }else {
                                double highByte = (readData.get(t) * 256);
                                double lowByte = readData.get(t + 1) & 0x00ff;
                                actual_temp = (((highByte + lowByte) - 27315) / 100.00); //Double.valueOf(decimalFormat.format((((256 * readData.get(t)) + readData.get(t + 1)) - 27315)/100.00));
                                //Log.d("OTP16",highByte+ "("+String.format("%x",readData.get(t))+")"+", "+lowByte + "("+String.format("%x",readData.get(t + 1))+") "+", "+readData.get(t)+", "+readData.get(t+1) +"-> "+(actual_temp));
                                mat.add(actual_temp);
                            }

                        }
                    }

                    // Log.d("OTP16","Original-> "+mat+"");
                    Collections.sort(mat, Collections.reverseOrder());


                    double maxTemp = 0.00;

                    for(int p=0;p<maxAvg;p++){
                        maxTemp = maxTemp + mat.get(p);
                    }

                    //Log.d("OTP16","Sorted by High to Low -> "+mat);

                    maxTemp = maxTemp / maxAvg;

                    //Log.d("Scaling","Max Temp->"+maxTemp);
                    //double maxTemp = Collections.max(mat);
                    //Log.d("OTP16","Max Temp -> "+maxTemp);

                    if (CaraManager.getInstance().getScaleTemp() > 1.0) {

                        //if(maxTemp<30 && maxTemp>26){
                        //maxTemp=30;  //correction made for 1st reading, reading above 30 will be taken as it is
                        //}

                        //Log.d("OTP","Ref->"+amb_temp+", Object->"+maxTemp+", "+(amb_temp-maxTemp));

                        //proven method to compensate amb temp vs max temp
                        if (maxTemp > 21 && amb_temp>35) {
                            //summer caliberation, when amb temp goes above 30 degree cel
                            if ((amb_temp - maxTemp) >= convergePoint) {
                                double stepUp = (amb_temp - maxTemp) / convergePoint;
                                maxTemp = (27.50 + stepUp);
                            }
                        }else if (maxTemp<27 && amb_temp<23){
                            //winter caliberation, when amb temp goes below 23 degree cel
                            double diff = (maxTemp-amb_temp);
                            if (diff>0){
                                maxTemp = maxTemp+diff;
                                if (maxTemp<27){
                                    maxTemp = maxTemp * CaraManager.getInstance().getScaleTemp();
                                }
                            }
                        }

                        //correction for amb temp
                            /*if((maxTemp<amb_temp) && amb_temp>30){
                                if (maxTemp<=29) {
                                    maxTemp = maxTemp + 1.00;
                                }
                            }*/


                        if (maxTemp < 31 && maxTemp > 22) {
                            maxTemp = maxTemp * CaraManager.getInstance().getScaleTemp();
                            // Log.d("OTP16", "scaling applied for " + maxTemp + "," + CaraManager.getInstance().getScaleTemp());
                        } else if (maxTemp >= 31) {
                            maxTemp = maxTemp * (float) (CaraManager.getInstance().getScaleTemp() - 0.1);
                            //Log.d("OTP16", "scaling applied for " + maxTemp + "," + (CaraManager.getInstance().getScaleTemp() - 0.1));
                        }

                    }

                    //double degF = (maxTemp*1.8)+32;
                    mat.clear();

                    //Log.d("OTP16","> "+maxTemp+", in  DegF->"+degF);

                    //Log.d("Result ","Row count -> "+row+" ,Sensor Temp -> "+ptat_temp/100+"°C Object Temp -> "+(actual_temp)/100+"°C ,"+"Total pixel ->"+dcnt);
                    //String ptv=ptat_temp/100 +"°C";
                    //String atr=actual_temp/100 +"°C";

                    CaraManager.getInstance().setUsbError(false);
                    return new ThermalModel(maxTemp, amb_temp, (maxTemp - amb_temp));
                }
            }
        } catch (Exception er) {
            Utility.printStack(er);
        }
        CaraManager.getInstance().setUsbError(true);
        return new ThermalModel(DEFAULT_TEMP, DEFAULT_TEMP, DEFAULT_TEMP);
    }

    public boolean isReadyForCapture() {
        return isReadyForCapture;
    }

    public void setReadyForCapture(boolean readyForCapture) {
        isReadyForCapture = readyForCapture;
    }

    /**
     * @param totalPasses - total number of read cycles
     * @return ThermalModel object
     */
    public ThermalModel getThermalTemp(int totalPasses) {

        // Log.d("Thermal","is gate command -> "+CaraManager.getInstance().isGateCommand());
        if (isThermal() && !CaraManager.getInstance().isGateCommand()) {
            try {
                double finalTemp = 0;
                double ambTemp = 0;
                for (int i = 0; i < totalPasses; i++) {
                    ThermalModel thermalModel = readTemperature();
                    finalTemp = finalTemp + thermalModel.getHeat();
                    //finalTemp =  thermalModel.getHeat();
                    ambTemp = ambTemp + thermalModel.getRefHeat();
                    //Thread.sleep(300);
                }

                finalTemp = (finalTemp / totalPasses);
                ambTemp = ambTemp / totalPasses;

                //Log.d("Temp","Before->"+finalTemp+", "+ambTemp);

                /*if (finalTemp<36 && finalTemp>28){
                    finalTemp = 36 + (finalTemp % 1.0f);
                }*/

                //Log.d("Temp","After->"+finalTemp+", "+ambTemp);

                return new ThermalModel(finalTemp, ambTemp, finalTemp - ambTemp);

            } catch (Exception er) {
                Utility.printStack(er);
            }
        }

        return new ThermalModel(DEFAULT_TEMP, DEFAULT_TEMP, DEFAULT_TEMP);
    }

    public void setFadeAnimation(View view) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(300);

        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(300);
        fadeOut.setDuration(300);

        view.startAnimation(fadeOut);
        view.startAnimation(fadeIn);
    }

    private String getMacAddr() {
        try {
            List<NetworkInterface> all = Collections.list(NetworkInterface.getNetworkInterfaces());
            for (NetworkInterface nif : all) {
                if (!nif.getName().equalsIgnoreCase("wlan0")) continue;

                byte[] macBytes = nif.getHardwareAddress();
                if (macBytes == null) {
                    return "";
                }

                StringBuilder res1 = new StringBuilder();
                for (byte b : macBytes) {
                    res1.append(String.format("%02X:", b));
                }

                if (res1.length() > 0) {
                    res1.deleteCharAt(res1.length() - 1);
                }
                return res1.toString();
            }
        } catch (Exception ex) { }

        String settings = getSharedPreferences().getString("settings", "");
        if (!settings.equals("")) {
            try {
                JSONObject data = new JSONObject(settings);
                return data.getString(Helper.SETTING_MAC);
            } catch (Exception er) { }
        }
        return "02:00:00:00:00:00";
    }

    public void enrollOnline(Context context, JSONObject data, HelperCallback helperCallback) {
        //Utilities.writeToFile(byteImg, System.currentTimeMillis() + "_enroll.jpg");
        try {
            Uri.Builder uribuilder = Helper.getBaseUri("");
            uribuilder.appendPath("postapi");
            uribuilder.appendPath("api");
            uribuilder.appendPath("faceEnroll");
            String enrollapi = uribuilder.build().toString();
            new Helper().networkRequst(context, Helper.POST_REQUEST, Helper.urlSchme, enrollapi, data.toString(), helperCallback);
        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

    private void ackActionCall(Context context, String responce, String action) {
        Uri.Builder uribuilder = Helper.getBaseUri("postapi");
        uribuilder.appendPath("api");
        uribuilder.appendPath("Actionapi");
        uribuilder.appendQueryParameter("apikey", getAPI_KEY());
        uribuilder.appendQueryParameter("action", action);
        uribuilder.appendQueryParameter("src", "cara");
        uribuilder.appendQueryParameter("ack", "true");
        uribuilder.appendQueryParameter("macid", getDEVICE_MACID());
        uribuilder.appendQueryParameter("ver", BuildConfig.VERSION_NAME + "");

        String ActionApi = uribuilder.build().toString();
        new Helper().networkRequst(context, "POST", Helper.urlSchme, ActionApi, responce, new HelperCallback() {
            @Override
            public void onResult(String result) {
                Log.d("ackActionCall", result);
            }

            @Override
            public void onError(String result) {
            }
        });
    }

    private void processPendingCmd(Context context,ArrayList<String> pendingCommands){

        if (isCommand()){
            return;
        }
        //Log.d(TAGS,"into process pending cmd "+pendingCommands.size());
        if (pendingCommands.size()>0) {
            executeAction(pendingCommands.get(0), context, new HelperCallback() {
                @Override
                public void onResult(String result) {
                        pendingCommands.remove(0);
                        if (pendingCommands.size()>0){
                            //Log.d(TAGS,"into onResult of executeAction "+pendingCommands.size());
                            setCommand(false);
                            processPendingCmd(context,pendingCommands);
                        }
                }

                @Override
                public void onError(String result) {
                    //Log.d(TAGS,"into onError of executeAction "+result);
                }
            });
        }
    }

    public void checkCommands(Context context){

        //Log.d(TAGS,"into check commands "+isCommand());
        if (isCommand()){
            return;
        }
        Uri.Builder uribuilder = Helper.getBaseUri("BS_REST");
        uribuilder.appendPath("checkCommands");
        uribuilder.appendQueryParameter("apikey", getAPI_KEY());
        uribuilder.appendQueryParameter("src","cara");
        uribuilder.appendQueryParameter("ack", "false");
        uribuilder.appendQueryParameter("macid", getDEVICE_MACID());
        uribuilder.appendQueryParameter("ver", BuildConfig.VERSION_CODE + "");
        pendingCommands.clear();

        String chkCommands = uribuilder.build().toString();
        //Log.d(TAGS,chkCommands);

        new Helper().networkRequst(context, "GET", Helper.urlSchme, chkCommands,
                new JSONObject().toString(), new HelperCallback() {
                    @Override
                    public void onResult(String result) {

                        Log.d(TAGS,result);
                        try {
                            if (result != null) {
                                JSONObject data = new JSONObject(result);
                                if (data.getString("ErrorString").equalsIgnoreCase("success")) {
                                    if (data.getString("Parcelpending").equalsIgnoreCase("true") || data.getString("Parcelpending").equalsIgnoreCase("1")) {
                                        JSONArray commands = data.getJSONArray("commands");
                                        if (commands != null) {
                                            if (commands.length() > 0) {

                                                for (int c = 0; c < commands.length(); c++) {
                                                    if (c<maxCommands) {
                                                        String action = commands.getJSONObject(c).getString("Action");
                                                        pendingCommands.add(action);
                                                    }else {
                                                        break;
                                                    }
                                                }

                                         processPendingCmd(context,pendingCommands);

                                     }

                                }

                            }

                        }

                    }
                }catch (Exception er){
                    Utility.printStack(er);
                }

            }

            @Override
            public void onError(String result) {

            }
        });

    }

    public void executeAction(String action, Context context, HelperCallback helperCallback) {

        if (CaraManager.getInstance().isCommand()){
            return;
        }

        //Log.d(TAGS,"into executeAction -> "+action);

        CaraManager.getInstance().setCommand(true);
        Uri.Builder uribuilder = Helper.getBaseUri("postapi");
        uribuilder.appendPath("api");
        uribuilder.appendPath("Actionapi");
        uribuilder.appendQueryParameter("apikey", getAPI_KEY());
        uribuilder.appendQueryParameter("action", action);
        uribuilder.appendQueryParameter("src", "cara");
        uribuilder.appendQueryParameter("ack", "false");
        uribuilder.appendQueryParameter("macid", getDEVICE_MACID());
        uribuilder.appendQueryParameter("ver", BuildConfig.VERSION_CODE + "");

        String ActionApi = uribuilder.build().toString();

        //Log.d(TAGS,ActionApi);

        new Helper().networkRequst(context, "POST", Helper.urlSchme, ActionApi, new JSONObject().toString(), new HelperCallback() {
            @Override
            public void onResult(String result) {

                try {
                    //Log.d(TAGS,"into result -> "+result);
                    String isSucess="error";
                    String errorMsg="Verification failed !";
                    int errorCode=0;
                    JSONObject data = new JSONObject(result);
                    if (data.getString("errorstring").equalsIgnoreCase("success")) {

                        String datatype = data.getString("datatype");
                        JSONArray responce = new JSONArray();

                        if (action.equalsIgnoreCase(Helper.COMMAND_ADD)) {
                            JSONArray templateData = data.getJSONArray("data");
                            if (templateData != null) {
                                FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(context));
                                if (datatype.equals("template")) {
                                    //add template to sdk
                                    //Log.d("Duplicate","Total users -> "+templateData.length()+"");
                                    for (int t = 0; t < templateData.length(); t++) {
                                        JSONObject user = templateData.getJSONObject(t);
                                        String empid = user.getString("empid");
                                        String name = user.getString("name");


                                        byte[] template = Utility.decodeBase64(user.getString("temp"));
                                        if(!user.getString("temp").equals("") && !empid.equals("") && !name.equals("") && template!=null) {
                                           // Log.d("Duplicate","Adding user ->"+empid+", Hash->"+createMd5(template));
                                            String outmsg=FaceManager.getInstance().checkDuplicate(template);
                                            if (outmsg.equals("success")) {
                                                boolean isSuccess = FaceManager.getInstance().enrollUserFromTemplate(context, empid, template);
                                                if (isSuccess) {
                                                    CaraManager.getInstance().getCaraDb().insertUsers(empid, name);
                                                    responce.put(new JSONObject().put("empid",
                                                            empid).put("errorstring", isSuccess ? "success" : "error").put("msg",
                                                            "Added with no error").put("ErrorCode",ErrorCodes.ERROR_ADD_REMOVE_SUCCESS+""));
                                                }else{
                                                    //Log.d("Duplicate","Error in add user"+empid);
                                                    responce.put(new JSONObject().put("empid",
                                                            empid).put("errorstring",
                                                            "error").put("msg",
                                                            "Error in template data or invalid user id").put("ErrorCode",ErrorCodes.ERROR_INVALID_USER_ID));
                                                }
                                            }else{
                                                //Log.d("Duplicate","duplicate id -> "+outmsg+" for ->"+empid);
                                                responce.put(new JSONObject().put("empid",
                                                        empid).put("errorstring",
                                                        "error").put("msg",
                                                        outmsg).put("ErrorCode",ErrorCodes.ERROR_DUPLICATE_TEMP+""));
                                            }
                                        } else {
                                            //Log.d("Duplicate","Error in add user"+empid);
                                            responce.put(new JSONObject().put("empid",
                                                    empid).put("errorstring",
                                                    "error").put("msg",
                                                    "Error in template data or invalid user id").put("ErrorCode",ErrorCodes.ERROR_INVALID_USER_ID));
                                        }
                                    }

                                } else if (data.getString("datatype").equals("img")) {

                                }
                            }
                        } else if (action.equalsIgnoreCase(Helper.COMMAND_REMOVE)) {
                            JSONArray removeData = data.getJSONArray("data");
                            if (removeData != null) {
                                FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(context));
                                for (int t = 0; t < removeData.length(); t++) {
                                    String empid = removeData.getJSONObject(t).getString("empid");
                                    if (empid != null) {
                                        if (!empid.equals("")) {
                                            boolean isSuccess = FaceManager.getInstance().removeUserById(empid);
                                            responce.put(new JSONObject().put("empid",
                                                    empid).put("errorstring",
                                                    isSuccess ? "success" : "error").put("msg",isSuccess ? "Remove success for "+empid : "Error in remove "+empid).put("ErrorCode",isSuccess ? ErrorCodes.ERROR_ADD_REMOVE_SUCCESS+"" : ErrorCodes.ERROR_NOT_FOUND+""));
                                            if (isSuccess) {
                                                CaraManager.getInstance().getCaraDb().remUserByIdDb(empid);
                                            }
                                        }else{
                                            responce.put(new JSONObject().put("empid",
                                                    empid).put("errorstring",
                                                    "error").put("msg","Invalid user id").put("ErrorCode",ErrorCodes.ERROR_INVALID_USER_ID+""));
                                        }
                                    }else {
                                        responce.put(new JSONObject().put("empid",
                                                empid).put("errorstring",
                                                "error").put("msg","Invalid user id").put("ErrorCode",ErrorCodes.ERROR_INVALID_USER_ID+""));
                                    }
                                }
                            }
                        } else if (action.equalsIgnoreCase(Helper.COMMAND_APP_UPDATE)){

                            responce.put(new JSONObject().put("data", getHealth()).put("errorstring", "success"));
                            context.sendBroadcast(new Intent("CMD_INSTALL").putExtra("appName",
                                    context.getPackageName()).putExtra("url",data.getString("apikey")));

                        } else if (action.equalsIgnoreCase(Helper.COMMAND_HEALTH)) {
                            responce.put(new JSONObject().put("data", getHealth()).put("errorstring", "success"));
                        } else if (action.equalsIgnoreCase(Helper.COMMAND_REBOOT)) {
                            responce.put(new JSONObject().put("data", getHealth()).put("errorstring", "success"));
                        } else if(action.equalsIgnoreCase(Helper.COMMAND_UPDATE_SETTINGS)){

                            boolean isSuccess=false;
                            try {
                                JSONObject settings = data.getJSONObject("jdata");
                                if (settings != null) {
                                    if (!settings.equals("")) {
                                        if (Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "settings", settings.toString())) {
                                            isSuccess = true;
                                            responce.put(new JSONObject().put("data", getHealth()).put("errorstring", "success"));
                                        }
                                    }
                                }
                                updateSettings(context);
                            }catch (Exception er){}

                            if (!isSuccess){
                                responce.put(new JSONObject().put("data", getHealth()).put("errorstring", "error"));
                            }

                        }else if (action.equalsIgnoreCase(Helper.COMMAND_IMG_UPLOAD)) {
                            responce = getLocalImageData(responce);
                        } else if (action.equalsIgnoreCase(Helper.COMMAND_FLUSH_TEMP)) {
                            if (CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS) > 0) {
                                if (FaceManager.getInstance().getLocalCacheManager() == null)
                                    FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(context));

                                FaceManager.getInstance().getLocalCacheManager().deleteAllFaceRecords();
                                CaraManager.getInstance().getCaraDb().flushUsers();
                            }
                            responce.put(new JSONObject().put("data", "").put("errorstring", "success"));
                        } else if (action.equalsIgnoreCase(Helper.COMMAND_VERIFY_DNS)){
                            String apikey = data.getString("apikey");

                            if (apikey!=null && !apikey.equals("")) {
                                //Log.d("SERVER_DNS",apikey);
                                String decoded_data = CaraManager.getInstance().decryptData(apikey);
                                //Log.d("SERVER_DNS",decoded_data);

                                if (decoded_data!=null && decoded_data.length()>0){
                                    //isSucess="error";
                                    String[] keyData=decoded_data.split(",");
                                    if(keyData.length>0){
                                        String[] expDate=keyData[1].split("-");
                                        if (expDate.length==3){
                                            String date=expDate[2]+"-"+expDate[1]+"-"+expDate[0]+" 23:59";
                                            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
                                            long expDateTimeInMili = sdf.parse(date).getTime();
                                            if (expDateTimeInMili>Calendar.getInstance().getTimeInMillis()){
                                                isSucess="success";
                                                errorMsg="success";
                                                errorCode=0;
                                            }else {
                                                errorMsg="Device license expired on "+date;
                                                errorCode=-3;
                                            }
                                        }else {
                                            errorMsg="Invalid expiry date";
                                            errorCode=-2;
                                        }
                                    }else {
                                        errorMsg="Invalid license data received";
                                        errorCode=-1;
                                    }
                                }else {
                                    errorMsg="Invalid license data received";
                                    errorCode=-1;
                                }

                                responce.put(new JSONObject().put("data",
                                        Helper.SERVER_DNS).put("errorstring",
                                        isSucess).put("msg",
                                        isSucess.equals("success") ? "Verification success !" : errorMsg).put("ErrorCode",errorCode+""));

                            }


                            if (errorCode!=0) {
                                removeDns(finalDns, context); //added this feature due to api issues at alif
                            }

                        } else if (action.equalsIgnoreCase(Helper.COMMAND_GET_USERS)){
                            JSONArray users=new JSONArray();
                            Log.d("cara_logs","into "+Helper.COMMAND_GET_USERS);
                            for (String key : CaraManager.getInstance().getCaraUsers().keySet()) {
                                Log.d("cara_logs",key);
                                users.put(new JSONObject().put("empid", key).put("errorstring", "success"));
                                //usersModels.add(new UsersModel(CaraManager.getInstance().getCaraUsers().get(key),
                                        //key));
                            }
                            if (users.length()==0){
                                if (!Listener.isSDKInitialized) {
                                    users.put(new JSONObject().put("empid","").put("errorstring","error").put("msg","face sdk not init."));
                                }else{
                                    users.put(new JSONObject().put("empid","").put("errorstring","error").put("msg","No users found"));
                                }
                            }
                            responce=users;
                            //Log.d("TAG",responce+"");
                        }

                        //Log.d("cara_logs","command received "+action);

                        // Utilities.enrollFromCache(context);
                        //called to send feedback to etam
                        if (responce.length() > 0) {
                             //Log.d("TAG", "ack send ->> "+responce.length()+", "+responce.toString());
                            //Utility.writeToFile(responce.toString(),"commands_data.txt",false);
                            ackActionCall(context, responce.toString(), action);
                            if (action.equalsIgnoreCase(Helper.COMMAND_REBOOT)) {
                                context.sendBroadcast(new Intent("CMD_REBOOT").putExtra("access", context.getPackageName()));
                            }
                        }else{
                            ackActionCall(context, responce.toString(), action);
                        }

                    }else if (action.equalsIgnoreCase(Helper.COMMAND_VERIFY_DNS)){
                        errorMsg=data.getString("errorstring");
                        errorCode=-1;
                    }

                    if (action.equalsIgnoreCase(Helper.COMMAND_VERIFY_DNS)) {
                        helperCallback.onResult(errorMsg+"");
                        if (errorCode!=0) {
                            //removeDns(finalDns, context); //disabled this feature due to api issues at alif
                        }
                        //Log.d("DNS","callback called");
                    }else {
                        helperCallback.onResult("FINISH");
                    }
                    CaraManager.getInstance().setCommand(false);

                } catch (Exception er) {
                    helperCallback.onError(er.getMessage());
                    CaraManager.getInstance().setCommand(false);
                    Utility.printStack(er);
                }
            }

            @Override
            public void onError(String result) {
                Log.d(TAGS,result);
                CaraManager.getInstance().setCommand(false);
                helperCallback.onError(result);
            }

        });

    }

    public void updateSettings(Context context){
        String settings= CaraManager.getInstance().getSharedPreferences().getString("settings","");
        if(settings.equals("")){
            try {
                JSONObject saveData = new JSONObject();
                saveData.put("uuid", Utility.getUUID(context.getApplicationContext()));
                saveData.put("ver", BuildConfig.VERSION_NAME);
                saveData.put("version_release",BuildConfig.VERSION_CODE+","+BuildConfig.BUILD_TIME);
                saveData.put(Helper.SETTING_LATCH_MAC,"");
                saveData.put(Helper.SETTING_PRINTER_MAC_ID,"");
                saveData.put(Helper.SETTING_MAC, CaraManager.getInstance().getDEVICE_MACID());
                saveData.put(Helper.SETTING_DNS, Helper.SERVER_DNS);
                saveData.put(Helper.SETTING_ACCESS_CONTROL,"false");
                saveData.put(Helper.SETTING_IS_PRINTER,"false");
                saveData.put(Helper.SETTING_AUDIO,"false");
                saveData.put(Helper.SETTING_IS_ACTIVE,Helper.TRUE);
                saveData.put(Helper.SETTING_ACCESS_TIMEOUT,"8");
                saveData.put(Helper.SETTING_PHOTO_TIMEOUT,"4");
                saveData.put(Helper.SETTING_CAMERA,FaceManager.getInstance().getCameraId()+"");
                saveData.put(Helper.SETTING_LANG,"0,true");
                saveData.put(Helper.SETTING_LATCHTYPE, CaraManager.getInstance().getLatchComType());
                saveData.put(Helper.SETTING_PRINTER_TYPE,"");
                saveData.put(Helper.SETTING_THERMAL,"false");
                saveData.put(Helper.SETTING_INMODE,"false");
                saveData.put(Helper.SETTING_OUTMODE,"false");
                saveData.put(Helper.SETTING_REBOOT_HHMM,"03:00");
                saveData.put(Helper.SETTING_ISREBOOT,"false");
                saveData.put(Helper.SETTING_ISQUALITY,"false");
                saveData.put(Helper.SETTING_AUTOFLASH,"false");
                saveData.put(Helper.SETTING_SCALE_TEMP,"1.18");
                saveData.put(Helper.SETTING_PLAYALARM,"false");
                saveData.put(Helper.SETTING_MAXTEMP,"37.5");
                saveData.put(Helper.SETTING_ISDEGREE,"F");
                saveData.put(Helper.SETTING_ISLIVENESS,Helper.TRUE);
                saveData.put(Helper.SETTING_ISCENTER,Helper.TRUE);
                saveData.put(Helper.SETTING_URLTYPE,"https");
                saveData.put(Helper.SETTING_FACE_THR,"6.0");
                saveData.put(Helper.SETTING_MINTEMP,CaraManager.getInstance().getMinTempVal()+"");
                saveData.put(Helper.SETTING_LIVENESS_THR,CaraManager.getInstance().getLiveness_thr()+"");

                Helper.CAMERA_CAPTURE_TIME=4;
                Helper.TIMEOUT_ACTIVITY=8;
                Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(),"settings",saveData.toString());
            }catch (Exception er){
                Utility.printStack(er);
            }
        }else{
            try{
                JSONObject jdata = new JSONObject(settings);
                if (jdata.getString(Helper.SETTING_PHOTO_TIMEOUT)!=null) {
                    Helper.CAMERA_CAPTURE_TIME = Integer.valueOf(jdata.getString(Helper.SETTING_PHOTO_TIMEOUT));
                    Helper.TIMEOUT_ACTIVITY = Integer.valueOf(jdata.getString(Helper.SETTING_ACCESS_TIMEOUT));
                    CaraManager.getInstance().setDOOR_TIMEOUT(Helper.TIMEOUT_ACTIVITY);
                }

                if (jdata.getString(Helper.SETTING_ACCESS_CONTROL).equals(Helper.TRUE)) {
                    try{
                        //Log.d("usb",CaraManager.getInstance().getLatchComType()+"");
                        CaraManager.getInstance().setLatchEnable(true);
                        if (!CaraManager.getInstance().getLatchComType().equals("2")) {
                            //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
                            BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();                                         //BluetoothAdapter controls the Bluetooth radio in the phone
                            //mBluetoothAdapter = bluetoothManager.getAdapter();
                            if (!mBluetoothAdapter.isEnabled()) {                                           //Check if BT is not enabled
                                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
                                //startActivity(enableBtIntent);                  //Fire the intent to start the activity that will return a result based on user response
                                mBluetoothAdapter.enable();
                            } else {
                                Intent bleservice = new Intent(context, BleService.class);
                                if (!CaraManager.getInstance().isLatchConnected()) {
                                    context.stopService(bleservice);
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            context.startService(bleservice);
                                        }
                                    }, 1500);
                                }
                            }
                        }else{
                            //usb latch type
                            if(UsbLatchConnector.getInstance().getMcp2221()==null){
                                if (CaraManager.getInstance().isLatchEnable() || CaraManager.getInstance().isThermal()) {
                                    //if (UsbLatchConnector.getInstance().getMcp2221()==null)
                                       // UsbLatchConnector.getInstance().startUsb(context);
                                }
                            }
                        }

                    }catch (Exception er){
                        Utility.printStack(er);
                    }
                }else{
                    CaraManager.getInstance().setLatchConnected(false);
                    CaraManager.getInstance().setLatchEnable(false);
                }

                if (jdata.getString(Helper.SETTING_IS_PRINTER).equals(Helper.TRUE)){
                    if (!isMyServiceRunning(PrintService.class,context)){
                        context.startService(new Intent(context,PrintService.class));
                    }
                }

                if (jdata.getString(Helper.SETTING_LANG)!=""){
                    String lang[]=jdata.getString(Helper.SETTING_LANG).split(",");
                    Helper.setAuralLang(lang[0]);
                    if (lang[1].equals("true")) {
                        Helper.setVoiceOverOn(true);
                    }else{
                        Helper.setVoiceOverOn(false);
                    }
                }

                if (jdata.getString(Helper.SETTING_DNS)!=null){
                    Helper.SERVER_DNS=jdata.getString(Helper.SETTING_DNS);
                    CaraManager.getInstance().addWhiteDns(Helper.SERVER_DNS);
                }

                if (jdata.getString(Helper.SETTING_THERMAL)!=null){
                    if (jdata.getString(Helper.SETTING_THERMAL).equals(Helper.TRUE)){
                        CaraManager.getInstance().setThermal(true);
                        //if(UsbLatchConnector.getInstance().getMcp2221()==null)
                           // UsbLatchConnector.getInstance().startUsb(context);
                    }else {
                        CaraManager.getInstance().setThermal(false);
                    }
                }else{
                    CaraManager.getInstance().setThermal(false);
                }

                CaraManager.getInstance().setInoutMode("-1");

                if (jdata.getString(Helper.SETTING_INMODE)!=null){
                    if (jdata.getString(Helper.SETTING_INMODE).equalsIgnoreCase(Helper.TRUE)){
                        CaraManager.getInstance().setInoutMode("1");
                    }
                }

                if (jdata.getString(Helper.SETTING_OUTMODE)!=null){
                    if (jdata.getString(Helper.SETTING_OUTMODE).equalsIgnoreCase(Helper.TRUE)){
                        CaraManager.getInstance().setInoutMode("0");
                    }
                }

                if (jdata.getString(Helper.SETTING_ISREBOOT)!=null) {
                    CaraManager.getInstance().setReboot(jdata.getString(Helper.SETTING_ISREBOOT).equals(Helper.TRUE));
                    CaraManager.getInstance().setRebootTime(jdata.getString(Helper.SETTING_REBOOT_HHMM));
                }else{
                    CaraManager.getInstance().setReboot(false);
                }

                if (jdata.getString(Helper.SETTING_ISQUALITY)!=null){
                    CaraManager.getInstance().setQuality(jdata.getBoolean(Helper.SETTING_ISQUALITY));
                }else{
                    CaraManager.getInstance().setQuality(false);
                }

                if (jdata.getString(Helper.SETTING_AUTOFLASH)!=null){
                    CaraManager.getInstance().setAutoFlash(jdata.getBoolean(Helper.SETTING_AUTOFLASH));
                }else{
                    CaraManager.getInstance().setAutoFlash(false);
                }


                if (jdata.getString(Helper.SETTING_ISDEGREE)!=null){
                    CaraManager.getInstance().setTempUnit(jdata.getString(Helper.SETTING_ISDEGREE).equals(Helper.TRUE)?"C":"F");
                }else{
                    CaraManager.getInstance().setTempUnit("F");
                }

                if (jdata.getString(Helper.SETTING_PLAYALARM)!=null){
                    CaraManager.getInstance().setAlarm(jdata.getString(Helper.SETTING_PLAYALARM).equals(Helper.TRUE));
                }else{
                    CaraManager.getInstance().setAlarm(false);
                }

                if (jdata.getString(Helper.SETTING_SCALE_TEMP)!=null){
                    if (!jdata.getString(Helper.SETTING_SCALE_TEMP).equals(""))
                        CaraManager.getInstance().setScaleTemp(Float.parseFloat(jdata.getString(Helper.SETTING_SCALE_TEMP)));
                }

                if (jdata.getString(Helper.SETTING_MAXTEMP)!=null){
                    if (!jdata.getString(Helper.SETTING_MAXTEMP).equals(""))
                        CaraManager.getInstance().setMaxTemp(Float.parseFloat(jdata.getString(Helper.SETTING_MAXTEMP)));
                }

                try {
                    if (jdata.getString(Helper.SETTING_ISLIVENESS) != null) {
                        if (!jdata.getString(Helper.SETTING_ISLIVENESS).equals(""))
                            CaraManager.getInstance().setLiveness(jdata.getString(Helper.SETTING_ISLIVENESS).equals(Helper.TRUE));
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setLiveness(true);
                }

                try{
                    if (jdata.getString(Helper.SETTING_ISCENTER) != null) {
                        if (!jdata.getString(Helper.SETTING_ISCENTER).equals(""))
                            CaraManager.getInstance().setReadCenter(jdata.getString(Helper.SETTING_ISCENTER).equals(Helper.TRUE));
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setReadCenter(true);
                }

                try{

                    if (jdata.getString(Helper.SETTING_URLTYPE)!=null){
                        if (!jdata.getString(Helper.SETTING_URLTYPE).equals("")){
                            Helper.urlSchme = jdata.getString(Helper.SETTING_URLTYPE).equalsIgnoreCase("http")? "http" : "https";
                        }
                    }

                }catch (Exception er){
                    Helper.urlSchme="https";
                }

                try{
                    if (jdata.getString(Helper.SETTING_CAMERA)!=null){
                        if (!jdata.getString(Helper.SETTING_CAMERA).equals("")){
                            FaceManager.getInstance().setCameraId(Integer.parseInt(jdata.getString(Helper.SETTING_CAMERA)));
                        }
                    }
                }catch (Exception er){
                    FaceManager.getInstance().setCameraId(CameraCharacteristics.LENS_FACING_FRONT);
                }

                try{
                    if (jdata.getString(Helper.SETTING_FACE_THR)!=null){
                        if (!jdata.getString(Helper.SETTING_FACE_THR).equals("")){
                            FaceManager.getInstance().setFaceTheshold(Float.valueOf(jdata.getString(Helper.SETTING_FACE_THR)));
                        }else {
                            FaceManager.getInstance().setFaceTheshold(CaraManager.getInstance().getDefaultFaceThr());
                        }
                    }else{
                        FaceManager.getInstance().setFaceTheshold(CaraManager.getInstance().getDefaultFaceThr());
                    }
                }catch (Exception er){
                    FaceManager.getInstance().setFaceTheshold(CaraManager.getInstance().getDefaultFaceThr());
                }


                try {
                    if (jdata.getString(Helper.SETTING_MINTEMP) != null) {
                        if (!jdata.getString(Helper.SETTING_MINTEMP).equals("")) {
                            String minTempVal = jdata.getString(Helper.SETTING_MINTEMP);
                            if (minTempVal != null) {
                                CaraManager.getInstance().setMinTempVal(Float.parseFloat(minTempVal.trim()));
                            } else {
                                CaraManager.getInstance().setMinTempVal(31.0f);
                            }
                        } else {
                            CaraManager.getInstance().setMinTempVal(31.0f);
                        }
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setMinTempVal(31.0f);
                }

                try {
                    if (jdata.getString(Helper.SETTING_SHIFT_TIME) != null) {
                        if (!jdata.getString(Helper.SETTING_SHIFT_TIME).equals("")) {
                            CaraManager.getInstance().setShifttime(jdata.getString(Helper.SETTING_SHIFT_TIME));
                        }
                    }else {
                        CaraManager.getInstance().setShifttime("20");
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setShifttime("20");
                }

                try {
                    if (jdata.getString(Helper.SETTING_RESET_TIME) != null) {
                        if (!jdata.getString(Helper.SETTING_RESET_TIME).equals("")) {
                            CaraManager.setResetTime(jdata.getString(Helper.SETTING_RESET_TIME));
                        }
                    }else {
                        CaraManager.setResetTime("23:59");
                    }
                }catch (Exception er){
                    CaraManager.setResetTime("23:59");
                }

                try {
                    if (jdata.getString(Helper.SETTING_FLIP_TIMEOUT) != null) {
                        if (!jdata.getString(Helper.SETTING_FLIP_TIMEOUT).equals("")) {
                            CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(
                                    Long.valueOf(jdata.getString(Helper.SETTING_FLIP_TIMEOUT))*1000);
                        }
                    }else {
                        CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(60*1000);
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(60*1000);
                }

                try {
                    if (jdata.getString(Helper.SETTING_ENROL_QUALITY) != null) {
                        if (!jdata.getString(Helper.SETTING_ENROL_QUALITY).equals("")) {
                            CaraManager.getInstance().setEnrol_quality(Float.valueOf(jdata.getString(Helper.SETTING_ENROL_QUALITY)));
                        }
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setEnrol_quality(20.0f);
                }


                try {
                    if (jdata.getString(Helper.SETTING_LIVENESS_THR) != null) {
                        if (!jdata.getString(Helper.SETTING_LIVENESS_THR).equals("")) {
                            CaraManager.getInstance().setLiveness_thr(Float.valueOf(jdata.getString(Helper.SETTING_LIVENESS_THR)));
                        }
                    }
                }catch (Exception er){
                    CaraManager.getInstance().setLiveness_thr(0.80f);
                }

                try{
                    String dnsvalue= Helper.getBaseUri("").build().toString();

                    if (!dnsvalue.equals("")){
                        dnsvalue=dnsvalue.replaceAll("https://","");
                        dnsvalue=dnsvalue.replaceAll("http://","");
                    }

                    //Log.d("SERVER_DNS","input value ->"+dnsvalue);

                    if (!dnsvalue.equals("")){
                        for (DnsModel wt_dns:CaraManager.getInstance().getWhiteListDns()) {
                            if (wt_dns.getDns().equalsIgnoreCase(dnsvalue)){
                                if (wt_dns.getDnstype().equalsIgnoreCase("oem")) {
                                    CaraManager.getInstance().setImgLog(true);
                                }else {
                                    CaraManager.getInstance().setImgLog(false);
                                }
                            }
                        }
                    }

                }catch (Exception er){}
            }catch (Exception er){
                Utility.printStack(er);
            }
        }

    }

    private boolean isMyServiceRunning(Class<?> serviceClass,Context context) {
        ActivityManager manager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
        for (ActivityManager.RunningServiceInfo service : manager.getRunningServices(Integer.MAX_VALUE)) {
            if (serviceClass.getName().equals(service.service.getClassName())) {
                return true;
            }
        }
        return false;
    }


    public JSONArray getLocalImageData(final JSONArray responce) {
        try {
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "CaraFaces";
            File root = new File(rootPath);
            if (root.isDirectory() && root.listFiles().length > 0) {
                for (File file : root.listFiles()) {

                    if (file.getName().substring(file.getName().lastIndexOf(".")).equalsIgnoreCase(".dat")) {
                        //Log.d("fileExt",file.getName().substring(file.getName().lastIndexOf(".dat")));
                        String empid = file.getName().split("_")[0];
                        byte[] imgdata = Utilities.scaleDown(Utilities.readfileFrom(file), 150, 150, false);
                        if (imgdata != null && empid != null && responce!=null) {
                            responce.put(new JSONObject().put("data",
                                    Utility.encodeBase64(imgdata)).put("errorstring",
                                    "success").put("empid", empid));
                        }
                    }

                }
            }
        } catch (Exception er) {
            Utility.printStack(er);
        }
        return responce;
    }

    public void setupLoader(Activity activity, String message) {
        if (progressDialog == null)
            progressDialog = new ProgressDialog(activity, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
        // Set progress dialog style spinner
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // Set the progress dialog title and message
        //pd.setTitle("Title of progress dialog.");
        progressDialog.setMessage(message);
        // Set the progress dialog background color
        //pd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFD4D9D0")));
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
    }

    public String getHealth() {
        try {
            JSONObject data = new JSONObject();
            data.put("MAC", getDEVICE_MACID());
            data.put("Version", (BuildConfig.VERSION_CODE) + "");
            String settings = getSharedPreferences().getString("settings", "");
            if (!settings.equals("")) {
                try {

                    JSONObject setting = new JSONObject(settings);
                    setting.put("users", CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS) + "");
                    setting.put("update_time", getLastUpdate());
                    setting.put("device_rec", CaraManager.getInstance().getCaraDb().getDeviceRecords() + "");
                    setting.put("device_pen", CaraManager.getInstance().getCaraDb().getPendingRecords() + "");
                    setting.put("version_type", CaraManager.getInstance().isLicVersion() ? "lic" : "unlic");
                    setting.put("last_reboot", getLastReboot());

                    if (CaraManager.getInstance().isThermal()) {
                        setting.put("thermal_center", CaraManager.getInstance().getThermalTemp(1).getHeat() + "");
                        setting.put("thermal_ref", CaraManager.getInstance().getThermalTemp(1).getRefHeat() + "");
                    }

                    data.put("Settings", settings);
                    return data.toString();

                } catch (Exception er) { }
            } else {

                JSONObject setting = new JSONObject();
                setting.put("users", CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS) + "");
                setting.put("update_time", getLastUpdate());
                setting.put("device_rec", CaraManager.getInstance().getCaraDb().getDeviceRecords() + "");
                setting.put("device_pen", CaraManager.getInstance().getCaraDb().getPendingRecords() + "");
                setting.put("version_type", CaraManager.getInstance().isLicVersion() ? "lic" : "unlic");
                setting.put("last_reboot", getLastReboot());
                if (CaraManager.getInstance().isThermal()) {
                    setting.put("thermal_center", CaraManager.getInstance().getThermalTemp(1).getHeat() + "");
                    setting.put("thermal_ref", CaraManager.getInstance().getThermalTemp(1).getRefHeat() + "");
                }

                data.put("Settings", setting);
                return data.toString();
            }
        } catch (Exception er) { }
        return new JSONObject().toString();
    }

    public void reportHealth(Context context) {
        Uri.Builder uribuilder = Helper.getBaseUri("postapi");
        uribuilder.appendPath("api");
        uribuilder.appendPath("dv");
        String url = uribuilder.build().toString();
        new Helper().networkRequst(context.getApplicationContext(), "POST", Helper.urlSchme, url, getHealth(), new HelperCallback() {
            @Override
            public void onResult(String result) {
            }

            @Override
            public void onError(String result) {

            }
        });
    }

    private void processCmd(String action, Context context) {
        if (!CaraManager.getInstance().isCommand()) {
            //CaraManager.getInstance().showDialog("Device sync in progress...");
            executeAction(action, context, new HelperCallback() {
                @Override
                public void onResult(String result) {
                    CaraManager.getInstance().setCommand(false);
                    //CaraManager.getInstance().hideDialog();
                }

                @Override
                public void onError(String result) {
                    CaraManager.getInstance().setCommand(false);
                    //CaraManager.getInstance().hideDialog();
                }
            });
        }
    }

    public void sendAlert(Context context, String empId, String realTime,
                          String realDate, String inOutStatus, String heat,
                          String refHeat, String value_type, String alerttype) {
        try {

            Uri.Builder uribuilder = Helper.getBaseUri("BS_REST");
            uribuilder.appendPath("tempalert");
            uribuilder.appendQueryParameter("apikey", getAPI_KEY());
            uribuilder.appendQueryParameter("RealTime", realTime);
            uribuilder.appendQueryParameter("RealDate", realDate);
            uribuilder.appendQueryParameter("ptype", inOutStatus);
            uribuilder.appendQueryParameter("macid", getDEVICE_MACID());
            uribuilder.appendQueryParameter("heat", heat);
            uribuilder.appendQueryParameter("refHeat", refHeat);
            //uribuilder.appendQueryParameter("score", matchScore);
            uribuilder.appendQueryParameter("src", "cara");
            uribuilder.appendQueryParameter("empid", empId);
            //uribuilder.appendQueryParameter("timereq",timereq);
            uribuilder.appendQueryParameter("islic", CaraManager.getInstance().isLicVersion() + "");
            uribuilder.appendQueryParameter("ver", BuildConfig.VERSION_CODE + "");
            uribuilder.appendQueryParameter("alertType", alerttype);
            uribuilder.appendQueryParameter("valueType", value_type);

            String ALERT_API = uribuilder.build().toString();
            //pd.show();

            new Helper().networkRequst(context, Helper.GET_REQUEST, Helper.urlSchme, ALERT_API, "", new HelperCallback() {
                @Override
                public void onResult(String result) { }

                @Override
                public void onError(final String result) {
                }

            });
        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

    public void punchOnline(Context context,
                            String inOutStatus,
                            String empId, String realTime,
                            String realDate, String heat,
                            String matchScore, String txId, String timereq,String livenessTime,String refHeat) {
        try {

            Uri.Builder uribuilder = Helper.getBaseUri("BS_REST");
            uribuilder.appendPath("MT");
            uribuilder.appendQueryParameter("apikey", getAPI_KEY());
            uribuilder.appendQueryParameter("RealTime", realTime);
            uribuilder.appendQueryParameter("RealDate", realDate);
            uribuilder.appendQueryParameter("ptype", inOutStatus);
            uribuilder.appendQueryParameter("macid", getDEVICE_MACID());
            uribuilder.appendQueryParameter("heat", heat);
            uribuilder.appendQueryParameter("refHeat", refHeat);
            uribuilder.appendQueryParameter("score", matchScore);
            uribuilder.appendQueryParameter("src", "cara");
            uribuilder.appendQueryParameter("empid", empId);
            uribuilder.appendQueryParameter("timereq", timereq);
            uribuilder.appendQueryParameter("liveness_time",livenessTime);
            uribuilder.appendQueryParameter("islic", CaraManager.getInstance().isLicVersion() + "");
            uribuilder.appendQueryParameter("ver", BuildConfig.VERSION_CODE + "");

            String MTAPI = uribuilder.build().toString();

            new Helper().networkRequst(context, Helper.GET_REQUEST, Helper.urlSchme, MTAPI, "", new HelperCallback() {
                @Override
                public void onResult(String result) {
                    try {
                        JSONObject output = new JSONObject(result);
                        final String errorString = output.getString("ErrorString");
                        if (errorString.equalsIgnoreCase("Success")) {
                            //online marked success
                            CaraManager.getInstance().getCaraDb().updateAttendance(txId, MARK_ONLINE);

                            if (output.getString("Parcelpending").equalsIgnoreCase("true")) {
                                String action = output.getString("Action");
                                //Log.d("MT_API", " command received " + action);
                                if (!action.equals("")) {
                                    processCmd(action, context);
                                } else {
                                    CaraManager.getInstance().setCommand(false);
                                }

                            } else {
                                CaraManager.getInstance().setCommand(false);
                            }

                        } else {
                            CaraManager.getInstance().setCommand(false);
                            //mark offline
                            //Toast.makeText(context,"Error in online update !",Toast.LENGTH_SHORT).show();
                        }
                    } catch (Exception er) {
                        CaraManager.getInstance().setCommand(false);
                        Utility.printStack(er);
                    }
                }

                @Override
                public void onError(final String result) {
                    //mark offline
                    //Log.d("TAG", result);
                    CaraManager.getInstance().setCommand(false);
                    //Toast.makeText(context,result,Toast.LENGTH_SHORT).show();
                }

            });
        } catch (Exception er) {
            CaraManager.getInstance().setCommand(false);
            Utility.printStack(er);
        }

    }

    public long getPastMiliSeconds(int days) {
        return (System.currentTimeMillis() - (days * CaraManager.miliSecondsInDay));
    }

    public synchronized void uploadOfflinePunches(Context context) {

        if (CaraManager.getInstance().getCaraDb().getPendingRecords() == 0) {
            CaraManager.getInstance().getCaraDb().flushRecordsByLimit(getPastMiliSeconds(62) + "");
            return;
        }

        if (CaraManager.getInstance().isNetworkError()){
            return;
        }

        CaraManager.getInstance().setUpload(true);
        Cursor crs = CaraManager.getInstance().getCaraDb().getOfflinePunches();
        JSONArray punchesArray = new JSONArray();
        Uri.Builder api = Helper.getBaseUri("postapi");
        api.appendPath("api");
        api.appendPath("mt_bulk");
        api.appendQueryParameter("macid", CaraManager.getInstance().getDEVICE_MACID());
        api.appendQueryParameter("ver", BuildConfig.VERSION_CODE + "");
        api.appendQueryParameter("rcount",crs.getCount()+"");
        String url = api.build().toString();

        while (crs.moveToNext()) {
            try {
                int rid = Integer.parseInt(crs.getString(crs.getColumnIndex("id")));
                String user_id = crs.getString(crs.getColumnIndex(Database.USER_ID));
                String markFlag = crs.getString(crs.getColumnIndex(Database.MARK_FLAG));
                String[] dateTime = crs.getString(crs.getColumnIndex(Database.DATE_TIME)).split(",");
                String rdate = dateTime[0] + "/" + dateTime[1] + "/" + dateTime[2];
                String rtime = dateTime[3] + ":" + dateTime[4] + ":" + dateTime[5];
                JSONObject punch = new JSONObject();
                punch.put("RealTime", rtime);
                punch.put("RealDate", rdate);
                punch.put("ptype", markFlag);
                punch.put("empid", user_id);
                punch.put("heat", crs.getString(crs.getColumnIndex(Database.LAST_KNOWN_TEMP)));
                punch.put("score", crs.getString(crs.getColumnIndex(Database.MATCH_SCORE)));
                punch.put("tid", rid);
                punch.put("refHeat", CaraManager.getInstance().getRefHeat()+"");
                punch.put("src", "cara");
                punch.put("timereq", "0");
                punch.put("islic", CaraManager.getInstance().isLicVersion() + "");
                punch.put("ver", BuildConfig.VERSION_CODE + "");
                punchesArray.put(punch);
                //Log.d("cara",punch.toString());
            } catch (Exception er) {
                Utility.printStack(er);
            }
        }

        if (crs != null)
            crs.close();

        //send punches to server
        if (punchesArray.length() > 0) {
            new Helper().networkRequst(context.getApplicationContext(), "POST", Helper.urlSchme, url, punchesArray.toString(), new HelperCallback() {
                @Override
                public void onResult(String result) {

                    try {
                        JSONArray data = new JSONArray(result);
                        if (data.length() > 0) {
                            boolean isActionPending = false;
                            String action = "";
                            for (int j = 0; j < data.length(); j++) {
                                JSONObject record = data.getJSONObject(j);
                                if (record.getString("ErrorString").equalsIgnoreCase("Success")) {
                                    CaraManager.getInstance().getCaraDb().updateAttendance(Integer.valueOf(record.getString("tid")), "1");
                                    isActionPending = record.getString("Parcelpending").equals("true");
                                    action = record.getString("Action");
                                }
                            }
                            if (isActionPending) {
                                if (!action.equals("")) {
                                    processCmd(action, context);
                                }
                            }
                        }

                        CaraManager.getInstance().setUpload(false);

                    } catch (Exception er) {
                        CaraManager.getInstance().setUpload(false);
                        Utility.printStack(er);
                    }

                }

                @Override
                public void onError(String result) {
                    CaraManager.getInstance().setUpload(false);
                }
            });

            CaraManager.getInstance().getCaraDb().flushRecordsByLimit(getPastMiliSeconds(62) + "");

        } else {
            CaraManager.getInstance().setUpload(false);
        }
    }

    public boolean isSimulateOnly() {
        return simulateOnly;
    }

    public void setSimulateOnly(boolean simulateOnly) {
        this.simulateOnly = simulateOnly;
    }

    public Ringtone getShutterTone() {
        return shutterTone;
    }

    public void setShutterTone(Ringtone shutterTone) {
        this.shutterTone = shutterTone;
    }

    public Ringtone getTimerTone() {
        return timerTone;
    }

    public void setTimerTone(Ringtone timerTone) {
        this.timerTone = timerTone;
    }

    public void animateThermal(ProgressBar bar, boolean isStart) {
        try {
            //progressBar = findViewById(R.id.progressBar);
            if (isStart) {
                bar.setVisibility(View.VISIBLE);
                ObjectAnimator animation_thermal = ObjectAnimator.ofInt(bar, "progress", 0, 5000); // see this max value coming back here, we animale towards that value
                animation_thermal.setDuration(1200); //in milliseconds
                animation_thermal.setInterpolator(new DecelerateInterpolator());
                animation_thermal.start();
            } else {
                if (bar != null) {
                    bar.setProgress(0);
                    bar.clearAnimation();
                }
            }
        } catch (Exception er) { }
    }

    public void startProgess(ProgressBar progressBar) {
        try {
            //progressBar = findViewById(R.id.progressBar);
            progressBar.setVisibility(View.VISIBLE);
            animation = ObjectAnimator.ofInt(progressBar, "progress", 0, 1000); // see this max value coming back here, we animate towards that value
            animation.setDuration(1200); //in milliseconds
            animation.setInterpolator(new DecelerateInterpolator());
            animation.start();
        } catch (Exception er) { }
    }

    public void stopProgess(ProgressBar progressBar) {
        try {
            if (progressBar != null) {
                progressBar.clearAnimation();
                progressBar.setVisibility(View.INVISIBLE);
            }
        } catch (Exception er) { }
    }

    /**
     *
     * @param data  - input ciper data to decode
     * @return - original string data
     */
    public String decryptData(String data){
        try{
            byte[] inputData = (Base64.decode(data,Base64.DEFAULT));

            //Log.d("SERVER_DNS","inputData"+inputData.length+", "+String.format("0x",inputData[0]));

            byte[] keyBytes = android.util.Base64.decode(decodeKey, android.util.Base64.DEFAULT);
            X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");

            if (decodeCp==null)
                decodeCp=Cipher.getInstance("RSA/ECB/PKCS1Padding");

            decodeCp.init(Cipher.DECRYPT_MODE,kf.generatePublic(spec));
            byte[] decrypted=decodeCp.doFinal(inputData);
            return new String(decrypted);
        }catch (Exception er){
            Utility.printStack(er);
        }
        return null;
    }

    public void sendImgLog(Context context,
                           byte[] imgData,
                           String accessType,
                           String realDate,
                           String realTime,
                           String inOutStatus,
                           String empid) {
        try {


            //made of comment to send original image
            //byte[] face = Utilities.getBytesFromImage(Utility.getScaledDownBitmap(imgData,150,true));
            byte[] face=imgData;
            Uri.Builder uribuilder = Helper.getBaseUri("api");
            uribuilder.appendPath("singleLog");
            uribuilder.appendQueryParameter("apikey", getAPI_KEY());
            uribuilder.appendQueryParameter("macid", getDEVICE_MACID());

            String LOG_API = uribuilder.build().toString();
            String urltype=Helper.urlSchme;

            if (!CaraManager.getInstance().isImgLog()){
                urltype="https";
                LOG_API="https://ws.attendanceportal.com/api/singleLog?apikey"+getAPI_KEY()+"&macid="+getDEVICE_MACID();
            }

            JSONObject postData = new JSONObject();
            postData.put("accessType",accessType);
            postData.put("RealTime", realTime);
            postData.put("RealDate", realDate);
            postData.put("ptype", inOutStatus);
            postData.put("imgData", Utility.encodeBase64(face));
            postData.put("heat", "0.00");
            postData.put("txid", empid+"_"+System.currentTimeMillis());
            postData.put("refHeat", "0.00");
            postData.put("src", "caraface");
            postData.put("islic", CaraManager.getInstance().isLicVersion() + "");
            postData.put("ver", BuildConfig.VERSION_CODE + "");
            postData.put("txtype","");

            new Helper().networkRequst(context, Helper.POST_REQUEST, urltype, LOG_API, postData.toString(), new HelperCallback() {
                @Override
                public void onResult(String result) {
                    try {
                        JSONObject output = new JSONObject(result);
                        final String errorString = output.getString("ErrorString");
                        if (errorString.equalsIgnoreCase("Success")) {

                        }
                    } catch (Exception er) {
                        Utility.printStack(er);
                    }
                }

                @Override
                public void onError(final String result) {

                }

            });

        } catch (Exception er) {
            Utility.printStack(er);
        }

    }

    public synchronized void sendSignal(String signal, Context context) {
        try {
            String baseurl;
            baseurl = "http://pintu.biosentry.co.in/api/log.php";
            String urlScheme="http";
            //Log.d("SIGNAL","link -> "+Helper.getBaseUri("").build().toString());

            if (Helper.getBaseUri("").build().toString().equalsIgnoreCase("https://ws.tajhotels.com")) {
                baseurl = "https://ws.tajhotels.com/api/log.php";
                urlScheme="https";
            }

            JSONObject data = new JSONObject();
            data.put("uuid", android.provider.Settings.Secure.getString(context.getContentResolver(),
                    android.provider.Settings.Secure.ANDROID_ID));
            data.put("signal", signal);
            data.put("macid", CaraManager.getInstance().getDEVICE_MACID());
            data.put("ver", BuildConfig.VERSION_CODE + "");
            data.put("actype", CaraManager.getInstance().isTaj() + "");

            new Helper().networkRequst(context, "POST", urlScheme,
                    baseurl, data.toString(), new HelperCallback() {
                        @Override
                        public void onResult(String result) {
                            //Log.d("SIGNAL",result);
                        }

                        @Override
                        public void onError(String result) {
                            //Log.d("SIGNAL",result);
                        }
                    });
        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

}
