package android.wyse.face;

import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.wyse.face.adaptors.BtnCallBack;
import android.wyse.face.adaptors.UserViewAdaptor;
import android.wyse.face.models.UsersModel;
import android.wyse.face.tech5.db.LocalCacheManager;

import androidx.annotation.Nullable;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class UsersActivity extends Activity {

    private RecyclerView listview;
    private UserViewAdaptor userAdaptor;
    private ArrayList<UsersModel> usersModels=new ArrayList<>();
    private TextView userinfo;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_users);

        listview = findViewById(R.id.listview);
        listview.setLayoutManager(new LinearLayoutManager(this));
        listview.setHasFixedSize(true);
        userinfo=findViewById(R.id.userinfo);

        CaraManager.getInstance().initUsers(getApplicationContext());

        EditText searchBar=findViewById(R.id.searchBar);
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                if (s!=null){
                    filter(s.toString().toUpperCase());
                }
            }

            @Override
            public void afterTextChanged(Editable s) {

            }
        });

        Button backbtn=findViewById(R.id.backbtn);
        backbtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Utility.setFadeAnimation(v);
                startActivity(new Intent(UsersActivity.this, MainActivity.class));
                finish();
            }
        });

        if (CaraManager.getInstance().getCaraUsers()==null) {
            userinfo.setText("0 USERS");
            return;
        }

        for (String key : CaraManager.getInstance().getCaraUsers().keySet()) {
            //Log.d("Users",key);
            usersModels.add(new UsersModel(CaraManager.getInstance().getCaraUsers().get(key),
                    key));
        }

        showUsers(usersModels);

    }

    private ArrayList<UsersModel> filtered=new ArrayList<>();
    private void filter(String input){
        filtered.clear();
        if (!input.equals("")) {
            //Log.d("filterInput",input);
            for(UsersModel key : usersModels) {
                //Log.d("Users", key.getName());

                if (key.getName().contains(input) || key.getUserid().contains(input) || input.startsWith(key.getName()) || input.equalsIgnoreCase(key.getName())) {
                    //Log.d("filter","user found "+key.getName());
                    filtered.add(new UsersModel(key.getName(), key.getUserid()));
                }
            }
            if (filtered.size()>0) {
                showUsers(filtered);
            }else{
                showUsers(usersModels);
            }
        }else {
            showUsers(usersModels);
        }
    }

    public void removeAllUsers(View view){
        CaraManager.getInstance().setFadeAnimation(view);
        showMessageDia(this,"FLUSH ALL USERS","Are you sure? this will remove all users","error");
    }

    private void flashUsers(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                if (FaceManager.getInstance().getLocalCacheManager() == null)
                    FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(getApplicationContext()));

                if (FaceManager.getInstance().getLocalCacheManager()!=null){

                    if (CaraManager.getInstance().getCaraDb().numberOfRows(Database.TABLE_USERS) > 0) {
                        FaceManager.getInstance().getLocalCacheManager().deleteAllFaceRecords();
                        int r=CaraManager.getInstance().getCaraDb().flushUsers();
                        if (r>0){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    usersModels.clear();
                                    userAdaptor.notifyDataSetChanged();
                                    showMessage("Users deleted !");
                                }
                            });

                        }else {
                            showMessage("Error in delete !");
                        }
                    }else {
                        showMessage("No users found !");
                    }
                }else {
                    showMessage("Error in delete users");
                }
            }
        }).start();

    }

    public void showMessage(String msg){
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Toast.makeText(getApplicationContext(),msg,Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showMessageDia(Context context, String message, String subtext, final String type) {

        try {
            final Dialog alertDialog = new Dialog(context);
            alertDialog.setCancelable(false);
            alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
            alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
            alertDialog.setContentView(R.layout.custom_dialog_layout);
            TextView tv = alertDialog.findViewById(R.id.alerttext);
            TextView background = alertDialog.findViewById(R.id.textView22);
            TextView yourmessage = alertDialog.findViewById(R.id.yourmessage);
            Button allowBtn = alertDialog.findViewById(R.id.allowBtn);
            Button denyBtn = alertDialog.findViewById(R.id.denyBtn);
            if (type.equals("error")) {
                background.setBackgroundColor(context.getResources().getColor(R.color.red));
            } else {
                background.setBackgroundColor(context.getResources().getColor(R.color.green));
            }

            tv.setText(message);
            yourmessage.setText(subtext);

            denyBtn.setText("CANCEL");

            allowBtn.setText("REMOVE ALL");
            allowBtn.setTextColor(context.getResources().getColor(R.color.red));


            allowBtn.setOnClickListener(v -> {
                flashUsers();
                alertDialog.dismiss();
            });
            denyBtn.setOnClickListener(v -> alertDialog.dismiss());
            alertDialog.show();
        } catch (Exception er) {
            Utility.printStack(er);
        }

    }

    private void showUsers(ArrayList<UsersModel> usersModels){
        userAdaptor=new UserViewAdaptor(getApplicationContext(), usersModels, new BtnCallBack() {
            @Override
            public void onClick(String userid,int pos) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        if (FaceManager.getInstance().getLocalCacheManager() == null)
                            FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(getApplicationContext()));

                        boolean isSuccess = FaceManager.getInstance().removeUserById(userid);
                        if (isSuccess) {
                            if (CaraManager.getInstance().getCaraDb().remUserByIdDb(userid)) {
                                runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        Toast.makeText(getApplicationContext(), "User " + userid + " removed successfully !", Toast.LENGTH_SHORT).show();
                                    }
                                });
                            }
                        } else {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    Toast.makeText(getApplicationContext(), "Error in remove for " + userid, Toast.LENGTH_SHORT).show();
                                }
                            });

                        }
                    }
                }).start();
            }
        });

        if (listview != null && userAdaptor != null)
            listview.setAdapter(userAdaptor);

        listview.getRecycledViewPool().clear();
        userAdaptor.notifyDataSetChanged();
        userinfo.setText("TOTAL USERS ( "+usersModels.size()+" )");
    }

    @Override
    protected void onResume() {
        super.onResume();
        CaraManager.getInstance().hideHomeBar(getWindow());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Utility.disablePullNotificationTouch(this);
            }
        }
    }

    private View decorView;
    @Override
    public void onWindowFocusChanged(boolean hasFocus) {
        super.onWindowFocusChanged(hasFocus);
        // Hide Status Bar
        if (Build.VERSION.SDK_INT < 16) {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
        } else {
            decorView = getWindow().getDecorView();
            // Hide Status Bar.

            decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                    | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
            decorView.requestLayout();
        }
    }



    @Override
    protected void onPause() {
        super.onPause();
    }
}
