package android.wyse.face;
/**
 * Created by cis on 24/04/18.
 */

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.provider.Settings;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;



public class ProtectActivity extends Activity {

    private EditText passCodeText;
    private String DevicePassCode="7998";
    private String type="";
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        try {
            View decorView = getWindow().getDecorView();
            int uiOptions = View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                    | View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_FULLSCREEN | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN;
            decorView.setSystemUiVisibility(uiOptions);
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON); Helper.secureScreen(getWindow());
            //getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        }catch (Exception er){}
        setContentView(R.layout.protect_activity);

        passCodeText =  findViewById(R.id.passText);
        passCodeText.setCursorVisible(false);
        passCodeText.setEnabled(false);

        if (Build.VERSION.SDK_INT>=28){
            passCodeText.setActivated(true);
            passCodeText.setEnabled(true);
            passCodeText.setFocusableInTouchMode(true);
            passCodeText.requestFocus();
            passCodeText.setOnTouchListener(new View.OnTouchListener() {
                @Override
                public boolean onTouch(View v, MotionEvent event) {
                    return true;
                }
            });
        }


        if (getIntent()!=null){
            type=getIntent().getStringExtra("type");
        }


        try{
            timeOutAccess();
        }catch (Exception e){}


        //Typeface typeface = Typeface.createFromAsset(getAssets(), "fonts/visiapp.ttf");
        new CustomKeyboard(null,this,findViewById(R.id.keybord ), (s, type) -> {
            //Log.d("keypressed",s+" ,"+type);
            switch (type){
                case "key":
                    if(passCodeText.isFocused()){
                        String text=passCodeText.getText().toString();
                        passCodeText.setText(text+""+s);
                    }
                    break;
                case "del":
                    if(passCodeText.isFocused()){
                        String text=passCodeText.getText().toString();
                        if (text.length()>0) {
                            passCodeText.setText(text.substring(0, text.length()-1));
                        }
                    }
                    break;
                case "done":
                    //onGetOtpBtn(otpBtn);
                    onGoClick(passCodeText);
                    break;
                default:
                    break;
            }
        });

        // new Logger("@ProtectVisi opened",ProtectVisi.this);
    }

    public void onGoClick(View view){
        CaraManager.getInstance().setFadeAnimation(view);
        passCodeText =  findViewById(R.id.passText);
        String passcode = passCodeText.getText().toString().trim();

        if (passcode.equals("")){
            Toast.makeText(getApplicationContext(),"Password should not be empty !",Toast.LENGTH_SHORT).show();
            return;
        }

        if ( DevicePassCode.equals(passcode)){

            if (type.equals("setting")) {
                Toast.makeText(getApplicationContext(), "Access Granted !", Toast.LENGTH_SHORT).show();
                openOptions();
            }else if (type.equals("enrol")){
                Intent vb=new Intent(ProtectActivity.this,EtamUserEnrol.class);
                vb.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
                vb.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                //vb.addFlags(Intent.FLAG_ACTIVITY_BROUGHT_TO_FRONT);
                vb.addFlags(Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
                vb.addFlags(Intent.FLAG_ACTIVITY_NO_HISTORY);
                startActivity(vb);
                finish();
            }

        }else {
            passCodeText.setText("");
            Toast.makeText(getApplicationContext(),"Incorrect passcode !",Toast.LENGTH_SHORT).show();
        }

    }

    private void openOptions(){

        try{
            String[] menutypes = {"SETTINGS","ENROLL USER"};
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("SELECT MENU");
            builder.setCancelable(true);
            //builder.setIcon(R.drawable.logo);
            builder.setItems(menutypes, (dialog, which) -> {
                Intent startsettting;
                switch (which) {
                    case 0:
                        startsettting = new Intent(ProtectActivity.this, Configurations.class);
                        startActivity(startsettting);
                        finish();
                        break;
                    case 1:
                        Intent startEnroll = new Intent(ProtectActivity.this,EtamUserEnrol.class);
                        startActivity(startEnroll);
                        finish();
                        break;
                    default:
                        startsettting = new Intent(ProtectActivity.this, Configurations.class);
                        startActivity(startsettting);
                        finish();
                }
            });
            builder.show();
        }catch (Exception er){
            //  er.printStackTrace();
        }

    }

    public void onBackBtn(View view){
        CaraManager.getInstance().setFadeAnimation(view);
        //Intent gohome=new Intent(this,MainActivity.class);
        // startActivity(gohome);
        finish();
    }


    private CountDownTimer countDownTimer;
    private void timeOutAccess(){

        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
        countDownTimer=new CountDownTimer(60*1000,1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                Toast.makeText(getApplicationContext(),"TIMEOUT !",Toast.LENGTH_SHORT).show();
                //Intent gohome=new Intent(ProtectActivity.this,MainActivity.class);

                finish();
            }
        };
        countDownTimer.start();
    }


    @Override
    public void onPause(){
        super.onPause();
        // MainActivity.isAppRunning=false;

    }

    @Override
    public void onStop(){
        super.onStop();
        if(countDownTimer!=null){
            countDownTimer.cancel();
        }
    }

    @Override
    public void onResume(){
        super.onResume();
        CaraManager.getInstance().hideHomeBar(getWindow());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Utility.disablePullNotificationTouch(this);
            }
        }
    }


    @Override
    public void onBackPressed(){
        //super.onBackPressed();
    }

}

