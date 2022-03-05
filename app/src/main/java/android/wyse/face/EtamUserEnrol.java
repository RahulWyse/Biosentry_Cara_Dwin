package android.wyse.face;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.os.StrictMode;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;
import android.wyse.face.adaptors.ListViewAdapter;
import android.wyse.face.adaptors.UserIdNameModel;
import android.wyse.face.tech5.db.FaceRecord;
import android.wyse.face.tech5.db.LocalCacheManager;
import android.wyse.face.tech5.utilities.Listener;
import android.wyse.face.tech5.utilities.LogUtils;
import android.wyse.face.tech5.utilities.Utilities;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ai.tech5.sdk.abis.face.t5face.CreateFaceTemplateResult;


/**
 * Created by sonu on 10/01/17.
 */
public class EtamUserEnrol extends Activity implements OnItemClickListener {

    private final ArrayList<UserIdNameModel> filter=new ArrayList<>();
    private final  char[] hexArray = "0123456789ABCDEF".toCharArray();
    private ListView lview;
    private ListViewAdapter lviewAdapter;
    private Button enrollBtn;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedPref;
    private String NEW_TEMPLATE_URI= "" ; //NewEnroll?apikey="+CaraManager.getInstance().getAPI_KEY()+"&src=cara&Resp=J&Mac=";
    private String OLD_TEMPLATE_URI= ""; //""/ModifyEnroll?apikey="+CaraManager.getInstance().getAPI_KEY()+"&src=cara&list=1&Resp=J&Mac=";
    //private String TEMPLATE_UPDATE_URI="/EnrollUser?apikey="+CaraManager.getInstance().getAPI_KEY()+"&src=cara&Resp=J&Mac=";
    private String USER_ID="";
    private String USER_NAME="";
    private CheckBox newFinger;
    private CheckBox updateFinger;
    private String response="";
    private EditText search_box_text;
    private LinearLayout headerBar;
    private TextView idtext;
    private ArrayList<UserIdNameModel> userIdNameModels;
    private InputMethodManager imm;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            Helper.secureScreen(getWindow());
            StrictMode.ThreadPolicy policy = new StrictMode.ThreadPolicy.Builder().permitAll().build();
            StrictMode.setThreadPolicy(policy);
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }catch (Exception er){}

        setContentView(R.layout.activity_enroll_new);
        userIdNameModels=new ArrayList<>();

        Uri.Builder newEnrolDns = Helper.getBaseUri("BS_REST");
        newEnrolDns.appendPath("NewEnroll");
        newEnrolDns.appendQueryParameter("apikey",CaraManager.getInstance().getAPI_KEY());
        newEnrolDns.appendQueryParameter("src","cara");
        newEnrolDns.appendQueryParameter("Resp","J");
        newEnrolDns.appendQueryParameter("Mac",CaraManager.getInstance().getDEVICE_MACID());

        Uri.Builder updateEnrolDns = Helper.getBaseUri("BS_REST");
        updateEnrolDns.appendPath("ModifyEnroll");
        updateEnrolDns.appendQueryParameter("apikey",CaraManager.getInstance().getAPI_KEY());
        updateEnrolDns.appendQueryParameter("src","cara");
        updateEnrolDns.appendQueryParameter("Resp","J");
        updateEnrolDns.appendQueryParameter("list","1");
        updateEnrolDns.appendQueryParameter("Mac",CaraManager.getInstance().getDEVICE_MACID());

        //use new dns
        NEW_TEMPLATE_URI = newEnrolDns.build().toString();
        OLD_TEMPLATE_URI = updateEnrolDns.build().toString();
        //TEMPLATE_UPDATE_URI = inputDns+TEMPLATE_UPDATE_URI;

        //Button searchBtn = findViewById(R.id.searchBtn);
        search_box_text= findViewById(R.id.search_box_text);
        search_box_text.setVisibility(View.INVISIBLE);
        headerBar= findViewById(R.id.headerBar);
        enrollBtn=findViewById(R.id.enrollBtn);

        if (CaraManager.getInstance().getDEVICE_MACID().length()>=6) {
            getServerList(NEW_TEMPLATE_URI);
        }

        //morphoDatabase = ProcessInfo.getInstance().getMorphoDatabase();
        //morphoDevice = ProcessInfo.getInstance().getMorphoDevice();

        newFinger = findViewById(R.id.enrollNewFinger);
        updateFinger = findViewById(R.id.updateUserFinger);
        idtext=findViewById(R.id.fingerText);

        newFinger.setOnClickListener(v -> {
            setFadeAnimation(v);
            updateFinger.setChecked(false);
            try {
                if (newFinger.isChecked()) {
                    getServerList(NEW_TEMPLATE_URI);
                }
                USER_NAME = "";
                USER_ID = "";
            }catch (Exception e){
                newFinger.setClickable(true);
                newFinger.setEnabled(true);
            }
        });

        updateFinger.setOnClickListener(v -> {
            setFadeAnimation(v);
            newFinger.setChecked(false);
            try {

                if (updateFinger.isChecked()) {
                    getServerList(OLD_TEMPLATE_URI);
                }

                USER_NAME = "";
                USER_ID = "";
            }catch (Exception e){
                //e.printStackTrace();
            }

        });

        lview = findViewById(R.id.listView2);

        search_box_text.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter.clear();
                if (!s.equals("") && userIdNameModels.size()>0){
                    String input=s.toString().toUpperCase();
                    for (int f=0;f<userIdNameModels.size();f++){
                        if (userIdNameModels.get(f).getUserId().contains(input) || userIdNameModels.get(f).getUserName().contains(input)){
                            filter.add(userIdNameModels.get(f));
                        }
                    }
                }

                if (filter.size()>0){
                    lviewAdapter = new ListViewAdapter(EtamUserEnrol.this, filter);
                    lview.setAdapter(lviewAdapter);
                    lview.setOnItemClickListener(EtamUserEnrol.this);
                    lviewAdapter.notifyDataSetChanged();
                }else{
                    lviewAdapter = new ListViewAdapter(EtamUserEnrol.this, userIdNameModels);
                    lview.setAdapter(lviewAdapter);
                    lview.setOnItemClickListener(EtamUserEnrol.this);
                    lviewAdapter.notifyDataSetChanged();
                }

            }

            @Override
            public void afterTextChanged(Editable s) { }

        });
        //Log.d("Sensor","Device address in DeviceUsers "+ProcessInfo.getInstance().getMorphoDatabase());
    }

    public void onCancelCapture(View view){
        setFadeAnimation(view);

        USER_ID = "";
        USER_NAME ="";

        TextView userName = findViewById(R.id.personName);
        userName.setText("");
        idtext.setText("");
        enrollBtn.setVisibility(View.VISIBLE);
        enrollBtn.setEnabled(true);
        enrollBtn.setClickable(true);

        if (lview!=null){
            lview.setVisibility(View.VISIBLE);
        }

        //setDefaultImage(R.drawable.finger180);
        //closeDeviceAndFinishActivity();
    }

    private void openOptions(final View view){

        try{
            String[] menutypes = {"DATABASE","IMAGES"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("SELECT SYNC FROM");
            builder.setCancelable(true);
            //builder.setIcon(R.drawable.logo);
            builder.setItems(menutypes, (dialog, which) -> {
                Intent startsettting;
                switch (which) {
                    case 0:
                        syncFromLocal(view);
                        break;
                    case 1:
                        CaraManager.successCnt=0;
                        syncFromBackup(view);
                        break;
                    default:
                }
            });
            builder.show();
        }catch (Exception er){
            //  er.printStackTrace();
        }

    }

    private void callUpdateApi(String id,JSONObject finalData){
        try {

            //LogUtils.debug("TAG", "face inserted " + record.id);
            //Log.d("TAG",finalData.toString());
            CaraManager.getInstance().enrollOnline(getApplicationContext(), finalData, new HelperCallback() {
                @Override
                public void onResult(String result) {
                    try {
                        //Log.d("TAG",result);
                        if (new JSONObject(result).getString("ErrorString").equalsIgnoreCase("success")) {
                            CaraManager.successCnt++;
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                progressDialog.setMessage(CaraManager.successCnt+" Users uploaded successfully");
                            }
                        });


                    }catch (Exception er){}
                }

                @Override
                public void onError(String result) {
                    Log.d("TAG",result);
                }
            });

        } catch (Exception e) {
            Utility.printStack(e);
        }
    }

    /**
     * sync device enrolment on server
     * @param view
     */
    public void onSyncBtn(View view){
        CaraManager.getInstance().setFadeAnimation(view);
        openOptions(view);
    }

    private void syncFromLocal(View view){
        CaraManager.successCnt=0;
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (Listener.isSDKInitialized) {

                        LocalCacheManager localCacheManager = new LocalCacheManager(getApplicationContext());
                        List<FaceRecord> list = localCacheManager.getAllRecords();
                        LogUtils.debug("TAG", "total cache size  " + (list != null ? list.size() : 0));
                        long size = Listener.t5TemplateMatcher.Size();

                        if (size>0) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    view.setVisibility(View.INVISIBLE);
                                    progressDialog.show();
                                    progressDialog.setMessage("Upload begin...");
                                    progressDialog.setMessage("Uploading " + size + " users...");
                                }
                            });
                        }

                        if (list != null && list.size() > 0) {

                            // if (list.size() > size) {

                            for (FaceRecord record : list) {
                                try {
                                    String confidance = "";
                                    String glass = "";
                                    String blur = "";
                                    String closedEye = "";
                                    String mask = ""; // added in version 1011
                                    long t2=System.currentTimeMillis();
                                    JSONObject finalData = new JSONObject();
                                    finalData.put("imgData", Utility.encodeImage(record.template));
                                    finalData.put("empid", record.id);
                                    finalData.put("imgq", confidance);
                                    finalData.put("macid", CaraManager.getInstance().getDEVICE_MACID());
                                    finalData.put("apikey", CaraManager.getInstance().getAPI_KEY());
                                    finalData.put("src", "cara");
                                    finalData.put("glass", glass);
                                    finalData.put("blur", blur);
                                    finalData.put("closedEye", closedEye);
                                    finalData.put("ismask",mask);
                                    finalData.put("ver", BuildConfig.VERSION_CODE + "");
                                    finalData.put("timereq",0);
                                    finalData.put("srcImg","");

                                    callUpdateApi(record.id, finalData);

                                }catch (Exception er){}
                                //callUpdateApi(record.id,record.template);
                            }

                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    view.setVisibility(View.VISIBLE);
                                    progressDialog.setMessage(CaraManager.successCnt+" Users uploaded successfully");
                                    new Handler().postDelayed(new Runnable() {
                                        @Override
                                        public void run() {
                                            if (progressDialog!=null)
                                                progressDialog.hide();
                                        }
                                    },5000);
                                }
                            });

                            //  }
                        }
                        LogUtils.debug("TAG", "total enrollments " + Listener.t5TemplateMatcher.Size());
                        localCacheManager.closeDbConnection();
                    }
                }catch (Exception er){
                    Utility.printStack(er);
                }

            }
        }).start();
    }

    private DecimalFormat decimalFormat = new DecimalFormat("###.##");
    private void syncFromBackup(View view){
        try {
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "CaraFaces";
            File root = new File(rootPath);
            if (root.isDirectory() && root.listFiles().length>0){

                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        view.setVisibility(View.INVISIBLE);
                        progressDialog.show();
                        progressDialog.setMessage("Upload begin...");
                        progressDialog.setMessage("Uploading " + root.listFiles().length + " users...");
                    }
                });

                new Thread(new Runnable() {
                    @Override
                    public void run() {

                        CaraManager.successCnt=0;
                        for (File file : root.listFiles()) {
                            String empid=file.getName().split("_")[0];
                            byte[] imgdata= Utilities.readfileFrom(file);
                            if (imgdata!=null && empid!=null) {
                                long t1=System.currentTimeMillis();
                                ArrayList <CreateFaceTemplateResult> templates = FaceManager.getInstance().getFaceTemp(imgdata);
                                //Log.d("TAG", "EMP ID" + file.getName());
                                    if (templates!=null && templates.size()>0) {
                                        try {
                                            long t2=System.currentTimeMillis();
                                            String confidance = decimalFormat.format(templates.get(0).Confidence);
                                            String glass = decimalFormat.format(templates.get(0).Glasses);
                                            String blur = decimalFormat.format(templates.get(0).Blur);
                                            String closedEye = decimalFormat.format(templates.get(0).ClosedEyes);
                                            String mask = decimalFormat.format(templates.get(0).Mask); // added in version 1011

                                            JSONObject finalData = new JSONObject();
                                            finalData.put("imgData", Utility.encodeImage(templates.get(0).Template));
                                            finalData.put("empid", empid);
                                            finalData.put("imgq", confidance);
                                            finalData.put("macid", CaraManager.getInstance().getDEVICE_MACID());
                                            finalData.put("apikey", CaraManager.getInstance().getAPI_KEY());
                                            finalData.put("src", "cara");
                                            finalData.put("glass", glass);
                                            finalData.put("blur", blur);
                                            finalData.put("closedEye", closedEye);
                                            finalData.put("ismask",mask);
                                            finalData.put("ver", BuildConfig.VERSION_CODE + "");
                                            finalData.put("timereq",(t2-t1));
                                            //finalData.put("srcImg",imgdata);
                                            finalData.put("srcImg",Utility.encodeBase64(imgdata));  //added to send original image
                                            callUpdateApi(empid, finalData);
                                            Thread.sleep(3000); //added to stop quee for sending images
                                        }catch (Exception er){}
                                    }
                            }
                        }

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                view.setVisibility(View.VISIBLE);
                                progressDialog.setMessage(CaraManager.successCnt+" Users uploaded successfully");
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        if (progressDialog!=null)
                                            progressDialog.hide();
                                    }
                                },8000);
                            }
                        });
                    }
                }).start();
            }
        }catch (Exception er){
            Utility.printStack(er);
        }
    }

    public void onSearchTap(View view){
        headerBar.setVisibility(View.INVISIBLE);
        //setFadeAnimation(view);
        search_box_text.setVisibility(View.VISIBLE);
        search_box_text.requestFocus();
        imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);

        if (imm!=null)
            imm.toggleSoftInput(InputMethodManager.SHOW_FORCED, 0);

    }

    public void onItemClick(AdapterView<?> parent, View arg1, int position, long id) {
        if(position>=0) {
            USER_ID = ((TextView)arg1.findViewById(R.id.user_id)).getText().toString(); //userIdNameModels.get(position).getUserId();
            USER_NAME =  ((TextView)arg1.findViewById(R.id.user_name)).getText().toString() ; //userIdNameModels.get(position).getUserName();
            TextView userName = findViewById(R.id.personName);
            userName.setText(USER_NAME);
            idtext.setText(USER_ID+"");
        }
    }

    private void openCamera(){
        FaceManager.getInstance().setEnroll(true);
        Intent startCapture = new Intent(this, CameraActivity.class);
        startCapture.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(startCapture);
    }

    public void enrollBtnClick(View view){
        view.setEnabled(false);
        setFadeAnimation(view);

        try {

            if ( !(USER_ID.equals("") || USER_NAME.trim().equals("")) ) {

                EnrollModel.getInstance().setEnrolId(USER_ID.trim());
                EnrollModel.getInstance().setName(USER_NAME.trim());

                if (Listener.isSDKInitialized) {
                    if (lview!=null){
                        lview.setVisibility(View.INVISIBLE);
                    }
                    openCamera();
                }else{
                    view.setEnabled(true);
                    Toast.makeText(getApplicationContext(),"Go to home and Try again !",Toast.LENGTH_SHORT).show();
                }

            }else{
                view.setEnabled(true);
                Toast.makeText(getApplicationContext(),"Please select user from the list",Toast.LENGTH_SHORT).show();
            }

        }catch (Exception ers){
            Utility.printStack(ers);
            view.setEnabled(true);
        }
    }

    private void setFadeAnimation(View view) {
        Animation fadeIn = new AlphaAnimation(0, 1);
        fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
        fadeIn.setDuration(1000);
        Animation fadeOut = new AlphaAnimation(1, 0);
        fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
        fadeOut.setStartOffset(500);
        fadeOut.setDuration(500);
        view.startAnimation(fadeOut);
        view.startAnimation(fadeIn);
    }

    private void getServerList(final String URI){

       //
         //Log.d("Tag",URI);

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (progressDialog!=null) {
                    progressDialog.setMessage("Getting users list...");
                    progressDialog.show();
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (progressDialog!=null)
                                progressDialog.hide();
                        }
                    },5000);
                }
            }
        });

        Thread getDataTh=new Thread(() -> {
            try {
                URL url = new URL(URI);
                HttpURLConnection urlConnection = (HttpURLConnection)  url.openConnection();
                //urlConnection.setDoOutput(true);
                //urlConnection.setDoInput(true);
                urlConnection.setReadTimeout(15000);
                urlConnection.setConnectTimeout(10000);
                BufferedReader reader = new BufferedReader(new InputStreamReader(urlConnection.getInputStream()));
                String line;
                response="";
                while ((line = reader.readLine()) != null) {
                    response += line + "\n";
                }
            } catch (Exception e) { }

            if (response!=null) {
                if (!response.equals("")) {
                    parseJSON(response.trim());
                }else {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            Toast.makeText(getApplicationContext(),"Empty responce received from server",Toast.LENGTH_SHORT).show();
                        }
                    });
                }
            }else{
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        if (progressDialog!=null) {
                            progressDialog.hide();
                        }

                        Toast.makeText(getApplicationContext(),"Error in getting list",Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
        getDataTh.setPriority(Thread.NORM_PRIORITY);
        getDataTh.start();
    }

    private void parseJSON(String json){
        try {

            JSONArray jsonArray = new JSONArray(json);
            JSONObject entry;
            boolean isSuccess=true;
            userIdNameModels.clear();

            for (int i = 0 ; i < jsonArray.length(); i++ ) {
                entry =(JSONObject) jsonArray.get(i);
                if (entry.getString("ErrorString").equalsIgnoreCase("Success") ) {
                    //Log.d("EnrolUser",entry.getString("NAME")+", "+entry.getString("ID")+", "+entry.getString("ID").length());
                    userIdNameModels.add(new UserIdNameModel(entry.getString("NAME").trim().toUpperCase(),entry.getString("ID").trim()));
                }else {
                    isSuccess=false;
                }
            }

            if (!isSuccess){
                runOnUiThread(() -> {
                    try {
                        // Toast.makeText(DeviceUsers.this,"Error in Getting Name(s)", Toast.LENGTH_SHORT).show();
                        if (lview==null) {
                            lview =  findViewById(R.id.listView2);
                        }
                        lview.setVisibility(View.INVISIBLE);
                    }catch (Exception r){}
                });

                return;
            }

            runOnUiThread(() -> {
                if (newFinger.isChecked()){
                    newFinger.setText("NEW ("+userIdNameModels.size()+")");
                }
                if (updateFinger.isChecked()){
                    updateFinger.setText("UPDATE ("+userIdNameModels.size()+")");
                }
            });

            runOnUiThread(() -> {
                if (lview==null) {
                    lview =  findViewById(R.id.listView2);
                }
                lviewAdapter = new ListViewAdapter(EtamUserEnrol.this, userIdNameModels);
                lview.setVisibility(View.VISIBLE);
                setFadeAnimation(lview);
                lview.setAdapter(lviewAdapter);
                lview.setOnItemClickListener(EtamUserEnrol.this);

                if (progressDialog!=null)
                progressDialog.hide();
                //lviewAdapter.notifyDataSetChanged();
            });
        }catch (Exception jsonEx){
            //jsonEx.printStackTrace();
            Utility.printStack(jsonEx);
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (progressDialog!=null)
                    progressDialog.hide();
                }
            });
        }
    }

    private void setupProgress(){
        progressDialog=new ProgressDialog(this);
        // ---------------------- ProgressDialog set Title added by Rohini Wagh --------------------//
        progressDialog.setTitle("Wait...");

        /// progressDialog.setMessage("Wait...");
        progressDialog.setCancelable(false);
        //progressDialog.show();
    }

    @Override
    public void onResume(){
        super.onResume();
        if (sharedPref == null) {
            sharedPref = getApplicationContext().getSharedPreferences(CaraManager.PREF_FILE_NAME, Context.MODE_PRIVATE);
        }

        setupProgress();
        EnrollModel.getInstance().clearAll();
    }

    public void onSurfaceTouch(View view){
        if (imm!=null) {
            imm.hideSoftInputFromWindow(search_box_text.getWindowToken(), 0);
        }
    }

    @Override
    public void onPause(){
        super.onPause();
        try{
            // BioSentryManager.getInstance().setSettingUpdate(true);
            if (imm!=null) {
                imm.hideSoftInputFromWindow(search_box_text.getWindowToken(), 0);
            }
            if(progressDialog!=null){
                progressDialog.hide();
                progressDialog.cancel();
            }
        }catch (Exception e){}

    }

    @Override
    public void onStop(){
        super.onStop();
    }

    @Override
    public void onBackPressed() {
        // do nothing.
        if (progressDialog!=null)
            progressDialog.hide();
    }

    private void goback(){
        FaceManager.getInstance().setEnroll(false);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

    public void onBackBtn(View view){
        setFadeAnimation(view);
        goback();
    }


}
