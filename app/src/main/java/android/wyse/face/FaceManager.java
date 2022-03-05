package android.wyse.face;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.Paint;
import android.hardware.Camera;
import android.hardware.camera2.CameraCharacteristics;
import android.os.Environment;
import android.os.Handler;
import android.util.Log;
import android.wyse.face.tech5.db.LocalCacheManager;
import android.wyse.face.tech5.utilities.IdentifyFaceResults;
import android.wyse.face.tech5.utilities.Listener;
import android.wyse.face.tech5.utilities.LogUtils;
import android.wyse.face.tech5.utilities.Utilities;

import androidx.palette.graphics.Palette;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Core;
import org.opencv.core.CvType;
import org.opencv.core.Mat;
import org.opencv.core.MatOfDouble;
import org.opencv.core.MatOfInt;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.ml.SVM;
import org.opencv.objdetect.CascadeClassifier;
import org.tensorflow.lite.DataType;
import org.tensorflow.lite.Interpreter;
import org.tensorflow.lite.support.image.ImageProcessor;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.image.ops.ResizeOp;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.text.DecimalFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import ai.tech5.sdk.abis.face.t5face.CreateFaceTemplateResult;
import ai.tech5.sdk.abis.face.t5face.IdentifyFaceResult;

/**
 *  FaceManager class
 *  written by Sonu Auti as utilty calls for face operations
 *  following operations are perfomed in the calls
 *  1. Identify
 *  2. Enroll
 *  3. Add/Remove
 *  4. Resize
 *  5. Save
 *  6. Liveness
 */
public class FaceManager {

    public static String SDK_TECH5 = "1";
    public static String SDK_IDEMIA = "2";
    public static String SDK_CIS = "3";
    public static boolean isLowlight;
    private static FaceManager faceManager;
    public final int INPUT_IMAGE_SIZE = 256;
    /**
     * WE HAVE ASSUMED THAT 0TH result is always max score
     * @param template
     * @return
     */

    DecimalFormat df = new DecimalFormat("###.#####");
    DecimalFormat df_1 = new DecimalFormat("###.##");
    int inputImgH=1024;
    int inputImgW=600;
    private boolean isEnroll = false;
    private int CameraId= CameraCharacteristics.LENS_FACING_FRONT;  //made zero for single camera devices, 1 for two camera devices
    private String sdkType = "1";
    private String detectedId = "-1";
    private boolean isFaces;
    private Bitmap CameraCaptureImg;
    private Bitmap Face;
    private Camera camera;
    private float faceTheshold = 6.0f;  //for matching
    //private float duplicateThr = 6.5f; //for de-duplication
    private float Blur,overExp,  smile,eye,eye_X=2,eye_Y=2,smile_X=2,smile_Y=2;
    private int frame_Count=0;
    private ArrayList<byte[]> frames=new ArrayList<>();
    private ArrayList<ArrayList<CreateFaceTemplateResult>> templates;
    private LocalCacheManager localCacheManager;
    private boolean lowLightPicture;
    private boolean isDarkModeActivated;
    private int frameavg=0;
    private ArrayList<Integer> rgbFrames=new ArrayList<>();
    private Paint mDebugPainter,mDebugAreaPainter;
    private CascadeClassifier cascadeClassifier;
    private float noFaceBlur = 14.0f;
    private float blurScore=0;
    //private Rect ForeGrect=new Rect(40,40,260,300);
    private Rect cropArea=new Rect(100,200,(int)(inputImgW+140)/2,(int)(inputImgH/2));
    private Interpreter faceObjectInterpreter;
    private DataType ImageDataTypeFace;
    private int faceImgSize = 224;
    private boolean checkSmile;
    private boolean isSmileDetected;
    private Interpreter interpreter;
    private DataType myImageDataType;
    private ImageProcessor imageProcessor;
    private TensorImage tImage;
    private Interpreter faceSpoofInterpreter;
    private DataType faceSpoofDataType;
    private ImageProcessor faceSpoofImgProc;
    private TensorImage faceSpoofTensorImg;
    private byte[] oriCamImg;
    private boolean isFaceDetectionStarted;
    private boolean captureComplete;
    private Runnable captureHandler;
    private FaceManager(){ }


    public static FaceManager getInstance(){
        if (faceManager==null){
            return faceManager = new FaceManager();
        }
        return faceManager;
    }

    public void setCaptureHandler(Runnable captureHandler) {
        this.captureHandler = captureHandler;
    }

    public Runnable getCaptureHandler() {
        return captureHandler;
    }

    public void setFaceDetectionStarted(boolean faceDetectionStarted) {
        isFaceDetectionStarted = faceDetectionStarted;
    }

    public boolean isFaceDetectionStarted() {
        return isFaceDetectionStarted;
    }

    public boolean isCaptureComplete() {
        return captureComplete;
    }

    public void setCaptureComplete(boolean captureComplete) {
        this.captureComplete = captureComplete;
    }

    public void setOriCamImg(byte[] oriCamImg) {
        this.oriCamImg = oriCamImg;
    }

    public byte[] getOriCamImg() {
        return oriCamImg;
    }

    public static void writeToFile(byte[] capturedBytes, String fileName) {
        try {
            // fileName = removeFileExtension(fileName);
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "T5Face";
            File root = new File(rootPath);
            if (!root.exists()) {
                root.mkdirs();
            }

            File f = new File(rootPath + File.separator + fileName);
            if (f.exists()) {
                f.delete();
            }
            f.createNewFile();
            FileOutputStream out = new FileOutputStream(f);
            out.write(capturedBytes);
            out.flush();
            out.close();
        } catch (Exception e) {
           Utility.printStack(e);
        }
    }

    public static float[][][] normalizeImage(Bitmap bitmap) {
        int h = bitmap.getHeight();
        int w = bitmap.getWidth();
        float[][][] floatValues = new float[h][w][3];

        float imageStd = 255;
        int[] pixels = new int[h * w];
        bitmap.getPixels(pixels, 0, bitmap.getWidth(), 0, 0, w, h);
        for (int i = 0; i < h; i++) {
            for (int j = 0; j < w; j++) {
                final int val = pixels[i * w + j];
                float r = ((val >> 16) & 0xFF) / imageStd;
                float g = ((val >> 8) & 0xFF) / imageStd;
                float b = (val & 0xFF) / imageStd;

                float[] arr = {r, g, b};
                floatValues[i][j] = arr;
            }
        }
        return floatValues;
    }

    public Camera getCamera() {
        return camera;
    }

    public void setCamera(Camera camera) {
        this.camera = camera;
    }

    public Bitmap getCameraCaptureImg() {
        return CameraCaptureImg;
    }

    public void setCameraCaptureImg(Bitmap cameraCaptureImg) {
        CameraCaptureImg = cameraCaptureImg;
    }

    public Bitmap getFace() {
        return Face;
    }

    public void setFace(Bitmap face) {
        Face = face;
    }

    public boolean isFaces() {
        return isFaces;
    }

    public void setFacesDetected(boolean faces) { isFaces = faces; }

    public String getDetectedId() {
        return detectedId;
    }

    public void setDetectedId(String detectedId) {
        this.detectedId = detectedId;
    }

    public int getCameraId() { return CameraId; }

    public void setCameraId(int cameraId) {
        CameraId = cameraId;
    }

    public String getSdkType() {
        return sdkType;
    }

    public void setSdkType(String sdkType) {
        this.sdkType = sdkType;
    }

    public boolean isEnroll() {
        return isEnroll;
    }

    public void setEnroll(boolean enroll) {
        isEnroll = enroll;
    }

    public float getFaceTheshold() { return faceTheshold; }

    public void setFaceTheshold(float faceTheshold) {
        this.faceTheshold = faceTheshold;
    }

    public float getDuplicateThr() {
        return getFaceTheshold();  //to make duplicate and face thr same
    }

    public byte[] getRoatedImage(byte[] byteArray,int rotation) {
        // byte[] byteArray=null;
        try {
            if (null != byteArray) {
               // int rotation = Utilities.getExifRotation(byteArray);
                //rotation=-90;
                //LogUtils.debug("TAG", "rotation " + rotation);
                if (rotation == 0) {
                    return byteArray;
                } else {

                    Bitmap rotatedImage2 = null;
                    if (getCameraId() == 0) {
                        rotatedImage2 = Utilities.rotateImage(360 - rotation, byteArray);
                    } else {
                        rotatedImage2 = Utilities.rotateImage(rotation, byteArray);
                    }

                    byteArray = Utilities.getBytesFromImage(rotatedImage2);

                }
                //System.err.println("capturedFaceBytes :: " + byteArray.length);
            } else {
                // System.err.println("Failed to capture image..Please try again :: ");
            }
        }catch (Exception er){
            Utility.printStack(er);
        }
        return byteArray;
    }

    public byte[] getRoatedImage(byte[] byteArray) {
        // byte[] byteArray=null;
        try {
            if (null != byteArray) {
                int rotation = Utilities.getExifRotation(byteArray);
                 rotation=-90;
                 //LogUtils.debug("TAG", "rotation " + rotation);
                if (rotation == 0) {
                    return byteArray;
                } else {

                    Bitmap rotatedImage2 = null;
                    if (getCameraId() == 0) {
                        rotatedImage2 = Utilities.rotateImage(360 - rotation, byteArray);
                    } else {
                        rotatedImage2 = Utilities.rotateImage(rotation, byteArray);
                    }

                    byteArray = Utilities.getBytesFromImage(rotatedImage2);

                }
                //System.err.println("capturedFaceBytes :: " + byteArray.length);
            } else {
               // System.err.println("Failed to capture image..Please try again :: ");
            }
        }catch (Exception er){
           Utility.printStack(er);
        }
        return byteArray;
    }

    public void stopCameraPreview(Camera camera){
        try {
            if (camera != null) {
                camera.stopPreview();
                camera.release();
            }
        } catch (Exception er) { }
    }

    public Bitmap getBitmapFromByte(byte[] bytes){
        return BitmapFactory.decodeByteArray(bytes, 0, bytes.length);
    }

    public byte[] getByteFromBitmap(Bitmap img,int quality){
        byte[] byteArray=null;
        try {
            ByteArrayOutputStream stream = new ByteArrayOutputStream();
            img.compress(Bitmap.CompressFormat.JPEG, quality, stream);
            byteArray = stream.toByteArray();
            stream.close();

            if (null != byteArray) {

                int rotation = Utilities.getExifRotation(byteArray);
                // rotation=0;

                LogUtils.debug("TAG", "rotation " + rotation);
                if (rotation == 0) {
                    return byteArray;
                } else {

                    Bitmap rotatedImage2 = null;
                    if (getCameraId() == 0) {
                        rotatedImage2 = Utilities.rotateImage(360 - rotation, byteArray);
                    } else {
                        rotatedImage2 = Utilities.rotateImage(rotation, byteArray);
                    }

                    byteArray = Utilities.getBytesFromImage(rotatedImage2);

                }
                System.err.println("capturedFaceBytes :: " + byteArray.length);
            } else {
                System.err.println("Failed to capture image..Please try again :: ");
            }
        }catch (Exception er){
            er.printStackTrace();
        }
        //img.recycle();
        //Log.d("imgsize",byteArray.length+"");
        return byteArray;
    }

    public String checkDuplicate(final byte[] template){

        try {
            if (template != null) {
                IdentifyFaceResults results = FaceManager.getInstance().identifyFaceTempForDuplicate(template);
                if (results != null) {
                    if (results.identifyFaceResultl != null) {
                        if (results.identifyFaceResultl.length > 0) {
                            String did = "";
                            boolean isDuplicate = false;
                            for (int i = 0; i < results.identifyFaceResultl.length; i++) {

                                if (!Float.isNaN(results.identifyFaceResultl[i].Score)) {
                                    if (results.identifyFaceResultl[i].Score >= getDuplicateThr()) {
                                        did = results.identifyFaceResultl[i].Id;
                                        //Log.d("Duplicate", "Duplicate found with -> " + did + " -> " + results.identifyFaceResultl[i].Score);
                                        //Log.d("Duplicate", did + " -> " + results.identifyFaceResultl[i].Score);
                                        isDuplicate = true;
                                    } else {
                                        //Log.d("Duplicate", did + " -> " + results.identifyFaceResultl[i].Score);
                                    }
                                }
                            }
                            if (isDuplicate) {
                                //Log.d("Duplicate","Found duplicate -> "+did);
                                return did;
                            }
                        }
                    }
                }
            }
        }catch (Exception er){};
        //Log.d("Duplicate","success");
        return "success";
    }

   public ArrayList<CreateFaceTemplateResult> getFaceTemp(byte[] image){
       ArrayList<CreateFaceTemplateResult> createFaceTemplateResults = Listener.t5TemplateCreator.CreateFaceTemplate(image);
       return createFaceTemplateResults;
   }

    public ArrayList<ArrayList<CreateFaceTemplateResult>> getFaceTemp(ArrayList<byte[]> image){
        ArrayList<ArrayList<CreateFaceTemplateResult>> createFaceTemplateResults = Listener.t5TemplateCreator.CreateFaceTemplate(image);
        return createFaceTemplateResults;
    }

    public IdentifyFaceResults identifyFaceTemplate(byte[] template){
        IdentifyFaceResult[] results = Listener.t5TemplateMatcher.IdenfityFace(template, getFaceTheshold(), 10);
        //Log.d("TAG", "mask value ->" + isMask + "");
        return new IdentifyFaceResults(results, 0, 0, 0, 0);
    }

    /**
     *
     * @param template
     * @return
     */
    public IdentifyFaceResults identifyFaceTempForDuplicate(byte[] template){
        IdentifyFaceResult[] results = Listener.t5TemplateMatcher.IdenfityFace(template, getDuplicateThr(), 10);
        //Log.d("TAG", "mask value ->" + isMask + "");
        return new IdentifyFaceResults(results, 0, 0, 0, 0);
    }

    public IdentifyFaceResults identifyFace(ArrayList<CreateFaceTemplateResult> createFaceTemplateResults) throws Exception {
        try {
            //LogUtils.debug("TAG", "Listener.t5TemplateCreator.CreateFaceTemplate from oneShot");
            if (createFaceTemplateResults!=null) {

                if (createFaceTemplateResults.size()>0) {
                    float isMask = createFaceTemplateResults.get(0).Mask;
                    float smile = createFaceTemplateResults.get(0).Smile;
                    float glasses = createFaceTemplateResults.get(0).Glasses;
                    float gender = createFaceTemplateResults.get(0).Gender;

                    //LogUtils.debug("TAG", "after Listener.t5TemplateCreator.CreateFaceTemplate");
                    if (createFaceTemplateResults != null) {
                        if (createFaceTemplateResults.size() > 0) {
                            //Utilities.writeTemplateToFile(createFaceTemplateResults.get(0).Template, "");
                            IdentifyFaceResult[] results = Listener.t5TemplateMatcher.IdenfityFace(createFaceTemplateResults.get(0).Template, faceTheshold, 10);
                            //Log.d("TAG", "mask value ->" + isMask + "");
                            return new IdentifyFaceResults(results, isMask, smile, glasses, gender);
                        }
                    } else {
                        //throw new Exception("Unable to create Face template");
                        return new IdentifyFaceResults(null, 0, 0, 0, 0);
                    }
                }
            }
        } catch (Exception e) {
            Utility.printStack(e);
        }

        return new IdentifyFaceResults(null, 0, 0, 0, 0);
    }

    public boolean processFrames() {

        if (frames.size() == 3){
            templates = FaceManager.getInstance().getFaceTemp(frames);
            frames.clear();
        }else {
            frames.add(Utilities.scaleDown(
                    FaceManager.getInstance().getByteFromBitmap(
                            FaceManager.getInstance().getCameraCaptureImg(),100),
                    400,400,false));

            if (frames.size()<3){
                return false;
            }
        }

        /*ArrayList<CreateFaceTemplateResult> templates = FaceManager.getInstance().getFaceTemp(Utilities.scaleDown(
                FaceManager.getInstance().getByteFromBitmap(
                        FaceManager.getInstance().getCameraCaptureImg(),100),
                500,500,false)); */

        if (templates != null) {

            if (templates.size()<=0) {
                return false;
            }

            for (int f=0;f<templates.size();f++) {

                Log.d("SKM", "=========== BEGIN PROCESS ==========");
                Log.d("SKM", "Frame Count -> " + frame_Count);

                try {
                    //float glass = templates.get(0).Glass;
                    float smilely = templates.get(f).get(0).Smile;
                    float eyesValues = templates.get(f).get(0).ClosedEyes;
                    float mask = templates.get(f).get(0).Mask;

                    //msk = Math.round(mask);
                    //smile = Math.round(smilely);
                    smile = Float.valueOf(df_1.format(smilely));
                    eye = Float.valueOf(df_1.format(eyesValues));
                    //glass = (int) Math.round(glass);


                    float oveEx = templates.get(f).get(0).Overexposure;
                    Blur = templates.get(f).get(0).Blur;
                    overExp = Math.round(templates.get(f).get(0).Overexposure);
                    // int valueChck = Math.abs((128-ove)/255);
                    float valueChck = Math.abs((128 - overExp) / 128);
                    float overExValue = Float.valueOf(df_1.format(valueChck));
                    Blur = Float.valueOf(df_1.format(Blur));

                    //txt_overEX.setText("Overx= " + overExValue);
                    //txt_blur.setText("Blr= " + Blur);
                    Log.d("SKM", "Overx ->" + overExValue + ", Blur->" + Blur);

                    /*Log.d("CheckSmile", "frame_Count = " + frame_Count);
                    Log.d("CheckSmile", "OVErexplore = " + oveEx);
                    Log.d("CheckSmile", "OVErexplore = " + overExp);
                    Log.d("CheckSmile", "OVErexplore = " + valueChck);
                    Log.d("CheckSmile", "OVErexplore = " + overExValue);
                    Log.d("CheckSmile", "BLUR = " + Blur);*/

                    if (Blur > 0.80) {

                        smile_X = 2;
                        smile_Y = 2;
                        eye_X = 2;
                        eye_Y = 2;
                        frame_Count = 0;

                        //txt_blur.setText("Blr= " + Blur);
                        //xt_Fake_img.setText("!! ## Fake FACE. ## !!");
                        Log.d("SKM", "Fake Face " + Blur);

                    /*long blurval = System.currentTimeMillis();
                    SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("hh:mm:ss");
                    Date blurvalue = new Date(blurval);
                    String blurvaluess = simpleDateFormat2.format(blurvalue);
                    Log.d("TemplatedCount", "blurvaluess=blur-->" + blurvaluess);
                    Log.d("Testing", "FRAMECOUNT==BLURRR-->"); */

                        return false;
                    } else {

                        if (frame_Count == 0) {
                            //txt_Fake_img.setText("Checking for FAKE FACE. Smile Or Blink your eyes." + frame_Count);
                            Log.d("SKM", "Checking for fake face " + frame_Count);

                            smile_X = 2;
                            smile_Y = 2;
                            eye_X = 2;
                            eye_Y = 2;
                            long frame = System.currentTimeMillis();
                            SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("hh:mm:ss");
                            Date framecoun = new Date(frame);
                            String frame00 = simpleDateFormat2.format(framecoun);
                            Log.d("TemplatedCount", "frame00=count-->" + frame00);

                        }

                        if (frame_Count == 1) {
                            //All odd face detect counts

                            //txt_Fake_img.setText("Checking for FAKE FACE. Smile Or Blink your eyes." + frame_Count);
                            Log.d("SKM", "checking smile");
                            smile_X = smile;
                            eye_X = eye;

                            //txt_smile_x.setText("Smile_X = " + smile_X);
                            //txt_eye_x.setText("Eye_X = " + eye_X);

                            Log.d("SKM", "smile_X ->" + smile_X + ", eye_X ->" + eye_X);

                            //long frame1 = System.currentTimeMillis();
                            //SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("hh:mm:ss");
                            //Date framecoun1 = new Date(frame1);
                            //String framec1 = simpleDateFormat2.format(framecoun1);
                            //Log.d("TemplatedCount", "frame11=count-->" + framec1);

                        /* txt_smile_x.setText("Smile_X = "+smile_X);
                        txt_eye_x.setText("Eye_X = "+eye_X);*/

                        }
                        /// Log.d("CheckSmile", "Frame Count = " + frame_Count);
                        if (frame_Count == 2) { //All even face detect count

                            //txt_Fake_img.setText("Checking for FAKE FACE. Smile Or Blink your eyes." + frame_Count);
                            smile_Y = smile;
                            eye_Y = eye;
                            ///  Log.d("CheckSmile", "smile_Y = " + smile_Y);
                            frame_Count = -1;

                            //txt_smile_y.setText("Smile_Y = " + smile_Y);
                            //txt_eye_y.setText("Eye_Y = " + eye_Y);
                            // Log.d("SKM","Original Smile ->"+smile+", "+"Ori Eye ->"+eye_ori);
                            Log.d("SKM", "smile_X ->" + smile_X + ", eye_X ->" + eye_X);
                            Log.d("SKM", "Smile_Y ->" + smile_Y + ", EYE_Y ->" + eye_Y);

                            //long frame2 = System.currentTimeMillis();
                            //SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("hh:mm:ss");
                            //Date framecoun2 = new Date(frame2);
                            //String framec2 = simpleDateFormat2.format(framecoun2);
                            //Log.d("TemplatedCount", "frame22=count-->" + framec2);
                                          /*txt_smile_y.setText("Smile_Y = "+smile_Y);
                                           txt_eye_y.setText("Eye_Y = "+eye_Y);*/
                        }

                        //txt_smile_y.setText("Smile_Y = " + smile_Y);
                        //txt_eye_y.setText("Eye_Y = " + eye_Y);

                        //txt_smile_x.setText("Smile_X = " + smile_X);
                        //txt_eye_x.setText("Eye_X = " + eye_X);
                        frame_Count++;

                        if (((smile_X != smile_Y) || (eye_X != eye_Y)) && (smile_Y != 2) && (smile_X != 2) && (eye_X != 2) && (eye_Y != 2)) {

                            long frameLi = System.currentTimeMillis();
                            SimpleDateFormat simpleDateFormat2 = new SimpleDateFormat("hh:mm:ss");
                            Date frameLive = new Date(frameLi);
                            String frameLives = simpleDateFormat2.format(frameLive);
                            Log.d("SKM", "frameLives=count-->" + frameLives);

                        /*txt_smile_x.setText("Smile_X = " + smile_X);
                        txt_eye_x.setText("Eye_X = " + eye_X);

                        txt_smile_y.setText("Smile_Y = " + smile_Y);
                        txt_eye_y.setText("Eye_Y = " + eye_Y);

                        txt_overEX.setText("Overx= " + overExValue);
                        txt_blur.setText("Blr= " + Blur);
                        txt_Fake_img.setText("LIVE FACE"); */
                            // Log.d("CheckSmile", "capturePhoto called for indentification. ");
                            //capturePhoto(); // <--Identify is called here.
                            //Log.d("SKM","call capture photo");

                            smile_X = 2;
                            smile_Y = 2;
                            eye_X = 2;
                            eye_Y = 2;
                            return true;

                        } else {
                            if (Blur < 0.80) {
                                Log.d("SKM", "Validating for live face");
                            }
                            //txt_Fake_img.setText("Validating for Live FACE.\n Smile Or Blink your eyes." + frame_Count);
                            //CaraManager.getInstance().getTimerTone().play();
                        }
                    }

                } catch (Exception e) {
                    Utility.printStack(e);
                }
            }

        }

        Log.d("SKM", "Logic failed ");
        return false;
    }

    public LocalCacheManager getLocalCacheManager() {
        return localCacheManager;
    }

    public void setLocalCacheManager(LocalCacheManager localCacheManager) {
        this.localCacheManager = localCacheManager;
    }

    public boolean removeUserById(String id){
       try {
           if (Listener.isSDKInitialized) {
               if (Listener.t5TemplateMatcher.Exists(id)) {
                   Listener.t5TemplateMatcher.RemoveFace(id);
                   getLocalCacheManager().removeUserById(id);

                   Log.d("TAG", id + " removed successfully");
                   return true;
               } else {
                   Log.d("TAG", id + " not exits");
                   return false;
               }
           } else {
               Log.d("TAG", "sdk not init");
               return false;
           }
       }catch (Exception er){
           Utility.printStack(er);
          // Log.d("TAG","error in delete");
           return false;
       }
   }

   public boolean enrollUserFromTemplate(Context context,String id,byte[] template){
       try {
           if (Listener.isSDKInitialized) {
               if (!Listener.t5TemplateMatcher.Exists(id)) {
                   Listener.t5TemplateMatcher.InsertFace(id, template);
                   getLocalCacheManager().addFaceRecordToDb(id, template, template);
                   Log.d("TAG", id + " enrolled successfully");
               } else {
                   Log.d("TAG", id + " already exits");
               }
               return true;
           } else {
               return false;
           }
       }catch (Exception er){
           return false;
       }
   }

    public void isDarkCondition(Bitmap bitmap){
        FaceManager.getInstance().getImageColors(bitmap, new Palette.PaletteAsyncListener() {
            public void onGenerated(Palette p) {
                // Use generated instance
                lowLightPicture=isDark(p.getDominantColor(Color.WHITE),70,20,false);
            }
        });
    }

    public void toggleFlash(Camera.Parameters parm, boolean isOn){
        try {
            if (parm!=null && isOn) {
                parm.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
            }else if (parm!=null && !isOn){
                parm.setFlashMode(Camera.Parameters.FLASH_MODE_OFF);
            }
        } catch (Exception e) {
            Utility.printStack(e);
        }
    }

    public void getImageColors(Bitmap bitmap, Palette.PaletteAsyncListener listener) {
        Palette.from(bitmap).generate(listener);
    }

    public boolean isDarkModeActivated() {
        return isDarkModeActivated;
    }

    public void setDarkModeActivated(boolean darkModeActivated) {
        isDarkModeActivated = darkModeActivated;
    }

    public boolean isDark(int color,int lowLightThr,int frameCount,boolean setGlobal){
        int R = (color >> 16) & 0xff;
        int G = (color >>  8) & 0xff;
        int B = (color) & 0xff;
        //Log.d("isDark",R+","+G+","+B);
        int rgb_avg=((R+G+B)/3);
        rgbFrames.add(rgb_avg);
        if (rgbFrames.size()>frameCount){
            frameavg=0;
            for (int value:rgbFrames) {
                frameavg=frameavg+value;
            }
            frameavg=frameavg/rgbFrames.size();
            rgbFrames.clear();
        }
       // Log.d("liveness","light -> "+frameavg);
        if (setGlobal) {
            isLowlight = frameavg <= lowLightThr;
            return isLowlight;
        }else {
            return frameavg <= lowLightThr && frameavg>50;
        }
    }

    private void initPaints() {
        mDebugPainter = new Paint();
        mDebugPainter.setColor(Color.RED);
        mDebugPainter.setAlpha(80);

        mDebugAreaPainter = new Paint();
        mDebugAreaPainter.setColor(Color.GREEN);
        mDebugAreaPainter.setAlpha(80);
    }

    public Paint getmDebugAreaPainter() {
        if (mDebugAreaPainter!=null){
            initPaints();
        }
        return mDebugAreaPainter;
    }

    public Paint getmDebugPainter() {
        return mDebugPainter;
    }

    public Bitmap getFaceOnly(Bitmap facebitmap) {
        FaceCropper mFaceCropper = new FaceCropper();
        mFaceCropper.setMaxFaces(1);
        mFaceCropper.setFaceMinSize(256);
        //mFaceCropper.setDebug(true);
        return mFaceCropper.getCroppedImage(facebitmap);
    }

    public CascadeClassifier getCascadeClassifier() {
        return cascadeClassifier;
    }

    public void setCascadeClassifier(CascadeClassifier cascadeClassifier) {
        this.cascadeClassifier = cascadeClassifier;
    }

    public FaceDetectionResult detectLiveFaces(){
        if (getCascadeClassifier()==null) {
            CascadeClassifier cascadeClassifier = new CascadeClassifier();
            cascadeClassifier.load(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + ".caratemp" + File.separator + "face_cascade.xml");
            setCascadeClassifier(cascadeClassifier);
        }

        Mat bmptemp=new Mat();
        Utils.bitmapToMat(getCameraCaptureImg(), bmptemp);

        Mat graymat=new Mat();
        Imgproc.cvtColor(bmptemp,graymat,Imgproc.COLOR_RGB2GRAY);

        MatOfRect faces=new MatOfRect();
        MatOfInt rejectLevels=new MatOfInt();
        MatOfDouble levelWt=new MatOfDouble();
        //getCascadeClassifier().detectMultiScale(graymat,faces,1.1,5,5,new Size(200,200),new Size());
        //detectMultiScale3(Mat image, MatOfRect objects, MatOfInt rejectLevels, MatOfDouble levelWeights, double scaleFactor, int minNeighbors, int flags, Size minSize, Size maxSize, boolean outputRejectLevels)
        getCascadeClassifier().detectMultiScale3(graymat,faces,rejectLevels,levelWt,1.1,5, 2,new Size(150,150),new Size(),true);

        if (faces!=null && faces.toArray().length>0) {
            Rect rect = faces.toArray()[0];
            double faceDiagonal = Math.sqrt(Math.pow(rect.width, 2) + Math.pow(rect.height, 2));
            //Log.d("FaceArea",faceDiagonal+", weight "+levelWt.toArray()[0]+","+levelWt.toArray().length+", reject levels "+rejectLevels.toArray()[0]);
            if (levelWt!=null && levelWt.toArray()[0]>=2.0) {
                return new FaceDetectionResult(true, "", faceDiagonal);
            }else{
                return new FaceDetectionResult(false, "", faceDiagonal);
            }
        }else{
            Log.d("FaceArea","No face detected");
        }
        return new FaceDetectionResult(false,"",0);
    }

    public Bitmap detectFaces(){

        if (getCascadeClassifier()==null) {
            CascadeClassifier cascadeClassifier = new CascadeClassifier();
            cascadeClassifier.load(Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "faces" + File.separator + "face_cascade.xml");
            setCascadeClassifier(cascadeClassifier);
        }
        Mat bmptemp=new Mat();
        Utils.bitmapToMat(getCameraCaptureImg(), bmptemp);

        Mat graymat=new Mat();
        Imgproc.cvtColor(bmptemp,graymat,Imgproc.COLOR_RGB2GRAY);

        MatOfRect faces=new MatOfRect();
        getCascadeClassifier().detectMultiScale(graymat,faces,1.1,5,5,new Size(180,180),new Size());

        if (faces!=null && faces.toArray().length>0) {
            Mat face_roi=new Mat();
            Mat mask = new Mat();
            Rect rect = faces.toArray()[0];
            Imgproc.rectangle(mask,rect.tl(),rect.br(),new Scalar(255,255,255));
            bmptemp.copyTo(face_roi,mask);
            Bitmap bitmap=Bitmap.createBitmap(face_roi.width(),face_roi.height(),Bitmap.Config.RGB_565);
            Utils.matToBitmap(face_roi,bitmap);
            return bitmap;
        }
        return null;
    }

    public float getNoFaceBlur() {
        return noFaceBlur;
    }

    public float getBlurScore() {
        return blurScore;
    }

    public void setBlurScore(float blurScore) {
        this.blurScore = blurScore;
    }

    /*private int last_eye_value;
    public boolean throwChallenge(){

         ArrayList<CreateFaceTemplateResult> results=FaceManager.getInstance().getFaceTemp(getByteFromBitmap(
                 Utility.getScaledDownBitmap(getCameraCaptureImg(),400,true),70));

        if (results.size()>0){
            int closedEyes = Math.round(results.get(0).ClosedEyes);
            int exposure = Math.round(results.get(0).Overexposure);

            Log.d("SVM","closedEyes value -> "+closedEyes+", expo->"+exposure);
             if(closedEyes!=last_eye_value){
                 Log.d("SVM","closedEyes detected ->"+closedEyes);
                 FaceManager.getInstance().setSmileDetected(true);
             }else {
                 FaceManager.getInstance().setSmileDetected(false);
                 last_eye_value=closedEyes;
             }
        }
       return isSmileDetected();
    }*/

    public FaceResults faceSpoofDetector(String id){

        //long t1=System.currentTimeMillis();
        Mat bmptemp=new Mat();
       // Mat hsv=new Mat();

        //Utilities.writeToFile(Utilities.getBytesFromImage(getCameraCaptureImg()),  System.currentTimeMillis()+"_train.jpg");
        if (getCameraCaptureImg()!=null) {
            Utils.bitmapToMat(getCameraCaptureImg(), bmptemp);
            int oriHeight=getCameraCaptureImg().getHeight();
            int oriWeight=getCameraCaptureImg().getWidth();
            bmptemp=bmptemp.submat(cropArea);
        }else {
            return new FaceResults(0.0f,0+"",false);
        }

        //adaptive histogram equlization
        /*Mat labimg=new Mat();
        Imgproc.cvtColor(bmptemp,labimg,Imgproc.COLOR_RGB2Lab);
        List<Mat> l_a_b=new ArrayList<>();
        Core.split(labimg,l_a_b);
        CLAHE clahe= Imgproc.createCLAHE(3.0,new Size(8,8));
        Mat cl=new Mat();
        clahe.apply(l_a_b.get(0),cl);
        Mat c_lab=new Mat();
        List<Mat> merg=new ArrayList<>();
        merg.add(cl);
        merg.add(l_a_b.get(1));
        merg.add(l_a_b.get(2));
        Core.merge(merg,c_lab);

        Imgproc.cvtColor(c_lab,bmptemp,Imgproc.COLOR_Lab2RGB); */
        //adaptive histogram equlization ends here

        /*List<Mat> rgb=new ArrayList<>();
        Core.split(bmptemp,rgb);

        //Log.d("TAG","size of mat "+rgb.size());
        Mat rg=new Mat();
        Core.subtract(rgb.get(0),rgb.get(1),rg);

        Mat r_g=new Mat();
        Core.add(rgb.get(0),rgb.get(1),r_g);
        Core.multiply(r_g,new Scalar(0.5),r_g);

        Mat yb=new Mat();
        Core.subtract(r_g,rgb.get(2),yb);

        MatOfDouble colorMean=new MatOfDouble();
        MatOfDouble colorDev=new MatOfDouble();

        Core.meanStdDev(rg,colorMean,colorDev);

        double rgMean = colorMean.get(0,0)[0];
        double rgStd = colorDev.get(0,0)[0];

        Core.meanStdDev(yb,colorMean,colorDev);

        double ybMean = colorMean.get(0,0)[0];
        double ybStd = colorDev.get(0,0)[0];

        double stdv = Math.sqrt((Math.pow(rgStd,2) + Math.pow(ybStd,2)));
        double mean = Math.sqrt((Math.pow(rgMean,2) + Math.pow(ybMean,2)));

        double colorfullness = stdv + (0.3 * mean);

        Imgproc.cvtColor(bmptemp,hsv,Imgproc.COLOR_RGB2HSV,3);
        MatOfDouble hsvmean=new MatOfDouble();
        MatOfDouble hsvstd=new MatOfDouble();

        Core.split(hsv,rgb);
        Core.meanStdDev(rgb.get(0),hsvmean,hsvstd);
        double hmean=hsvmean.get(0,0)[0];
        double hstd=hsvstd.get(0,0)[0];

        Core.meanStdDev(rgb.get(1),hsvmean,hsvstd);
        double smean=hsvmean.get(0,0)[0];
        double sstd=hsvstd.get(0,0)[0];

        Core.meanStdDev(rgb.get(2),hsvmean,hsvstd);
        double vmean=hsvmean.get(0,0)[0];
        double vstd=hsvstd.get(0,0)[0]; */


        //scale image
        //Mat newImg=new Mat();
        //Imgproc.resize(bmptemp,newImg,new Size(300,500));

        //convert to gray scale
        Mat graymat=new Mat();
        Imgproc.cvtColor(bmptemp,graymat,Imgproc.COLOR_RGB2GRAY);


        //calculate the depth of image
        /*Mat disparity = new Mat();
        MatOfDouble stdDisparity = new MatOfDouble();
        Mat rightImg=new Mat();
        if (steroImages.size()==2) {
            StereoBM stereoBM = StereoBM.create(32, 15);
            Core.flip(graymat,rightImg,1);
            stereoBM.compute(graymat, rightImg, disparity);
            Core.meanStdDev(disparity, new MatOfDouble(), stdDisparity);
            steroImages.clear();
            double disparity_std = stdDisparity.get(0,0)[0];
            Log.d("disparity",disparity_std+"");
        }else {
            steroImages.add(graymat);
        }*/

        //image smoothing
        /*Mat blurMat=new Mat();
        Imgproc.GaussianBlur(bmptemp,blurMat,new Size(5,5),0);
        MatOfDouble stdDevBlur=new MatOfDouble();
        Core.meanStdDev(blurMat,new MatOfDouble(),stdDevBlur);
        Log.d("STD","Imblur -> "+stdDevBlur.rows()+", "+stdDevBlur.get(0,0)[0]);//+", "+stdDevBlur.get(1,0)[0]+", "+stdDevBlur.get(2,0)[0]);*/

        //find high freq
        Mat lapmat=new Mat();
        Imgproc.Laplacian(graymat,lapmat,3);
        MatOfDouble stdblur=new MatOfDouble();
        Core.meanStdDev(lapmat,new MatOfDouble(),stdblur);

        /**
         * Count edges and calculate the standard deviation
         */
        Mat edges=new Mat();
        Imgproc.Canny(graymat,edges,100,200);

        MatOfDouble stdEdge=new MatOfDouble();
        MatOfDouble stdMean=new MatOfDouble();
        Core.meanStdDev(edges,stdMean,stdEdge);
        double edgeDev=stdEdge.get(0,0)[0];

        //Log.d("STD","edge -> "+stdEdge.get(0,0)[0]);

        //double val= Double.valueOf(new DecimalFormat("0.00").format(Math.pow(std.get(0,0)[0],2.0)));

        //calculate lbp
        //Mat lbp=new Mat();
        //MatOfDouble stdlbp=new MatOfDouble();
        //LBP(graymat,lbp);
        //Core.meanStdDev(lbp,stdlbp,new MatOfDouble());
        //double variance_lbp = stdlbp.get(0,0)[0];

        double blur = (stdblur.get(0,0)[0]);

        //String sample = id+","+df.format(edgeDev)+","+df.format(blur); //+","+stdv+","+mean+","+colorfullness+","+hstd+","+hmean+","+sstd+","+smean+","+vstd+","+vmean+"\n";

        //double[] testdata={blur,stdv,mean,colorfullness,hstd,hmean,sstd,smean,vstd,vmean};
        //this is for standad deviation of fake and true image over edge detection

        /*if (edgeDev<=30) {
            return false;
        } else {
            return true;
        }*/

        //Utility.writeToFile(sample,"train.txt",true);

        double[] testdata = {edgeDev,blur};
        FaceResults results = classifyInput("",testdata,id);

        //String result_test=result+","+id+"\n";
        //String r = result ? "1":"0";
        //String line=r+","+df.format(edgeDev)+","+df.format(blur)+"\n";
        //Utility.writeToFile(line,"results.txt",true);

        float isMask=0;

        //long t2=System.currentTimeMillis();
        //Log.d("SVM","time required -> "+(t2-t1));
        String blurvalue=df_1.format(blur);

        //Log.d("liveness","svm result -> "+sample+","+results.isReal);
       // Log.d("SVM","Result of deep-> "+classifyWithDeep(bmptemp));

        Bitmap facebitmap = Bitmap.createBitmap(bmptemp.width(), bmptemp.height(), Bitmap.Config.ARGB_8888);
        Utils.matToBitmap(bmptemp, facebitmap);

        double ssa=identifyWithDeep(facebitmap);
        //double mtt= antiSpoofing(facebitmap); //detectFaceObject(facebitmap,true);
        Log.d("liveness","svm->"+results.isReal+",  SSA->"+ssa);

        if (results.isReal && ssa>CaraManager.getInstance().getLiveness_thr()) {

            if (blur<getNoFaceBlur() || edgeDev< 30){
               // Log.d("SVM","edge and blur is too low -> "+blur+". "+edgeDev);
                if (blur<7) {
                    //check for false negetive by throwing challenge
                    //boolean iseye = throwChallenge();
                    //Log.d("SVM","Eye closed -> "+iseye);
                    //return new FaceResults(isMask,iseye);
                    return new FaceResults(isMask,blurvalue,false);
                }
                //return new FaceResults(isMask,false);
            }

            //results.isReal = identifyWithDeep(getCameraCaptureImg());
            return new FaceResults(isMask,blurvalue,true);
        } else {
            /**
             * This is correction for dark conditions
             */
            //isDarkCondition(getCameraCaptureImg());
            //result = lowLightPicture && identifyWithDeep(getCameraCaptureImg());
            //lowLightPicture=false;
            //results.isReal = identifyWithDeep(getCameraCaptureImg());

            return new FaceResults(isMask,blurvalue,false);
        }
        //return new FaceResults(isMask,blurvalue+"(end),"+results.isReal,results.isReal);
    }

    public Interpreter getFaceObjectInterpreter() {
        return faceObjectInterpreter;
    }

    public void setFaceObjectInterpreter(Interpreter faceObjectInterpreter) {
        this.faceObjectInterpreter = faceObjectInterpreter;
    }

    public float detectFaceObject(Bitmap bitmap,boolean isWrite){
        try{
            long t1=System.currentTimeMillis();
            if (getFaceObjectInterpreter()==null) {
                String output = Environment.getExternalStorageDirectory()+File.separator+".caratemp/face_obj.bin";
                setFaceObjectInterpreter(new Interpreter(new File(output)));
                ImageDataTypeFace = getFaceObjectInterpreter().getInputTensor(0).dataType();
            }

            // Analysis code for every frame
            ImageProcessor imageProcessorFace;
            imageProcessorFace = new ImageProcessor.Builder()
                    .add(new ResizeOp(faceImgSize, faceImgSize, ResizeOp.ResizeMethod.BILINEAR))
                    .build();
            // Preprocess the image
            TensorImage tImageFace = new TensorImage(ImageDataTypeFace);
            tImageFace.load(bitmap);

            tImageFace = imageProcessorFace.process(tImageFace);
            TensorBuffer probabilityBuffer = TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
            // run inference
            getFaceObjectInterpreter().run(tImageFace.getBuffer(), probabilityBuffer.getBuffer());

            float confidence = (probabilityBuffer.getFloatValue(0));
            long t2=System.currentTimeMillis();

            if (confidence<0.01)
                confidence=0;

            if (isWrite)
                writeToFile(getByteFromBitmap(bitmap,95),"face_"+System.currentTimeMillis()+"_"+confidence+"_face.jpg");

            Log.d("TENSOR","Result face obj ->"+ confidence+", timereq -> "+(t2-t1));

            return confidence;

        }catch (Exception er){
            Utility.printStack(er);
        }
        return 0;
    }

    public boolean isSmileDetected() {
        return isSmileDetected;
    }

    public void setSmileDetected(boolean smileDetected) {
        isSmileDetected = smileDetected;
    }

    public boolean isCheckSmile() {
        return checkSmile;
    }

    public void setCheckSmile(boolean checkSmile) {
        this.checkSmile = checkSmile;
    }

    public Interpreter getInterpreter() {
        return interpreter;
    }

    public void setInterpreter(Interpreter interpreter) {
        this.interpreter = interpreter;
    }

    private double identifyWithDeep(Bitmap bitmap){
        try {
            long t1 = System.currentTimeMillis();
            if (getInterpreter()==null) {
                setInterpreter(new Interpreter(new File(Utility.getFilePath("liveness.bin",
                        Environment.getExternalStorageDirectory()+File.separator+".caratemp"))));
                myImageDataType = getInterpreter().getInputTensor(0).dataType();
                imageProcessor = new ImageProcessor.Builder()
                        .add(new ResizeOp(200, 200, ResizeOp.ResizeMethod.BILINEAR))
                        .build();
                tImage = new TensorImage(myImageDataType);
            }

            // Analysis code for every frame
            // Preprocess the image
            tImage.load(bitmap);
            tImage = imageProcessor.process(tImage);
            TensorBuffer probabilityBuffer =
                    TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
            // run inference
            interpreter.run(tImage.getBuffer(), probabilityBuffer.getBuffer());

            float confidence = (probabilityBuffer.getFloatValue(0));
            long t2=System.currentTimeMillis();

            //if(!CaraManager.getInstance().isProd())
            //writeToFile(getByteFromBitmap(bitmap,90),System.currentTimeMillis()+"_"+confidence+"_face.jpg");

            Log.d("liveness","Keras Result ->"+ confidence+", time req-> "+(t2-t1));
            return confidence;
        }catch (Exception er){
            Utility.printStack(er);
        }
        return 0;
    }

    public Interpreter getFaceSpoofInterpreter() {
        return faceSpoofInterpreter;
    }

    public void setFaceSpoofInterpreter(Interpreter faceSpoofInterpreter) {
        this.faceSpoofInterpreter = faceSpoofInterpreter;
    }

    public float antiSpoofing(Bitmap bitmap) {

        if (getFaceSpoofInterpreter()==null) {
            setFaceSpoofInterpreter(new Interpreter(new File(Utility.getFilePath("FaceAntiSpoofing.bin",
                    Environment.getExternalStorageDirectory()+File.separator+".caratemp"))));
            faceSpoofDataType = getFaceSpoofInterpreter().getInputTensor(0).dataType();
            faceSpoofImgProc = new ImageProcessor.Builder()
                    .add(new ResizeOp(INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, ResizeOp.ResizeMethod.BILINEAR))
                    .build();
            faceSpoofTensorImg = new TensorImage(faceSpoofDataType);
        }

        long t1= System.currentTimeMillis();

        /*faceSpoofTensorImg.load(bitmap);
        faceSpoofTensorImg = faceSpoofImgProc.process(faceSpoofTensorImg);
        TensorBuffer probabilityBuffer =
                TensorBuffer.createFixedSize(new int[]{1, 2}, DataType.FLOAT32);
        // run inference
        getFaceSpoofInterpreter().run(faceSpoofTensorImg.getBuffer(), probabilityBuffer.getBuffer());

        float confidence = (probabilityBuffer.getFloatValue(0));

        //long t2=System.currentTimeMillis();*/

        Bitmap bitmapScale = Bitmap.createScaledBitmap(bitmap, INPUT_IMAGE_SIZE, INPUT_IMAGE_SIZE, true);

        long n1=System.currentTimeMillis();
        float[][][] img = normalizeImage(bitmapScale);
        long n2=System.currentTimeMillis();
        Log.d("liveness","time for norm -> "+(n2-n1));
        float[][][][] input = new float[1][][][];
        input[0] = img;
        float[][] clss_pred = new float[1][8];
        float[][] leaf_node_mask = new float[1][8];
        Map<Integer, Object> outputs = new HashMap<>();
        outputs.put(getFaceSpoofInterpreter().getOutputIndex("Identity"), clss_pred);
        outputs.put(getFaceSpoofInterpreter().getOutputIndex("Identity_1"), leaf_node_mask);
        getFaceSpoofInterpreter().runForMultipleInputsOutputs(new Object[]{input}, outputs);

        Log.d("FaceAntiSpoofing", "[" + clss_pred[0][0] + ", " + clss_pred[0][1] + ", "
                + clss_pred[0][2] + ", " + clss_pred[0][3] + ", " + clss_pred[0][4] + ", "
                + clss_pred[0][5] + ", " + clss_pred[0][6] + ", " + clss_pred[0][7] + "]");
        Log.d("FaceAntiSpoofing", "[" + leaf_node_mask[0][0] + ", " + leaf_node_mask[0][1] + ", "
                + leaf_node_mask[0][2] + ", " + leaf_node_mask[0][3] + ", " + leaf_node_mask[0][4] + ", "
                + leaf_node_mask[0][5] + ", " + leaf_node_mask[0][6] + ", " + leaf_node_mask[0][7] + "]");

        long t2=System.currentTimeMillis();
        Log.d("liveness","antispoof time req "+(t2-t1));
        return leaf_score1(clss_pred, leaf_node_mask);
    }

    private float leaf_score1(float[][] clss_pred, float[][] leaf_node_mask) {
        float score = 0;
        for (int i = 0; i < 8; i++) {
            score += Math.abs(clss_pred[0][i]) * leaf_node_mask[0][i];
        }
        return score;
    }

    public FaceResults classifyInput(String filename,double[] testfeature,String id){
        try {

            if (CaraManager.getInstance().getSvm()!=null) {
                Mat test=new Mat(1,2, CvType.CV_32F);

                for (int i=0;i<testfeature.length;i++){
                    test.put(0,i,testfeature[i]);
                }

                float r=CaraManager.getInstance().getSvm().predict(test);
                //Log.d("SVM","Result of predict -> "+r);

                return new FaceResults(0,r+"",r>0);
            }



            /*String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myDir = new File(root + "/faces");
            File f = new File(myDir, filename);

            BufferedReader b = new BufferedReader(new FileReader(f));
            String readLine = "";
            //System.out.println("Reading file using Buffered Reader"); */

            SVM svm;// = SVM.create();

            /*svm.setType(SVM.C_SVC);
            svm.setKernel(SVM.RBF);
            svm.setDegree(3);
            svm.setC(10);
            svm.setGamma(0.001);

            int samples_cnt = 598;
            int feature_col=2;
            Mat samples=new Mat(samples_cnt,feature_col,CvType.CV_32F);
            Mat labels=new Mat(1,samples_cnt,CvType.CV_32S);
            //Mat features;

            int rowcnt=0;
            int fcnt=0;
            while ((readLine = b.readLine()) != null) {
                if (readLine!=null){
                    String[] feature=readLine.split(",");
                    System.out.println(feature.length+","+readLine);
                    //Mat features=new Mat(1,10,CvType.CV_32F);
                    for(int i=0;i<feature.length;i++) {
                        if (i==0) {
                            labels.put(0, rowcnt++, Integer.parseInt(feature[0]));
                        }else {
                            samples.put(fcnt, i-1, Double.parseDouble(feature[i]));
                        }
                    }
                    fcnt++;
                    //samples.push_back(features);
                }
            }
            Log.d("SVM","fcnt->"+fcnt+" data type ->"+samples.type() +", samples col-> "+samples.cols()+",samples rows -> "+samples.rows()+", lables->"+labels.cols()+","+labels.rows());
             */

            String filePath = Environment.getExternalStorageDirectory()+File.separator+".caratemp/liveness.dat";
            //Log.d("LIVENESS",filePath);
           if (new File(filePath).exists()) {
               //Log.d("LIVENESS","FILE LOAD SUCCESS");
               //use this for already trained data
               svm = SVM.load(filePath);

               //use this for fresh training
               //svm.train(samples, Ml.ROW_SAMPLE,labels);
               CaraManager.getInstance().setSvm(svm);
               //svm.save(myDir.getAbsolutePath()+"/"+"spoof.xml");

               //TEST setup
               Mat test = new Mat(1, 2, CvType.CV_32F);

               for (int i = 0; i < testfeature.length; i++) {
                   test.put(0, i, testfeature[i]);
               }

               float r = CaraManager.getInstance().getSvm().predict(test);
               //Log.d("SVM","Result of predict -> "+r);

               return new FaceResults(0,r+"", r > 0);
           }else {
               return new FaceResults(0,0+"",true);
           }

           /* Mat test=new Mat(1,10,CvType.CV_32F);

            for (int i=0;i<testfeature.length;i++){
               test.put(0,i,testfeature[i]);
            }
            Log.d("SVG","Result of svm for "+id+"-> "+svm.predict(test)); */

        } catch (Exception e) {
            Utility.printStack(e);
        }
        return new FaceResults(0,0+"",false);
    }

    public class FaceResults {
        boolean isMask;
        boolean isReal;
        String fakeScore;
        public FaceResults(float isMask,String fakeScore,boolean isReal){
            this.isMask=isMask >= 0.95 ? true:false;
            this.fakeScore=fakeScore;
            this.isReal=isReal;
        }
    }

    /*private Net deepModel;
    public void setDeepModel(Net deepModel) {
        this.deepModel = deepModel;
    }
    public Net getDeepModel() {
        return deepModel;
    }
    public boolean classifyWithDeep(Mat inputImg){



        //deep neural network
        if (getDeepModel()==null) {
            Net model = Dnn.readNetFromTensorflow(Utility.getFilePath("liveness_keras.pb"),
                    Utility.getFilePath("train.pbtxt"));
            if (model != null) {
                setDeepModel(model);
            }
        }

        Imgproc.cvtColor(inputImg, inputImg, Imgproc.COLOR_RGBA2BGR);

        Mat testimg=Dnn.blobFromImage(inputImg,1,new Size(64,64));

        getDeepModel().setInput(testimg.reshape(0,new int[]{1,64,64,3}));

        Mat output = getDeepModel().forward();

            if (output.rows()>0) {
                Log.d("SVM", "Accuracy of deep -> " + output.get(0, 0)[0]);
                if (output.get(0, 0)[0] >= 0.5) {
                    return true;
                }
            }

        return false;
    } */

    public class FaceDetectionResult{
        boolean isFace;
        String msg;
        double faceArea;
        public FaceDetectionResult(boolean isFace,String msg,double faceArea){
            this.isFace=isFace;
            this.msg=msg;
            this.faceArea=faceArea;
        }
    }

}
