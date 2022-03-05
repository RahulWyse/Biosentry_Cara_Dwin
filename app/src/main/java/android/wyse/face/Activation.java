package android.wyse.face;

import android.Manifest;
import android.app.Activity;
import android.app.ProgressDialog;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.content.ContextCompat;

import org.json.JSONObject;

public class Activation extends Activity {

    private Button donebtn;
    private EditText activiation_code;
    private ProgressDialog progressDialog;


    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
        decorView.setSystemUiVisibility(uiOptions);
        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        Helper.secureScreen(getWindow());
        setContentView(R.layout.activation);
        donebtn=findViewById(R.id.donebtn);
        activiation_code=findViewById(R.id.activiation_code);
        activiation_code.setText("");
        activiation_code.setHint("TYPE ACTIVATION CODE");
        progressDialog=new ProgressDialog(getWindow().getContext());
        progressDialog.setCancelable(false);
        TextView macid=findViewById(R.id.macid);
        macid.setText(CaraManager.getInstance().getDEVICE_MACID());

        progressDialog.setMessage("Checking....");

        if (CaraManager.getInstance().getSharedPreferences() == null) {
            CaraManager.getInstance().setSharedPreferences(this.getSharedPreferences(CaraManager.PREF_FILE_NAME, Context.MODE_PRIVATE));
        }

        donebtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                    if (activiation_code.getText()!=null){
                        String text=activiation_code.getText().toString();
                        if (!text.equals("") && text.length()>=6){
                                progressDialog.show();
                                checkActivation(text,CaraManager.getInstance().getDEVICE_MACID());
                        }
                    }
            }
        });

        mDPM = (DevicePolicyManager) this.getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = CaraAdmin.getComponentName(this);
        enableAppAdmin(this);

    }

    private DevicePolicyManager mDPM;
    private ComponentName adminComponent;
    private void enableAppAdmin(Context context) {
        if (!mDPM.isAdminActive(adminComponent)) {
            Intent activateDeviceAdmin = new Intent(DevicePolicyManager.ACTION_ADD_DEVICE_ADMIN);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_DEVICE_ADMIN, adminComponent);
            activateDeviceAdmin.putExtra(DevicePolicyManager.EXTRA_ADD_EXPLANATION, "Tap on Activate to use this device");
            context.startActivity(activateDeviceAdmin);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if(ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)!= PackageManager.PERMISSION_GRANTED) {
            if (Build.VERSION.SDK_INT>=23)
                if (mDPM.isAdminActive(adminComponent)) {
                    requestPermissions(new String[]{Manifest.permission.CAMERA, Manifest.permission.WRITE_EXTERNAL_STORAGE}, 4444);
                }
        }
    }

    private void checkActivation(final String code, final String mac_id_device){
        try{
            JSONObject data = new JSONObject();
            data.put("uuid", android.provider.Settings.Secure.getString(getApplicationContext().getContentResolver(), android.provider.Settings.Secure.ANDROID_ID));
            data.put("c", code);
            data.put("macid", mac_id_device);
            data.put("ver", BuildConfig.VERSION_CODE);
            String baseurl = "http://pintu.biosentry.co.in/api/";
            new Helper().networkRequst(getApplicationContext(), Helper.POST_REQUEST, "http",baseurl + "check.php", data.toString(), new HelperCallback() {

                @Override
                public void onResult(String result) {
                    try {
                        JSONObject object = new JSONObject(result);
                        if (object.getString("result").equals("success")){
                            //activated
                            if (CaraManager.getInstance().getSharedPreferences()!=null){
                                CaraManager.getInstance().setSharedPref(CaraManager.getInstance().getSharedPreferences(),"isactivated","true");
                                CaraManager.getInstance().setSharedPref(CaraManager.getInstance().getSharedPreferences(),"active_code",code);
                            }
                            if (CaraManager.getInstance().getSharedPreferences().getString("isactivated","false").equals("true")) {
                                startMain();
                            }
                        }else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.hide();
                                    Toast.makeText(Activation.this,"Invalid or Expired code, try again or contact for help !",Toast.LENGTH_SHORT).show();
                                }
                            });
                        }
                    }catch (Exception er){
                        Utility.printStack(er);
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.hide();
                                Toast.makeText(Activation.this,"Error in request,  try again !",Toast.LENGTH_SHORT).show();
                            }
                        });
                    }
                }

                @Override
                public void onError(String result) {
                    //Log.d("Activation",result);
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                            Toast.makeText(getApplicationContext(),"Error in request,  try again !",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            });
        }catch (Exception er){
           Utility.printStack(er);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(getApplicationContext(),er.getMessage(),Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void startMain(){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                progressDialog.hide();
                Intent startMain = new Intent(Activation.this, MainActivity.class);
                startMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP);
                startMain.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                startMain.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(startMain);
                finish();
            }
        });
    }

    @Override
    public void onPause(){
        super.onPause();
        if (progressDialog!=null){
            progressDialog.hide();
            progressDialog.cancel();
        }
    }

    @Override
    public void onBackPressed(){
        //
    }
}
