package android.wyse.face;

import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.graphics.Typeface;
import android.hardware.Camera;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.StrictMode;
import android.provider.Settings;
import android.telephony.TelephonyManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.Display;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemSelectedListener;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.TimeZone;

/**
 * Created by cis on 20/05/17.
 */
public class RegisterKiosk extends Activity implements OnItemSelectedListener {


    private final HashMap<String, Integer> mLengths = new HashMap<>();
    private EditText kiosName;
    private EditText kiosMobile;
    private EditText kiosAddress;
    private EditText kiosEmail;
    private EditText kiosPassword;
    private String name;
    private String mobile;
    private String address;
    private String kiosip;
    private String kiosport;
    private String kiospassword;
    private String kiosemail;
    private String type = "check";
    private String version = "";
    private String osname = "";
    private String deviceName = "";
    private String deviceManf = "";
    private String networkType = "";
    private ProgressDialog pd;
    private int totalCams = 0;
    private String timeZone = "";
    private String utimeZone = "";
    private int width;
    private int height;
    private int phoneNumberLen = 10;
    private String uuid = "";
    private String build = "";
    private Helper helper;

    private DevicePolicyManager mDPM;
    private ComponentName adminComponent;
    private Thread sendPost;

    private void enableAppAdmin(Context context) {
        if (!mDPM.isAdminActive(adminComponent)) {
            Intent activateDeviceAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Tap on Activate to use this device");
            context.startActivity(activateDeviceAdmin);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);


        if (android.os.Build.VERSION.SDK_INT > 9) {
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
        }

        try {
            requestWindowFeature(Window.FEATURE_NO_TITLE);
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION |
                    View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
            Helper.secureScreen(getWindow());
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
            setContentView(R.layout.activity_kios);
            setWindowSize();
        } catch (Exception er) {
        }

        mDPM = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = CaraAdmin.getComponentName(this);

        if (android.os.Build.VERSION.SDK_INT >= 23 && !Settings.canDrawOverlays(this)) {
            Intent intent = new Intent(Settings.ACTION_MANAGE_OVERLAY_PERMISSION, Uri.parse("package:" + this.getApplicationContext().getPackageName()));
            startActivity(intent);
        } else {
            try {
                enableAppAdmin(this);
            } catch (Exception er) {
            }
        }

        try {
            PackageInfo pInfo = this.getPackageManager().getPackageInfo(getPackageName(), 0);
            version = pInfo.versionName;
            build = pInfo.versionCode + "";
            uuid = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            //Field[] fields = Build.VERSION_CODES.class.getFields();
            osname = android.os.Build.VERSION.SDK_INT + "";
            deviceName = android.os.Build.MODEL;
            deviceManf = android.os.Build.MANUFACTURER;
            totalCams = Camera.getNumberOfCameras();

            networkType = getNetworkClass(this);
            Calendar cal = Calendar.getInstance();
            TimeZone tz = cal.getTimeZone();
            timeZone = tz.getID();

            // Log.d("Android OsName:",deviceName+","+deviceManf);
        } catch (PackageManager.NameNotFoundException e) {
        }

        helper = new Helper();
        //getSelfImage();
        Typeface typeface = Typeface.createFromAsset(getAssets(), getString(R.string.font_name));
        kiosName = findViewById(R.id.nameText);
        kiosName.setTypeface(typeface);
        kiosMobile = findViewById(R.id.mobileText);
        kiosMobile.setTypeface(typeface);
        kiosAddress = findViewById(R.id.addressText);
        kiosAddress.setTypeface(typeface);
        kiosEmail = findViewById(R.id.emailText);
        kiosEmail.setTypeface(typeface);
        kiosPassword = findViewById(R.id.passwordText);
        kiosPassword.setTypeface(typeface);
        uuid = android.provider.Settings.Secure.getString(getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
        kiosMobile.setInputType(InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_VARIATION_PASSWORD);
        kiosMobile.setTransformationMethod(new NumericKeyBoardTransformationMethod());
        setupSpinner();
        setupCountry();
    }

    private void setupCountry() {
        // Spinner element
        Spinner spinner = findViewById(R.id.countrys);
        // Spinner click listener
        spinner.setOnItemSelectedListener(this);
        // Spinner Drop down elements
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Asia/Kolkata");
        categories.add("Asia/Singapore");
        categories.add("America/New_York");
        categories.add("America/Toronto");
        categories.add("Asia/Qatar");
        categories.add("Asia/Muscat");
        categories.add("Asia/Dubai");
        categories.add("UAE");
        categories.add("UK");
        categories.add("Asia/Sri_Lanka");
        categories.add("Europe/Spain");
        categories.add("Pacific/Midway");
        categories.add("US/Samoa");
        categories.add("US/Hawaii");
        categories.add("US/Alaska");
        categories.add("US/Pacific");
        categories.add("America/Tijuana");
        categories.add("US/Arizona");
        categories.add("US/Mountain");
        categories.add("America/Chihuahua");
        categories.add("America/Mazatlan");
        categories.add("America/Mexico_City");
        categories.add("America/Monterrey");
        categories.add("Canada/Saskatchewan");
        categories.add("US/Central");
        categories.add("US/Eastern");
        categories.add("US/East-Indiana");
        categories.add("America/Lima");
        categories.add("America/Caracas");
        categories.add("Canada/Atlantic");
        categories.add("America/La_Paz");
        categories.add("America/Santiago");
        categories.add("Canada/Newfoundland");
        categories.add("America/Buenos_Aires");
        categories.add("Greenland");
        categories.add("Atlantic/Stanley");
        categories.add("Atlantic/Azores");
        categories.add("Atlantic/Cape_Verde");
        categories.add("Africa/Casablanca");
        categories.add("Africa/Casablanca");
        categories.add("Europe/Dublin");
        categories.add("Europe/Lisbon");
        categories.add("Europe/London");
        categories.add("Africa/Monrovia");
        categories.add("Europe/Amsterdam");
        categories.add("Europe/Belgrade");
        categories.add("Europe/Berlin");
        categories.add("Europe/Bratislava");
        categories.add("Europe/Brussels");
        categories.add("Europe/Budapest");
        categories.add("Europe/Copenhagen");
        categories.add("Europe/Ljubljana");
        categories.add("Europe/Madrid");
        categories.add("Europe/Paris");
        categories.add("Europe/Prague");
        categories.add("Europe/Rome");
        categories.add("Eurpose/Norway");
        categories.add("Europe/Sarajevo");
        categories.add("Europe/Skopje");
        categories.add("Europe/Stockholm");
        categories.add("Europe/Vienna");
        categories.add("Europe/Warsaw");
        categories.add("Europe/Zagreb");
        categories.add("Europe/Athens");
        categories.add("Europe/Bucharest");
        categories.add("Africa/Cairo");
        categories.add("Africa/Harare");
        categories.add("Europe/Helsinki");
        categories.add("Europe/Istanbul");
        categories.add("Asia/Jerusalem");
        categories.add("Europe/Kiev");
        categories.add("Europe/Minsk");
        categories.add("Europe/Riga");
        categories.add("Europe/Sofia");
        categories.add("Europe/Tallinn");
        categories.add("Europe/Vilnius");
        categories.add("Asia/Baghdad");
        categories.add("Asia/Kuwait");
        categories.add("Africa/Nairobi");
        categories.add("Asia/Riyadh");
        categories.add("Europe/Moscow");
        categories.add("Asia/Tehran");
        categories.add("Asia/Baku");
        categories.add("Europe/Volgograd");
        categories.add("Asia/Tbilisi");
        categories.add("Asia/Yerevan");
        categories.add("Asia/Kabul");
        categories.add("Asia/Karachi");
        categories.add("Asia/Tashkent");
        categories.add("Asia/Kathmandu");
        categories.add("Asia/Yekaterinburg");
        categories.add("Asia/Almaty");
        categories.add("Asia/Dhaka");
        categories.add("Asia/Novosibirsk");
        categories.add("Asia/Bangkok");
        categories.add("Asia/Jakarta");
        categories.add("Asia/Krasnoyarsk");
        categories.add("Asia/Chongqing");
        categories.add("Asia/Hong_Kong");
        categories.add("Asia/Kuala_Lumpur");
        categories.add("Asia/Taipei");
        categories.add("Asia/Ulaanbaatar");
        categories.add("Asia/Urumqi");
        categories.add("Asia/Irkutsk");
        categories.add("Asia/Seoul");
        categories.add("Asia/Tokyo");
        categories.add("Asia/Yakutsk");
        categories.add("Asia/Vladivostok");
        categories.add("Asia/Magadan");
        categories.add("Australia/Perth");
        categories.add("Australia/Adelaide");
        categories.add("Australia/Brisbane");
        categories.add("Australia/Darwin");
        categories.add("Australia/Canberra");
        categories.add("Australia/Hobart");
        categories.add("Australia/Melbourne");
        categories.add("Australia/Sydney");
        categories.add("Pacific/Guam");
        categories.add("Pacific/Port_Moresby");
        categories.add("Pacific/Auckland");
        categories.add("Pacific/Fiji");

        // Creating adapter for spinner
        ArrayAdapter<String> dataAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, categories);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        // attaching data adapter to spinner
        spinner.setAdapter(dataAdapter);

        mLengths.put("Asia/Kolkata", 10);
        mLengths.put("Asia/Singapore", 8);
        mLengths.put("America/New_York", 10);
        mLengths.put("America/Toronto", 10);
        mLengths.put("Asia/Qatar", 8);
        mLengths.put("Asia/Muscat", 8);
        mLengths.put("Asia/Dubai", 8);
        mLengths.put("UAE", 9);
        mLengths.put("UK", 10);
        mLengths.put("Asia/Sri_Lanka", 7);
        mLengths.put("Europe/Spain", 9);
        mLengths.put("Pacific/Midway", 10);
        mLengths.put("US/Samoa", 10);
        mLengths.put("US/Hawaii", 10);
        mLengths.put("US/Alaska", 10);
        mLengths.put("US/Pacific", 10);
        mLengths.put("America/Tijuana", 10);
        mLengths.put("US/Arizona", 10);
        mLengths.put("US/Mountain", 10);
        mLengths.put("America/Chihuahua", 10);
        mLengths.put("America/Mazatlan", 10);
        mLengths.put("America/Mexico_City", 10);
        mLengths.put("America/Monterrey", 10);
        mLengths.put("Canada/Saskatchewan", 10);
        mLengths.put("US/Central", 10);
        mLengths.put("US/Eastern", 10);
        mLengths.put("US/East-Indiana", 10);
        mLengths.put("America/Lima", 10);
        mLengths.put("America/Caracas", 10);
        mLengths.put("Canada/Atlantic", 10);
        mLengths.put("America/La_Paz", 10);
        mLengths.put("America/Santiago", 10);
        mLengths.put("Canada/Newfoundland", 10);
        mLengths.put("America/Buenos_Aires", 10);
        mLengths.put("Greenland", 10);
        mLengths.put("Atlantic/Stanley", 10);
        mLengths.put("Atlantic/Azores", 10);
        mLengths.put("Atlantic/Cape_Verde", 10);
        mLengths.put("Africa/Casablanca", 9);
        mLengths.put("Africa/Casablanca", 9);
        mLengths.put("Europe/Dublin", 10);
        mLengths.put("Europe/Lisbon", 10);
        mLengths.put("Europe/London", 10);
        mLengths.put("Africa/Monrovia", 10);
        mLengths.put("Europe/Amsterdam", 10);
        mLengths.put("Europe/Belgrade", 10);
        mLengths.put("Europe/Berlin", 10);
        mLengths.put("Europe/Bratislava", 10);
        mLengths.put("Europe/Brussels", 10);
        mLengths.put("Europe/Budapest", 10);
        mLengths.put("Europe/Copenhagen", 10);
        mLengths.put("Europe/Ljubljana", 10);
        mLengths.put("Europe/Madrid", 10);
        mLengths.put("Europe/Paris", 10);
        mLengths.put("Europe/Prague", 10);
        mLengths.put("Europe/Rome", 10);
        mLengths.put("Eurpose/Norway", 10);
        mLengths.put("Europe/Sarajevo", 10);
        mLengths.put("Europe/Skopje", 10);
        mLengths.put("Europe/Stockholm", 10);
        mLengths.put("Europe/Vienna", 10);
        mLengths.put("Europe/Warsaw", 10);
        mLengths.put("Europe/Zagreb", 10);
        mLengths.put("Europe/Athens", 10);
        mLengths.put("Europe/Bucharest", 9);
        mLengths.put("Africa/Cairo", 10);
        mLengths.put("Africa/Harare", 10);
        mLengths.put("Europe/Helsinki", 10);
        mLengths.put("Europe/Istanbul", 10);
        mLengths.put("Asia/Jerusalem", 10);
        mLengths.put("Europe/Kiev", 10);
        mLengths.put("Europe/Minsk", 10);
        mLengths.put("Europe/Riga", 10);
        mLengths.put("Europe/Sofia", 10);
        mLengths.put("Europe/Tallinn", 10);
        mLengths.put("Europe/Vilnius", 10);
        mLengths.put("Asia/Baghdad", 10);
        mLengths.put("Asia/Kuwait", 8);
        mLengths.put("Africa/Nairobi", 9);
        mLengths.put("Asia/Riyadh", 10);
        mLengths.put("Europe/Moscow", 10);
        mLengths.put("Asia/Tehran", 10);
        mLengths.put("Asia/Baku", 10);
        mLengths.put("Europe/Volgograd", 10);
        mLengths.put("Asia/Tbilisi", 9);
        mLengths.put("Asia/Yerevan", 10);
        mLengths.put("Asia/Kabul", 10);
        mLengths.put("Asia/Karachi", 10);
        mLengths.put("Asia/Tashkent", 10);
        mLengths.put("Asia/Kathmandu", 10);
        mLengths.put("Asia/Yekaterinburg", 10);
        mLengths.put("Asia/Almaty", 10);
        mLengths.put("Asia/Dhaka", 10);
        mLengths.put("Asia/Novosibirsk", 10);
        mLengths.put("Asia/Bangkok", 9);
        mLengths.put("Asia/Jakarta", 11);
        mLengths.put("Asia/Krasnoyarsk", 10);
        mLengths.put("Asia/Chongqing", 11);
        mLengths.put("Asia/Hong_Kong", 8);
        mLengths.put("Asia/Kuala_Lumpur", 9);
        mLengths.put("Asia/Taipei", 9);
        mLengths.put("Asia/Ulaanbaatar", 8);
        mLengths.put("Asia/Urumqi", 11);
        mLengths.put("Asia/Irkutsk", 10);
        mLengths.put("Asia/Seoul", 10);
        mLengths.put("Asia/Tokyo", 10);
        mLengths.put("Asia/Yakutsk", 10);
        mLengths.put("Asia/Vladivostok", 10);
        mLengths.put("Asia/Magadan", 10);
        mLengths.put("Australia/Perth", 9);
        mLengths.put("Australia/Adelaide", 9);
        mLengths.put("Australia/Brisbane", 9);
        mLengths.put("Australia/Darwin", 9);
        mLengths.put("Australia/Canberra", 9);
        mLengths.put("Australia/Hobart", 9);
        mLengths.put("Australia/Melbourne", 9);
        mLengths.put("Australia/Sydney", 9);
        mLengths.put("Pacific/Guam", 10);
        mLengths.put("Pacific/Port_Moresby", 9);
        mLengths.put("Pacific/Auckland", 9);
        mLengths.put("Pacific/Fiji", 10);

        utimeZone = spinner.getSelectedItem().toString();

        //Log.d("utimeZone", utimeZone);

        for (int i = 0; i < categories.size(); i++) {
            if (categories.get(i).equalsIgnoreCase(timeZone)) {
                spinner.setSelection(i);
                phoneNumberLen = mLengths.get(spinner.getSelectedItem().toString());
            }
        }

    }

    public void onNothingSelected(AdapterView<?> arg0) {
        // TODO Auto-generated method stub
    }

    @Override
    public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
        // On selecting a spinner item
        utimeZone = parent.getItemAtPosition(position).toString();
        phoneNumberLen = mLengths.get(utimeZone);
        // Showing selected spinner item
        //Toast.makeText(parent.getContext(), "Selected: " + item+" "+phoneNumberLen, Toast.LENGTH_LONG).show();
    }

    private void setupRegistration() {

        kiosName.setVisibility(View.VISIBLE);
        kiosAddress.setVisibility(View.VISIBLE);
        kiosEmail.setVisibility(View.VISIBLE);
        kiosEmail.setVisibility(View.VISIBLE);
        kiosPassword.setVisibility(View.VISIBLE);
    }

    private void setupSpinner() {
        pd = new ProgressDialog(RegisterKiosk.this);
        // Set progress dialog style spinner
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        pd.setMessage("Wait...");
        pd.setIndeterminate(false);
    }

    public void onRegister(View view) {
        setFadeAnimation(view);

        name = kiosName.getText().toString().trim();
        mobile = kiosMobile.getText().toString().trim();
        address = kiosAddress.getText().toString().trim();
        kiosemail = kiosEmail.getText().toString().trim();
        kiospassword = kiosPassword.getText().toString().trim();

        String KIOS_REG_API = "";
        if (type.equals("check")) {

            if (!mobile.equals("")) {

                Uri.Builder uribuilder = Helper.getBaseUri("");
                uribuilder.appendPath("register.php");
                uribuilder.appendQueryParameter("type", type);
                uribuilder.appendQueryParameter("uid", mobile);
                KIOS_REG_API = uribuilder.build().toString();
                //Log.d(getLocalClassName(),KIOS_REG_API);
                //String url = KIOS_REG_API + "uid=" + mobile+"&type="+type;
                try {
                    JSONObject kiosData = new JSONObject();
                    kiosData.put("mobile", mobile);
                    kiosData.put("uuid", uuid);
                    kiosData.put("pw", kiospassword);
                    kiosData.put("email", kiosemail);
                    kiosData.put("build", build);
                    kiosData.put("ver", version);
                    kiosData.put("osname", osname);
                    kiosData.put("devicename", deviceName);
                    kiosData.put("maf", deviceManf);
                    kiosData.put("t_cam", totalCams);
                    kiosData.put("otp_enable", true);
                    kiosData.put("net_type", networkType);
                    kiosData.put("window_size", width + "," + height);
                    kiosData.put("timeZone", timeZone);
                    kiosData.put("utimeZone", timeZone);
                    kiosData.put("phoneNumLen", phoneNumberLen);

                    Toast.makeText(this, "checking for login, wait...", Toast.LENGTH_SHORT).show();
                    try {
                        pd.show();
                    } catch (Exception er) {
                    }
                    sendPostData(KIOS_REG_API, kiosData.toString());

                } catch (Exception jsonEx) {
                }
            } else {
                Toast.makeText(this, "Fill all details", Toast.LENGTH_SHORT).show();
                return;
            }

        } else if (type.equals("register")) {
            if (!(name.equals("") || mobile.equals("") || address.equals("") || kiosemail.equals("") || kiospassword.equals(""))) {

                if (kiospassword.length() < 6) {
                    Toast.makeText(this, "Pass code length must be 6 or more digits", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mobile == null) {
                    Toast.makeText(RegisterKiosk.this, "Mobile number missing or invalid", Toast.LENGTH_SHORT).show();
                    return;
                }

                if (mobile.length() >= 10) {
                    try {
                        JSONObject kiosData = new JSONObject();
                        kiosData.put("mobile", mobile);
                        kiosData.put("address", address);
                        kiosData.put("name", name);
                        kiosData.put("uuid", uuid);
                        kiosData.put("email", kiosemail);
                        kiosData.put("pw", kiospassword);
                        kiosData.put("ver", version);
                        kiosData.put("build", build);
                        kiosData.put("osname", osname);
                        kiosData.put("devicename", deviceName);
                        kiosData.put("maf", deviceManf);
                        kiosData.put("t_cam", totalCams);
                        kiosData.put("otp_enable", true);
                        kiosData.put("net_type", networkType);
                        kiosData.put("window_size", width + "," + height);
                        kiosData.put("timeZone", timeZone);
                        kiosData.put("utimeZone", utimeZone);
                        kiosData.put("phoneNumberLen", phoneNumberLen);

                        Uri.Builder uribuilder = Helper.getBaseUri("");
                        uribuilder.appendPath("register.php");
                        uribuilder.appendQueryParameter("type", type);
                        uribuilder.appendQueryParameter("uid", mobile);
                        KIOS_REG_API = uribuilder.build().toString();

                        Toast.makeText(this, "CHECKING...", Toast.LENGTH_SHORT).show();
                        pd.show();
                        sendPostData(KIOS_REG_API, kiosData.toString());
                    } catch (Exception er) {
                        //er.printStackTrace();
                    }

                } else {
                    Toast.makeText(RegisterKiosk.this, "Mobile number for VisiMaster, must be 10 or more digit long", Toast.LENGTH_LONG).show();
                }

            } else {
                Toast.makeText(this, "Fill all  fields !", Toast.LENGTH_SHORT).show();
            }

        } else if (type.equals("login")) {

            if (!(mobile.equals("") || kiospassword.equals(""))) {

                Uri.Builder uribuilder = Helper.getBaseUri("");
                uribuilder.appendPath("register.php");
                uribuilder.appendQueryParameter("type", type);
                uribuilder.appendQueryParameter("uid", mobile);
                KIOS_REG_API = uribuilder.build().toString();
                // Log.d(getLocalClassName(),KIOS_REG_API);
                // String url = KIOS_REG_API + "uid=" + mobile+"&type="+type;
                try {
                    JSONObject kiosData = new JSONObject();
                    kiosData.put("mobile", mobile);
                    kiosData.put("uuid", uuid);
                    kiosData.put("pw", kiospassword);
                    kiosData.put("ver", version);
                    kiosData.put("build", build);
                    kiosData.put("osname", osname);
                    kiosData.put("devicename", deviceName);
                    kiosData.put("maf", deviceManf);
                    kiosData.put("t_cam", totalCams);
                    kiosData.put("otp_enable", true);
                    kiosData.put("net_type", networkType);
                    kiosData.put("window_size", width + "," + height);
                    kiosData.put("timeZone", timeZone);
                    kiosData.put("utimeZone", utimeZone);
                    kiosData.put("phoneNumberLen", phoneNumberLen);

                    Toast.makeText(this, "Login into System, wait...", Toast.LENGTH_SHORT).show();
                    pd.show();
                    sendPostData(KIOS_REG_API, kiosData.toString());
                } catch (Exception jsonEx) {
                    // jsonEx.printStackTrace();
                }

            } else {
                Toast.makeText(this, "Mobile or Login pin is missing", Toast.LENGTH_SHORT).show();
                return;
            }
        }

    }

    private void showNoNetwork() {
        Toast.makeText(RegisterKiosk.this, "No Network Connection !", Toast.LENGTH_LONG).show();
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

    private void setWindowSize() {

        Display display = getWindowManager().getDefaultDisplay();
        Point size = new Point();
        display.getSize(size);
        width = size.x;
        height = size.y;

    }

    private String getNetworkClass(Context context) {
        TelephonyManager mTelephonyManager = (TelephonyManager)
                context.getSystemService(Context.TELEPHONY_SERVICE);
        int networkType = mTelephonyManager.getNetworkType();
        switch (networkType) {
            case TelephonyManager.NETWORK_TYPE_GPRS:
            case TelephonyManager.NETWORK_TYPE_EDGE:
            case TelephonyManager.NETWORK_TYPE_CDMA:
            case TelephonyManager.NETWORK_TYPE_1xRTT:
            case TelephonyManager.NETWORK_TYPE_IDEN:
                return "2G";
            case TelephonyManager.NETWORK_TYPE_UMTS:
            case TelephonyManager.NETWORK_TYPE_EVDO_0:
            case TelephonyManager.NETWORK_TYPE_EVDO_A:
            case TelephonyManager.NETWORK_TYPE_HSDPA:
            case TelephonyManager.NETWORK_TYPE_HSUPA:
            case TelephonyManager.NETWORK_TYPE_HSPA:
            case TelephonyManager.NETWORK_TYPE_EVDO_B:
            case TelephonyManager.NETWORK_TYPE_EHRPD:
            case TelephonyManager.NETWORK_TYPE_HSPAP:
                return "3G";
            case TelephonyManager.NETWORK_TYPE_LTE:
                return "4G";
            default:
                return "Unknown";
        }
    }

    private void sendPostData(final String uri, final String json) {
        sendPost = new Thread() {
            @Override
            public void run() {

                Log.d("uri", uri);
                Log.d("data", json);

                if (helper.isNetworkConnected(getApplicationContext())) {

                    HttpURLConnection urlConnection;
                    String result = null;
                    try {
                        //Connect
                        urlConnection = (HttpURLConnection) ((new URL(uri).openConnection()));
                        //urlConnection.setDoOutput(true);
                        //urlConnection.setConnectTimeout(10000);
                        urlConnection.setRequestProperty("Content-Type", "application/json");
                        urlConnection.setRequestProperty("Accept", "application/json");
                        urlConnection.setRequestMethod("POST");
                        urlConnection.connect();

                        //Write
                        OutputStream outputStream = urlConnection.getOutputStream();
                        BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                        writer.write(json);
                        writer.close();
                        outputStream.close();
                        //Read
                        BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream(), "UTF-8"));

                        String line = null;
                        StringBuilder sb = new StringBuilder();

                        while ((line = bufferedReader.readLine()) != null) {
                            sb.append(line);
                        }

                        bufferedReader.close();
                        result = sb.toString().trim();
                        Log.d("result", result);
                        if (!result.equals("")) {

                            JSONObject res = new JSONObject(result);
                            if ((res.getString("output").equals("success"))) {

                                String type_ = res.getString("type");

                                if (type_.equals("register")) {

                                    //  Log.d("update", "udpate code success");
                                    kiosip = res.getString("ip");
                                    kiosport = res.getString("port");
                                    final String hasUsers = res.getString("hasUsers");

                                    try {
                                        Helper.SERVER_DNS = res.getString("dns");
                                        Helper.SERVER_IP = kiosip;
                                        Helper.SERVER_PORT = Integer.valueOf(kiosport);
                                        Helper.urlSchme = res.getString("urltype");
                                    } catch (Exception er) {
                                    }

                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "KIOSK_ID", mobile);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "KIOSK_HASH", helper.createMd5(mobile));
                                    //Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(),"KIOSK_HASH",helper.createMd5(mobile));
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "SERVER_IP", Helper.SERVER_IP);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "SERVER_PORT", Helper.SERVER_PORT + "");
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "urlSchme", Helper.urlSchme + "");
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "dns", Helper.SERVER_DNS + "");
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "passcode", helper.createMd5(kiospassword));
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "uuid", uuid);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "name", name);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "session", "true");


                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            pd.hide();
                                            Toast.makeText(RegisterKiosk.this, "REGISTRATION DONE", Toast.LENGTH_SHORT).show();
                                            Intent startApp = new Intent(RegisterKiosk.this, MainActivity.class);
                                            startApp.putExtra("hasUsers", hasUsers);
                                            startActivity(startApp);
                                            finish();
                                        }
                                    });

                                } else if (type_.equals("login")) {

                                    kiosip = res.getString("ip");
                                    kiosport = res.getString("port");
                                    name = res.getString("name");
                                    mobile = res.getString("mobile");
                                    String hasUsers = res.getString("hasUsers");

                                    final String face = res.getString("face");
                                    final String passCode = res.getString("passcode");
                                    final String hasUsers_ = res.getString("hasUsers");

                                    try {
                                        Helper.SERVER_DNS = res.getString("dns");
                                        Helper.SERVER_IP = kiosip;
                                        Helper.SERVER_PORT = Integer.valueOf(kiosport);
                                        Helper.urlSchme = res.getString("urltype");
                                    } catch (Exception er) {
                                    }


                                    //KIOSK_ID
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "KIOSK_ID", mobile);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "KIOSK_HASH", helper.createMd5(mobile));
                                    //Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(),"KIOSK_HASH",helper.createMd5(mobile));
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "SERVER_IP", Helper.SERVER_IP);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "SERVER_PORT", Helper.SERVER_PORT + "");
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "urlSchme", Helper.urlSchme + "");
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "dns", Helper.SERVER_DNS + "");
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "passcode", helper.createMd5(kiospassword));
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "uuid", uuid);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "name", name);
                                    Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), "session", "true");


                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            pd.hide();
                                            Toast.makeText(RegisterKiosk.this, "LOGIN SUCCESS", Toast.LENGTH_SHORT).show();
                                            Intent startApp = new Intent(RegisterKiosk.this, MainActivity.class);
                                            startApp.putExtra("hasUsers", hasUsers_);
                                            startActivity(startApp);
                                            finish();

                                        }
                                    });

                                } else if (type_.equals("checklogin")) {
                                    type = "login";
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            pd.hide();
                                            kiosPassword.requestFocus();
                                            kiosPassword.setCursorVisible(true);
                                            kiosPassword.setVisibility(View.VISIBLE);
                                            kiosPassword.setHint("YOUR LOGIN PIN");
                                            kiosPassword.setSelection(0);
                                            kiosMobile.setEnabled(false);
                                            //kiosPassword.invalidate();
                                        }
                                    });

                                } else if (type_.equals("checkreg")) {
                                    type = "register";
                                    runOnUiThread(new Runnable() {
                                        public void run() {
                                            pd.hide();
                                            kiosPassword.requestFocus();
                                            kiosPassword.setCursorVisible(true);
                                            kiosName.setVisibility(View.VISIBLE);
                                            kiosPassword.setVisibility(View.VISIBLE);
                                            kiosEmail.setVisibility(View.VISIBLE);
                                            kiosAddress.setVisibility(View.VISIBLE);
                                        }
                                    });
                                }

                            } else {
                                runOnUiThread(new Runnable() {
                                    public void run() {
                                        pd.hide();
                                        kiosPassword.setText("");
                                        Toast.makeText(getApplicationContext(), "ERROR IN REGISTRATION", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        }
                        // Log.d("Service post result", result);

                    } catch (SocketException e) {
                        runOnUiThread(new Runnable() {
                            public void run() {
                                pd.hide();
                                kiosPassword.setText("");
                                Toast.makeText(getApplicationContext(), "CONNECTION ERROR", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } catch (Exception er) {
                        er.printStackTrace();
                        runOnUiThread(new Runnable() {
                            public void run() {
                                pd.hide();
                                kiosPassword.setText("");
                                Toast.makeText(RegisterKiosk.this, "ERROR IN REGISTRATION !", Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                } else {
                    runOnUiThread(new Runnable() {
                        public void run() {
                            pd.hide();
                            showNoNetwork();
                            Toast.makeText(RegisterKiosk.this, "CONNECTION ERROR", Toast.LENGTH_SHORT).show();
                        }
                    });
                }

            }
        };
        sendPost.setPriority(Thread.NORM_PRIORITY);
        sendPost.start();

    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (pd != null) {
                pd.cancel();
            }
        } catch (Exception er) {
        }

    }

    @Override
    public void onResume() {
        super.onResume();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Utility.disableBottomButtons(this);
                Utility.disablePullNotificationTouch(this);
            }
        }
    }

    @Override
    public void onBackPressed() {
    }

    private class NumericKeyBoardTransformationMethod extends PasswordTransformationMethod {
        @Override
        public CharSequence getTransformation(CharSequence source, View view) {
            return source;
        }
    }

}
