package android.wyse.face.tech5.utilities;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.os.Environment;
import android.util.Log;
import android.wyse.face.BuildConfig;
import android.wyse.face.CaraManager;
import android.wyse.face.FaceManager;
import android.wyse.face.Helper;
import android.wyse.face.Utility;
import android.wyse.face.models.ThermalModel;
import android.wyse.face.tech5.authenticate.AuthResponse;
import android.wyse.face.tech5.db.FaceRecord;
import android.wyse.face.tech5.db.LocalCacheManager;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;

import ai.tech5.sdk.abis.face.t5face.CreateFaceTemplateResult;
import ai.tech5.sdk.abis.face.t5face.IdentifyFaceResult;

import static android.system.Os.setenv;

import org.json.JSONObject;

public class OneShotProcessorTask {

    private String type = null;
    private byte[] image1;
    private byte[] image2;
    private String id;
    private float faceTheshold;
    private Context context;
    private Bitmap bitmap;
    private String licPathDir="/lic";
    private double blurthr=0.5;

    public OneShotProcessorTask(Context context, String type, byte[] image1,
                                byte[] image2, String id, float faceTheshold,Bitmap bitmap) {
        this.type = type;
        this.image1 = image1;
        this.image2 = image2;
        this.faceTheshold = faceTheshold;
        this.context = context;
        this.id = id;
        this.bitmap=bitmap;
    }

    public Context getContext() {
        return context;
    }

    public String getTaskType() {
        return type;
    }

    public boolean initSDK() {

        try {
            if(Listener.isSDKInitialized)
                return true;

            //String builderVersion = appSharedPreferenc.getBuilder();
            int builder = 211;
            int detectorVer = 211;

            String binFilesPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "share" + File.separator + "face_sdk_210";
            String zipFileName = "face_sdk_210.zip";
            String matchTableCode = "gn";

            if (builder==211) {
                binFilesPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "share" + File.separator + "face_sdk_211";
                zipFileName = "face_sdk_211.zip";
                matchTableCode = "in";
            }

            if (!CaraManager.getInstance().getSharedPreferences().getString("version_code",
                    "101").equalsIgnoreCase(BuildConfig.VERSION_CODE+"")) {

                LogUtils.debug("TAG", "version miss match " + CaraManager.getInstance().getSharedPreferences().getString("version_code",
                        "101"));
                //version has been changed, load fresh libs
                Utilities.deleteSDKDir("share");
                Utilities.deleteSDKDir("tmp");

                // Utilities.copyFilesFromAssets(context,zipFileName,bbinFilesPath);
                Utilities.startDownload(Helper.getBaseUri("pk"),zipFileName,binFilesPath);

            }else if (!Utilities.isLibs("share")){
                //if lib not present load it
                //Utilities.loadBinFiles(context, binFilesPath, zipFileName);
                LogUtils.debug("TAG", "File count differ");
                Utilities.startDownload(Helper.getBaseUri("pk"),zipFileName,binFilesPath);
            }

            //long t2 = System.currentTimeMillis();
            //System.err.println("Time Taken for BIN LOADERS:::" + (t2 - t1));
            //copyFaceSdkFilesToInternalStorage();
            LogUtils.debug("TAG", "cpu architecture " + Utilities.getSystemArchitecture());
            LogUtils.debug("TAG", "native lib dir " + context.getApplicationInfo().nativeLibraryDir);

             // doSymLinking();
             // setenv("ld_library_paths", binFilesPath, true);
             System.loadLibrary("c++_shared");
             System.loadLibrary("face_sdk");

            //System.load(binFilesPath+File.separator+"libface_sdk.so.");
            //System.loadLibrary("mxnet");
            System.loadLibrary("T5FaceNativeJNI");
            //System.loadLibrary("passportizer_jni");
            setenv("FACE_SDK_BIN_ROOT", binFilesPath, true);

            //done for lic
            setenv("FACE_SDK_REMOTE_LICENSE_DEFAULT","1",true);
            setenv("FACE_SDK_REMOTE_LICENSE_TOKEN", Helper.getTech5Token(),true);


            //Log.d("TAG","project key ->"+getenv("FACE_SDK_REMOTE_LICENSE_TOKEN"));
            //Log.d("TAG","bin path ->"+getenv("FACE_SDK_BIN_ROOT"));



            String licPath = getLicFilePath("face_sdk.lic");
            Log.d("TAG","lic file path -> "+licPath);
            Listener.initSDK(licPath,detectorVer,builder, matchTableCode, 0.9f);
            //"/mnt/sdcard/share/face_sdk"
            Utilities.enrollFromCache(context);

            loadLivenessLib("liveness.dat");
            loadLib("face_cascade.xml","face_cascade.xml");

            LogUtils.debug("TAG","libraries loaded successfully");
            //loadLib("FaceAntiSpoofing.bin","FaceAntiSpoofing.bin");

            return Listener.isSDKInitialized;
        } catch (Exception e) {
            Utility.printStack(e);

            if (context!=null)
            CaraManager.getInstance().sendSignal("ACTION_SDK_FAILED",context.getApplicationContext());

            return false;
        }
    }

    private boolean loadLib(String srcFileName,String destFileName){
        try {
            String output = Environment.getExternalStorageDirectory()+File.separator+".caratemp/"+destFileName;

            if (!new File(Environment.getExternalStorageDirectory()+File.separator+".caratemp").exists()){
                new File(Environment.getExternalStorageDirectory()+File.separator+".caratemp").mkdirs();
            }
            File output_file=new File(output);
            if (output_file.exists()){
                output_file.delete();
            }
            if (!output_file.exists()) {
                InputStream inputStream = getContext().getApplicationContext().getAssets().open(srcFileName);
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output_file));
                byte[] bytesIn = new byte[4096];
                int read = 0;
                while ((read = inputStream.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
                bos.close();
                inputStream.close();
            }
            return true;

        }catch (Exception er){
            Utility.printStack(er);
        }
        return false;
    }

    private void loadLivenessLib(String fileName){
        try {
            File myDir = new File(Environment.getExternalStorageDirectory()+File.separator+".caratemp");
            if (!myDir.exists()) {
                myDir.mkdir();
            }

            String output = Environment.getExternalStorageDirectory()+File.separator+".caratemp/"+fileName;
            File output_file=new File(output);
            if (!output_file.exists()) {
                InputStream inputStream = getContext().getApplicationContext().getAssets().open("spoof.xml");
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output_file));
                byte[] bytesIn = new byte[4096];
                int read = 0;
                while ((read = inputStream.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
                bos.close();
                inputStream.close();
            }

             output = Environment.getExternalStorageDirectory()+File.separator+".caratemp/liveness.bin";
             output_file=new File(output);
             try {
                 if (output_file.exists()) {
                     output_file.delete();
                     output_file = new File(output);
                 }
             }catch (Exception er){}

            if (!output_file.exists()) {
                InputStream inputStream = getContext().getApplicationContext().getAssets().open("liveness.bin");
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(output_file));
                byte[] bytesIn = new byte[4096];
                int read = 0;
                while ((read = inputStream.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
                bos.close();
                inputStream.close();
            }
        }catch (Exception er){
            Utility.printStack(er);
        }
    }

    private String getLicFilePath(String fileName){
        String path="face_sdk.lic";
        try {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myDir = new File(root + licPathDir);

            if (!myDir.exists()) {
                myDir.mkdir();
            }
            File file = new File(myDir, fileName);
            //if (!file.exists()) {
             //   file.createNewFile();
            //}
            return file.getAbsolutePath();
        }catch (Exception er){
            Utility.printStack(er);
        }
        return path;
    }

    public boolean checkQuality(){
        ArrayList<CreateFaceTemplateResult> results= FaceManager.getInstance().getFaceTemp(
                FaceManager.getInstance().getByteFromBitmap(bitmap,70));
        image1=null;
        if (results.size()>0){
            //Log.d("blur",results.get(0).Blur+"");
            if (results.get(0).Blur>=blurthr){
                return true;
            }
        }
        return false;
    }

    public ThermalModel scanTemperature(){
        //read thermal temp
       return CaraManager.getInstance().getThermalTemp(1);
    }

    public IdentifyFaceResults identifyFace() throws Exception {
        try {
            //LogUtils.debug("TAG", "Listener.t5TemplateCreator.CreateFaceTemplate from oneShot");
            // OneShotResult faceTcResult = Listener.mOneShotProcessor.processImage(image1);
            if (image1==null){
                return new IdentifyFaceResults(null, 0, 0, 0, 0);
            }
            ArrayList<CreateFaceTemplateResult> createFaceTemplateResults = Listener.t5TemplateCreator.CreateFaceTemplate(image1);

            if (createFaceTemplateResults!=null) {

                if (createFaceTemplateResults.size()>0) {
                    float isMask = createFaceTemplateResults.get(0).Mask;
                    float smile = createFaceTemplateResults.get(0).Smile;
                    float glasses = createFaceTemplateResults.get(0).Glasses;
                    float gender = createFaceTemplateResults.get(0).Gender;
                    float confidence = createFaceTemplateResults.get(0).Confidence;
                    float blur = createFaceTemplateResults.get(0).Blur;
                    float closedEye = createFaceTemplateResults.get(0).ClosedEyes;

                    if (Float.isNaN(isMask))
                    {
                        isMask = 0;
                    }
                    if (Float.isNaN(smile))
                    {
                        smile = 0;
                    }if (Float.isNaN(glasses))
                    {
                        glasses = 0;
                    }
                    if (Float.isNaN(gender))
                    {
                        gender = 0;
                    }
                    if (Float.isNaN(confidence))
                    {
                        confidence = 0;
                    }
                    if (Float.isNaN(blur))
                    {
                        blur = 0;
                    }
                    if (Float.isNaN(closedEye))
                    {
                        closedEye = 0;
                    }

                    LogUtils.debug("TAG", "after Listener.t5TemplateCreator.CreateFaceTemplate");
                    if (createFaceTemplateResults != null) {
                        if (createFaceTemplateResults.size() > 0) {
                            //Utilities.writeTemplateToFile(createFaceTemplateResults.get(0).Template, "");
                            IdentifyFaceResult[] results = Listener.t5TemplateMatcher.IdenfityFace(createFaceTemplateResults.get(0).Template, faceTheshold, 10);
                            //Log.d("TAG", "mask value ->" + isMask + "");

                            //Logging the test data
                            Helper testHelper = new Helper();

                            Bitmap image = AppUtils.convertByteArrayToBitmap(image1);
                            JSONObject logValue = new JSONObject();
                            logValue.put("Action", "Identify face");
                            logValue.put("EmpId", results != null ? results[0].Id : "NA");
                            logValue.put("ImageSize", image.getWidth() + " X " + image.getHeight());
                            logValue.put("Image", image1 == null ? "" : Utility.encodeBase64(image1));
                            logValue.put("TemplateSize",image1 == null ? 0 : image1.length);
                            logValue.put("Confidence", confidence);
                            logValue.put("macid", CaraManager.getInstance().getDEVICE_MACID());
                            logValue.put("glass", glasses);
                            logValue.put("blur", blur);
                            logValue.put("closedEye", closedEye);
                            logValue.put("ismask", isMask);
                            logValue.put("Identify Score", results != null ? results[0].Score : 0);
                            logValue.put("macid", CaraManager.getInstance().getDEVICE_MACID());

                            testHelper.logData(logValue.toString(), "identifyFace", "Info");

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

    public AuthResponse authenticateFace() {
        AuthResponse response = new AuthResponse();
        try {
            LocalCacheManager cacheManager = new LocalCacheManager(context);
            FaceRecord faceRecord = cacheManager.getFaceRecordById(id);
            if (faceRecord != null && faceRecord.template != null && faceRecord.template.length > 0) {
                ArrayList<byte[]> faces = new ArrayList<>();
                faces.add(image1);
                //  OneShotResult faceTcResult = Listener.mOneShotProcessor.processImage(image1);
                ArrayList<CreateFaceTemplateResult> createFaceTemplateResults = Listener.t5TemplateCreator.CreateFaceTemplate(image1);
                if (createFaceTemplateResults != null && createFaceTemplateResults.size() > 0) {
                    Utilities.writeTemplateToFile(createFaceTemplateResults.get(0).Template, "Auth");
                    float score = Listener.t5TemplateMatcher.MatchWithId(createFaceTemplateResults.get(0).Template, id);
                    response.score = score;
                    response.faceImage = faceRecord.faceImage;
                    response.errorMesssage = null;

                    //Logging the test data
                    Helper testHelper = new Helper();

                    Bitmap image = AppUtils.convertByteArrayToBitmap(image1);
                    JSONObject logValue = new JSONObject();
                    logValue.put("Action", "Authenticate Face");
                    logValue.put("EmpId", faceRecord.id);
                    logValue.put("ImageSize", image.getWidth() + " X " + image.getHeight());
                    logValue.put("TemplateSize", image1.length);
                    logValue.put("macid", CaraManager.getInstance().getDEVICE_MACID());
                    logValue.put("Identify Score", score);

                    testHelper.logData(logValue.toString(), "authenticateFace()", "Info");
                } else {
                    response.errorMesssage = "Unable to create face template";
                }
            } else {
                response.errorMesssage = " Id  " + id + " not exists ";
            }
        } catch (Exception e) {
            Utility.printStack(e);
            response.errorMesssage = e.getLocalizedMessage();
        }
        return response;
    }

    public boolean enrollFromTemplate(String id,byte[] template){
        Listener.t5TemplateMatcher.InsertFace(id, template);
        try {
            LogUtils.debug("TAG", "scaled down to insert in sqlite");
            new LocalCacheManager(context).addFaceRecordToDb(id, template, template);
            LogUtils.debug("TAG", "face record inserted in to sqlite");
        } catch (Exception e) {
            e.printStackTrace();
            Listener.t5TemplateMatcher.RemoveFace(id);
        }
        return false;
    }


    public EnrollResponse enrollFace() {
        EnrollResponse response = new EnrollResponse();
        try {
            long timeBeforeTemplateCreation = System.currentTimeMillis();
            // OneShotResult faceTcResult = Listener.mOneShotProcessor.processImage(image1);
            ArrayList<CreateFaceTemplateResult> createFaceTemplateResults = Listener.t5TemplateCreator.CreateFaceTemplate(image1);
            response.timetakenForTemplateCreation = System.currentTimeMillis() - timeBeforeTemplateCreation;
            if (createFaceTemplateResults != null && createFaceTemplateResults.size() > 0) {
                long timeBeforeEnroll = System.currentTimeMillis();
                //Utilities.writeTemplateToFile(createFaceTemplateResults.get(0).Template, "enroll");
                Listener.t5TemplateMatcher.InsertFace(id, createFaceTemplateResults.get(0).Template);
                response.timetakenForEnrollment = System.currentTimeMillis() - timeBeforeEnroll;
                response.isInserted = true;
                response.errorMessage = null;

                try {
                    // Changed resolution from 800x800 to 640x480
                   // byte[] scaledFaceImage = Utilities.scaleDown(image1, 800, 800, false);
                    byte[] scaledFaceImage = Utilities.scaleDown(image1, 640, 480, false);
                    LogUtils.debug("TAG", "scaled down to insert in sqlite");
                    new LocalCacheManager(context).addFaceRecordToDb(id, createFaceTemplateResults.get(0).Template, (scaledFaceImage != null && scaledFaceImage.length > 0) ? scaledFaceImage : image1);
                    LogUtils.debug("TAG", "face record inserted in to sqlite");
                } catch (Exception e) {
                    Utility.printStack(e);

                    Listener.t5TemplateMatcher.RemoveFace(id);
                    response.isInserted = false;
                    response.errorMessage = "Unable insert face to Cache";

                }
            } else {
                response.isInserted = false;
                response.errorMessage = "Unable to create face template";
            }
        } catch (Exception e) {
            Utility.printStack(e);
            response.isInserted = false;
            response.errorMessage = e.getLocalizedMessage();
        }
        return response;
    }


    public ResultObject matchFaceImages() throws Exception {

        LogUtils.debug("TAG", "matchFaceImages() called");
        ResultObject resultObject = new ResultObject();
        ArrayList<CreateFaceTemplateResult> galleryTCResult1 = null, galleryTCResult2 = null;
        long t1 = System.currentTimeMillis();
        // galleryTCResult1 = Listener.mOneShotProcessor.processImage(image1);
        galleryTCResult1 = Listener.t5TemplateCreator.CreateFaceTemplate(image1);


        if (galleryTCResult1 == null || galleryTCResult1.size() == 0 || galleryTCResult1.get(0).Template == null) {
            throw new Exception("Unable to create face template1");
        }

        long t2 = System.currentTimeMillis();
        resultObject.setTimeTakenForTemplateCreation(t2 - t1);

        //  galleryTCResult2 = Listener.mOneShotProcessor.processImage(image2);
        galleryTCResult2 = Listener.t5TemplateCreator.CreateFaceTemplate(image2);

        if (galleryTCResult2 == null || galleryTCResult2.size() == 0 || galleryTCResult2.get(0).Template == null) {
            throw new Exception("Unable to create face template2");
        }

        Utilities.writeTemplateToFile(galleryTCResult1.get(0).Template, "enroll_1");
        Utilities.writeTemplateToFile(galleryTCResult2.get(0).Template, "enroll2");

        t1 = System.currentTimeMillis();
        float score = matchTemplates(galleryTCResult1.get(0).Template, galleryTCResult2.get(0).Template);
        t2 = System.currentTimeMillis();
        resultObject.setTimeTakenForMatching(t2 - t1);
        resultObject.setMatchingScore(score);
        LogUtils.debug("TAG", "face matching done");

        return resultObject;
    }

    private float matchTemplates(byte[] template1, byte[] template2) {
        return Listener.t5TemplateMatcher.Match(template1, template2);
    }

    private int getAppVersionCode(Context mContext) {
        try {
            PackageInfo pInfo = mContext.getPackageManager().getPackageInfo(mContext.getPackageName(), 0);
            return pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return 0;
    }

}
