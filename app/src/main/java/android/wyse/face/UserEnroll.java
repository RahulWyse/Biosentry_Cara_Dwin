package android.wyse.face;

import android.app.Activity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.text.InputType;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Toast;

import org.json.JSONObject;

import java.util.ArrayList;

public class UserEnroll extends Activity {

    private CheckBox enrolltodevice;
    private EditText euserMobile;
    private EditText euserName;
    private EditText euserid;
    private EditText euseremail;
    private EditText edob;
    private EditText euserAddress;
    private Spinner erole_type;
    private int genderid = 0;
    private String gender = "";
    private String USER_ID = "";
    private boolean isRequestSuccess = false;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enroll_user);
        euserMobile = findViewById(R.id.euserMobile);
        euserName = findViewById(R.id.euserName);
        euserid = findViewById(R.id.euserid);
        euseremail = findViewById(R.id.euseremail);
        edob = findViewById(R.id.edob);
        euserAddress = findViewById(R.id.euserAddress);
        erole_type = findViewById(R.id.erole_type);

        euserid.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        euserName.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);
        euseremail.setInputType(InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS);

        // Creating adapter for spinner
        ArrayList<String> categories = new ArrayList<>();
        categories.add("Select Gender");
        categories.add("Male");
        categories.add("Female");
        categories.add("Transgender");
        ArrayAdapter dataAdapter = new ArrayAdapter<>(this, R.layout.spinnerlayout, categories);
        // Drop down layout style - list view with radio button
        dataAdapter.setDropDownViewResource(android.R.layout.select_dialog_item);
        // attaching data adapter to spinner
        erole_type.setAdapter(dataAdapter);
        // Spinner click listener
        erole_type.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> adapterView, View view, int pos, long l) {
                gender = adapterView.getItemAtPosition(pos).toString();
                genderid = pos;
            }

            @Override
            public void onNothingSelected(AdapterView<?> adapterView) {

            }
        });
        setupLoader();
    }

    public void onMobileCheck(View view) {
        Utility.setFadeAnimation(view);
        String mobile = euserMobile.getText().toString();
        if (mobile.length() > 0 || euserid.getText().length() >= 4) {
            try {
                JSONObject data = new JSONObject();
                data.put("mb", mobile);
                data.put("userid", euserid.getText().toString());

                //data.put("kid", Helper.KIOSK_HASH);
                data.put("uuid", Utility.getUUID(this));

                Uri.Builder uribuilder = Helper.getBaseUri("");
                uribuilder.appendPath("enroll.php");
                uribuilder.appendQueryParameter("type", "check");
                final String checkuser = uribuilder.build().toString();
                new Helper().networkRequst(getApplicationContext(),Helper.POST_REQUEST,"http",checkuser, data.toString(), new HelperCallback() {
                    @Override
                    public void onResult(String result) {
                        if (result != null) {
                            try {
                                final JSONObject r = new JSONObject(result);
                                final String msg = r.getString("msg");
                                if (r.getString("output").equals("success")) {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            try {
                                                euserName.setText(r.getString("name"));
                                                euserid.setText(r.getString("userid"));
                                                euseremail.setText(r.getString("email"));
                                                Button takepic = findViewById(R.id.takephotoBtn);
                                                takepic.setVisibility(View.VISIBLE);
                                                Button enrollbtn = findViewById(R.id.enrollBtn);
                                                enrollbtn.setVisibility(View.INVISIBLE);
                                                FaceManager.getInstance().setDetectedId(r.getString("userid"));
                                                Toast.makeText(UserEnroll.this, msg, Toast.LENGTH_SHORT).show();
                                            } catch (Exception er) {
                                            }
                                        }
                                    });
                                } else {
                                    runOnUiThread(new Runnable() {
                                        @Override
                                        public void run() {
                                            Toast.makeText(UserEnroll.this, msg, Toast.LENGTH_SHORT).show();
                                        }
                                    });
                                }
                            } catch (Exception er) {
                                //er.printStackTrace();
                            }
                        }
                    }

                    @Override
                    public void onError(String result) {

                    }
                });
            } catch (Exception er) {
                //er.printStackTrace();
            }
        } else {
            Toast.makeText(this, "Invalid mobile number or user id !", Toast.LENGTH_SHORT).show();
        }
    }

    public void enrollBtnClick(View view) {
        Utility.setFadeAnimation(view);
        final String mobile = euserMobile.getText().toString();
        final String name = euserName.getText().toString();
        final String userid = euserid.getText().toString();
        final String email = euseremail.getText().toString();
        final String dob = edob.getText().toString();
        final String address = euserAddress.getText().toString();


        if (mobile.length() < 10) {
            showToast("Invalid mobile number !");
            return;
        }
        if (name.equals("") || name.length() < 3) {
            showToast("Invalid name, must be first and last name !");
            return;
        }
        if (userid.equals("") || userid.length() < 3) {
            showToast("Invalid user id, must be alpha numeric !");
        }
        if (email.equals("") || email.length() < 3) {
            showToast("Invalid email id");
            return;
        }
        if (dob.equals("") || dob.length() < 8) {
            showToast("Invalid dob !");
            return;
        }
        if (address.equals("") || address.length() < 3) {
            showToast("Invalid address");
            return;
        }
        if (dob.split("/").length != 3) {
            showToast("Invalid DOB !");
            return;
        }
        final String country = "IN";
        final String role = "USER";
        if (gender.equals("") || genderid == 0) {
            showToast("Select Gender !");
            return;
        }

        EnrollModel.getInstance().setEnrolId(userid);
        EnrollModel.getInstance().setName(name);

        try {
            String[] flname = name.split(" ");
            String fname = name;
            String lname = "";
            if (flname.length == 2) {
                fname = flname[0];
                lname = flname[1];
            }
            JSONObject data = new JSONObject();
            data.put("fname", fname);
            data.put("lname", lname);
            data.put("gender", gender);
            data.put("email", email);
            data.put("mb", mobile);
            data.put("dob", dob);
            data.put("role", role);
            data.put("country", country);
            data.put("userid", userid);
            data.put("address", address);
            data.put("kid", Helper.KIOSK_HASH);
            data.put("uuid", Utility.getUUID(this));
            data.put("payLoadOTT", Helper.NONCE);

            Uri.Builder uribuilder = Helper.getBaseUri("");
            uribuilder.appendPath("enroll.php");
            uribuilder.appendQueryParameter("type", "reg");
            final String enroll_api = uribuilder.build().toString();

            if (progressDialog != null) {
                progressDialog.show();
            }
            isRequestSuccess = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    progressDialog.hide();
                }
            }, 10000);


            new Helper().networkRequst(getApplicationContext(),Helper.POST_REQUEST,"http",enroll_api, data.toString(), new HelperCallback() {
                @Override
                public void onResult(final String result) {
                    try {
                        final JSONObject resp = new JSONObject(result);
                        final String msg = resp.getString("msg");
                        if (resp.getString("output").equals("success")) {
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    progressDialog.hide();
                                    showToast(msg);
                                    Button takephotoBtn = findViewById(R.id.takephotoBtn);
                                    takephotoBtn.setVisibility(View.VISIBLE);
                                    Button enrollBtn = findViewById(R.id.enrollBtn);
                                    enrollBtn.setVisibility(View.INVISIBLE);
                                }
                            });
                        } else {

                            //Helper.getNonce(UserEnroll.this);
                        }
                    } catch (Exception er) {
                        //Helper.getNonce(UserEnroll.this);
                        er.printStackTrace();
                    }
                }

                @Override
                public void onError(String result) {
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            progressDialog.hide();
                            showToast("Error in registration, try again !");
                        }
                    });
                    //Helper.getNonce(UserEnroll.this);
                }
            });
        } catch (Exception er) {
            progressDialog.hide();
            //Helper.getNonce(UserEnroll.this);
            er.printStackTrace();
        }

    }

    private void setupLoader() {
        progressDialog = new ProgressDialog(UserEnroll.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
        // Set progress dialog style spinner
        progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // Set the progress dialog title and message
        //pd.setTitle("Title of progress dialog.");
        progressDialog.setMessage("Wait...");
        // Set the progress dialog background color
        //pd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFD4D9D0")));
        progressDialog.setIndeterminate(false);
        progressDialog.setCancelable(false);
    }

    private void showToast(String message) {
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
    }

    public void onTakePhoto(View view) {
        Utility.setFadeAnimation(view);
        if (EnrollModel.getInstance().getEnrolId().equals("")) {
            showToast("Invalid user id !");
            return;
        }

        FaceManager.getInstance().setEnroll(true);
        Intent startCapture = new Intent(this, CameraActivity.class);
        startCapture.addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP);
        startActivity(startCapture);
    }


    @Override
    public void onResume() {
        super.onResume();
        //Helper.getNonce(this);
        EnrollModel.getInstance().clearAll();
        UserModel.getInstance().clearAll();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (progressDialog != null) {
            progressDialog.hide();
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        UserModel.getInstance().clearAll();
        EnrollModel.getInstance().clearAll();
        FaceManager.getInstance().setEnroll(false);
        startActivity(new Intent(this, MainActivity.class));
        finish();
    }

}
