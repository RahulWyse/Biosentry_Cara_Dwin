package android.wyse.face;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.SurfaceTexture;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.ColorDrawable;
import android.hardware.Camera;
import android.hardware.Camera.Face;
import android.hardware.Camera.FaceDetectionListener;
import android.os.Build;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.Handler;
import android.os.Message;
import android.provider.Settings;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.wyse.face.models.ThermalModel;
import android.wyse.face.tech5.db.LocalCacheManager;
import android.wyse.face.tech5.utilities.AppUtils;
import android.wyse.face.tech5.utilities.Constants;
import android.wyse.face.tech5.utilities.IdentifyFaceResults;
import android.wyse.face.tech5.utilities.Listener;
import android.wyse.face.tech5.utilities.OneShotProcessorTask;
import android.wyse.face.tech5.utilities.Utilities;
import android.wyse.face.tech5.utilities.Worker;

import androidx.core.content.ContextCompat;
import androidx.palette.graphics.Palette;

import com.dwin.dwinpio.GpioControlUtil;
import com.jackandphantom.blurimage.BlurImage;

import org.json.JSONObject;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.List;

import ai.tech5.sdk.abis.face.t5face.CreateFaceTemplateResult;
import ai.tech5.sdk.abis.face.t5face.IFaceSDKFaceDetector;
import ai.tech5.sdk.abis.face.t5face.IdentifyFaceResult;


/**
 * Created by sonu auti on 09/01/18.
 */
public class CameraActivity extends Activity implements TextureView.SurfaceTextureListener {

    private final Handler handler = new Handler();
    private int errorSend = 0;
    private DecimalFormat decimalFormat = new DecimalFormat("###.##");
    private ProgressDialog pd;
    private FaceOverlayView mFaceView;
    private CountDownTimer faceTime;
    private CountDownTimer clearui;
    private RelativeLayout cameraPreviewLayout;
    //private ImageSurfaceView mImageSurfaceView;
    private ImageView bracket,imagequality,instructions;
    private TextureView mTextureView;
    private int width_ = 400;
    private int height_ = 400;
    private int FaceCount = 0;
    private Bitmap bgBlur;
    private ImageView facebracket, thermameter;
    private TextView countdown_text;
    private TextView flname;
    private TextView userid;
    private TextView temptext;
    private int cam_time_out;
    private Worker worker;
    private int callOnce = 0;
    private int MAX_AREA = 44;   //TESTED if changed, need to test liveness feature
    private int MIN_AREA = 28;  //TESTED if changed, need to test liveness feature
    private int FACE_QUALITY = 98;
    private ProgressBar thermalProgress;
    private Button retakebtn;
    private RelativeLayout bottomView;
    //private Face[] mFaces;
    //private RectF faceRect;
    private Runnable postrun;
    private ProgressBar progressBar;
    private TextView inoutText;
    private TextView currentTime, currentDate;
    private View decorView;
    private int cntScreenTap = 0;
    private Camera.Parameters camParms;
    private long t1, t2;
    private ThermalModel thermalModel;
    private double truepos = 0;
    private double trueneg = 0;
    private int framecnt = 0;
    //private double fakeAccuracy = 0.70;
    private float MAX_FRAME = 3.0f;
    private TextView livenesstext;
    private TextView infomsg;
    private boolean isExpiryChecked=false;
    private BroadcastReceiver receiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (intent.getAction().equals(Intent.ACTION_TIME_TICK)) {
                updateDateTime();

                CaraManager.getInstance().setUploadCount((CaraManager.getInstance().getUploadCount() + 1));
                // Log.d("cara","upload cnt "+CaraManager.getInstance().getUploadCount());
                try {
                    if (CaraManager.getInstance().getUploadCount() == 10) {
                        CaraManager.getInstance().setUploadCount(0);
                        CaraManager.getInstance().uploadOfflinePunches(getApplicationContext());
                    }
                }catch (Exception er){}

                try {
                    if (CaraManager.getInstance().getUploadCount() == 8) {
                        CaraManager.getInstance().checkCommands(getApplicationContext());
                    }
                }catch (Exception er){}

                CaraManager.getInstance().checkForReset();
                CaraManager.getInstance().checkForHalt(getApplicationContext());

                if (CaraManager.getInstance().getCurrentDateTime("hh:mm").equals(CaraManager.RESET_TIME)) {
                    if (!isExpiryChecked) {
                        isExpiryChecked=true;
                        CaraManager.getInstance().checkForExpiry(getApplicationContext());
                    }
                }

            }
        }
    };

    private int faceEnrolCnt=6;
    private CountDownTimer faceEnrolTmr=new CountDownTimer(faceEnrolCnt*1000,1000) {
        @Override
        public void onTick(long millisUntilFinished) {
            faceEnrolCnt--;
            countdown_text.setText(faceEnrolCnt+"");
            countdown_text.setVisibility(View.VISIBLE);
            countdown_text.setTextColor(getResources().getColor(R.color.white));
        }

        @Override
        public void onFinish() {
            countdown_text.setVisibility(View.INVISIBLE);
            if (!isTooFarClose) {
                if (FaceManager.getInstance().getBlurScore()>=CaraManager.getInstance().getEnrol_quality()) {
                    FaceManager.getInstance().setCheckSmile(false);
                    FaceManager.getInstance().setFacesDetected(true);
                    CaraManager.getInstance().setReadyForCapture(true);
                    isTooFarClose = true;
                    callOnce = 1;
                    capturePhoto();
                }else {
                    Toast.makeText(getApplicationContext(),"Face Enrol quality is bad, try again !",Toast.LENGTH_SHORT).show();
                }
            }
        }
    };

    private boolean isTooFarClose = false;
    private Camera.AutoFocusMoveCallback autoFocusCallback = new Camera.AutoFocusMoveCallback() {
        @Override
        public void onAutoFocusMoving(boolean start, Camera camera) {
            //Log.d("AutoFocus", "AutoFoucs -> " + start);
        }
    };
    //temperature calibration variables
    private double prevTemp = 0.0f;
    private double minError = 0.5f;
    private int TEMP_MAX_ITR = 4;
    private int TEMP_ITR = 0;
    private ImageView bluerview;// = new ImageView(getApplicationContext());

    private final Camera.PictureCallback pictureCallback = new Camera.PictureCallback() {
        @Override
        public void onPictureTaken(byte[] originalData, Camera camera) {

            //Log.d("Cara", "in picture callback data size is " + originalData.length);
            //stop camera preview and process images
            FaceManager.getInstance().stopCameraPreview(camera);

            if (!FaceManager.getInstance().isFaces()) {
                Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_NOT_MATCHED);
                Toast.makeText(getApplicationContext(), "Face not detected !", Toast.LENGTH_SHORT).show();
                if (postrun != null) {
                    handler.removeCallbacks(postrun);
                }
                postrun = new Runnable() {
                    @Override
                    public void run() {
                        if (clearui != null) {
                            clearui.cancel();
                        }
                        //gotoHome();
                    }
                };

                handler.postDelayed(postrun, 3000);
                return;
            }

            if (originalData == null) {
                Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_NOT_MATCHED);
                //Toast.makeText(getApplicationContext(), "Error in capture, try again !", Toast.LENGTH_SHORT).show();
                if (postrun != null) {
                    handler.removeCallbacks(postrun);
                }
                postrun = new Runnable() {
                    @Override
                    public void run() {
                        if (clearui != null) {
                            clearui.cancel();
                        }
                        gotoHome(false);
                    }
                };
                handler.postDelayed(postrun, 3000);
                return;
            } else {

                if (!FaceManager.getInstance().isFaces()) {
                    //Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_NOT_MATCHED);
                    Toast.makeText(getApplicationContext(), "Face not detected !", Toast.LENGTH_SHORT).show();
                    if (postrun != null) {
                        handler.removeCallbacks(postrun);
                    }
                    postrun = new Runnable() {
                        @Override
                        public void run() {
                            if (clearui != null) {
                                clearui.cancel();
                            }
                            // gotoHome(false);
                        }
                    };
                    handler.postDelayed(postrun, 3000);
                    return;
                }

                //Toast.makeText(EnrollCameraActivity.this,"Capture Success !",Toast.LENGTH_SHORT).show();
                try {
                    if (CaraManager.getInstance().getTimerTone() != null)
                        CaraManager.getInstance().getTimerTone().play();
                } catch (Exception er) { }

                //Toast.makeText(VisitorActivity.this, "Tap on next !", Toast.LENGTH_LONG).show();
                try {
                    bracket.setVisibility(View.INVISIBLE);
                    cameraPreviewLayout.setVisibility(View.VISIBLE);
                    infomsg.setVisibility(View.INVISIBLE);
                    //cameraPreviewLayout.setBackgroundResource(R.drawable.cara);

                    //ImageView bluerview = new ImageView(getApplicationContext());
                    //bluerview.setRotation(90);
                    bluerview.setLayoutParams(new ViewGroup.LayoutParams(
                            ViewGroup.LayoutParams.MATCH_PARENT,
                            ViewGroup.LayoutParams.MATCH_PARENT));
                    cameraPreviewLayout.addView(bluerview);
                    bluerview.setVisibility(View.INVISIBLE);

                    if (mTextureView != null && cameraPreviewLayout != null) {
                        //if (cameraPreviewLayout.indexOfChild(mTextureView)>0)
                        cameraPreviewLayout.removeView(mTextureView);
                    }

                    if (Build.VERSION.SDK_INT<=24) {
                        //rotate the image
                        // Changed resolution from 400x400 to 640x480
                        //originalData = AppUtils.flip(FaceManager.getInstance().getRoatedImage(Utilities.scaleDown(originalData, 400, 400, true),90));
                        originalData = AppUtils.flip(FaceManager.getInstance().getRoatedImage(Utilities.scaleDown(originalData, 640, 480, true),90));
                        if (originalData == null) {
                            Toast.makeText(getApplicationContext(), "Invalid Image data", Toast.LENGTH_SHORT).show();
                            return;
                        }
                    }else if (Build.MODEL.equalsIgnoreCase("DMG12800T070-33WTC")){
                        // Changed resolution from 400x400 to 640x480
                        //  originalData = AppUtils.flip(FaceManager.getInstance().getRoatedImage(Utilities.scaleDown(originalData, 400, 400, true),90));
                        originalData = AppUtils.flip(FaceManager.getInstance().getRoatedImage(Utilities.scaleDown(originalData, 640, 480, true),90));
                        if (originalData == null) {
                            Toast.makeText(getApplicationContext(), "Invalid Image data", Toast.LENGTH_SHORT).show();
                            return;
                        }

                        /*check quality of captured image
                        long t1=System.currentTimeMillis();
                        FaceManager.getInstance().setCameraCaptureImg(FaceManager.getInstance().getBitmapFromByte(originalData));
                        if(!FaceManager.getInstance().detectLiveFaces().isFace){
                            if (clearui != null) {
                                clearui.cancel();
                            }
                            Log.d("FaceValid",(System.currentTimeMillis()-t1)+", false");
                            FaceManager.getInstance().getCameraCaptureImg().recycle();

                            if (postrun != null) {
                                handler.removeCallbacks(postrun);
                            }
                            if (clearui != null) {
                                clearui.cancel();
                            }
                            initCamera();
                            return;
                        }*/

                    }

                    //testing, send original image
                    FaceManager.getInstance().setOriCamImg(originalData);
                    //Log.d("Cara",Build.MODEL);

                    if (FaceManager.getInstance().getFace() != null) {
                        if (!FaceManager.getInstance().getFace().isRecycled())
                            FaceManager.getInstance().getFace().recycle();
                    }

                    //this is used to crop face
                    //FaceManager.getInstance().setCameraCaptureImg(FaceManager.getInstance().getBitmapFromByte(originalData));

                    //get face image from camera
                    FaceManager.getInstance().setFace(FaceManager.getInstance().getFaceOnly(FaceManager.getInstance().getBitmapFromByte(originalData)));

                    //this is used for background blur not used in identification
                    if (bgBlur != null) {
                        if (!bgBlur.isRecycled())
                            BlurImage.with(getApplicationContext()).load(bgBlur).intensity(10).Async(true).into(bluerview);

                        bluerview.setVisibility(View.VISIBLE);
                        Utility.FadeView(bluerview);
                    }

                    //this is used for animation of views on camera screen
                    //Utility.setImageOn(FaceManager.getInstance().getFace(), bracket);
                    bracket.setVisibility(View.VISIBLE);

                    if (FaceManager.getInstance().getFace() != null && !FaceManager.getInstance().getFace().isRecycled())
                        bracket.setBackground(new BitmapDrawable(getResources(), FaceManager.getInstance().getFace()));

                    Utility.animateObject(bracket, -100f);

                    facebracket.setVisibility(View.VISIBLE);
                    Utility.animateObject(facebracket, -100f);

                    /*if (!FaceManager.getInstance().isEnroll()) {
                        if (progressBar != null)
                            CaraManager.getInstance().startProgess(progressBar);
                    }*/

                    //Log.d("ImageSize", );

                    if (!FaceManager.getInstance().isEnroll()) {
                        //identify
                        IdentifyMe(null, originalData);//FaceManager.getInstance().getByteFromBitmap(Facebitmap));

                    } else {
                        ensureEnrol("Are you sure ?",originalData);
                        //enrollFace(null, originalData);//FaceManager.getInstance().getByteFromBitmap(Facebitmap));
                    }

                } catch (Exception er) {
                    Utility.printStack(er);
                    gotoHome(false);
                }
            }
        }
    };
    private boolean isPaused;
    private long livenessTime=0;
    private final FaceDetectionListener faceDetectionListener = new FaceDetectionListener() {
        @Override
        public void onFaceDetection(Face[] faces, Camera camera) {

            //Log.d("thermal", "Number of Faces:" + faces.length);
            // Update the view now
            if (faces.length == 1 && !FaceManager.getInstance().isFaces()) {

                if (faces[0].score >= FACE_QUALITY) {
                    //mFaceView.setFaces(faces);
                    double faceDiagonal = Math.sqrt(Math.abs((faces[0].rect.right - faces[0].rect.left) + (faces[0].rect.bottom - faces[0].rect.top)));
                    //Log.d("faceDetector", "face Score -> " + faces[0].score + ", faceDiagonal dist -> " + faceDiagonal + ", isTooFar " + isTooFarClose + ", isReadyForTemp" + CaraManager.getInstance().isReadyForCapture());

                    if (faceDiagonal <= MAX_AREA && faceDiagonal >= MIN_AREA) {
                        //FaceManager.getInstance().setFacesDetected(true);
                        //callOnce = 1;
                        isTooFarClose = false;
                        FaceManager.getInstance().setCameraCaptureImg(mTextureView.getBitmap());

                        if (CaraManager.getInstance().isLiveness()) {
                            if (livenessTime==0)
                            livenessTime=System.currentTimeMillis();

                            if (livenesstext != null)
                                livenesstext.setVisibility(View.INVISIBLE);

                            worker.addTask(new OneShotProcessorTask(getApplicationContext(),
                                    Constants.TYPE_LIVENESS, null, null,
                                    EnrollModel.getInstance().getEnrolId(),
                                    FaceManager.getInstance().getFaceTheshold(), null));

                        } else {

                            livenesstext.setVisibility(View.INVISIBLE);
                            infomsg.setVisibility(View.INVISIBLE);
                            if (CaraManager.getInstance().isThermal()) {
                                if (CaraManager.getInstance().isReadyForCapture()) {
                                    isTooFarClose = true;
                                    CaraManager.getInstance().setReadyForCapture(false);
                                    callOnce = 1;
                                    FaceManager.getInstance().setFacesDetected(true);
                                    capturePhoto();
                                }
                            } else {
                                CaraManager.getInstance().setReadyForCapture(false);
                                callOnce = 1;
                                FaceManager.getInstance().setFacesDetected(true);
                                capturePhoto();
                            }

                        }

                    } else {
                        isTooFarClose = true;
                        if (livenesstext != null) {
                            livenesstext.setText(faceDiagonal > MAX_AREA ? "TOO CLOSE" : "COME CLOSER");
                            //livenesstext.setBackgroundColor(getResources().getColor(R.color.gray));
                            livenesstext.setVisibility(View.VISIBLE);
                            infomsg.setVisibility(View.INVISIBLE);
                            if (faceDiagonal < MIN_AREA && !CaraManager.getInstance().isComeCloser()) {
                                CaraManager.getInstance().setComeCloser(true);
                                Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_COME_CLOSE);
                            }
                        }
                    }
                } else {
                    isTooFarClose = true;
                    livenesstext.setVisibility(View.INVISIBLE);
                }

            } else {
                isTooFarClose = true;
                livenesstext.setVisibility(View.INVISIBLE);
            }

            if (CaraManager.getInstance().isThermal()) {
                if (!CaraManager.getInstance().isReadyForCapture()) {
                    //Log.d("thermal","into face detector");
                    worker.addTask(new OneShotProcessorTask(getApplicationContext(),
                            Constants.TYPE_THERMAL_SCAN, null, null,
                            EnrollModel.getInstance().getEnrolId(),
                            FaceManager.getInstance().getFaceTheshold(),
                            null));

                }
            }
            //Log.d("faceDetector",FaceManager.getInstance().isFaces()+"");
            //mFaceView.setFaces(faces);
        }

    };
    //private int readingCnt=0;
    private Handler captureHandler=new Handler();
    @SuppressLint("HandlerLeak")
    private final Handler workerHandler = new Handler() {
        public void handleMessage(Message msg) {


            if (msg.what == Constants.FACE_DETECT_SUCCESS){
                FaceManager.FaceDetectionResult faceDetectionResult =  (FaceManager.FaceDetectionResult) msg.obj;

                    //do liveness, thermal and other face related stuff here;
                    //Log.d("thermal", "Number of Faces:" + faces.length);
                    // Update the view now
                    if (faceDetectionResult.isFace  && !FaceManager.getInstance().isFaces()) {

                            //mFaceView.setFaces(faces);
                            //double faceDiagonal = Math.sqrt(Math.abs((faces[0].rect.right - faces[0].rect.left) + (faces[0].rect.bottom - faces[0].rect.top)));
                            //Log.d("faceDetector", "face Score -> " + faces[0].score + ", faceDiagonal dist -> " + faceDiagonal + ", isTooFar " + isTooFarClose + ", isReadyForTemp" + CaraManager.getInstance().isReadyForCapture());

                            //Log.d("cara","Face ara -> "+faceDetectionResult.faceArea);
                            //face area for close to camera 750(1.2feet), for far from camera 300 (2.4feet)
                            if (faceDetectionResult.faceArea <= 750 && faceDetectionResult.faceArea >= 300) {
                                //FaceManager.getInstance().setFacesDetected(true);
                                //callOnce = 1;
                                isTooFarClose = false;
                                FaceManager.getInstance().setCameraCaptureImg(mTextureView.getBitmap());

                                //this code capture photo if there is delay in capture due to liveness and temperature.
                                if (!FaceManager.getInstance().isFaceDetectionStarted() && CaraManager.getInstance().isThermal()){
                                   // Log.d("cara","into isFaceDetectionStarted");
                                    FaceManager.getInstance().setFaceDetectionStarted(true);
                                    if (!FaceManager.getInstance().isCaptureComplete()) {
                                        FaceManager.getInstance().setCaptureHandler(new Runnable() {
                                            @Override
                                            public void run() {
                                                if (!FaceManager.getInstance().isCaptureComplete()){
                                                   //Log.d("cara","capture executed from handler");
                                                    isTooFarClose = true;
                                                    CaraManager.getInstance().setReadyForCapture(false);
                                                    callOnce = 1;
                                                    FaceManager.getInstance().setFacesDetected(true);
                                                    capturePhoto();
                                                }else{
                                                    FaceManager.getInstance().setFaceDetectionStarted(false);
                                                }
                                            }
                                        });
                                        captureHandler.postDelayed(FaceManager.getInstance().getCaptureHandler(), 4000);
                                    }
                                }
                                //code end here for capture

                                if (CaraManager.getInstance().isLiveness()) {

                                    if (livenesstext != null)
                                        livenesstext.setVisibility(View.INVISIBLE);

                                    worker.addTask(new OneShotProcessorTask(getApplicationContext(),
                                            Constants.TYPE_LIVENESS, null, null,
                                            EnrollModel.getInstance().getEnrolId(),
                                            FaceManager.getInstance().getFaceTheshold(), null));

                                } else {

                                    livenesstext.setVisibility(View.INVISIBLE);
                                    infomsg.setVisibility(View.INVISIBLE);
                                    if (CaraManager.getInstance().isThermal()) {
                                        if (CaraManager.getInstance().isReadyForCapture()) {
                                            isTooFarClose = true;
                                            CaraManager.getInstance().setReadyForCapture(false);
                                            callOnce = 1;
                                            FaceManager.getInstance().setFacesDetected(true);
                                            capturePhoto();
                                        }
                                    } else {
                                        CaraManager.getInstance().setReadyForCapture(false);
                                        callOnce = 1;
                                        FaceManager.getInstance().setFacesDetected(true);
                                        capturePhoto();
                                    }

                                }

                            } else {
                                isTooFarClose = true;
                                if (livenesstext != null) {
                                    livenesstext.setText(faceDetectionResult.faceArea > 750 ? "TOO CLOSE" : "COME CLOSER");
                                    //livenesstext.setBackgroundColor(getResources().getColor(R.color.gray));
                                    livenesstext.setVisibility(View.VISIBLE);
                                    infomsg.setVisibility(View.INVISIBLE);
                                    if (faceDetectionResult.faceArea < 300 && !CaraManager.getInstance().isComeCloser()) {
                                        CaraManager.getInstance().setComeCloser(true);
                                        Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_COME_CLOSE);
                                    }
                                }
                            }

                    } else {
                        isTooFarClose = true;
                        livenesstext.setVisibility(View.INVISIBLE);
                    }

                    if (CaraManager.getInstance().isThermal() && faceDetectionResult.isFace) {
                        if (!CaraManager.getInstance().isReadyForCapture()) {
                            //Log.d("thermal","into face detector");
                            worker.addTask(new OneShotProcessorTask(getApplicationContext(),
                                    Constants.TYPE_THERMAL_SCAN, null, null,
                                    EnrollModel.getInstance().getEnrolId(),
                                    FaceManager.getInstance().getFaceTheshold(),
                                    null));

                        }
                    }

            }
            //hideProgress();
            if (msg.what == Constants.IDENTIFY_SUCCESS) {

                //LogUtils.debug("TAG", "argq " + msg.arg1 + " arg2 " + msg.arg2);
                IdentifyFaceResults faceResults = (IdentifyFaceResults) msg.obj;

                t2 = System.currentTimeMillis();
                String timereq = (t2 - t1) + "";

                if (faceResults == null) {
                    showError("No match found !");
                    return;
                }

                IdentifyFaceResult[] result = faceResults.identifyFaceResultl;
                if (result != null) {

                    if (result.length > 0) {

                        float maxScore = 0f;
                        for (int i = 0; i < result.length; i++) {
                            //Log.d("TAG", "User detected " + result[i].Score + " , " + result[i].Id);
                            if (result[i].Score > maxScore) {
                                maxScore = result[i].Score;
                                FaceManager.getInstance().setDetectedId(result[i].Id);
                            }
                        }

                        if (FaceManager.getInstance().getDetectedId().equals("")) {
                            showError("No match found !");
                            sendLog("NO_MATCH");
                            return;
                        }

                        if (thermalModel == null) {
                            thermalModel = new ThermalModel(0, 0, 0);
                        }


                        UserModel.getInstance().setMatchScore(maxScore + "");
                        UserModel.getInstance().setUserId(FaceManager.getInstance().getDetectedId());
                        UserModel.getInstance().setUserName(CaraManager.getInstance().getCaraUserById(FaceManager.getInstance().getDetectedId()));

                        if (!CaraManager.getInstance().isThermal()) {
                            UserModel.getInstance().setHeat(thermalModel.getHeat() + "");  //get temperature from sensor for user
                            UserModel.getInstance().setRecordHeat(thermalModel.getHeat()); //get temperature from sensor for store
                        }

                        if (UserModel.getInstance().getHeat("C") < 0 && CaraManager.getInstance().isThermal()) {
                            Toast.makeText(getApplicationContext(), "Temperature error, please reboot device !", Toast.LENGTH_SHORT).show();
                        }

                        //Log.d("Temp","user temp "+UserModel.getInstance().getRecordHeat());
                        //if (CaraManager.getInstance().isThermal()) {
                        //checkForLastTemp(thermalModel.getHeat(), UserModel.getInstance().getUserId());
                        //}

                        if (CaraManager.getInstance().getInoutMode().equals("-1")) {
                            UserModel.getInstance().setInOutStatus(getUserIOStatus(UserModel.getInstance().getUserId()));
                        } else {
                            UserModel.getInstance().setInOutStatus(CaraManager.getInstance().getInoutMode());
                        }

                        if (UserModel.getInstance().getInOutStatus().equalsIgnoreCase("1")) {
                            Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_MARK_IN);
                        } else if (UserModel.getInstance().getInOutStatus().equalsIgnoreCase("0")) {
                            Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_MARK_OUT);
                        }

                        //Log.d("getUserIOStatus","user -> "+ UserModel.getInstance().getInOutStatus()+"");
                        //Log.d("USER_TEMP",UserModel.getInstance().getRecordHeat()+"");

                        if (CaraManager.getInstance().isAlarm()) {
                            if (UserModel.getInstance().getHeat("C") >= CaraManager.getInstance().getMaxTemp()) {
                                Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_TEMP_HIGH);
                            } else {
                                //Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_TEMP_NORMAL);
                            }
                        }

                        String recordId = System.currentTimeMillis() + "";
                        CaraManager.getInstance().getCaraDb().insertAttendance(
                                FaceManager.getInstance().getDetectedId(),
                                UserModel.getInstance().getInOutStatus(),
                                CaraManager.getInstance().getCurrentDateTime(""),
                                UserModel.getInstance().getUploadStatus(),
                                UserModel.getInstance().getRecordHeat(),
                                UserModel.getInstance().getMatchScore() + "",
                                recordId);

                        showScreenInfo();

                        //open gate
                        CaraManager.getInstance().openGate(CameraActivity.this);

                        CaraManager.getInstance().getCaraDb().updateUser(UserModel.getInstance().getUserId()
                                , Database.LAST_STATUS, UserModel.getInstance().getInOutStatus());

                        CaraManager.getInstance().getCaraDb().updateUser(UserModel.getInstance().getUserId()
                                , Database.LAST_DATETIME, System.currentTimeMillis() + "");
                        CaraManager.getInstance().setRefHeat(thermalModel.getRefHeat());

                        CaraManager.getInstance().punchOnline(getApplicationContext(),
                                UserModel.getInstance().getInOutStatus(),
                                FaceManager.getInstance().getDetectedId(),
                                CaraManager.getInstance().getCurrentDateTime("time"),
                                CaraManager.getInstance().getCurrentDateTime("date"),
                                UserModel.getInstance().getRecordHeat(),
                                UserModel.getInstance().getMatchScore(),
                                recordId, timereq, livenessTime+"",CaraManager.getInstance().getRefHeat() + "");

                        Log.d("liveness_time","liveness-> "+livenessTime+"");

                        if (CaraManager.getInstance().isThermal()) {
                            if (UserModel.getInstance().getHeat("C") > CaraManager.getInstance().getMaxTemp()) {
                                CaraManager.getInstance().sendAlert(getApplicationContext(),
                                        FaceManager.getInstance().getDetectedId(),
                                        CaraManager.getInstance().getCurrentDateTime("time"),
                                        CaraManager.getInstance().getCurrentDateTime("date"),
                                        UserModel.getInstance().getInOutStatus(),
                                        UserModel.getInstance().getRecordHeat(),
                                        CaraManager.getInstance().getRefHeat() + "", "C", "temp");
                            }
                        }


                        sendLog("MATCH");

                    } else {
                        showError("No match found");
                        sendLog("NO_MATCH");
                    }
                } else {
                    showError("No match found");
                    sendLog("NO_MATCH");
                }

            } else if (msg.what == Constants.IDENTIFY_FAILURE) {
                //String error = (String) msg.obj;
                showError("No match found !");
                //onError(new Throwable(error));
            } else if (msg.what == Constants.INIT_SDK_SUCCESS) {
                //boolean isSdkInitialized = (Boolean) msg.obj;
                // onSDKinitialized(isSdkInitialized);
                //initCamera();
            } else if (msg.what == Constants.ENROLL_SUCCESS) {
                //Log.d("enroll", UserModel.getInstance().getUserName());
                try {
                    CaraManager.getInstance().getCaraDb().insertUsers(EnrollModel.getInstance().getEnrolId(),
                            EnrollModel.getInstance().getName());
                } catch (Exception er) {
                    try {
                        //remove user if error in database addition
                        if (FaceManager.getInstance().getLocalCacheManager() != null) {
                            FaceManager.getInstance().getLocalCacheManager().removeUserById(EnrollModel.getInstance().getEnrolId());
                        }
                    } catch (Exception err) {
                    }
                }
                hideProgress();
                showMessage("ENROLL SUCCESS !", "USER " + EnrollModel.getInstance().getName() + " added successfully !", "");

            } else if (msg.what == Constants.ENROLL_FAILURE) {
                hideProgress();
                showMessage("ERROR", "Error adding user " + EnrollModel.getInstance().getName(), "error");
            } else if (msg.what == Constants.QUALITY_CHECK_SUCCESS) {
                if ((boolean) msg.obj) {
                    callOnce = 1;
                    //capturePhoto();
                }
            } else if (msg.what == Constants.THERMAL_SCAN_SUCCESS) {

                //Log.d("Cameraliveness","isThermalReady -> "+CaraManager.getInstance().isReadyForCapture());

                if (CaraManager.getInstance().isThermal() && !CaraManager.getInstance().isReadyForCapture()) {
                    thermalModel = (ThermalModel) msg.obj;

                    // Log.d("thermal", "into camera " + thermalModel);

                    if (thermalModel == null)
                        return;

                    //readingCnt++;
                    double diff = Math.abs(thermalModel.getHeat() - prevTemp);
                    //Log.d("TEMP","Reading count -> "+0+", DIFF -> "+diff+" optimization pass ->"+TEMP_ITR+" prevTemp->"+prevTemp);

                    if (prevTemp == 0) {
                        prevTemp = thermalModel.getHeat();
                    } else if (diff <= minError) {

                        if (TEMP_ITR >= TEMP_MAX_ITR) {
                            TEMP_ITR = 0;
                            //Log.d("TEMP", "ready for capture");
                            //proceed only if temperature is greater than 31 degree centigrade
                            if (thermalModel.getHeat() >= CaraManager.getInstance().getMinTempVal() && thermalModel.getHeat() <= 40) {
                                infomsg.setText("");
                                infomsg.setVisibility(View.INVISIBLE);
                                CaraManager.getInstance().setReadyForCapture(true);
                                isTooFarClose = true;
                            }

                        }

                        TEMP_ITR++;
                        prevTemp = thermalModel.getHeat();

                    } else {
                        //TEMP_ITR--;
                        prevTemp = thermalModel.getHeat();
                    }

                    if (thermalModel.getHeat() >= CaraManager.getInstance().getMinTempVal() && thermalModel.getHeat() <= 40) {
                        UserModel.getInstance().setHeat(thermalModel.getHeat() + "");
                        UserModel.getInstance().setRecordHeat(thermalModel.getHeat());
                    }

                    float Temp = CaraManager.getInstance().convertHeat(CaraManager.getInstance().getTempUnit(), thermalModel.getHeat());
                    String showTemp = Temp + " " + UserModel.getInstance().getUnit(CaraManager.getInstance().getTempUnit());
                    infomsg.setVisibility(View.VISIBLE);

                    if (thermalModel.getHeat() > 0) {
                        infomsg.setText("Reading Temperature  " + showTemp);
                    } else {
                        infomsg.setText("Reboot Required");
                        if (errorSend == 0) {
                            errorSend = 1;
                            CaraManager.getInstance().sendSignal("ACTION_TEMP_FAILED", getApplicationContext());
                        }
                    }

                }
            } else if (msg.what == Constants.LIVENESS_SUCCESS) {

                FaceManager.FaceResults faceResults = (FaceManager.FaceResults) msg.obj;

                //Log.d("Cameraliveness", "Final result ->" + faceResults.isReal +", framecnt ->"+framecnt+", Max Frame -> "+MAX_FRAME);
                //Log.d("Blur",faceResults.fakeScore);
                if (FaceManager.getInstance().isEnroll()){
                    float qscore=Float.parseFloat(faceResults.fakeScore);
                    imagequality.setVisibility(View.VISIBLE);
                    boolean isEnrol=false;
                    FaceManager.getInstance().setBlurScore(qscore);

                    if (qscore<CaraManager.getInstance().getEnrol_quality()){
                        return;
                    }

                    if (qscore<15){
                        isEnrol=false;
                        imagequality.setImageResource(R.drawable.badq);
                    }else if (Float.parseFloat(faceResults.fakeScore)>=30){
                        imagequality.setImageResource(R.drawable.bestq);
                        isEnrol=true;
                    }else if (Float.parseFloat(faceResults.fakeScore)>20 && Float.parseFloat(faceResults.fakeScore)<30){
                        isEnrol=true;
                        imagequality.setImageResource(R.drawable.goodq);
                    }

                    if (isEnrol && !CaraManager.getInstance().isReadyForCapture()){
                        //Log.d("EnrolImg","called from good and best");
                        FaceManager.getInstance().setCheckSmile(false);
                        FaceManager.getInstance().setFacesDetected(true);
                        CaraManager.getInstance().setReadyForCapture(true);
                        isTooFarClose = true;
                        callOnce = 1;
                        capturePhoto();
                        return;
                    }

                }

                if (faceResults.isReal) {
                    truepos = (truepos + 1);
                } else {
                    trueneg = (trueneg + 1);
                }

                framecnt = framecnt + 1;

                if (framecnt == MAX_FRAME) {

                    double Accuracy = (truepos) / MAX_FRAME;
                    //Log.d("Cameraliveness", "Accuracy ->" + Accuracy);
                    if (Accuracy >= CaraManager.getInstance().getLiveness_thr()) {

                        livenessTime=(System.currentTimeMillis()-livenessTime);
                        if (CaraManager.getInstance().isThermal()) {
                            if (UserModel.getInstance().getHeat("C") >= CaraManager.getInstance().getMinTempVal() && UserModel.getInstance().getHeat("C") <= 40) {
                                callOnce = 1;
                                framecnt = 0;
                                trueneg = 0;
                                truepos = 0;
                                FaceManager.getInstance().setCheckSmile(false);
                                FaceManager.getInstance().setFacesDetected(true);
                                CaraManager.getInstance().setReadyForCapture(true);
                                isTooFarClose = true;
                                capturePhoto();
                                return;
                            }
                        } else {
                            callOnce = 1;
                            framecnt = 0;
                            trueneg = 0;
                            truepos = 0;
                            FaceManager.getInstance().setCheckSmile(false);
                            FaceManager.getInstance().setFacesDetected(true);
                            CaraManager.getInstance().setReadyForCapture(true);
                            isTooFarClose = true;
                            capturePhoto();
                            return;
                        }
                    }

                    framecnt = 0;
                    trueneg = 0;
                    truepos = 0;

                }

            }
        }
    };

    private void sendLog(String type) {

            /*CaraManager.getInstance().sendImgLog(getApplicationContext(),
                    FaceManager.getInstance().getFace(), "1",
                    CaraManager.getInstance().getCurrentDateTime("date"),
                    CaraManager.getInstance().getCurrentDateTime("time")
                    , UserModel.getInstance().getInOutStatus(),
                    type + "_" + FaceManager.getInstance().getDetectedId()+"_"+livenessTime);*/

            try {
                CaraManager.getInstance().sendImgLog(getApplicationContext(),
                        FaceManager.getInstance().getOriCamImg(), "1",
                        CaraManager.getInstance().getCurrentDateTime("date"),
                        CaraManager.getInstance().getCurrentDateTime("time")
                        , UserModel.getInstance().getInOutStatus(),
                        type + "_" + FaceManager.getInstance().getDetectedId() + "_" + livenessTime);
            }catch (Exception er){
                ///er.printStackTrace();
                //error in getting image
            }

            //test - done for testing on 8th June 2021 //remove for production use
            //Utilities.writeToFile(FaceManager.getInstance().getByteFromBitmap(FaceManager.getInstance().getCameraCaptureImg(),100),
            //FaceManager.getInstance().getDetectedId()+"_"+type+"_"+System.currentTimeMillis()+".jpg");
    }

    private void updateDateTime() {
        if (currentTime == null) {
            currentTime = findViewById(R.id.currentTime);
            currentDate = findViewById(R.id.currentDate);
        }
        currentDate.setText(CaraManager.getInstance().getCurrentDateTime("W:D"));
        currentTime.setText(CaraManager.getInstance().getCurrentDateTime("hh:mm"));

        if (infomsg != null)
            infomsg.setVisibility(View.INVISIBLE);
    }

    private void checkForLastTemp(double currentTemp, String empid) {
        // thermalModel.getHeat(),CaraManager.getInstance().isThermal())
        Cursor crs = CaraManager.getInstance().getCaraDb().getLastKnownTemp(empid);
        if (crs.moveToFirst()) {
            double lastKnownTemp = Double.parseDouble(crs.getString(crs.getColumnIndex(Database.LAST_KNOWN_TEMP)));
            long timeStamp = Long.parseLong(crs.getString(crs.getColumnIndex("timeStamp")));
            if ((System.currentTimeMillis() - timeStamp) < CaraManager.getInstance().getMINIMUM_ALLOWED_TIME()) {
                if (Math.abs(currentTemp - lastKnownTemp) > 1) {
                    //Log.d("Temp",currentTemp+", last-> "+crs.getString(crs.getColumnIndex(Database.LAST_KNOWN_TEMP))+", temp correction "+(currentTemp-lastKnownTemp));
                    double newTemp = (currentTemp + lastKnownTemp) / 2;
                    UserModel.getInstance().setHeat(newTemp + "");
                    UserModel.getInstance().setRecordHeat(newTemp);
                }
            }
        }
        crs.close();
    }

    private String getUserIOStatus(String empid) {
        String last_status = "0";
        Cursor crs = CaraManager.getInstance().getCaraDb().getInOutStatus(empid);
        if (crs.moveToFirst()) {
            last_status = crs.getString(crs.getColumnIndex(Database.LAST_STATUS));
            long timeStamp = Long.parseLong(crs.getString(crs.getColumnIndex(Database.LAST_DATETIME)));
            if ((System.currentTimeMillis() - timeStamp) < CaraManager.getInstance().getMINIMUM_ALLOWED_TIME()) {
                crs.close();
                return last_status;
            }
        }
        crs.close();

        if (last_status.equals("0")) {
            return "1";
        } else if (last_status.equals("1")) {
            return "0";
        } else {
            return "1";
        }
        //return last_status.equals("0") ? "1" : "0";
    }

    private synchronized void capturePhoto() {

        try {
            if (isPaused) {
                return;
            }

            countdown_text.setVisibility(View.INVISIBLE);
            if (faceEnrolTmr != null)
                faceEnrolTmr.cancel();

            FaceManager.getInstance().setCheckSmile(false);
            FaceManager.getInstance().setSmileDetected(false);
            captureHandler.removeCallbacks(FaceManager.getInstance().getCaptureHandler());
            FaceManager.getInstance().setCaptureComplete(true);

            if (worker.queue.size() > 0)
                worker.queue.clear();

            try {
                if (FaceManager.getInstance().isEnroll()) {
                    imagequality.setVisibility(View.INVISIBLE);
                    instructions.setVisibility(View.INVISIBLE);
                    View bg = findViewById(R.id.relativeLayout);
                    bg.setBackgroundResource(R.color.transperant);
                }
            } catch (Exception er) {
                Utility.printStack(er);
            }

            if (callOnce == 1) {
                callOnce = 0;

                if (bgBlur != null) {
                    if (!bgBlur.isRecycled()) bgBlur.recycle();
                }

                //Facebitmap = mTextureView.getBitmap();
                bgBlur = FaceManager.getInstance().getCameraCaptureImg();
                FaceManager.getInstance().setFacesDetected(true);

                if (livenesstext != null)
                    livenesstext.setVisibility(View.INVISIBLE);

                if (infomsg != null)
                    infomsg.setVisibility(View.INVISIBLE);


                try {
                    countdown_text.setVisibility(View.INVISIBLE);
                    countdown_text.setText("");

                    FaceManager.getInstance().getCamera().takePicture(null, null, pictureCallback);
                    cameraPreviewLayout.setVisibility(View.VISIBLE);
                } catch (Exception er) {
                    initCamera();
                    Utility.printStack(er);
                    Toast.makeText(getApplicationContext(), "Error in camera, try again !", Toast.LENGTH_SHORT).show();
                }
            }
        }catch (Exception er){
            Utility.printStack(er);
        }

    }

    void initializeSDK() {
        worker = Worker.getInstance();
        worker.sethandler(workerHandler);
        worker.addTask(new OneShotProcessorTask(CameraActivity.this, Constants.TYPE_INIT_SDK, null, null, null, 0, null));
    }

    public void onScreenTap(View view) {
        cntScreenTap++;
        if (cntScreenTap == 3) {
            cntScreenTap = 0;
            gotoHome(false);
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
        updateSettings();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        CaraManager.getInstance().hideUi(getWindow(), this);
        setContentView(R.layout.activity_capture);

        mTextureView = new TextureView(this);
        mTextureView.setSurfaceTextureListener(this);
        countdown_text = findViewById(R.id.countdown_text);
        countdown_text.setText(Helper.CAMERA_CAPTURE_TIME + "");
        //countdown_text.setVisibility(View.VISIBLE);
        flname = findViewById(R.id.flname);
        userid = findViewById(R.id.userid);
        userid.setText("");
        userid.setVisibility(View.INVISIBLE);
        facebracket = findViewById(R.id.facebracket);
        facebracket.setVisibility(View.INVISIBLE);
        imagequality=findViewById(R.id.imagequality);
        imagequality.setVisibility(View.INVISIBLE);
        instructions=findViewById(R.id.instructions);
        instructions.setVisibility(View.INVISIBLE);
        progressBar = findViewById(R.id.progressBar);
        inoutText = findViewById(R.id.inOutStatus);
        thermalProgress = findViewById(R.id.thermalProgress);
        thermameter = findViewById(R.id.thermameter);
        temptext = findViewById(R.id.temptext);
        bluerview = new ImageView(getApplicationContext());

        livenesstext = findViewById(R.id.livenesstext);
        infomsg = findViewById(R.id.infomsg);

        mFaceView = new FaceOverlayView(this);
        addContentView(mFaceView, new ViewGroup.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));

        cameraPreviewLayout = findViewById(R.id.camera_preview);
        bracket = findViewById(R.id.bracket);


        setupLoader();
        initializeSDK();

        // setTimeout();
        //CaraManager.getInstance().setupLoader(this,"Wait...");
        //CaraManager.getInstance().showDialog("Getting users...");


        if (!FaceManager.getInstance().isEnroll()) {

            if (CaraManager.getInstance().isNetworkError()) {
                CaraManager.getInstance().setCommand(false);
                initCamera();
            } else {
                showProgress("Device sync in progress...");

                CaraManager.getInstance().executeAction(Helper.COMMAND_ADD, getApplicationContext(), new HelperCallback() {
                    @Override
                    public void onResult(String result) {
                        //CaraManager.getInstance().hideDialog();
                        CaraManager.getInstance().setCommand(false);
                        hideProgress();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initCamera();
                            }
                        });

                    }

                    @Override
                    public void onError(String result) {
                        CaraManager.getInstance().setCommand(false);
                        hideProgress();
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                initCamera();
                            }
                        });
                    }
                });
            }
        } else {
            bottomView = findViewById(R.id.bottomView);
            bottomView.setVisibility(View.INVISIBLE);
            retakebtn = findViewById(R.id.retakebtn);
            retakebtn.setVisibility(View.GONE);
            instructions.setImageResource(R.drawable.instructions);
            instructions.setVisibility(View.VISIBLE);
            View bg=findViewById(R.id.relativeLayout);
            bg.setBackgroundResource(R.drawable.overlay);
            initCamera();
            faceEnrolTmr.start();
        }

        CaraManager.getInstance().initUsers(getApplicationContext());
        //Helper.getAuralFeedback(this, Helper.getAuralLang(), Helper.AURAL_PHOTO);

        //check for expiry
        //CaraManager.getInstance().checkForExpiry(getApplicationContext());

        if (!Listener.isSDKInitialized)
            CaraManager.getInstance().sendSignal("ACTION_SDK_FAILED", getApplicationContext());


    }

    private void initViews() {
        countdown_text.setText("");
        facebracket.setVisibility(View.INVISIBLE);
        userid.setText("");
        flname.setText("");
        flname.setVisibility(View.INVISIBLE);
        userid.setVisibility(View.INVISIBLE);
        thermalProgress.setVisibility(View.INVISIBLE);
        thermameter.setVisibility(View.INVISIBLE);
        bracket.setBackground(getDrawable(R.drawable.bracket));
        inoutText.setText("");
        temptext.setText("");
        userid.setTextColor(getResources().getColor(R.color.name_id_color));
        thermalModel = null;
        livenesstext.setVisibility(View.INVISIBLE);
        infomsg.setVisibility(View.INVISIBLE);
        isTooFarClose = false;
        livenessTime=0;

        UserModel.getInstance().clearAll();
        FaceManager.getInstance().setFacesDetected(false);
        FaceManager.getInstance().setCheckSmile(false);
        FaceManager.getInstance().setSmileDetected(false);
        CaraManager.getInstance().setReadyForCapture(false);
        CaraManager.getInstance().setComeCloser(false);
        CaraManager.getInstance().setGateCommand(false);
        FaceManager.getInstance().setBlurScore(0);
        FaceManager.getInstance().setFaceDetectionStarted(false);
        FaceManager.getInstance().setCaptureComplete(false);

        if (FaceManager.getInstance().isEnroll()){
            MIN_AREA=35;
        }else {
            MIN_AREA=30;
        }

        prevTemp = 0f;
        TEMP_ITR = 0;
        //readingCnt=0;

        //reverse the animation
        Utility.animateObject(facebracket, 100f);
        Utility.animateObject(flname, 100f);
        Utility.animateObject(bracket, 100f);

        if (CaraManager.getInstance().isThermal()) {
            resizeView(200, 190);
            Utility.animateObject(findViewById(R.id.inoutProgress), 0f);
        }

        if (cameraPreviewLayout != null && bluerview != null) {
            cameraPreviewLayout.removeView(bluerview);
        }

        if (mTextureView != null)
            mTextureView.setVisibility(View.VISIBLE);

        /*if (!FaceManager.getInstance().isEnroll()) {
            if (FaceManager.getInstance().getFace() != null) {
                if (!FaceManager.getInstance().getFace().isRecycled()) {
                    // FaceManager.getInstance().getFace().recycle();
                }
            }

            if (FaceManager.getInstance().getCameraCaptureImg() != null) {
                if (!FaceManager.getInstance().getCameraCaptureImg().isRecycled()) ;
                //FaceManager.getInstance().getCameraCaptureImg().recycle();
            }
        } */

        // facebracket.setVisibility(View.VISIBLE);
    }

    private synchronized void initCamera() {

        initViews();

        if (isCameraOkay()) {
            FaceManager.getInstance().setCamera(checkDeviceCamera());
        } else {
            if (Build.VERSION.SDK_INT >= 23)
                requestPermissions(new String[]{Manifest.permission.CAMERA}, 4444);

            Toast.makeText(getApplicationContext(), "Allow camera permission", Toast.LENGTH_SHORT).show();
            return;
        }

        Camera.Parameters params = FaceManager.getInstance().getCamera().getParameters();
        //params.setExposureCompensation(2);

        List<Camera.Size> mSupportedPreviewSizes;
        mSupportedPreviewSizes = FaceManager.getInstance().getCamera().getParameters().getSupportedPreviewSizes();

        DisplayMetrics displayMetrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(displayMetrics);

        int DSI_height = displayMetrics.heightPixels;
        int DSI_width = displayMetrics.widthPixels;


        for (int s = 0; s < mSupportedPreviewSizes.size(); s++) {
            //Log.d("CameraSize",mSupportedPreviewSizes.get(s).width+","+mSupportedPreviewSizes.get(s).height);
            if (mSupportedPreviewSizes.get(s).height >= DSI_height) {
                width_ = mSupportedPreviewSizes.get(s).width;
                height_ = mSupportedPreviewSizes.get(s).height;
                params.setPreviewSize(width_, height_);
                break;
            }
        }

        hasFlash(params);

        try {
            if (params.isAutoExposureLockSupported()) {
                params.setAutoExposureLock(false);
            }
            if (params.isAutoWhiteBalanceLockSupported()) {
                params.setAutoWhiteBalanceLock(false);
                params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            }

            params.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
            // params.setFocusMode(Camera.Parameters.FOCUS_MODE_CONTINUOUS_PICTURE);
            //params.set("iso-speed", "1600");
            FaceManager.getInstance().getCamera().setParameters(params);
        } catch (Exception er) { }

        if (cameraPreviewLayout != null) {
            cameraPreviewLayout.removeView(mTextureView);
        }
        cameraPreviewLayout.addView(mTextureView);
       // Log.d("cara","camera init");
        cam_time_out = Helper.CAMERA_CAPTURE_TIME;

        try {

            if (faceTime != null) {
                faceTime.cancel();
            }

            faceTime = new CountDownTimer(Helper.CAMERA_CAPTURE_TIME * 1000, 1000) {
                @Override
                public void onTick(long millisUntilFinished) {
                    cam_time_out--;
                    // countdown_text.setText(cam_time_out + "");
                    // countdown_text.setVisibility(View.VISIBLE);
                    CaraManager.getInstance().fullScreen(getWindow());
                    try {
                        CaraManager.getInstance().getShutterTone();

                        if (cam_time_out % 2 == 0) {
                            Utility.animateScale(bracket, 1.5f);
                        } else {
                            Utility.animateScale(bracket, 1.0f);
                        }
                    } catch (Exception er) {
                        // er.printStackTrace();
                        //gotoHome();
                    }
                }

                @Override
                public void onFinish() {
                    /*countdown_text.setVisibility(View.INVISIBLE);
                    countdown_text.setText("");
                    camera.takePicture(null, null, pictureCallback);
                    cameraPreviewLayout.setVisibility(View.VISIBLE);*/
                    cam_time_out = Helper.CAMERA_CAPTURE_TIME;
                    if (faceTime != null) {
                        faceTime.cancel();
                        faceTime.start();
                    }
                }

            };
            //camera.takePicture(null, null, pictureCallback);
        } catch (Exception er) {
            Utility.printStack(er);
            //gotoHome();
        }

        if (!CaraManager.getInstance().isSimulateOnly()) {
            countdown_text.setVisibility(View.INVISIBLE);
            countdown_text.setText("");
            faceTime.start();
        }
    }

    private void showProgress(String msg) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pd != null) {
                    pd.setMessage(msg);
                    pd.show();
                }
            }
        });
    }

    private void hideProgress() {
        if (pd != null) {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            pd.hide();
                        }
                    }, 1500);
                }
            });
        }
    }

    private void setupLoader() {
        if (pd != null) {
            pd.cancel();
        }
        pd = new ProgressDialog(CameraActivity.this, ProgressDialog.THEME_DEVICE_DEFAULT_LIGHT);
        // Set progress dialog style spinner
        pd.setProgressStyle(ProgressDialog.STYLE_SPINNER);
        // Set the progress dialog title and message
        //pd.setTitle("Title of progress dialog.");
        pd.setMessage("Checking...");
        // Set the progress dialog background color
        //pd.getWindow().setBackgroundDrawable(new ColorDrawable(Color.parseColor("#FFD4D9D0")));
        pd.setIndeterminate(false);
        pd.setCancelable(false);
    }

    private void gotoHome(boolean isRelaunch) {
        FaceManager.getInstance().stopCameraPreview(FaceManager.getInstance().getCamera());

        try {
            runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    startActivity(new Intent(CameraActivity.this, MainActivity.class));
                    finish();
                }
            });
        } catch (Exception er) {
        }
        Helper.stopAuralFb();

    }

    private Camera checkDeviceCamera() {

        Camera mCamera = null;
        Camera.CameraInfo ci = new Camera.CameraInfo();
        try {
            int totalCams = Camera.getNumberOfCameras();
            if (totalCams > 0) {
                mCamera = Camera.open(FaceManager.getInstance().getCameraId());
                // mCamera.enableShutterSound(true);
                mCamera.setFaceDetectionListener(faceDetectionListener);
                mCamera.setAutoFocusMoveCallback(autoFocusCallback);
                //mCamera.startFaceDetection();
                // Log.d("Total cameras", "Total cameras " + totalCams);
                Camera.getCameraInfo(FaceManager.getInstance().getCameraId(), ci);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.N) {
                    if (ci.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
                        mCamera.setDisplayOrientation(90);
                    }
                    if (ci.facing == Camera.CameraInfo.CAMERA_FACING_BACK) {
                        mCamera.setDisplayOrientation(90);
                    }
                } else {
                    //setCameraDisplayOrientation(CameraActivity.this, ci.facing, mCamera);
                    setCameraDisplayOrientation(CameraActivity.this, FaceManager.getInstance().getCameraId(), mCamera);
                }
            } else {
                Toast.makeText(getApplicationContext(), "Camera not available", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            mCamera = Camera.open(FaceManager.getInstance().getCameraId()==1?0:1);
            // mCamera.enableShutterSound(true);
            FaceManager.getInstance().getCamera().setFaceDetectionListener(faceDetectionListener);
            FaceManager.getInstance().getCamera().setAutoFocusMoveCallback(autoFocusCallback);
            if (Build.VERSION.SDK_INT<=24) {
                mCamera.startFaceDetection();
            }
            mCamera.setDisplayOrientation(90);
        }

        Camera.Parameters params = mCamera.getParameters();
        // params.setExposureCompensation(params.getMaxExposureCompensation());
        if (params.isAutoExposureLockSupported()) {
            params.setAutoExposureLock(false);
        }
        params.setAutoExposureLock(false);
        params.setAutoWhiteBalanceLock(true);
        //params.setExposureCompensation(2);
        //params.set("iso-speed", "1600");
        params.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
        //params.setFlashMode(Camera.Parameters.FLASH_MODE_AUTO);
        mCamera.setParameters(params);
        return mCamera;
    }

    private void setCameraDisplayOrientation(Activity activity, int cameraId, Camera camera) {
        Camera.CameraInfo info = new Camera.CameraInfo();
        Camera.getCameraInfo(cameraId, info);
        int rotation = activity.getWindowManager().getDefaultDisplay().getRotation();
        int degrees = 0;
        switch (rotation) {
            case Surface.ROTATION_0:
                degrees = 0;
                break;
            case Surface.ROTATION_90:
                degrees = 90;
                break;
            case Surface.ROTATION_180:
                degrees = 180;
                break;
            case Surface.ROTATION_270:
                degrees = 270;
                break;
        }

        int result;
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        } else {  // back-facing
            //FRONT FACING ONLY
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360;  // compensate the mirror
        }
        Log.d("Rotation",result+"");
        camera.setDisplayOrientation(270);
    }

    private boolean isCameraOkay() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                int permissionCamera = ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA);
                return permissionCamera == PackageManager.PERMISSION_GRANTED;
            } else {
                return true;
            }
        } catch (Exception er) {
            Utility.printStack(er);
        }
        return false;
    }

    private boolean hasFlash(Camera.Parameters parameters) {
        //Camera.Parameters params = camera.getParameters();
        camParms = parameters;
        List<String> flashModes = parameters.getSupportedFlashModes();
        if (flashModes == null) {
            return false;
        }

        for (String flashMode : flashModes) {
            if (Camera.Parameters.FLASH_MODE_ON.equals(flashMode)) {
                FaceManager.getInstance().toggleFlash(parameters, FaceManager.isLowlight);
                //parameters.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
                return true;
            }
        }
        return false;
    }

   /*Camera.PreviewCallback previewCallback=new Camera.PreviewCallback() {
       @Override
       public void onPreviewFrame(byte[] data, Camera camera) {
           Log.d("cameraFrame","into frame");
       }
   };*/

    public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
        try {

            FaceManager.getInstance().getCamera().setPreviewTexture(surface);

            //FaceManager.getInstance().getCamera().setPreviewCallback(previewCallback);

            Camera.Parameters parameters = FaceManager.getInstance().getCamera().getParameters();
            parameters.set("brightness_value", "middle");

            if (parameters.isAutoWhiteBalanceLockSupported()) {
                parameters.setWhiteBalance(Camera.Parameters.WHITE_BALANCE_AUTO);
            }

            if (parameters.isAutoExposureLockSupported()) {
                parameters.setSceneMode(Camera.Parameters.SCENE_MODE_AUTO);
                parameters.setAutoWhiteBalanceLock(false);
                parameters.setAutoExposureLock(false);
                //parameters.setExposureCompensation(2);
            }

            if (width_ != 0) {
                parameters.setPreviewSize(width_, height_);
            }
            hasFlash(parameters);

            try {
                FaceManager.getInstance().getCamera().setParameters(parameters);
            } catch (Exception er) {
            }

            FaceManager.getInstance().getCamera().setFaceDetectionListener(faceDetectionListener);
            FaceManager.getInstance().getCamera().setAutoFocusMoveCallback(autoFocusCallback);
            FaceManager.getInstance().getCamera().startPreview();
            if (Build.VERSION.SDK_INT<=24) {
                FaceManager.getInstance().getCamera().startFaceDetection();
            }

        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Ignored, Camera does all the work for us
    }

    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        try {
            FaceManager.getInstance().getCamera().stopPreview();
            FaceManager.getInstance().getCamera().release();
        } catch (Exception er) {
        }
        return true;
    }

    private void detectLowLight(Bitmap bitmap) {
        try {
            if (mTextureView != null && bitmap != null && !bitmap.isRecycled())
                FaceManager.getInstance().getImageColors(bitmap, new Palette.PaletteAsyncListener() {
                    public void onGenerated(Palette p) {
                        // Use generated instance
                        int color = p.getDominantColor(Color.WHITE);
                        boolean isDark = FaceManager.getInstance().isDark(color, 70, 48, true);
                        if (isDark && !FaceManager.getInstance().isDarkModeActivated()) {
                            FaceManager.getInstance().setDarkModeActivated(true);
                            //FaceManager.getInstance().stopCameraPreview(FaceManager.getInstance().getCamera());
                            //initCamera();
                        } else if (!isDark && FaceManager.getInstance().isDarkModeActivated()) {
                            //FaceManager.getInstance().stopCameraPreview(FaceManager.getInstance().getCamera());
                            //initCamera();
                            FaceManager.getInstance().setDarkModeActivated(false);
                        }
                        if (!bitmap.isRecycled())
                            bitmap.recycle();
                        //Log.d("color",color+"");
                    }
                });
        } catch (Exception er) {
            Utility.printStack(er);
        }
    }

    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Facebitmap = mTextureView.getBitmap();

        if (CaraManager.getInstance().isAutoFlash() && !FaceManager.getInstance().isFaces()) {
            detectLowLight(mTextureView.getBitmap());
        }


        if (Build.VERSION.SDK_INT>=27){
            //new face detection for android pie+
            FaceManager.getInstance().setCameraCaptureImg(mTextureView.getBitmap());
            worker.addTask(new OneShotProcessorTask(getApplicationContext(),
                    Constants.TYPE_FACE_DETECT, null, null,
                    EnrollModel.getInstance().getEnrolId(),
                    FaceManager.getInstance().getFaceTheshold(), null));

        }

        /*FaceManager.getInstance().setCameraCaptureImg(mTextureView.getBitmap());
        Bitmap bitmap=FaceManager.getInstance().detectFaces();
        /*if (face!=null) {
            if (face.length>0) {
                //Log.d("FACE_AREA",face[0].area()+"");
                //Toast.makeText(getApplicationContext(),"Faces detected is "+face.length,Toast.LENGTH_SHORT).show();
                android.graphics.Rect[] rects={new android.graphics.Rect((int)face[0].tl().x-150,
                        (int)face[0].tl().y-150,
                        (int)face[0].br().x+50,
                        (int)face[0].br().y+50)};
                mFaceView.setFaces(rects);
            }
        }
        if (bitmap!=null){
            bracket.setImageBitmap(bitmap);
        } */

    }

    private void showMessage(String title, String message, String type) {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (pd != null)
                    pd.hide();

                if (clearui != null) {
                    clearui.cancel();
                }
                //Toast.makeText(getApplicationContext(), message, Toast.LENGTH_SHORT).show();
                showMessageDia(title, message, type);
            }
        });
    }

    public void onRetakeBtn(View view) {
        FaceManager.getInstance().setEnroll(true);
        CaraManager.getInstance().setFadeAnimation(view);
        initCamera();
        view.setVisibility(View.INVISIBLE);
    }

    private synchronized void enrollFace(final String faceResult, byte[] byteImg) {
        try {

            if (FaceManager.getInstance().getSdkType().equals(FaceManager.SDK_TECH5)) {
                if (Listener.isSDKInitialized) {
                    //EnrollResponse enrollResponse = FaceManager.getInstance().enrollFace(getApplicationContext(), EnrollModel.getInstance().getEnrolId(), byteImg);
                    //showErrorMsg(enrollResponse.errorMessage);
                    showProgress("Enrol started ...");

                    new Thread(new Runnable() {
                        @Override
                        public void run() {

                            long t1 = System.currentTimeMillis();
                            ArrayList<CreateFaceTemplateResult> templates = FaceManager.getInstance().getFaceTemp(byteImg);

                            if (templates != null) {

                                try {

                                    if (templates.size() > 0) {
                                        IdentifyFaceResults results = FaceManager.getInstance().identifyFace(templates);
                                        if (results != null) {
                                            if (results.identifyFaceResultl != null) {
                                                //Log.d("DUPLICATE","Results-> "+results.identifyFaceResultl.length+", "+results.identifyFaceResultl[0].Id+", "+results.identifyFaceResultl[0].Score);
                                                if (results.identifyFaceResultl.length > 0) {
                                                    String id = (results.identifyFaceResultl[0].Id).trim();
                                                    //Log.d("EnrolUser","Total len->"+results.identifyFaceResultl.length+", User matched with->"+id+" len->"+id.length()+", New user id-> "+EnrollModel.getInstance().getEnrolId()+", len->"+EnrollModel.getInstance().getEnrolId().length());
                                                    if (!id.equalsIgnoreCase(EnrollModel.getInstance().getEnrolId())) {
                                                        if (results.identifyFaceResultl[0].Score >= 6.0) {
                                                            hideProgress();
                                                            showMessage("DUPLICATE ERROR", "Duplicate entry detected with id " + id, "error");
                                                            return;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    EnrollModel.getInstance().setImg(Utility.encodeImage(templates.get(0).Template));

                                    String confidance = decimalFormat.format(templates.get(0).Confidence);
                                    String glass = decimalFormat.format(templates.get(0).Glasses);
                                    String blur = decimalFormat.format(templates.get(0).Blur);
                                    String closedEye = decimalFormat.format(templates.get(0).ClosedEyes);
                                    String mask = decimalFormat.format(templates.get(0).Mask); // added in version 1011

                                    //Log.d("TAG", "face confidence " + confidance + " " + glass + " " + blur + " " + closedEye + " " + mask);
                                    boolean isQualityPass = false;
                                    if (CaraManager.getInstance().isQuality()) {
                                        if (Float.valueOf(confidance) > 0.8) {
                                            isQualityPass = true;
                                        }
                                    } else {
                                        isQualityPass = true;
                                    }

                                    if (isQualityPass) {

                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                retakebtn.setVisibility(View.GONE);
                                            }
                                        });

                                        //write image to disk
                                        Utilities.writeToFile(byteImg, EnrollModel.getInstance().getEnrolId() + "_enrol.dat");

                                        //done for testing
                                        //Utilities.writeToFile(FaceManager.getInstance().getOriCamImg(), EnrollModel.getInstance().getEnrolId() + "_enrol.dat");

                                        long t2 = System.currentTimeMillis();

                                        JSONObject finalData = new JSONObject();
                                        finalData.put("imgData", EnrollModel.getInstance().getBaseImg());
                                        finalData.put("empid", EnrollModel.getInstance().getEnrolId());
                                        finalData.put("imgq", confidance);
                                        finalData.put("macid", CaraManager.getInstance().getDEVICE_MACID());
                                        finalData.put("apikey", CaraManager.getInstance().getAPI_KEY());
                                        finalData.put("src", "cara");
                                        finalData.put("glass", glass);
                                        finalData.put("blur", blur);
                                        finalData.put("closedEye", closedEye);
                                        finalData.put("ismask", mask);
                                        finalData.put("ver", BuildConfig.VERSION_CODE);
                                        finalData.put("timereq", (t2 - t1) + "");
                                        //testing - remove after test
                                        //finalData.put("srcImg", Utility.encodeBase64(Utilities.scaleDown(byteImg, 150, 150, false)));
                                        finalData.put("srcImg", Utility.encodeBase64(FaceManager.getInstance().getOriCamImg()));   //send original image

                                        CaraManager.getInstance().enrollOnline(getApplicationContext(), finalData,
                                                new HelperCallback() {
                                                    @Override
                                                    public void onResult(String result) {
                                                        //Log.d("TAG", result);
                                                        try {
                                                            JSONObject res = new JSONObject(result);
                                                            if (res.getString("ErrorString").equalsIgnoreCase("success")) {

                                                                if (Listener.t5TemplateMatcher.Exists(EnrollModel.getInstance().getEnrolId())) {
                                                                    // Log.d("TAG", "User already exits and removed !");
                                                                    Listener.t5TemplateMatcher.RemoveFace(EnrollModel.getInstance().getEnrolId());
                                                                    if (FaceManager.getInstance().getLocalCacheManager() != null) {
                                                                        FaceManager.getInstance().getLocalCacheManager().removeUserById(EnrollModel.getInstance().getEnrolId());
                                                                    } else {
                                                                        FaceManager.getInstance().setLocalCacheManager(new LocalCacheManager(getApplicationContext()));
                                                                        FaceManager.getInstance().getLocalCacheManager().removeUserById(EnrollModel.getInstance().getEnrolId());
                                                                    }
                                                                }

                                                                worker.addTask(new OneShotProcessorTask(CameraActivity.this,
                                                                        Constants.TYPE_ENROLL, byteImg, null,
                                                                        EnrollModel.getInstance().getEnrolId(), FaceManager.getInstance().getFaceTheshold(), null));

                                                                //Logging the test data
                                                                Helper testHelper = new Helper();

                                                                Bitmap image = AppUtils.convertByteArrayToBitmap(byteImg);
                                                                JSONObject logValue = new JSONObject();
                                                                logValue.put("Action", "Adding Template to Tech5");
                                                                logValue.put("EmpId", EnrollModel.getInstance().getEnrolId());
                                                                logValue.put("ImageSize", image.getWidth() + " X " + image.getHeight());
                                                                logValue.put("TemplateSize", byteImg.length);
                                                                logValue.put("Confidence", confidance);
                                                                logValue.put("macid", CaraManager.getInstance().getDEVICE_MACID());
                                                                logValue.put("glass", glass);
                                                                logValue.put("blur", blur);
                                                                logValue.put("closedEye", closedEye);
                                                                logValue.put("ismask", mask);

                                                                testHelper.logData(logValue.toString(), "enrollFace method called", "Info");
                                                            } else {
                                                                hideProgress();
                                                                runOnUiThread(new Runnable() {
                                                                    @Override
                                                                    public void run() {
                                                                        try {
                                                                            Toast.makeText(getApplicationContext(), "Error in enrol process, refer " + res.getString("ErrorString"), Toast.LENGTH_SHORT).show();
                                                                            retakebtn.setVisibility(View.VISIBLE);
                                                                        }catch (Exception er){
                                                                            Toast.makeText(getApplicationContext(),"Unknown error",Toast.LENGTH_SHORT).show();
                                                                        }
                                                                    }
                                                                });
                                                            }
                                                        } catch (Exception er) {
                                                            Utility.printStack(er);
                                                        }
                                                    }

                                                    @Override
                                                    public void onError(String result) {
                                                        hideProgress();
                                                        showMessage("ERROR", result, "error");
                                                    }
                                                });
                                    } else {
                                        hideProgress();
                                        runOnUiThread(new Runnable() {
                                            @Override
                                            public void run() {
                                                imagequality.setVisibility(View.INVISIBLE);
                                                retakebtn.setVisibility(View.VISIBLE);
                                            }
                                        });
                                    }

                                } catch (Exception er) {
                                    hideProgress();
                                    showMessage("ERROR", "ERROR IN REQUEST " + er.getMessage(), "error");
                                    Utility.printStack(er);
                                }

                            } else {
                                hideProgress();
                                showMessage("ERROR", "ERROR IN TEMPLATE", "error");
                            }
                        }
                    }).start();

                } else {
                    hideProgress();
                    showMessage("ERROR", "REBOOT REQUIRED !", "error");
                }
                return;
            } else if (FaceManager.getInstance().getSdkType().equals(FaceManager.SDK_IDEMIA)) {

            } else if (FaceManager.getInstance().getSdkType().equals(FaceManager.SDK_CIS)) {

            }

        } catch (Exception er) {
            //er.printStackTrace();
            Utility.printStack(er);
            gotoHome(false);
        }

    }

    private synchronized void IdentifyMe(final String faceResult, final byte[] image) {
        FaceManager.getInstance().setDetectedId("");
        //Log.d("IdentifyMe", FaceManager.getInstance().getSdkType());

        //Utilities.writeToFile(image, System.currentTimeMillis() + "_enroll.jpg");
        if (FaceManager.getInstance().getSdkType().equals(FaceManager.SDK_TECH5)) {
            if (Listener.isSDKInitialized) {
                //Log.d("IdentifyMe", "started identify command");
                //IdentifyFaceResult[] result = FaceManager.getInstance().identifyFace(image);
                t1 = System.currentTimeMillis();
                //  Log.d("TAG", "Identify started " + t1);
                worker.addTask(new OneShotProcessorTask(CameraActivity.this,
                        Constants.TYPE_IDENTIFY, image, null,
                        null, FaceManager.getInstance().getFaceTheshold(), null));
            } else {
                showError("REBOOT REQUIRED !");
                CaraManager.getInstance().sendSignal("ACTION_SDK_FAILED", getApplicationContext());
                gotoHome(true);
            }
        } else if (FaceManager.getInstance().getSdkType().equals(FaceManager.SDK_IDEMIA)) {
            //cloudMatch(faceResult);
        } else if (FaceManager.getInstance().getSdkType().equals(FaceManager.SDK_CIS)) {
            //future implementation
        }
    }

    private void showScreenInfo() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (!FaceManager.getInstance().isEnroll()) {
                    if (progressBar != null)
                        CaraManager.getInstance().startProgess(progressBar);
                }

                infomsg.setVisibility(View.INVISIBLE);
                livenesstext.setVisibility(View.INVISIBLE);

                userid.setVisibility(View.VISIBLE);
                userid.setText(UserModel.getInstance().getUserId());
                Utility.FadeView(userid);

                inoutText.setVisibility(View.VISIBLE);
                if (UserModel.getInstance().getInOutStatus().equals("1")) {
                    inoutText.setText("IN");
                } else if (UserModel.getInstance().getInOutStatus().equals("0")) {
                    inoutText.setText("OUT");
                }

                if (UserModel.getInstance().getUserName() != null) {
                    flname.setVisibility(View.VISIBLE);
                    if (UserModel.getInstance().getUserName().length() >= 13) {
                        flname.setTextSize(40);
                    } else {
                        flname.setTextSize(46);
                    }
                    flname.setText(UserModel.getInstance().getUserName());
                    Utility.FadeView(flname);
                    Utility.animateObject(flname, -100f);
                }

                if (CaraManager.getInstance().isThermal()) {
                    //do not show negative values
                    if (UserModel.getInstance().getHeat("C") > 0) {

                        //if true values go ahead and show results
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {

                                resizeView(150, 140);
                                Utility.animateObject(findViewById(R.id.inoutProgress), 90f);

                                thermameter.setVisibility(View.VISIBLE);
                                CaraManager.getInstance().animateThermal(thermalProgress, true);

                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        temptext.setVisibility(View.VISIBLE);
                                        float Temp = UserModel.getInstance().getHeat(CaraManager.getInstance().getTempUnit());
                                        String showTemp = Temp + " " + UserModel.getInstance().getUnit(CaraManager.getInstance().getTempUnit());

                                    /*if (CaraManager.getInstance().getTempUnit().equals("F") && Temp>=104){
                                        showTemp= "HIGH";
                                    }else if (CaraManager.getInstance().getTempUnit().equals("F") && Temp<=94){
                                        showTemp= "LOW";
                                    }else if (CaraManager.getInstance().getTempUnit().equals("C") && Temp>=40){
                                        showTemp= "HIGH";
                                    }else if (CaraManager.getInstance().getTempUnit().equals("C") && Temp<34){
                                        showTemp= "LOW";
                                    }*/

                                        temptext.setText(showTemp);
                                    }
                                }, 400);

                            }
                        }, 500);
                    }
                }

                if (postrun != null)
                    handler.removeCallbacks(postrun);

                postrun = new Runnable() {
                    @Override
                    public void run() {
                        if (clearui != null) {
                            clearui.cancel();
                        }

                        if (progressBar != null) {
                            CaraManager.getInstance().stopProgess(progressBar);

                            if (CaraManager.getInstance().isThermal()) {
                                CaraManager.getInstance().animateThermal(thermalProgress, false);
                            }

                        }
                        // gotoHome();
                        initCamera();
                    }
                };
                handler.postDelayed(postrun, 1500);

            }
        });
    }

    private void showError(final String messsage) {
        Helper.getAuralFeedback(getApplicationContext(), Helper.getAuralLang(), Helper.AURAL_NOT_MATCHED);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {

                if (progressBar != null)
                    CaraManager.getInstance().stopProgess(progressBar);


                userid.setVisibility(View.VISIBLE);
                userid.setText("NO MATCH");
                userid.setTextColor(getResources().getColor(R.color.red));
                Utility.FadeView(userid);

                if (postrun != null)
                    handler.removeCallbacks(postrun);

                postrun = new Runnable() {
                    @Override
                    public void run() {
                        try {
                            Toast.makeText(getApplicationContext(), messsage, Toast.LENGTH_SHORT).show();
                            if (clearui != null) {
                                clearui.cancel();
                            }
                            // gotoHome();
                            initCamera();

                        } catch (Exception er) {
                            Utility.printStack(er);
                        }
                    }
                };
                handler.postDelayed(postrun, 600);
            }
        });
        cntScreenTap = 0;
    }


    private void ensureEnrol(String message,byte[] imgdata) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Dialog alertDialog = new Dialog(CameraActivity.this);
                    alertDialog.setCancelable(false);
                    alertDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
                    alertDialog.getWindow().setBackgroundDrawable(new ColorDrawable(android.graphics.Color.TRANSPARENT));
                    alertDialog.setContentView(R.layout.custom_img_dialog);

                    TextView yourmessage = alertDialog.findViewById(R.id.yourmessage);
                    Button allowBtn = alertDialog.findViewById(R.id.allowBtn);
                    Button denyBtn = alertDialog.findViewById(R.id.denyBtn);

                    yourmessage.setText(EnrollModel.getInstance().getEnrolId()+"\n"+EnrollModel.getInstance().getName()+"\n\n"+message);
                    ImageView faceImg=alertDialog.findViewById(R.id.faceImg);
                    faceImg.setImageBitmap(FaceManager.getInstance().getBitmapFromByte(imgdata));

                    allowBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            alertDialog.dismiss();
                            enrollFace(null, imgdata);//FaceManager.getInstance().getByteFromBitmap(Facebitmap));
                        }
                    });
                    denyBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            alertDialog.dismiss();
                            startActivity(new Intent(CameraActivity.this, EtamUserEnrol.class));
                            finish();
                        }
                    });
                    alertDialog.show();
                } catch (Exception er) {
                    Utility.printStack(er);
                }
            }
        });

    }

    private void showMessageDia(String message, String subtext, final String type) {

        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    final Dialog alertDialog = new Dialog(CameraActivity.this);
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
                        background.setBackgroundColor(getResources().getColor(R.color.red));
                    } else {
                        background.setBackgroundColor(getResources().getColor(R.color.green));
                    }

                    tv.setText(message);
                    yourmessage.setText(subtext);

                    denyBtn.setText(FaceManager.getInstance().isEnroll() ? "Enrol Next" : "Okay");

                    allowBtn.setText(FaceManager.getInstance().isEnroll() ? "Go Back" : "Close");

                    allowBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            alertDialog.dismiss();
                            //FaceManager.getInstance().setEnroll(false);
                            startActivity(new Intent(CameraActivity.this, MainActivity.class));
                            finish();
                        }
                    });
                    denyBtn.setOnClickListener(new View.OnClickListener() {
                        public void onClick(View v) {
                            alertDialog.dismiss();
                            if (FaceManager.getInstance().isEnroll()) {
                                startActivity(new Intent(CameraActivity.this, EtamUserEnrol.class));
                                finish();
                            }
                        }
                    });
                    alertDialog.show();
                } catch (Exception er) {
                    Utility.printStack(er);
                }
            }
        });

    }

    private void resizeView(int w, int h) {

        ViewGroup.LayoutParams layoutParams = progressBar.getLayoutParams();//new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);

        //layoutParams.addRule(RelativeLayout.ALIGN_TOP,R.id.bottomsheet);
        //layoutParams.addRule(RelativeLayout.ALIGN_RIGHT,R.id.bottomsheet);
        //layoutParams.topMargin = 30*1.5;
        //layoutParams.rightMargin = 30;
        layoutParams.width = w;
        layoutParams.height = h;
        progressBar.setLayoutParams(layoutParams);
        progressBar.postInvalidate();

        layoutParams = inoutText.getLayoutParams();
        //inoutText.setVisibility(View.VISIBLE);
        //layoutParams.addRule(RelativeLayout.CENTER_VERTICAL);
        //layoutParams.addRule(RelativeLayout.ALIGN_RIGHT, R.id.progressBar);
        //layoutParams.addRule(RelativeLayout.ALIGN_TOP, R.id.progressBar);
        layoutParams.width = w;
        layoutParams.height = h;
        inoutText.setTextSize(40);
        inoutText.setLayoutParams(layoutParams);
        inoutText.postInvalidate();

    }

    private void setTimeout() {
        if (clearui != null) {
            clearui.cancel();
        }
        //pd.show();
        clearui = new CountDownTimer(Helper.TIMEOUT_ACTIVITY * 1000, 1000) {
            @Override
            public void onTick(long millisUntilFinished) {
            }

            @Override
            public void onFinish() {
                pd.hide();

                gotoHome(false);
            }
        };
        clearui.start();
    }

    @Override
    public void onBackPressed() {
        //super.onBackPressed();

    }

    @Override
    public void onStop() {
        super.onStop();
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                if (Settings.canDrawOverlays(this)) {
                    Utility.removeBottomButtons();
                }
            }
        } catch (Exception er) {
        }


        if (FaceManager.getInstance().getFace() != null) {
            //if (!FaceManager.getInstance().getFace().isRecycled())
            // FaceManager.getInstance().getFace().recycle();
        }

        if (FaceManager.getInstance().getCameraCaptureImg() != null) {
            if (!FaceManager.getInstance().getCameraCaptureImg().isRecycled())
                FaceManager.getInstance().getCameraCaptureImg().recycle();
        }

        CaraManager.getInstance().sendSignal("ACTION_CAMERA_CLOSED", getApplicationContext());
    }

    private void updateSettings() {
        try {
            if (CaraManager.getInstance().getSharedPreferences() != null) {
                String settings = CaraManager.getInstance().getSharedPreferences().getString("settings", "");
                if (!settings.equals("")) {
                    JSONObject jdata = new JSONObject(settings);
                    CaraManager.getInstance().setInoutMode("-1");

                    if (jdata.getString(Helper.SETTING_INMODE) != null) {
                        if (jdata.getString(Helper.SETTING_INMODE).equalsIgnoreCase(Helper.TRUE)) {
                            CaraManager.getInstance().setInoutMode("1");
                        }
                    }

                    if (jdata.getString(Helper.SETTING_OUTMODE) != null) {
                        if (jdata.getString(Helper.SETTING_OUTMODE).equalsIgnoreCase(Helper.TRUE)) {
                            CaraManager.getInstance().setInoutMode("0");
                        }
                    }
                    Log.d("updateSettings", "mode selected " + CaraManager.getInstance().getInoutMode());
                }
            }
        } catch (Exception er) {
        }
    }


    @Override
    public void onResume() {
        super.onResume();
        isPaused = false;
        CaraManager.getInstance().fullScreen(getWindow());
        // Utility.disableBottomButtons(this);


        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
           // Utility.disableBottomButtons(this);
           // Utility.disablePullNotificationTouch(this);
        }


        CaraManager.getInstance().setUploadCount(0);
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(Intent.ACTION_TIME_TICK);
        intentFilter.addAction("SYNC_BEGIN");
        registerReceiver(receiver, intentFilter);

        updateDateTime();

    }

    @Override
    public void onPause() {
        super.onPause();
        isPaused = true;
        CaraManager.getInstance().setSimulateOnly(false);
        Utility.removeBottomButtons();
        if (pd != null) {
            pd.cancel();
        }
        if (clearui != null) {
            clearui.cancel();
        }
        if (faceTime != null) {
            faceTime.cancel();
        }
        if (faceEnrolTmr!=null){
            faceEnrolTmr.cancel();
        }
        captureHandler=null;
        Helper.stopAuralFb();
        UserModel.getInstance().clearAll();

        if (FaceManager.getInstance().getCamera() != null)
            FaceManager.getInstance().stopCameraPreview(FaceManager.getInstance().getCamera());


        if (!FaceManager.getInstance().isEnroll()) {
            if (FaceManager.getInstance().getFace() != null) {
                if (!FaceManager.getInstance().getFace().isRecycled()) {
                }
                // FaceManager.getInstance().getFace().recycle();
            }

            gotoHome(false);
        }

        try {
            if (receiver != null) {
                unregisterReceiver(receiver);
            }
            if (worker != null)
                worker.queue.clear();
        } catch (Exception er) {
        }

    }


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

}
