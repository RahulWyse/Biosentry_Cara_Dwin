package android.wyse.face;

import android.Manifest;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.bluetooth.BluetoothAdapter;
import android.bluetooth.BluetoothManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.hardware.camera2.CameraManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.provider.Settings;
import android.text.Editable;
import android.text.InputFilter;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;
import android.wyse.face.adaptors.CustomArrayAdaptor;
import android.wyse.face.models.DnsModel;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.dwin.dwinpio.GpioControlUtil;

import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;

public class Configurations extends Activity implements AdapterView.OnItemSelectedListener{

  private final int REQUEST_ENABLE_BT=44;
  private final String SENSOR_LOAD_FINISH="SENSOR_LOAD_FINISH";
  private final String ACTION_BLE_CONNECT="ACTION_BLE_CONNECT";
  private final BroadcastReceiver settingupdate=new BroadcastReceiver() {
    @Override
    public void onReceive(Context context, Intent intent) {
      String action=intent.getAction();

      if (action!=null) {
        //Log.d(getClass().getSimpleName(),action);
        if (action.equals("IM_DEAD")) {
          String type = intent.getStringExtra("type");
          Toast.makeText(Configurations.this, type + " stopped !", Toast.LENGTH_SHORT).show();
        }
        if (action.equals("ACCESS_CONTROL_STARTED")) {
          Toast.makeText(Configurations.this, "Access control started !", Toast.LENGTH_SHORT).show();
        }
        if (action.equals("SENSOR_STARTED")) {
          Toast.makeText(Configurations.this, "Sensor started !", Toast.LENGTH_SHORT).show();
        }
        if (action.equals("ACTION_BLE_DISCONNECT")) {
          Toast.makeText(Configurations.this, "Access control disconnected !", Toast.LENGTH_SHORT).show();
        }

        if (action.equals(SENSOR_LOAD_FINISH)) {
          Toast.makeText(Configurations.this, "Sensor started...", Toast.LENGTH_SHORT).show();
        }
        if (action.equals(ACTION_BLE_CONNECT)) {
          Toast.makeText(Configurations.this, "Access Control started...", Toast.LENGTH_SHORT).show();
        }
      }

    }
  };
  int mac_textcnt=0;
  int lastLength=0;
  private  String app_update = "";
  private TextView cameracntText;
  private RadioButton frontfacing;
  private RadioButton rearfacing;
  private int totalCams =0 ;
  private  int FRONT_FACEING_CAMERA = 0;
  private CheckBox inmode;
  private CheckBox outmode;
  private CheckBox check_fingerprint;
  private CheckBox access_ctrl_check;
  private CheckBox badge_print_check;
  private CheckBox play_voice_over,thermalCheck,qualitycheck,autoflash,thermalMode,playAlarm,liveness_check,readMode,securityType;
  private EditText thermal_temp_value,scalevalue,resettime,fliptimeout,qualityvalue,liveness_value;
  private Button enrollBtn;
  private Button updatebtn;
  private EditText ble_mac_id;
  private EditText photo_take_timeout;
  private EditText access_grant_timeout;
  private EditText printer_mac_id;
  private EditText dns;
  private EditText minTempVal;
  private EditText shifttime;
  private long total = 0;
  private int lenghtOfFile=0;
  private ProgressDialog progressDialog;
  private BluetoothAdapter mBluetoothAdapter;
  private CameraManager manager;
  private int logotap=0;
  private String seletedLang="English";
  private Spinner latchType,face_thr_spinner;
  private CheckBox rebootcheck;
  private EditText reboottime;
  private int cameraSel = 0;
  private boolean isValidDomain = false;
  private String dnsvalue = "";

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    try {
      requestWindowFeature(Window.FEATURE_NO_TITLE);
      View decorView = getWindow().getDecorView();
      int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
              View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
              | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
              | View.SYSTEM_UI_FLAG_FULLSCREEN
              | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
      decorView.setSystemUiVisibility(uiOptions);

      Helper.secureScreen(getWindow());
      //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

    } catch (Exception er){}
    setContentView(R.layout.activity_settings);

    if (Build.VERSION.SDK_INT>=21) {
      manager = (CameraManager) this.getSystemService(Context.CAMERA_SERVICE);
    }

    cameracntText = findViewById(R.id.cameracntText);
    TextView appver = findViewById(R.id.appver);
    TextView deviceRecords= findViewById(R.id.deviceRecords);
    frontfacing = findViewById(R.id.frontfacing);
    rearfacing = findViewById(R.id.rearfacing);
    frontfacing.setEnabled(false);
    rearfacing.setEnabled(false);
    enrollBtn = findViewById(R.id.enrollBtn);
    updatebtn= findViewById(R.id.updatebtn);
    updatebtn.setVisibility(View.VISIBLE);
    latchType=findViewById(R.id.latchType);
    thermalCheck=findViewById(R.id.thermalCheck);
    qualitycheck=findViewById(R.id.qualitycheck);
    autoflash=findViewById(R.id.autoflash);
    face_thr_spinner=findViewById(R.id.face_thrspinner);

    inmode=findViewById(R.id.inmode);
    outmode=findViewById(R.id.outmode);

    rebootcheck=findViewById(R.id.rebootcheck);
    reboottime=findViewById(R.id.reboottime);
    reboottime.setVisibility(View.INVISIBLE);

    thermal_temp_value=findViewById(R.id.thermal_temp_value);
    scalevalue=findViewById(R.id.scalevalue);
    thermalMode=findViewById(R.id.thermalMode);
    playAlarm=findViewById(R.id.playAlarm);

    liveness_check=findViewById(R.id.liveness_check);
    liveness_check.setChecked(CaraManager.getInstance().isLiveness());

    readMode=findViewById(R.id.readMode);
    readMode.setChecked(CaraManager.getInstance().isReadCenter());

    securityType=findViewById(R.id.securityType);
    securityType.setChecked(Helper.urlSchme.equalsIgnoreCase("http"));

    minTempVal=findViewById(R.id.minTempVal);
    minTempVal.setText(CaraManager.getInstance().getMinTempVal()+"");

    shifttime=findViewById(R.id.shift_change);
    resettime=findViewById(R.id.resettime);

    fliptimeout=findViewById(R.id.checkIOTimeoutValue);
    qualityvalue=findViewById(R.id.qualityvalue);
      liveness_value=findViewById(R.id.liveness_value);

    dns=findViewById(R.id.dns);
    updatebtn.setText("USERS");
    deviceRecords.setText("");

    //ScrollView scrollView = findViewById(R.id.settingScroll);
    //TextView deviceId = findViewById(R.id.phonenumber);

    badge_print_check= findViewById(R.id.badge_print_check);

    play_voice_over= findViewById(R.id.playvoice_over);
    printer_mac_id= findViewById(R.id.printer_mac_id);

    ArrayList<String> thrvalues=new ArrayList<>();
    thrvalues.add("6");
    thrvalues.add("6.5");
    thrvalues.add("7.0");
    thrvalues.add("7.5");
    thrvalues.add("8");
    thrvalues.add("8.5");
    thrvalues.add("9");
    //Creating the ArrayAdapter instance having the country list
    ArrayAdapter face_thr_array = new ArrayAdapter(this,android.R.layout.simple_spinner_dropdown_item,thrvalues);
    //aa.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

    //Setting the ArrayAdapter data on the Spinner
    face_thr_spinner.setAdapter(face_thr_array);
    face_thr_array.notifyDataSetChanged();
    for (int ftr=0;ftr<thrvalues.size();ftr++){
      if (thrvalues.get(ftr).equals(FaceManager.getInstance().getFaceTheshold()+"")){
        face_thr_spinner.setSelection(ftr);
        break;
      }
    }


    face_thr_spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        FaceManager.getInstance().setFaceTheshold (Float.valueOf(thrvalues.get(position)));
        //Toast.makeText(getApplicationContext(),"Face matching value changed to "+FaceManager.getInstance().getFaceTheshold(),Toast.LENGTH_SHORT).show();
      }
      @Override
      public void onNothingSelected(AdapterView<?> parent) { }

    });

    play_voice_over.setChecked(true);
    Spinner voice_over_lang = findViewById(R.id.voice_over_lang);
    ArrayList<String> languages = Utility.getLanguages();

    // Creating adapter for spinner
    ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, languages);

    // Drop down layout style - list view with radio button
    dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    voice_over_lang.setOnItemSelectedListener(this);
    // attaching data adapter to spinner
    voice_over_lang.setAdapter(dataAdapter);
    voice_over_lang.setSelection(1);


    // latchType.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
    latchType.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener(){
      @Override
      public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        CaraManager.getInstance().setLatchComType(position+"");
      }

      @Override
      public void onNothingSelected(AdapterView<?> parent) {

      }
    });

    latchType.setAdapter(new CustomArrayAdaptor(Configurations.this,android.R.layout.simple_spinner_dropdown_item, CaraManager.getInstance().getLatchCom()));
    latchType.setSelection(Integer.parseInt(CaraManager.getInstance().getLatchComType()));
    if (CaraManager.getInstance().isLatchEnable())
      latchType.setVisibility(View.VISIBLE);

    check_fingerprint = findViewById(R.id.check_fingerprint);
    access_ctrl_check = findViewById(R.id.access_ctrl_check);
    ble_mac_id= findViewById(R.id.ble_mac_id);

    access_grant_timeout= findViewById(R.id.access_grant_timeout);
    photo_take_timeout= findViewById(R.id.photo_take_timeout);

    try {
      printer_mac_id = findViewById(R.id.printer_mac_id);
      printer_mac_id.setText("");
    }catch (Exception er){}

    // ble_mac_id.setText("");
    ble_mac_id.setFilters(new InputFilter[]{new InputFilter.LengthFilter(17)});
    check_fingerprint.setChecked(false);
    access_ctrl_check.setChecked(false);

    photo_take_timeout.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {
        if(!s.toString().isEmpty()){
          int timeout=Integer.valueOf(photo_take_timeout.getText().toString());
          if (timeout>30 || timeout==0){
            Toast.makeText(Configurations.this,"TIMEOUT MUST BE LESS THAN 30 AND MORE THAN 0 sec",Toast.LENGTH_SHORT).show();
            photo_take_timeout.setText("4");
          }
        }
      }
    });

    access_grant_timeout.addTextChangedListener(new TextWatcher() {
      @Override
      public void beforeTextChanged(CharSequence s, int start, int count, int after) {

      }

      @Override
      public void onTextChanged(CharSequence s, int start, int before, int count) {

      }

      @Override
      public void afterTextChanged(Editable s) {

        if (!s.toString().isEmpty()) {
          int timeout = Integer.valueOf(access_grant_timeout.getText().toString());
          if (timeout > 30 || timeout == 0) {
            Toast.makeText(getApplicationContext(), "TIME OUT MUST BE LESS THAN 30 AND MORE THAN 0 sec", Toast.LENGTH_SHORT).show();
            access_grant_timeout.setText("15");
          }
        }

      }
    });

    //change this for release
    appver.setText(BuildConfig.VERSION_NAME + "- " + BuildConfig.BUILD_TYPE + "(" + BuildConfig.VERSION_CODE +", "+BuildConfig.BUILD_TIME+") " +
            ""+ (CaraManager.getInstance().isLicVersion() ? "L":"U"));


    String settings=CaraManager.getInstance().getSharedPreferences().getString("settings","");

    if (!settings.equals("")){
      try {
        JSONObject jdata = new JSONObject(settings);
        String camid =jdata.getString(Helper.SETTING_CAMERA);
        String access_control  = jdata.getString(Helper.SETTING_ACCESS_CONTROL);
        String access_timeout=jdata.getString(Helper.SETTING_ACCESS_TIMEOUT);
        String photo_timeout=jdata.getString(Helper.SETTING_PHOTO_TIMEOUT);
        String lang=jdata.getString(Helper.SETTING_LANG);

        try {
          String isPrinter=jdata.getString(Helper.SETTING_IS_PRINTER);
          String printer_mac = jdata.getString(Helper.SETTING_PRINTER_MAC_ID);

          printer_mac_id.setText(printer_mac.toUpperCase());

          if (!printer_mac.equals("") && isPrinter.equals("true")){
            badge_print_check.setChecked(true);
          }else{
            printer_mac_id.setVisibility(View.INVISIBLE);
            badge_print_check.setChecked(false);
          }
        }catch (Exception ignored){}
        //Log.d("lang","Setting: "+lang);

        try {
          if (!lang.equals("")) {
            String[] parm=lang.split(",");
            if (parm.length==2){
              String setLang=parm[0];
              String isVoice=parm[1];
              if (isVoice.equals("false")){
                play_voice_over.setChecked(false);
              }
              for (int l = 0; l< Utility.getLanguages().size(); l++){
                if (Utility.getLanguages().get(l).equals(setLang)){
                  voice_over_lang.setSelection(l);
                }
              }
            }
          }
        }catch (Exception ignored){}

        //Log.d("camera id from database",cameraId+"");
        if (!camid.equals("")) {
          int cameraId  = Integer.parseInt(camid);
          if (cameraId == CameraCharacteristics.LENS_FACING_FRONT) {
            frontfacing.setEnabled(true);
            frontfacing.setChecked(true);
            rearfacing.setChecked(false);
          }else if (cameraId == CameraCharacteristics.LENS_FACING_BACK) {
            rearfacing.setEnabled(true);
            rearfacing.setChecked(true);
            frontfacing.setChecked(false);
          }
        }

        ble_mac_id.setText(jdata.getString(Helper.SETTING_LATCH_MAC));

        if (jdata.getString(Helper.SETTING_LATCHTYPE)!=null){
          CaraManager.getInstance().setLatchComType(jdata.getString(Helper.SETTING_LATCHTYPE));
        }

        if(access_control.equals("false") || access_control.equals("")){
          access_ctrl_check.setVisibility(View.VISIBLE);
          ble_mac_id.setVisibility(View.INVISIBLE);
        }else{
          CaraManager.getInstance().setLatchEnable(true);
          access_ctrl_check.setChecked(true);
          if (!CaraManager.getInstance().getLatchComType().equals("2")){
            ble_mac_id.setText(jdata.getString(Helper.SETTING_LATCH_MAC));
          }else {
            latchType.setSelection(Integer.parseInt(CaraManager.getInstance().getLatchComType()));
          }
        }

        if (!access_timeout.equals("")){
          access_grant_timeout.setText(access_timeout);
          CaraManager.getInstance().setDOOR_TIMEOUT(Integer.parseInt(access_timeout));
        }
        if (!photo_timeout.equals("")){
          photo_take_timeout.setText(photo_timeout);
        }

        if (jdata.getString(Helper.SETTING_DNS)!=null){
          dns.setText(jdata.getString(Helper.SETTING_DNS).replaceAll("https://",""));
        }else{
          dns.setText(Helper.SERVER_DNS);
        }

        if (jdata.getString(Helper.SETTING_THERMAL)!=null){
          thermalCheck.setChecked(jdata.getString(Helper.SETTING_THERMAL).equalsIgnoreCase(Helper.TRUE));
        }

        CaraManager.getInstance().setInoutMode("-1");

        if (jdata.getString(Helper.SETTING_INMODE)!=null){
          inmode.setChecked(jdata.getString(Helper.SETTING_INMODE).equalsIgnoreCase(Helper.TRUE));
          if (inmode.isChecked()) {
            CaraManager.getInstance().setInoutMode("1");
          }
        }

        if (jdata.getString(Helper.SETTING_OUTMODE)!=null){
          outmode.setChecked(jdata.getString(Helper.SETTING_OUTMODE).equalsIgnoreCase(Helper.TRUE));
          if (outmode.isChecked()) {
            CaraManager.getInstance().setInoutMode("0");
          }
        }

        if (jdata.getString(Helper.SETTING_ISREBOOT)!=null) {
          CaraManager.getInstance().setReboot(jdata.getString(Helper.SETTING_ISREBOOT).equalsIgnoreCase(Helper.TRUE));
          if (CaraManager.getInstance().isReboot()) {
            rebootcheck.setChecked(true);
            reboottime.setText(jdata.getString(Helper.SETTING_REBOOT_HHMM));
            reboottime.setVisibility(View.VISIBLE);
            CaraManager.getInstance().setRebootTime(jdata.getString(Helper.SETTING_REBOOT_HHMM));
          }else{
            rebootcheck.setChecked(false);
          }
        }else{
          rebootcheck.setChecked(false);
        }

        if(jdata.getString(Helper.SETTING_ISQUALITY)!=null){
          CaraManager.getInstance().setQuality(jdata.getString(Helper.SETTING_ISQUALITY).equalsIgnoreCase(Helper.TRUE));
          qualitycheck.setChecked(CaraManager.getInstance().isQuality());
        }else{
          qualitycheck.setChecked(false);
        }

        if (jdata.getString(Helper.SETTING_AUTOFLASH)!=null){
          CaraManager.getInstance().setAutoFlash(jdata.getString(Helper.SETTING_AUTOFLASH).equalsIgnoreCase(Helper.TRUE));
          autoflash.setChecked(CaraManager.getInstance().isAutoFlash());
        }else{
          autoflash.setChecked(false);
        }

        if (jdata.getString(Helper.SETTING_ISDEGREE)!=null){
          CaraManager.getInstance().setTempUnit(jdata.getString(Helper.SETTING_ISDEGREE).equals(Helper.TRUE)?"C":"F");
          thermalMode.setChecked(CaraManager.getInstance().getTempUnit().equals("C"));
        }else{
          thermalMode.setChecked(false);
        }

        if (jdata.getString(Helper.SETTING_PLAYALARM)!=null){
          CaraManager.getInstance().setAlarm(jdata.getString(Helper.SETTING_PLAYALARM).equals(Helper.TRUE));
          playAlarm.setChecked(CaraManager.getInstance().isAlarm());
        }else{
          playAlarm.setChecked(false);
        }

        if (jdata.getString(Helper.SETTING_SCALE_TEMP)!=null){
          if (!jdata.getString(Helper.SETTING_SCALE_TEMP).equals(""))
            CaraManager.getInstance().setScaleTemp(Float.valueOf(jdata.getString(Helper.SETTING_SCALE_TEMP)));

          scalevalue.setText(CaraManager.getInstance().getScaleTemp()+"");
        }

        if (jdata.getString(Helper.SETTING_MAXTEMP)!=null){
          if (!jdata.getString(Helper.SETTING_MAXTEMP).equals(""))
            CaraManager.getInstance().setMaxTemp(Float.valueOf(jdata.getString(Helper.SETTING_MAXTEMP)));

          thermal_temp_value.setText(CaraManager.getInstance().getMaxTemp()+"");
        }

        if (jdata.getString(Helper.SETTING_ISLIVENESS)!=null){
          if (!jdata.getString(Helper.SETTING_ISLIVENESS).equals(""))
            CaraManager.getInstance().setLiveness(jdata.getString(Helper.SETTING_ISLIVENESS).equals(Helper.TRUE));

          liveness_check.setChecked(CaraManager.getInstance().isLiveness());
        }

        if (jdata.getString(Helper.SETTING_ISCENTER)!=null){
          if (!jdata.getString(Helper.SETTING_ISCENTER).equals(""))
            CaraManager.getInstance().setReadCenter(jdata.getString(Helper.SETTING_ISCENTER).equals(Helper.TRUE));

          readMode.setChecked(CaraManager.getInstance().isReadCenter());
        }

        if (jdata.getString(Helper.SETTING_URLTYPE)!=null){
          if (!jdata.getString(Helper.SETTING_URLTYPE).equals("")){
            Helper.urlSchme = jdata.getString(Helper.SETTING_URLTYPE).equalsIgnoreCase("http") ? "http" : "https";
            securityType.setChecked(Helper.urlSchme.equalsIgnoreCase("http"));
          }
        }

        if (jdata.getString(Helper.SETTING_FACE_THR)!=null){
          if (!jdata.getString(Helper.SETTING_FACE_THR).equals("")){
            String facethr = jdata.getString(Helper.SETTING_FACE_THR);
            if (facethr!=null) {
              FaceManager.getInstance().setFaceTheshold(Float.valueOf(facethr));
            }else{
              FaceManager.getInstance().setFaceTheshold(CaraManager.getInstance().getDefaultFaceThr());
            }
          }else{
            FaceManager.getInstance().setFaceTheshold(CaraManager.getInstance().getDefaultFaceThr());
          }
        }

        if (jdata.getString(Helper.SETTING_MINTEMP)!=null){
          if (!jdata.getString(Helper.SETTING_MINTEMP).equals("")){
            String minTemp = jdata.getString(Helper.SETTING_MINTEMP);
            if (minTemp!=null) {
              CaraManager.getInstance().setMinTempVal(Float.valueOf(minTemp));
            }else {
              CaraManager.getInstance().setMinTempVal(31.0f);
            }
          }else{
            CaraManager.getInstance().setMinTempVal(31.0f);
          }
        }

        try {
          if (jdata.getString(Helper.SETTING_SHIFT_TIME) != null) {
            if (!jdata.getString(Helper.SETTING_SHIFT_TIME).equals("")) {
              shifttime.setText(jdata.getString(Helper.SETTING_SHIFT_TIME));
              CaraManager.getInstance().setShifttime(shifttime.getText().toString());
            }
          }
        }catch (Exception er){
          shifttime.setText(CaraManager.getInstance().getShifttime());
        }

        try {
          if (jdata.getString(Helper.SETTING_RESET_TIME) != null) {
            if (!jdata.getString(Helper.SETTING_RESET_TIME).equals("")) {
              resettime.setText(jdata.getString(Helper.SETTING_RESET_TIME));
              CaraManager.setResetTime(resettime.getText().toString());
            }
          }
        }catch (Exception er){
          resettime.setText(CaraManager.getResetTime());
        }

        try {
          if (jdata.getString(Helper.SETTING_FLIP_TIMEOUT) != null) {
            if (!jdata.getString(Helper.SETTING_FLIP_TIMEOUT).equals("")) {
              fliptimeout.setText(jdata.getString(Helper.SETTING_FLIP_TIMEOUT));
              CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(Long.valueOf(fliptimeout.getText().toString())*1000);
            }
          }
        }catch (Exception er){
          CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(60*1000);
          fliptimeout.setText("60");
        }

        try {
          if (jdata.getString(Helper.SETTING_ENROL_QUALITY) != null) {
            if (!jdata.getString(Helper.SETTING_ENROL_QUALITY).equals("")) {
              qualityvalue.setText(jdata.getString(Helper.SETTING_ENROL_QUALITY));
              CaraManager.getInstance().setEnrol_quality(Float.valueOf(jdata.getString(Helper.SETTING_ENROL_QUALITY)));
            }
          }
        }catch (Exception er){
          CaraManager.getInstance().setEnrol_quality(20.0f);
          qualityvalue.setText("20");
        }

          try {
              if (jdata.getString(Helper.SETTING_LIVENESS_THR) != null) {
                  if (!jdata.getString(Helper.SETTING_LIVENESS_THR).equals("")) {
                      liveness_value.setText(jdata.getString(Helper.SETTING_LIVENESS_THR));
                      CaraManager.getInstance().setLiveness_thr(Float.valueOf(jdata.getString(Helper.SETTING_LIVENESS_THR)));
                  }
              }
          }catch (Exception er){
              CaraManager.getInstance().setLiveness_thr(0.80f);
              liveness_value.setText("0.80");
          }
      }catch (Exception er){
        securityType.setChecked(false);
        Utility.printStack(er);
      }
    } else{
      ble_mac_id.setVisibility(View.INVISIBLE);
      dns.setText(Helper.getBaseUri("").build().toString());
    }

    TextView mactext=findViewById(R.id.mactext);
    mactext.setText("DEVICE ID { "+ CaraManager.getInstance().getDEVICE_MACID()+" }");

    check_fingerprint.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        setFadeAnimation(v);
        if (check_fingerprint.isChecked()) {
          enrollBtn.setVisibility(View.VISIBLE);
        } else {
          enrollBtn.setVisibility(View.INVISIBLE);
        }
      }
    });

    configureProgress();
    getFrontFacingCameraId(manager);
    checkDeviceCamera();

    int pendingRecords = CaraManager.getInstance().getCaraDb().getPendingRecords();
    int AllRecords = CaraManager.getInstance().getCaraDb().getDeviceRecords();

    deviceRecords.setText("Device Records ("+AllRecords+")"+", Pending ("+pendingRecords+")");

  }

  public void onVoiceOver(View view){
    setFadeAnimation(view);
  }

  public void onSettingTap(View view){
    setFadeAnimation(view);
    startActivity(new Intent(Settings.ACTION_SETTINGS));
  }

  private void showAlert(String message, final String type){

    final Dialog alertDialog = new Dialog(this);
    if (alertDialog!=null) {
      alertDialog.setCancelable(false);
      alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
      alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
      alertDialog.setContentView(R.layout.custom_dialog_layout);
    }
    TextView tv = alertDialog.findViewById(R.id.alerttext);
    TextView yourmessage = alertDialog.findViewById(R.id.yourmessage);
    Button allowBtn = alertDialog.findViewById(R.id.allowBtn);
    Button denyBtn = alertDialog.findViewById(R.id.denyBtn);

    tv.setText(message);
    yourmessage.setText("Tap NO to exit");
    denyBtn.setText("NO");
    allowBtn.setText("YES");
    allowBtn.setTextColor(getResources().getColor(R.color.colorAccent));
    denyBtn.setTextColor(getResources().getColor(R.color.black));
    denyBtn.setTextSize(18);

    allowBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        alertDialog.dismiss();
        if (type.equals("logout")) {
          if (Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(),"session","false")) {
            Toast.makeText(Configurations.this, "Log Out Success !", Toast.LENGTH_SHORT).show();
            Intent startRegister = new Intent(Configurations.this, MainActivity.class);
            startActivity(startRegister);
            finish();
          } else {
            Toast.makeText(Configurations.this, "Error in Log Out  !", Toast.LENGTH_SHORT).show();
          }
        }
      }
    });

    denyBtn.setOnClickListener(new View.OnClickListener() {
      public void onClick(View v) {
        alertDialog.dismiss();

      }
    });
    alertDialog.show();

  }

  private void getFrontFacingCameraId(CameraManager cManager) {
    try {
      if (Build.VERSION.SDK_INT>=21) {
        String cameraId;
        int cameraOrientation;
        CameraCharacteristics characteristics;

        for (int i = 0; i < cManager.getCameraIdList().length; i++) {
          cameraId = cManager.getCameraIdList()[i];
          characteristics = cManager.getCameraCharacteristics(cameraId);
          cameraOrientation = characteristics.get(CameraCharacteristics.LENS_FACING);

          if (cameraOrientation == CameraCharacteristics.LENS_FACING_FRONT) {
            frontfacing.setEnabled(true);
          }

          if (cameraOrientation == CameraCharacteristics.LENS_FACING_BACK) {
            rearfacing.setEnabled(true);
          }
        }

      }
    } catch (Exception e) {
      //e.printStackTrace();
    }

  }

  private void configureProgress(){
    progressDialog=new ProgressDialog(this,ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
    progressDialog.setCancelable(false);
    progressDialog.setMessage("Downloading...");
    //progressDialog.show();
  }
  //APP DOWNLOAD AND INSTALL CODE END HERE

  private void checkDeviceCamera(){

    //Camera mCamera=null;
    //Camera.CameraInfo ci = new Camera.CameraInfo();
    try {
      totalCams=Camera.getNumberOfCameras();
      cameracntText.setText("Device have " + totalCams + " camera(s)");

      if(totalCams>0) {
        // mCamera = Camera.open(FRONT_FACEING_CAMERA);
        // Camera.getCameraInfo(FRONT_FACEING_CAMERA, ci);
                /*if (totalCams==1){
                  frontfacing.setEnabled(false);
                  rearfacing.setEnabled(false);
                }*/
                //if (totalCams==2){
            frontfacing.setEnabled(true);
            rearfacing.setEnabled(true);
          //}
      } else{
        frontfacing.setEnabled(false);
        rearfacing.setEnabled(false);
        cameracntText.setText("No any Camera Detected");
        Button camsimbtn= findViewById(R.id.camsimbtn);
        camsimbtn.setVisibility(View.INVISIBLE);
      }
    } catch (Exception e) {
       cameracntText.setText("Error accessing device camera");
    }

  }

  public void onAppUpdate(View view){
    setFadeAnimation(view);

    if (updatebtn.getText().equals("USERS")){
      startActivity(new Intent(this, UsersActivity.class));
      finish();
      return;
    }

    //progressDialog=new ProgressDialog(this,ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
    //progressDialog.setCancelable(false);89
    //progressDialog.setMessage("Downloading...");
    progressDialog.show();
    //askForPermission();
    if(ContextCompat.checkSelfPermission(Configurations.this, Manifest.permission.WRITE_EXTERNAL_STORAGE)== PackageManager.PERMISSION_GRANTED) {
      Thread tr=new Thread(new Runnable() {
        @Override
        public void run() {
          if(!app_update.equals("")) {
            if (isNetworkConnected()) {
              startDownload(app_update);
            }else{
              runOnUiThread(new Runnable() {
                @Override
                public void run() {
                  Toast.makeText(Configurations.this,"NETWORK CONNECTION NOT AVAILABLE !",Toast.LENGTH_SHORT).show();
                }
              });
            }
          }
        }
      });
      tr.start();
    }else{
      askForPermission();
    }

  }

  //APP INSTALL AND DOWNLAOD CODE BEGIN HERE
  private void askForPermission(){
    if(ContextCompat.checkSelfPermission(Configurations.this,
            Manifest.permission.WRITE_EXTERNAL_STORAGE)!= PackageManager.PERMISSION_GRANTED) {
      ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE},
              555);
    }
  }

  @Override
  public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
    super.onRequestPermissionsResult(requestCode, permissions, grantResults);

    if (requestCode == 555) {

      if (ContextCompat.checkSelfPermission(Configurations.this,
              Manifest.permission.WRITE_EXTERNAL_STORAGE) == PackageManager.PERMISSION_GRANTED) {

        Thread tr=new Thread(new Runnable() {
          @Override
          public void run() {
            if(!app_update.equals("")) {
              if (isNetworkConnected()) {
                startDownload(app_update);
              }
            }
          }
        });
        tr.start();
      }
    }
  }

  private void startDownload(String urlstring){

    try {
      Log.d("cara",urlstring);
      URL url = new URL(urlstring);
      URLConnection conection = url.openConnection();
      conection.connect();
      // getting file length
      lenghtOfFile = conection.getContentLength();
      String ctype=conection.getContentType();
      // input stream to read file - with 8k buffer
      InputStream input = new BufferedInputStream(url.openStream(), 8192);
      // Output stream to write file
      final String fileName="cara.apk";
      //Log.d("RocketActivity","directory created");
      final File f= Helper.checkFile(fileName);

      FileOutputStream output = new FileOutputStream(f);
      byte data[] = new byte[1024];
      int count = 0;
      total=0;
      while ((count = input.read(data)) != -1) {
        total += count;

        runOnUiThread(new Runnable() {
          @Override
          public void run() {
            int p =  (int) ((total * 100) / lenghtOfFile);
            progressDialog.setMessage("Downloading..."+p);
          }
        });

        // writing data to file
        output.write(data, 0, count);
      }

      runOnUiThread(new Runnable() {
        @Override
        public void run() {

          new CountDownTimer(1000,1000){
            @Override
            public void onTick(long millisUntilFinished) { }
            @Override
            public void onFinish() {
              sendBroadcast(new Intent("REFRESH"));
              progressDialog.hide();
              launchNow(f);
            }
          }.start();

        }
      });

      // flushing output
      output.flush();
      // closing streams
      output.close();
      input.close();

    }catch (Exception er){
      runOnUiThread(new Runnable() {
        @Override
        public void run() {
          Toast.makeText(getApplicationContext(),"ERROR IN DOWNLOAD !",Toast.LENGTH_SHORT).show();
          progressDialog.hide();
        }
      });
      Utility.printStack(er);
    }

  }

  private void launchNow(File file){
    try {
      StrictMode.VmPolicy.Builder builder = new StrictMode.VmPolicy.Builder();
      StrictMode.setVmPolicy(builder.build());
      int result = Settings.Secure.getInt(getContentResolver(), Settings.Secure.INSTALL_NON_MARKET_APPS, 0);
      if (result == 0) {
        Intent intent = new Intent();
        intent.setAction(Settings.ACTION_APPLICATION_SETTINGS);
        startActivity(intent);
      }else {
        file.setReadable(true, false);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.setDataAndType(Uri.fromFile(file), "application/vnd.android.package-archive");
        startActivity(intent);
        finish();
      }

    }catch (Exception er){
      Utility.printStack(er);
    }
  }

  private boolean isNetworkConnected() {
    try {
      ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
      if (cm.getActiveNetworkInfo()!=null) {
        return cm.getActiveNetworkInfo().isConnected();
      }
    }catch (Exception conn){
      Utility.printStack(conn);
    }
    return  false;
  }

  public void onLogoTap(View view){
    setFadeAnimation(view);
    logotap++;
    if (logotap==5){
      updatebtn.setText("UPDATE");
      updatebtn.setVisibility(View.VISIBLE);
      logotap=0;
    }
  }

  public void onFrontFace(View view){
    setFadeAnimation(view);
    frontfacing.setChecked(true);
    rearfacing.setChecked(false);
  }

  public void onRearFace(View view){
    setFadeAnimation(view);
    frontfacing.setChecked(false);
    rearfacing.setChecked(true);
  }

  public void onNewVisitor(View view){
    setFadeAnimation(view);
  }

  public void onExitVisitor(View view){
    setFadeAnimation(view);
  }

  public void onCheckInOutMode(View view){
    CheckBox v=(CheckBox)view;
    if(v.getText().equals("OUT MODE")){
      if (outmode.isChecked()){
        inmode.setChecked(false);
      }
    }else{
      if (inmode.isChecked()){
        outmode.setChecked(false);
      }
    }
  }

  public void onRebootCheck(View v){
    setFadeAnimation(v);
    if (rebootcheck.isChecked()){
      reboottime.setVisibility(View.VISIBLE);
      reboottime.setText("03:00");
    }else{
      reboottime.setVisibility(View.INVISIBLE);
    }
  }

  public void onAccessCtrl(View view){
    setFadeAnimation(view);
    try {
      if (access_ctrl_check.isChecked()) {
        ble_mac_id.setVisibility(View.VISIBLE);
        latchType.setVisibility(View.VISIBLE);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
        //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
        //startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);             //Fire the intent to start the activity that will return a result based on user response
        if (Build.VERSION.SDK_INT <= 22) {
          mBluetoothAdapter = bluetoothManager.getAdapter();
          if (mBluetoothAdapter != null) {
            mBluetoothAdapter.enable();
          }
        } else {
          Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
          //enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
          startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);                  //Fire the intent to start the activity that will return a result based on user response
        }
      } else {
        latchType.setVisibility(View.INVISIBLE);
        ble_mac_id.setVisibility(View.INVISIBLE);
      }
    }catch (Exception er){
      //new Logger("@onAccessCtrl "+er.getMessage(),SettingsActivity.this);
    }
  }

  public void onSoftReboot(View view){
    setFadeAnimation(view);
    try{
      //progressDialog.show();
      // progressDialog.setMessage("Rebooting....");
      stopService(new Intent(this, BleService.class));
      stopService(new Intent(this, PrinterService.class));
      //sleep the main thread for 5 sec to ensure that all services has been stopped
      //new Logger("softreboot",SettingsActivity.this);

      UsbLatchConnector.getInstance().flushConnection();

      /*new Handler().postDelayed(new Runnable() {
        @Override
        public void run() {
          if(check_fingerprint.isChecked()) {
            //startService(new Intent(Configurations.this, SensorService.class));
          }
          if (access_ctrl_check.isChecked()) {
            startService(new Intent(Configurations.this, BleService.class));
          }
          if (badge_print_check.isChecked()) {
            startService(new Intent(Configurations.this, PrinterService.class));
          }

          //   progressDialog.setMessage("Reboot finish");
          Toast.makeText(Configurations.this,"Soft reboot success",Toast.LENGTH_SHORT).show();
          progressDialog.hide();
        }
      },25000); */
      sendBroadcast(new Intent("CMD_REBOOT").putExtra("access",getApplicationContext().getPackageName()));

    }catch (Exception er){
      if (progressDialog!=null)
        progressDialog.hide();
      Toast.makeText(getApplicationContext(),"Error in reboot",Toast.LENGTH_SHORT).show();
      //new Logger("Error in reboot",SettingsActivity.this);

    }

  }

  public void onThermalTick(View view){
    CaraManager.getInstance().setFadeAnimation(view);
    if (thermalCheck.isChecked()) {
      if(UsbLatchConnector.getInstance().getMcp2221()==null) {
        UsbLatchConnector.getInstance().startUsb(this);
      }
      CaraManager.getInstance().setThermal(true);
    }else{
      CaraManager.getInstance().setThermal(false);
      UsbLatchConnector.getInstance().flushConnection();
    }
  }

  public void onBadgePrint(View view){
    setFadeAnimation(view);
    try {
      if (badge_print_check.isChecked()) {
        printer_mac_id.setVisibility(View.VISIBLE);
        final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
        if (!bluetoothManager.getAdapter().isEnabled()) {                                           //Check if BT is not enabled
          //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
          //startActivityForResult(enableBtIntent,REQUEST_ENABLE_BT);                  //Fire the intent to start the activity that will return a result based on user response
          if (Build.VERSION.SDK_INT <= 22) {
            mBluetoothAdapter = bluetoothManager.getAdapter();
            if (mBluetoothAdapter != null) {
              mBluetoothAdapter.enable();
            }
          } else {
            Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
            //enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
            startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);                  //Fire the intent to start the activity that will return a result based on user response
          }
        }
      } else {
        printer_mac_id.setText("");
        printer_mac_id.setVisibility(View.INVISIBLE);
      }
    }catch (Exception er){
      Utility.printStack(er);
    }
  }

  public void onRegularVisitor(View view){
    setFadeAnimation(view);
  }

  public void enrollBtnTap(View v){
    setFadeAnimation(v);
    Intent startEnroll = new Intent(Configurations.this, EtamUserEnrol.class);
    startEnroll.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
    startEnroll.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
    startEnroll.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
    startEnroll.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
    startActivity(startEnroll);
    finish();
  }

  public void onCameraSimulate(View view){
    setFadeAnimation(view);
    CaraManager.getInstance().setSimulateOnly(true);

    if (frontfacing.isChecked()) {
      FaceManager.getInstance().setCameraId(CameraCharacteristics.LENS_FACING_FRONT);
      FaceManager.getInstance().setCamera(null);
    }

    if (rearfacing.isChecked()){
      FaceManager.getInstance().setCameraId(CameraCharacteristics.LENS_FACING_BACK);
      FaceManager.getInstance().setCamera(null);
    }

    try{
      Camera mCamera;
      //Camera.CameraInfo ci = new Camera.CameraInfo();
      totalCams=Camera.getNumberOfCameras();
      if(totalCams>0) {
        mCamera=Camera.open(FaceManager.getInstance().getCameraId());
        mCamera.stopPreview();
        mCamera.release();
        Intent startSim = new Intent(Configurations.this, CameraActivity.class);
        startActivity(startSim);
      }
    }catch (Exception er){
       Toast.makeText(getApplicationContext(),"Error in camera access id "+FaceManager.getInstance().getCameraId(),Toast.LENGTH_SHORT).show();
       Utility.printStack(er);
    }

  }

  private void setupBle(){
    final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
    mBluetoothAdapter = bluetoothManager.getAdapter();
    if (!mBluetoothAdapter.isEnabled()) {                                           //Check if BT is not enabled
      if (Build.VERSION.SDK_INT<=22) {
        mBluetoothAdapter.enable();
      }else {
        Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
        //enableBtIntent.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivityForResult(enableBtIntent, REQUEST_ENABLE_BT);                  //Fire the intent to start the activity that will return a result based on user response
      }
    }

    if (mBluetoothAdapter == null) {                                                //Check if we got the BluetoothAdapter
      Toast.makeText(getApplicationContext(), "Bluetooth not supported", Toast.LENGTH_SHORT).show(); //Message that Bluetooth not supported
      // finish();                                                                   //End the app
    }
  }

  public void onNothingSelected(AdapterView<?> arg0) {
    // TODO Auto-generated method stub
  }

  @Override
  public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {

    seletedLang = parent.getItemAtPosition(position).toString();
    //Log.d("seletedLang", seletedLang);
  }

  public void onSaveandApply(View view){
    setFadeAnimation(view);
    try {

      if (frontfacing.isChecked()) {
        cameraSel= CameraCharacteristics.LENS_FACING_FRONT;
      }
      if (rearfacing.isChecked()){
        cameraSel = CameraCharacteristics.LENS_FACING_BACK;
      }

      FaceManager.getInstance().setCameraId(cameraSel);

      if (access_ctrl_check.isChecked()){
        if (CaraManager.getInstance().getLatchComType().equals("1")) {
          if (ble_mac_id.getText().toString().equals("")) {
            Toast.makeText(getApplicationContext(), "ENTER MAC ID TO START ACCESS CONTROL FEATURE OR DISABLE IT !", Toast.LENGTH_SHORT).show();
            return;
          } else {
            String blemac = ble_mac_id.getText().toString();
            if (blemac.split(":").length != 6) {
              Toast.makeText(getApplicationContext(), "INVALID MAC ID FOR ACCESS CONTROL", Toast.LENGTH_SHORT).show();
              return;
            }
          }
        }

        if (CaraManager.getInstance().getLatchComType().equals("0")){
          Toast.makeText(getApplicationContext(), "Select latch connection !", Toast.LENGTH_SHORT).show();
          return;
        }
      }

      if (badge_print_check.isChecked()){
        if (printer_mac_id.getText().toString().equals("")){
          Toast.makeText(getApplicationContext(),"ENTER MAC ID TO START PRINTER FEATURE OR DISABLE IT !",Toast.LENGTH_SHORT).show();
          return;
        }else{
          String bleprint=printer_mac_id.getText().toString();
          if (bleprint.split(":").length!=6) {
            Toast.makeText(getApplicationContext(), "INVALID MAC ID FOR BADGE PRINTING", Toast.LENGTH_SHORT).show();
            return;
          }
        }
      }


      Helper.urlSchme = securityType.isChecked() ? "http" : "https";

      dnsvalue= Helper.getBaseUri("").build().toString();
      if (!dns.getText().toString().equals("")){
        dnsvalue=dns.getText().toString().replaceAll("https://","");
        dnsvalue=dnsvalue.replaceAll("http://","");
        Helper.SERVER_DNS = dnsvalue;
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
        progressDialog.setMessage("Domain verification begin...");
        progressDialog.show();

        CaraManager.getInstance().executeAction(Helper.COMMAND_VERIFY_DNS, getApplicationContext(), new HelperCallback() {
          @Override
          public void onResult(String result) {
            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                new Handler().postDelayed(new Runnable() {
                  @Override
                  public void run() {
                    progressDialog.hide();
                    if (result.equalsIgnoreCase("success")){
                      isValidDomain=true;
                      //Log.d("SERVER_DNS","Verification success");
                      CaraManager.getInstance().addWhiteDns(dnsvalue);
                      executeSaveSetting(dnsvalue);
                    }else {
                      Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
                    }
                  }
                },1500);
              }
            });

          }

          @Override
          public void onError(String result) {

            runOnUiThread(new Runnable() {
              @Override
              public void run() {
                new Handler().postDelayed(new Runnable() {
                  @Override
                  public void run() {
                    progressDialog.hide();

                  }
                },1500);
                isValidDomain=false;
                Toast.makeText(getApplicationContext(),result,Toast.LENGTH_SHORT).show();
              }
            });

          }
        });

        Toast.makeText(getApplicationContext(),"DNS verification started !",Toast.LENGTH_SHORT).show();
        return;

      }else {
        executeSaveSetting(dnsvalue);
      }

    }catch (Exception er){
      Toast.makeText(getApplicationContext(),"Error in setting !",Toast.LENGTH_SHORT).show();
      Utility.printStack(er);
    }

  }

  private void executeSaveSetting(final String dnsvalue){
    try{
      if (rebootcheck.isChecked()){
        if (!(reboottime.getText().toString().length()==5 && reboottime.getText().toString().contains(":"))){
          Toast.makeText(getApplicationContext(),"Invalid reboot time, must in in HH:mm format !",Toast.LENGTH_SHORT).show();
          rebootcheck.setChecked(false);
          CaraManager.getInstance().setReboot(false);
        }else {
          CaraManager.getInstance().setRebootTime(reboottime.getText().toString());
          CaraManager.getInstance().setReboot(true);
        }
      }

      if (minTempVal.getText().toString().equalsIgnoreCase("")){
        minTempVal.setText("31.0");
      }else {
        if (Float.valueOf(minTempVal.getText().toString())<20){
          Toast.makeText(getApplicationContext(),"Minimum temperature must be more than 20",Toast.LENGTH_SHORT).show();
          return;
        }
      }

      CaraManager.getInstance().setMinTempVal(Float.valueOf(minTempVal.getText().toString()));

      if (shifttime.getText().toString().equals("")){
        Toast.makeText(getApplicationContext(),"Shift time should not be empty !",Toast.LENGTH_SHORT).show();
        return;
      }else {
        if (shifttime.getText().toString().length()==2) {
          CaraManager.getInstance().setShifttime(shifttime.getText().toString());
        }else {
          Toast.makeText(getApplicationContext(),"Invalid shift time, must be HH only",Toast.LENGTH_SHORT).show();
          return;
        }

        if (!(Integer.valueOf(CaraManager.getInstance().getShifttime())<=24)){
          Toast.makeText(getApplicationContext(),"Invalid shift time, must be less than 24 hours",Toast.LENGTH_SHORT).show();
          return;
        }
      }


      try {
        if (resettime.getText().toString().equals("")) {
          Toast.makeText(getApplicationContext(), "Reset time should not be empty !", Toast.LENGTH_SHORT).show();
          return;
        } else {
          if (resettime.getText().toString().length() == 5) {
            CaraManager.setResetTime(resettime.getText().toString());
          } else {
            Toast.makeText(getApplicationContext(), "Invalid reset time, must be hh:mm only", Toast.LENGTH_SHORT).show();
            return;
          }

          String rtime=CaraManager.getResetTime().replace(":","").trim();
          if (rtime.length()==4) {
            if (!(Integer.parseInt(rtime)<=2359)) {
              Toast.makeText(getApplicationContext(), "Invalid reset time, must be less than 24 hours", Toast.LENGTH_SHORT).show();
              return;
            }
          }
        }
      }catch (Exception er){}

      try {
        if (fliptimeout.getText().toString().equals("")) {
          Toast.makeText(getApplicationContext(), "Flip timeout time should not be empty !", Toast.LENGTH_SHORT).show();
          return;
        } else {

          if (!((Long.valueOf(fliptimeout.getText().toString())*1000)<=600000)) {
            Toast.makeText(getApplicationContext(), "Invalid Flip timeout, must be less than 10min", Toast.LENGTH_SHORT).show();
            return;
          }else {
            CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(Long.valueOf(fliptimeout.getText().toString())*1000);
          }

          if (!(CaraManager.getInstance().getMINIMUM_ALLOWED_TIME()>=6000)){
            Toast.makeText(getApplicationContext(), "Invalid Flip timeout, must be more than 6sec", Toast.LENGTH_SHORT).show();
            return;
          }

        }
      }catch (Exception er){
        CaraManager.getInstance().setMINIMUM_ALLOWED_TIME(60*1000);
      }


      try {
        if (qualityvalue.getText().toString().equals("")) {
          Toast.makeText(getApplicationContext(), "Enrol quality should not be empty !", Toast.LENGTH_SHORT).show();
          return;
        } else {
            if (Float.parseFloat(qualityvalue.getText().toString())<10){
              Toast.makeText(getApplicationContext(), "Enrol quality must be greater than 10 !", Toast.LENGTH_SHORT).show();
              return;
            }
            CaraManager.getInstance().setEnrol_quality(Float.valueOf(qualityvalue.getText().toString()));
        }
      }catch (Exception er){
        CaraManager.getInstance().setEnrol_quality(20.0f);
      }


       try {
            if (liveness_value.getText().toString().equals("")) {
                Toast.makeText(getApplicationContext(), "Liveness quality should not be empty !", Toast.LENGTH_SHORT).show();
                return;
            } else {
                float v=Float.parseFloat(liveness_value.getText().toString());
                if (v<0.52){
                    Toast.makeText(getApplicationContext(), "Liveness quality must be greater than 0.52 !", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (v>0.99){
                    Toast.makeText(getApplicationContext(), "Liveness quality must be less than 0.99 !", Toast.LENGTH_SHORT).show();
                    return;
                }
                CaraManager.getInstance().setLiveness_thr(Float.valueOf(liveness_value.getText().toString()));
            }
        }catch (Exception er){
            CaraManager.getInstance().setLiveness_thr(0.80f);
        }

      String voiceOverLang=seletedLang+","+play_voice_over.isChecked();
      JSONObject saveData = new JSONObject();
      saveData.put("uuid", Utility.getUUID(getApplicationContext()));
      saveData.put("ver", BuildConfig.VERSION_CODE);
      saveData.put("version_release",BuildConfig.VERSION_CODE+","+BuildConfig.BUILD_TIME);
      saveData.put(Helper.SETTING_LATCH_MAC,ble_mac_id.getText().toString().toUpperCase());
      saveData.put(Helper.SETTING_PRINTER_MAC_ID,printer_mac_id.getText().toString().toUpperCase());
      saveData.put(Helper.SETTING_MAC, CaraManager.getInstance().getDEVICE_MACID());
      saveData.put(Helper.SETTING_DNS,dnsvalue);
      saveData.put(Helper.SETTING_ACCESS_CONTROL,access_ctrl_check.isChecked()+"");
      saveData.put(Helper.SETTING_IS_PRINTER,badge_print_check.isChecked()+"");
      saveData.put(Helper.SETTING_AUDIO,Helper.FALSE);
      saveData.put(Helper.SETTING_IS_ACTIVE,Helper.TRUE);
      saveData.put(Helper.SETTING_ACCESS_TIMEOUT,access_grant_timeout.getText().toString());
      saveData.put(Helper.SETTING_PHOTO_TIMEOUT,photo_take_timeout.getText().toString());
      saveData.put(Helper.SETTING_CAMERA,FaceManager.getInstance().getCameraId()+"");
      saveData.put(Helper.SETTING_LANG,voiceOverLang);
      saveData.put(Helper.SETTING_LATCHTYPE, CaraManager.getInstance().getLatchComType());
      saveData.put(Helper.SETTING_PRINTER_TYPE,"");
      saveData.put(Helper.SETTING_THERMAL,thermalCheck.isChecked()+"");
      saveData.put(Helper.SETTING_INMODE,inmode.isChecked()+"");
      saveData.put(Helper.SETTING_OUTMODE,outmode.isChecked()+"");
      saveData.put(Helper.SETTING_ISREBOOT,rebootcheck.isChecked()+"");
      saveData.put(Helper.SETTING_REBOOT_HHMM,reboottime.getText().toString());
      saveData.put(Helper.SETTING_ISQUALITY,qualitycheck.isChecked()+"");
      saveData.put(Helper.SETTING_AUTOFLASH,autoflash.isChecked()+"");
      saveData.put(Helper.SETTING_SCALE_TEMP,scalevalue.getText().toString());
      saveData.put(Helper.SETTING_PLAYALARM,playAlarm.isChecked()+"");
      saveData.put(Helper.SETTING_MAXTEMP,thermal_temp_value.getText().toString());
      saveData.put(Helper.SETTING_ISDEGREE,thermalMode.isChecked()+"");
      saveData.put(Helper.SETTING_ISLIVENESS,liveness_check.isChecked()+"");
      saveData.put(Helper.SETTING_ISCENTER,readMode.isChecked()+"");
      saveData.put(Helper.SETTING_URLTYPE,securityType.isChecked()?"http":"https");
      saveData.put(Helper.SETTING_FACE_THR,FaceManager.getInstance().getFaceTheshold()+"");
      saveData.put(Helper.SETTING_MINTEMP,CaraManager.getInstance().getMinTempVal()+"");
      saveData.put(Helper.SETTING_SHIFT_TIME,CaraManager.getInstance().getShifttime());
      saveData.put(Helper.SETTING_RESET_TIME,CaraManager.getResetTime());
      saveData.put(Helper.SETTING_FLIP_TIMEOUT,fliptimeout.getText().toString());
      saveData.put(Helper.SETTING_ENROL_QUALITY,CaraManager.getInstance().getEnrol_quality()+"");
      saveData.put(Helper.SETTING_LIVENESS_THR,CaraManager.getInstance().getLiveness_thr()+"");


      Helper.urlSchme = securityType.isChecked() ? "http" : "https";
      if (thermal_temp_value.getText().toString().equals("")){
        Toast.makeText(getApplicationContext(),"Max Temperature value is 1",Toast.LENGTH_SHORT).show();
        return;
      }

      if (scalevalue.getText().toString().equals("") || scalevalue.getText().toString().equals("0")){
        Toast.makeText(getApplicationContext(),"Scale value must be 1 or more",Toast.LENGTH_SHORT).show();
        return;
      }

      if (!(inmode.isChecked() || outmode.isChecked())){
        CaraManager.getInstance().setInoutMode("-1");
      }else{
        if (inmode.isChecked()){
          CaraManager.getInstance().setInoutMode("1");
        }else if (outmode.isChecked()){
          CaraManager.getInstance().setInoutMode("0");
        }
      }

      CaraManager.getInstance().setThermal(thermalCheck.isChecked());
      CaraManager.getInstance().setQuality(qualitycheck.isChecked());
      CaraManager.getInstance().setAutoFlash(autoflash.isChecked());
      CaraManager.getInstance().setTempUnit(thermalMode.isChecked()?"C":"F");
      CaraManager.getInstance().setAlarm(playAlarm.isChecked());
      CaraManager.getInstance().setScaleTemp(Float.valueOf(scalevalue.getText().toString()));
      CaraManager.getInstance().setLiveness(liveness_check.isChecked());
      CaraManager.getInstance().setReadCenter(readMode.isChecked());

      if(Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(),"settings",saveData.toString())) {
         Toast.makeText(getApplicationContext(), "Settings updated", Toast.LENGTH_SHORT).show();
      }

      if (access_ctrl_check.isChecked()){
        try{
          CaraManager.getInstance().setLatchEnable(true);
          // Latch Connection for Dwin
          if (Build.MANUFACTURER.equalsIgnoreCase("dwin")) {
//            if (!CaraManager.getInstance().isLatchConnected()) {
//              CaraManager.getInstance().connectAccessControlDwin(getApplicationContext(),50);
//            }
//            else
//            {
//              GpioControlUtil.getInstance().getGpioIntputValue(166);
//
//              if (GpioControlUtil.nRet == -1) {
//                CaraManager.getInstance().connectAccessControlDwin(getApplicationContext(),100);
//              }
//            }

          }
          else
          {
            if (!CaraManager.getInstance().getLatchComType().equals("2")) {
              //final BluetoothManager bluetoothManager = (BluetoothManager) getSystemService(Context.BLUETOOTH_SERVICE); //Get the BluetoothManager
              BluetoothAdapter mBluetoothAdapter = BluetoothAdapter.getDefaultAdapter();                                         //BluetoothAdapter controls the Bluetooth radio in the phone
              //mBluetoothAdapter = bluetoothManager.getAdapter();
              if (!mBluetoothAdapter.isEnabled()) {                                           //Check if BT is not enabled
                //Intent enableBtIntent = new Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE); //Create an intent to get permission to enable BT
                //startActivity(enableBtIntent);                  //Fire the intent to start the activity that will return a result based on user response
                mBluetoothAdapter.enable();
              } else {
                Intent bleservice = new Intent(this, BleService.class);
                if (!CaraManager.getInstance().isLatchConnected()) {
                  stopService(bleservice);
                  new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                      startService(bleservice);
                    }
                  }, 1500);
                }
              }
              }else{
                //usb latch type
                if(UsbLatchConnector.getInstance().getMcp2221()==null){
                  if (CaraManager.getInstance().isLatchEnable()) {
                    UsbLatchConnector.getInstance().startUsb(this);
                  }
                }
              }
            }

        }catch (Exception er){
          Log.d("exception",er.toString());
          Utility.printStack(er);
        }
      }else{
        CaraManager.getInstance().setLatchEnable(false);
        CaraManager.getInstance().setLatchConnected(false);
        sendBroadcast(new Intent("STOP_BLE"));
      }

      if (badge_print_check.isChecked()){ startService(new Intent(this, PrinterService.class));  }else{
        stopService(new Intent(this, PrinterService.class));
      }

      writeToFile(saveData.toString(),"cara.config");
      CaraManager.getInstance().reportHealth(getApplicationContext());

      Intent intent=new Intent(Configurations.this, MainActivity.class);
      intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
      intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
      startActivity(intent);
      finish();
    }catch (Exception er){
      Utility.printStack(er);
    }
  }

  public void onSignOutBtn(View view){
    setFadeAnimation(view);
    showAlert("Are you sure, you want to logout?","logout");
  }

  private void setFadeAnimation(View view) {
    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
    fadeIn.setDuration(500);

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
    fadeOut.setStartOffset(500);
    fadeOut.setDuration(500);

    view.startAnimation(fadeOut);
    view.startAnimation(fadeIn);
  }

  private void writeToFile(String data, String fileName) {
    try {
      String root = Environment.getExternalStorageDirectory().getAbsolutePath();
      String dirPath = "/.caratemp";
      File myDir = new File(root + dirPath);
      myDir.mkdirs();

      try {
        File file = new File(myDir, fileName);
        if (!file.exists()) {
          file.createNewFile();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        OutputStreamWriter fileDis = new OutputStreamWriter(fileOutputStream);
        fileDis.write(data);
        fileDis.close();
        fileOutputStream.close();
        // sendNow(true,file);
      } catch (Exception e) {
        //e.printStackTrace();
      }
    }catch (Exception er){
      Utility.printStack(er);
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        /*if (requestCode ==  REQUEST_ENABLE_BT && resultCode == RESULT_OK) {

        }*/
    Intent keepthis =new Intent(this, Configurations.class);
    keepthis.addFlags(Intent.FLAG_ACTIVITY_REORDER_TO_FRONT);
    keepthis.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
    startActivity(keepthis);
  }

  @Override
  protected void onPause() {
    super.onPause();

    InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
    imm.hideSoftInputFromWindow(dns.getWindowToken(), 0);
  }

  @Override
  public void onStop() {
    super.onStop();

    if (progressDialog!=null){
      progressDialog.cancel();
    }
    if (settingupdate!=null){
      unregisterReceiver(settingupdate);
    }
  }

  @Override
  public void onResume(){
    super.onResume();

    try {
      IntentFilter intentFilter = new IntentFilter();
      intentFilter.addAction(SENSOR_LOAD_FINISH);
      intentFilter.addAction(ACTION_BLE_CONNECT);
      intentFilter.addAction("IM_DEAD");
      intentFilter.addAction("ACTION_BLE_DISCONNECT");
      intentFilter.addAction("ACCESS_CONTROL_STARTED");
      intentFilter.addAction("SENSOR_STARTED");
      registerReceiver(settingupdate, intentFilter);
    }catch (Exception er){}


    boolean isVendor=false;
    for (DnsModel wt_dns:CaraManager.getInstance().getWhiteListDns()) {
      //String wt_dns_ = wt_dns.substring(3);
      //Log.d("SERVER_DNS","white dns ->"+wt_dns);
      if (wt_dns.getDns().equalsIgnoreCase(Helper.SERVER_DNS) && wt_dns.getDnstype().equalsIgnoreCase("vendor")){
        isVendor=true;
      }

    }

    if (isVendor){
      app_update = "https://attendanceportal.com/pk/dwin/caralic/cara.apk";
    }else{
      String exturl = CaraManager.getInstance().isLicVersion() ? "/pk/dwin/caralic/cara.apk":"/pk/dwin/caralic/cara.apk";
      app_update = Helper.getBaseUri("").build().toString() + exturl;
    }

    //Log.d("SERVER_DNS",app_update);

    CaraManager.getInstance().sendSignal("SETTINGS_ACCESSED",getApplicationContext());

  }

  @Override
  public void onBackPressed() {
    Intent startWelcome= new Intent(this, MainActivity.class);
    startActivity(startWelcome);
    finish();
  }

}
