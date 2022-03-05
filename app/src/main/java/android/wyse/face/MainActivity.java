package android.wyse.face;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.BitmapDrawable;
import android.hardware.usb.UsbDevice;
import android.hardware.usb.UsbManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.os.PowerManager;
import android.os.StrictMode;
import android.os.UserManager;
import android.provider.Settings;
import android.util.Log;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import android.wyse.face.tech5.utilities.Constants;
import android.wyse.face.tech5.utilities.Listener;
import android.wyse.face.tech5.utilities.OneShotProcessorTask;
import android.wyse.face.tech5.utilities.Utilities;
import android.wyse.face.tech5.utilities.Worker;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.core.os.UserManagerCompat;

import com.dwin.dwinpio.GpioControlUtil;

import org.json.JSONException;
import org.json.JSONObject;
import org.opencv.android.BaseLoaderCallback;
import org.opencv.android.LoaderCallbackInterface;
import org.opencv.android.OpenCVLoader;

import java.io.IOException;

public class MainActivity extends Activity {

    private ImageView imageView;
    private ImageView bluetooth_lock;
    private int logotap=0;
    private CountDownTimer countDownTimer;
    private int modcnt=0;
    private ProgressDialog progressDialog;
    private View topbar,bgView,defaultView;

    private BroadcastReceiver mainRecv=new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {

            String cmd = intent.getAction();
            if (cmd==null){
                return;
            }

            if (intent.getAction().equals(Intent.ACTION_LOCKED_BOOT_COMPLETED)) {
                //set last in and out status
                Log.d("cara", "lock boot completed");
                CaraManager.getInstance().setSharedPreferences(context.getSharedPreferences(CaraManager.PREF_FILE_NAME, Context.MODE_PRIVATE));
                CaraManager.getInstance().setLastInout_resetTime(Long.valueOf(CaraManager.getInstance().getSharedPreferences().getString("inout_reset", System.currentTimeMillis() + "")));
                CaraManager.getInstance().initMac();
            }

            if (intent.getAction().equals(Intent.ACTION_TIME_TICK)){
                //if (CaraManager.getInstance().getInoutMode().equals("-1")) {
                //reset logic for new day
                CaraManager.getInstance().checkForReset();
                //}
                CaraManager.getInstance().checkForHalt(getApplicationContext());

                try {
                    if (CaraManager.getInstance().getUploadCount() == 8) {
                        CaraManager.getInstance().checkCommands(getApplicationContext());
                    }
                }catch (Exception er){}
            }

            if (intent.getAction().equalsIgnoreCase(UsbManager.ACTION_USB_DEVICE_DETACHED)){
                CaraManager.getInstance().setLatchConnected(false);
                UsbLatchConnector.getInstance().flushConnection();
                UsbLatchConnector.setUsbLatchConnector(null);
            }

            if (intent.getAction().equalsIgnoreCase(UsbManager.ACTION_USB_DEVICE_ATTACHED)){
                if(CaraManager.getInstance().isLatchConnected()) {
                    if (bluetooth_lock != null) {
                        bluetooth_lock.setVisibility(View.VISIBLE);
                    } else {
                        bluetooth_lock = findViewById(R.id.bluetooth_lock);
                        bluetooth_lock.setVisibility(View.VISIBLE);
                    }
                }
            }

            if (UsbLatchConnector.getInstance().getActionUsbPermission().equals(cmd)) {
                synchronized (this) {
                    if (CaraManager.getInstance().isLatchEnable()) {
                        final UsbDevice device = (UsbDevice) intent.getParcelableExtra(UsbManager.EXTRA_DEVICE);
                        // is usb permission has been granted, try to open a connection
                        if (intent.getBooleanExtra(UsbManager.EXTRA_PERMISSION_GRANTED, false)) {
                            if (device != null) {
                                UsbLatchConnector.getInstance().startUsb(MainActivity.this);
                            }
                        }
                    }
                }
            }

            if (cmd.equals("ACTION_BLE_DISCONNECT") || cmd.equals("ERROR_BLE")){
                if (bluetooth_lock!=null){  bluetooth_lock.setVisibility(View.INVISIBLE); }
                CaraManager.getInstance().setLatchConnected(false);
            }

            if (cmd.equals("ACTION_BLE_CONNECT")){

                if (bluetooth_lock!=null){  bluetooth_lock.setVisibility(View.VISIBLE); }else{
                    bluetooth_lock=findViewById(R.id.bluetooth_lock);
                    bluetooth_lock.setVisibility(View.VISIBLE);
                }
                CaraManager.getInstance().setLatchConnected(true);
            }

        }
    };
    private String TAG = "TAG";
    private BaseLoaderCallback mLoaderCallback = new BaseLoaderCallback(this) {
        @Override
        public void onManagerConnected(int status) {
            switch (status) {
                case LoaderCallbackInterface.SUCCESS:
                    break;
                default:
                    super.onManagerConnected(status);
                    break;
            }
        }
    };
    private CountDownTimer activityTimeout;
    private final Handler workerHandler = new Handler() {
        public void handleMessage(Message msg) {
            //hideProgress();
            if (msg.what == Constants.IDENTIFY_SUCCESS) {

                // onIdentifiedResponse(results);
            } else if (msg.what == Constants.IDENTIFY_FAILURE) {

            } else if (msg.what == Constants.INIT_SDK_SUCCESS) {
                loadOpenCV();

                CaraManager.getInstance().executeAction(Helper.COMMAND_ADD, getApplicationContext(), new HelperCallback() {
                    @Override
                    public void onResult(String result) {
                        CaraManager.getInstance().setCommand(false);
                        hideDialog();
                    }

                    @Override
                    public void onError(String result) {
                        CaraManager.getInstance().setCommand(false);
                        hideDialog();
                    }
                });

                hideDialog();
            } else if (msg.what == Constants.ENROLL_SUCCESS){

            } else if (msg.what == Constants.ENROLL_FAILURE) {

            } else if (msg.what == Constants.INIT_SDK_fAILURE){
                hideDialog();
            }

        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
        StrictMode.setThreadPolicy(policy);

        setContentView(R.layout.activity_main);

        checkDrawOverlayPermission();

        TextView version = findViewById(R.id.version);
        version.setText("v"+ BuildConfig.VERSION_CODE+" "+(CaraManager.getInstance().isLicVersion() ? "L":""));

        imageView= findViewById(R.id.logo);
        bluetooth_lock=findViewById(R.id.bluetooth_lock);



        /*try {
            String[] expDate = {"10", "11", "2020"};
            String date = expDate[2] + "-" + expDate[1] + "-" + expDate[0] + " 23:59";
            //date=expDate[0]+"/"+expDate[1]+"/"+expDate[2];
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm");
            long expDateTimeInMili = sdf.parse(date).getTime();
            Log.d("Date",expDateTimeInMili+", "+ Calendar.getInstance().getTimeInMillis()+", "+(expDateTimeInMili>Calendar.getInstance().getTimeInMillis()));
        }catch (Exception er){
            er.printStackTrace();
        } */

        topbar=findViewById(R.id.topBar);
        defaultView=findViewById(R.id.defaultView);
        bgView=findViewById(R.id.mainBg);


        try {
            if (CaraManager.getInstance().getMainTheme()==null) {
                //setup theme
                byte[] main = Utilities.readfileFrom(CaraManager.getInstance().getThemeDir() + "/main.jpg");

                if (main != null)
                    CaraManager.getInstance().setMainTheme(new BitmapDrawable(getResources(), FaceManager.getInstance().getBitmapFromByte(main)));
            }
        }catch (Exception er){
            Utility.printStack(er);
            defaultView.setVisibility(View.VISIBLE);
            bgView.setBackground(null);
            topbar.setVisibility(View.INVISIBLE);
        }


        if (CaraManager.getInstance().getMainTheme()==null){
            if (defaultView!=null) {
                defaultView.setVisibility(View.VISIBLE);
                bgView.setBackground(null);
                topbar.setVisibility(View.INVISIBLE);
            }
        }else {
            if (defaultView!=null) {
                defaultView.setVisibility(View.INVISIBLE);
                bgView.setBackground(CaraManager.getInstance().getMainTheme());
                bgView.setVisibility(View.VISIBLE);
                topbar.setVisibility(View.VISIBLE);
            }
        }


        //updateSettings();
        //CaraManager.getInstance().initUsers();
        // Check Latch Connection
//        if (CaraManager.getInstance().isLatchEnable() && Build.MANUFACTURER.equalsIgnoreCase("dwin")) {
//
//            try
//            {
//                if (! CaraManager.getInstance().isLatchConnected()) {
//                    CaraManager.getInstance().connectAccessControlDwin(getApplicationContext(),80);
//                }
//                else
//                {
//                    GpioControlUtil.getInstance().getGpioIntputValue(166);
//
//                    if (GpioControlUtil.nRet == -1) {
//                        CaraManager.getInstance().connectAccessControlDwin(getApplicationContext(),80);
//                    }
//
//                }
//            }
//            catch (Exception ex)
//            {
//
//            }
//            finally {
//
//            }
//        }

    }

    private void initSdk(){
        setupLoader(this,"Wait...");

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) {
            //LogUtils.debug("TAG", "initializeSDK() () called");
            showDialog("Device sync in progress...");
            Worker.getInstance().sethandler(workerHandler);
            Worker.getInstance().addTask(new OneShotProcessorTask(getApplicationContext(), Constants.TYPE_INIT_SDK, null, null, null, 0,null));
        }else {
            Toast.makeText(getApplicationContext(),"Camera permission not granted !",Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);

        if (requestCode==4444){
            //LogUtils.debug("TAG", "initializeSDK() () called");
            Worker.getInstance().sethandler(workerHandler);
            Worker.getInstance().addTask(new OneShotProcessorTask(MainActivity.this, Constants.TYPE_INIT_SDK, null, null, null, 0,null));
        }

    }

    public void onLogoTap(View view){
        Utility.setFadeAnimation(view);
        logotap++;
        if (logotap==5){
            logotap=0;
            CaraManager.getInstance().setProd(false);
            Intent protect=new Intent(this, ProtectActivity.class);
            protect.putExtra("type","setting");
            startActivity(protect);
            //finish();
        }
    }

    private void enableAppAdmin(Context context) {
        DevicePolicyManager mDPM = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName adminComponent = CaraAdmin.getComponentName(this);
        if (!mDPM.isAdminActive(adminComponent)) {
            Intent activateDeviceAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Tap on Activate");
            context.startActivity(activateDeviceAdmin);
        }
    }

    public void setupLoader(Activity activity,String message) {

        if (progressDialog==null)
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

    public void showDialog(String message){
        if (progressDialog!=null){
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    progressDialog.setMessage(message);
                    progressDialog.show();
                }
            });

        }
    }

    public void hideDialog(){
        if (progressDialog!=null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                        }
                    },1000);


                }
            });
        }

        setTimeout();
    }

    public void onTouchToStart(View view){
        CaraManager.getInstance().setSimulateOnly(false);

        if (CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS)>0) {
            Intent i = new Intent(this, CameraActivity.class);
            startActivity(i);
            //overridePendingTransition(android.R.anim.fade_in,android.R.anim.fade_out);
            overridePendingTransition(R.anim.fadein, R.anim.fadeout);
            //startActivity(new Intent(this,EnrollCameraActivity.class));
            finish();
        }else{
            Toast.makeText(getApplicationContext(),"No Users found !",Toast.LENGTH_SHORT).show();
            //showDialog("Device sync in progress...");
            CaraManager.getInstance().executeAction(Helper.COMMAND_ADD, getApplicationContext(), new HelperCallback() {
                @Override
                public void onResult(String result) {
                    CaraManager.getInstance().setCommand(false);
                    hideDialog();
                }

                @Override
                public void onError(String result) {
                    CaraManager.getInstance().setCommand(false);
                    hideDialog();
                }
            });
            hideDialog();
        }

    }

    private void checkDrawOverlayPermission() {
        try {
            if (Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
                Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:" + getApplicationContext().getPackageName()));
                startActivityForResult(intent, 4444);
            } else {
                enableAppAdmin(this);
                Utility.disableBottomButtons(this);
                Utility.disablePullNotificationTouch(this);
            }
        }catch (Exception er){}
    }

    private void statupInit(){
        if (CaraManager.getInstance().getSharedPreferences().getString("isactivated", "false").equals("false")) {
            if (!CaraManager.getInstance().isTaj()) {
                startActivity(new Intent(getApplicationContext(), Activation.class));
                finish();
                return;
            }
        }

        animateLogo();

        if (Build.VERSION.SDK_INT >= 23) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4444);
            }
        }

        CaraManager.getInstance().updateSettings(getApplicationContext());

        if (CaraManager.getInstance().isLatchConnected()) {
            if (bluetooth_lock != null) {
                bluetooth_lock.setVisibility(View.VISIBLE);
            } else {
                bluetooth_lock = findViewById(R.id.bluetooth_lock);
                bluetooth_lock.setVisibility(View.VISIBLE);
            }
        }

        if (UsbLatchConnector.getInstance().getMcp2221() == null) {
            if (CaraManager.getInstance().isLatchEnable() || CaraManager.getInstance().isThermal()) {
                if (UsbLatchConnector.getInstance().getMcp2221() == null)
                    UsbLatchConnector.getInstance().startUsb(this);
            }
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        CaraManager.getInstance().hideHomeBar(getWindow());
        FaceManager.getInstance().setEnroll(false);


        logotap=0;
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Utility.disablePullNotificationTouch(this);
            }
        }

        //init sdk and shared pref
        CaraManager.getInstance().setupLoader(this,"Wait...");
        IntentFilter intentFilter=new IntentFilter();
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION);
        intentFilter.addAction(Intent.ACTION_BATTERY_CHANGED);
        intentFilter.addAction(Intent.ACTION_BATTERY_LOW);
        intentFilter.addAction(Intent.ACTION_SCREEN_OFF);
        intentFilter.addAction(Intent.ACTION_SHUTDOWN);
        intentFilter.addAction("ACTION_BLE_CONNECT");
        intentFilter.addAction("ACTION_BLE_DISCONNECT");
        intentFilter.addAction("ERROR_BLE");
        intentFilter.addAction("PRINTER_STATUS_LIVE");
        intentFilter.addAction("PRINTER_CONNECTED");
        intentFilter.addAction("PRINTER_DISCONNECTED");
        intentFilter.addAction("SYNC_BEGIN");
        intentFilter.addAction(UsbLatchConnector.getInstance().getActionUsbPermission());
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
        intentFilter.addAction(UsbManager.ACTION_USB_DEVICE_DETACHED);
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction(Intent.ACTION_LOCKED_BOOT_COMPLETED);
        registerReceiver(mainRecv,intentFilter);


        if (Build.VERSION.SDK_INT>=24){
            if (UserManagerCompat.isUserUnlocked(getApplicationContext())){
                CaraManager.getInstance().setSharedPreferences(getApplicationContext().getSharedPreferences(CaraManager.PREF_FILE_NAME, Context.MODE_PRIVATE));
                CaraManager.getInstance().setLastInout_resetTime(Long.valueOf(CaraManager.getInstance().getSharedPreferences().getString("inout_reset", System.currentTimeMillis() + "")));
                CaraManager.getInstance().initMac();
                initSdk();
                statupInit();
            }else{
                while (!UserManagerCompat.isUserUnlocked(getApplicationContext())){

                }
                CaraManager.getInstance().setSharedPreferences(getApplicationContext().getSharedPreferences(CaraManager.PREF_FILE_NAME, Context.MODE_PRIVATE));
                CaraManager.getInstance().setLastInout_resetTime(Long.valueOf(CaraManager.getInstance().getSharedPreferences().getString("inout_reset", System.currentTimeMillis() + "")));
                CaraManager.getInstance().initMac();
                initSdk();
                statupInit();
            }
        }




    }

    private void loadOpenCV() {
        if (!OpenCVLoader.initDebug(false)) {
            //Log.d(TAG, "Internal OpenCV library not found. Using OpenCV Manager for initialization");
            OpenCVLoader.initAsync(OpenCVLoader.OPENCV_VERSION, this, mLoaderCallback);
        } else {
            //Log.d(TAG, "OpenCV library found inside package. Using it!");
            mLoaderCallback.onManagerConnected(LoaderCallbackInterface.SUCCESS);
        }
    }

    private void animateLogo(){

        if (countDownTimer!=null){
            countDownTimer.cancel();
        }

        countDownTimer=new CountDownTimer(10*1000,1000) {
            @Override
            public void onTick(long l) {
                modcnt++;
                if (modcnt%2==0) {
                    Utility.animateScale(imageView, 1.3f);
                }else{
                    Utility.animateScale(imageView, 1.0f);
                }
            }

            @Override
            public void onFinish() {
                Utility.animateScale(imageView, 1.0f);
            }
        };
        countDownTimer.start();
    }

    private void setTimeout() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (activityTimeout != null) {
                    activityTimeout.cancel();
                }
                //pd.show();
                activityTimeout = new CountDownTimer(15000, 1000) {
                    @Override
                    public void onTick(long millisUntilFinished) {
                    }

                    @Override
                    public void onFinish() {
                        if (Listener.isSDKInitialized && CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS)>0){
                            PowerManager powerManager=(PowerManager)getSystemService(POWER_SERVICE);
                            if (powerManager.isInteractive()) {
                                CaraManager.getInstance().setSimulateOnly(false);
                                startActivity(new Intent(MainActivity.this, CameraActivity.class));
                                overridePendingTransition(R.anim.fadein, R.anim.fadeout);
                                finish();
                            }
                        }
                    }
                };
                activityTimeout.start();
            }
        });

    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onPause(){
        super.onPause();
        if (progressDialog!=null){
            progressDialog.hide();
            progressDialog.cancel();
        }
        if (countDownTimer!=null) {
            countDownTimer.cancel();
        }
        if (activityTimeout!=null){
            activityTimeout.cancel();
        }
        try {
            if (mainRecv != null) {
                unregisterReceiver(mainRecv);
            }
        }catch (Exception er){}
    }


}
