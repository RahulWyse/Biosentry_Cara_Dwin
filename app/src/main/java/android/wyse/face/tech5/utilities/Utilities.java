package android.wyse.face.tech5.utilities;

import android.app.ProgressDialog;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Build;
import android.os.CountDownTimer;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.provider.OpenableColumns;
import android.util.Log;
import android.wyse.face.BuildConfig;
import android.wyse.face.CaraManager;
import android.wyse.face.Utility;
import android.wyse.face.tech5.db.FaceRecord;
import android.wyse.face.tech5.db.LocalCacheManager;

import androidx.exifinterface.media.ExifInterface;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;


/**
 * @author Shravan Nitta
 * @version 1.0
 */
public class Utilities {

    private Context parentActivity;
    private ProgressDialog progressDialog;
    private String language;

    public static String logFileName = null;

    public static final String TAG = Utilities.class.getSimpleName();

    public Utilities(Context parentActivity, String language) {
        this.parentActivity = parentActivity;
        this.language = language;
        progressDialog = new ProgressDialog(parentActivity);
        progressDialog.setMessage("Please wait...");
        progressDialog.setCancelable(false);
    }

    public Typeface getTypeFace(String type) {
        Typeface tf = Typeface.createFromAsset(parentActivity.getAssets(), "fonts/Cairo/Cairo-Regular.ttf");
        switch (type) {
            case "bold":
                tf = Typeface.createFromAsset(parentActivity.getAssets(), "fonts/Cairo/Cairo-Bold.ttf");
                break;
            case "regular":
                tf = Typeface.createFromAsset(parentActivity.getAssets(), "fonts/Cairo/Cairo-Regular.ttf");
                break;
            case "semi-bold":
                tf = Typeface.createFromAsset(parentActivity.getAssets(), "fonts/Cairo/Cairo-SemiBold.ttf");
        }
        return tf;
    }

    public void showProgressDialog(boolean flag) {

        if (progressDialog != null) {
            if (flag) {
                progressDialog.show();
            } else {
                if (progressDialog.isShowing()) {
                    progressDialog.dismiss();
                }
            }

        }

    }


    public void deleteCache(Context context) {
        try {
            File dir = context.getCacheDir();
            deleteDir(dir);
        } catch (Exception e) {
        }
    }

    public boolean deleteDir(File dir) {
        try {
            if (dir != null && dir.isDirectory()) {
                String[] children = dir.list();
                for (int i = 0; i < children.length; i++) {
                    boolean success = deleteDir(new File(dir, children[i]));
                    if (!success) {
                        return false;
                    }
                }
                return dir.delete();
            } else if (dir != null && dir.isFile()) {
                return dir.delete();
            } else {
                return false;
            }
        } catch (Exception e) {
            return false;
        }
    }

    public String getDeviceManufacturerName() {
        return Build.MANUFACTURER;
    }

//    public void writeToFile(byte[] capturedBytes, String fileName) {
//        try {
//            FileOutputStream fos = new FileOutputStream(Environment.getExternalStorageDirectory() + "/" + fileName);
//            fos.write(capturedBytes);
//            fos.close();
//        } catch (Exception e) {
//            e.printStackTrace();
//        }
//    }

    public static byte[] getBytesFromImage(Bitmap bitmap) {
        try {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, baos);
            byte[] imageBytes = baos.toByteArray();
            baos.close();
            return imageBytes;
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    public byte[] processImage(byte[] faceBytes, int cameraFacing) {
        byte[] processedBytes = null;
        try {
            if (getDeviceManufacturerName().equalsIgnoreCase(Constants.SAMSUNG_DEVICE)) {
                Bitmap capturedBitmap = BitmapFactory.decodeByteArray(faceBytes, 0, faceBytes.length);
                Matrix mat = new Matrix();
                if (cameraFacing == 0) {
                    LogUtils.debug(TAG, "Back Facing Camera....");
                    mat.postRotate(90);// for back facing camera
                } else {
                    LogUtils.debug(TAG, "Front Facing Camera....");
                    mat.postRotate(270);// for front facing camera
                }
                Bitmap bmpRotate = Bitmap.createBitmap(capturedBitmap, 0, 0, capturedBitmap.getWidth(), capturedBitmap.getHeight(), mat, true);
                final ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                bmpRotate.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
                processedBytes = stream2.toByteArray();
            } else {
                if (cameraFacing == 0) {
                    Bitmap capturedBitmap = BitmapFactory.decodeByteArray(faceBytes, 0, faceBytes.length);
                    Matrix mat = new Matrix();
                    LogUtils.debug(TAG, "Back Facing Camera....");
                    mat.postRotate(180);// for back facing camera
                    Bitmap bmpRotate = Bitmap.createBitmap(capturedBitmap, 0, 0, capturedBitmap.getWidth(), capturedBitmap.getHeight(), mat, true);
                    final ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
                    bmpRotate.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
                    processedBytes = stream2.toByteArray();
                } else {
                    return faceBytes;
                }
            }
        } catch (Exception e) {
            LogUtils.debug(TAG, "Exception while detecting faces in face detection task :: " + e.getMessage());
        }
        return processedBytes;
    }

    public File getFilePath(Intent data) {
        String actualfilepath = "";
        String fullerror = "";
        File myFile = null;
        try {
            Uri imageuri = data.getData();
            InputStream stream = null;
            String tempID = "", id = "";
            Uri uri = data.getData();
            Log.e(TAG, "file auth is " + uri.getAuthority());
            fullerror = fullerror + "file auth is " + uri.getAuthority();
            if (imageuri.getAuthority().equals("media")) {
                tempID = imageuri.toString();
                tempID = tempID.substring(tempID.lastIndexOf("/") + 1);
                id = tempID;
                Uri contenturi = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                String selector = MediaStore.Images.Media._ID + "=?";
                actualfilepath = getColunmData(contenturi, selector, new String[]{id});
            } else if (imageuri.getAuthority().equals("com.android.providers.media.documents")) {
                tempID = DocumentsContract.getDocumentId(imageuri);
                String[] split = tempID.split(":");
                String type = split[0];
                id = split[1];
                Uri contenturi = null;
                if (type.equals("image")) {
                    contenturi = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if (type.equals("video")) {
                    contenturi = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if (type.equals("audio")) {
                    contenturi = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }
                String selector = "_id=?";
                actualfilepath = getColunmData(contenturi, selector, new String[]{id});
            } else if (imageuri.getAuthority().equals("com.android.providers.downloads.documents")) {
                tempID = imageuri.toString();
                tempID = tempID.substring(tempID.lastIndexOf("/") + 1);
                id = tempID;
                Uri contenturi = ContentUris.withAppendedId(Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));
                // String selector = MediaStore.Images.Media._ID+"=?";
                actualfilepath = getColunmData(contenturi, null, null);
            } else if (imageuri.getAuthority().equals("com.android.externalstorage.documents")) {
                tempID = DocumentsContract.getDocumentId(imageuri);
                String[] split = tempID.split(":");
                String type = split[0];
                id = split[1];
                Uri contenturi = null;
                if (type.equals("primary")) {
                    actualfilepath = Environment.getExternalStorageDirectory() + "/" + id;
                }
            }
            myFile = new File(actualfilepath);
            // MessageDialog dialog = new MessageDialog(Home.this, " file details --"+actualfilepath+"\n---"+ uri.getPath() );
            // dialog.displayMessageShow();
            String temppath = uri.getPath();
            if (temppath.contains("//")) {
                temppath = temppath.substring(temppath.indexOf("//") + 1);
            }
            Log.e(TAG, " temppath is " + temppath);
            fullerror = fullerror + "\n" + " file details -  " + actualfilepath + "\n --" + uri.getPath() + "\n--" + temppath;
            if (actualfilepath.equals("") || actualfilepath.equals(" ")) {
                myFile = new File(temppath);
            } else {
                myFile = new File(actualfilepath);
            }
            //File file = new File(actualfilepath);
            //Log.e(TAG, " actual file path is "+ actualfilepath + "  name ---"+ file.getName());
//                    File myFile = new File(actualfilepath);
            Log.e(TAG, " myfile is " + myFile.getAbsolutePath());

            // lyf path  - /storage/emulated/0/kolektap/04-06-2018_Admin_1528088466207_file.xls
        } catch (Exception e) {
            Log.e(TAG, " read errro " + e.toString());
        }
        return myFile;
    }

    public String getColunmData(Uri uri, String selection, String[] selectarg) {
        String filepath = "";
        Cursor cursor = null;
        String colunm = "_data";
        String[] projection = {colunm};
        cursor = parentActivity.getContentResolver().query(uri, projection, selection, selectarg, null);
        if (cursor != null) {
            cursor.moveToFirst();
            Log.e(TAG, " file path is " + cursor.getString(cursor.getColumnIndex(colunm)));
            filepath = cursor.getString(cursor.getColumnIndex(colunm));
        }
        if (cursor != null)
            cursor.close();
        return filepath;
    }

//    public byte[] readfile2(File file) {
//
//        byte[] loadedBytes = null;
//        try {
//            loadedBytes = Files.readAllBytes(file.toPath());
//            System.out.println("Loaded Face Bytes::::" + loadedBytes.length);
//        } catch (IOException e) {
//            System.err.println("Failed to load template file");
//            e.printStackTrace();
//        }
//        return loadedBytes;
//    }

    public static byte[] readfile(String fileName) {

        String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "T5Face";
        File root = new File(rootPath);
        if (!root.exists()) {
            root.mkdirs();
        }

        File file = new File(rootPath + File.separator + fileName);

        int size = (int) file.length();
        byte[] loadedBytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(loadedBytes, 0, loadedBytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadedBytes;
    }

    public static byte[] readfileFrom(File file) {
        int size = (int) file.length();
        byte[] loadedBytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(loadedBytes, 0, loadedBytes.length);
            buf.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return loadedBytes;
    }

    public static byte[] readfileFrom(String file) {
        File f=new File(file);
        if (!f.exists())
            return null;

        int size =  (int)f.length();
        byte[] loadedBytes = new byte[size];
        try {
            BufferedInputStream buf = new BufferedInputStream(new FileInputStream(file));
            buf.read(loadedBytes, 0, loadedBytes.length);
            buf.close();
        } catch (Exception er){
            Utility.printStack(er);
        }
        return loadedBytes;
    }

    public static Bitmap rotateImage(float angle, byte[] loadedFaceBytes) {
        Bitmap rotatedImage = null;
        if (null != loadedFaceBytes) {
            Bitmap capturedBitmap = BitmapFactory.decodeByteArray(loadedFaceBytes, 0, loadedFaceBytes.length);
            Matrix mat = new Matrix();
            mat.postRotate(angle);
            Bitmap bmpRotate = Bitmap.createBitmap(capturedBitmap, 0, 0, capturedBitmap.getWidth(), capturedBitmap.getHeight(), mat, true);
            final ByteArrayOutputStream stream2 = new ByteArrayOutputStream();
            bmpRotate.compress(Bitmap.CompressFormat.JPEG, 100, stream2);
            rotatedImage = BitmapFactory.decodeByteArray(stream2.toByteArray(), 0, stream2.toByteArray().length);
        }
        return rotatedImage;
    }


    public static int getExifRotation(byte[] faceBytes) {

        // the URI you've received from the other app
        ByteArrayInputStream in = null;
        int rotation = 0;
        try {
            in = new ByteArrayInputStream(faceBytes);
            ExifInterface exifInterface = new ExifInterface(in);
            // Now you can extract any Exif tag you want
            // Assuming the image is a JPEG or supported raw format

            int orientation = exifInterface.getAttributeInt(
                    ExifInterface.TAG_ORIENTATION,
                    ExifInterface.ORIENTATION_NORMAL);
            switch (orientation) {
                case ExifInterface.ORIENTATION_ROTATE_90:
                    rotation = 90;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_180:
                    rotation = 180;
                    break;
                case ExifInterface.ORIENTATION_ROTATE_270:
                    rotation = 270;
                    break;
            }
        } catch (IOException e) {
            // Handle any errors
            e.printStackTrace();
        } finally {
            if (in != null) {
                try {
                    in.close();
                } catch (IOException ignored) {
                }
            }
        }

        return rotation;
    }


    public static byte[] scaleDown(byte[] image, float maxImageWidth, float maxImageHeight,
                                   boolean filter) {

        LogUtils.debug("TAG", "img size bfore scaling " + image.length);
        try {

            Bitmap bitmap = BitmapFactory.decodeByteArray(image, 0, image.length);
            LogUtils.debug("TAG", "img dims bfore scaling " + bitmap.getWidth() + " X " + bitmap.getHeight());

            float ratio = Math.min(maxImageWidth / bitmap.getWidth(), maxImageHeight / bitmap.getHeight());

            ///In order to ensure that the image will only be down scaled, detect if the result ratio is less than 1
            if (ratio < 1) {
                int width = Math.round(ratio * bitmap.getWidth());
                int height = Math.round(ratio * bitmap.getHeight());

                Bitmap newBitmap = Bitmap.createScaledBitmap(bitmap, width,
                        height, filter);

                LogUtils.debug("TAG", "img dims after scaling " + newBitmap.getWidth() + " X " + newBitmap.getHeight());

                ByteArrayOutputStream stream = new ByteArrayOutputStream();
                newBitmap.compress(Bitmap.CompressFormat.JPEG, 90, stream);
                byte[] faceBytesScaled = stream.toByteArray();

                LogUtils.debug("TAG", "img size after scaling " + faceBytesScaled.length);
                return faceBytesScaled;
            }
        } catch (Exception e) {
            Utility.printStack(e);
        }


        return null;
    }

    public static JSONArray getUsersList(Context context){
        JSONArray users=new JSONArray();
        try{
            if (Listener.isSDKInitialized) {
                LocalCacheManager localCacheManager = new LocalCacheManager(context);
                List<FaceRecord> list = localCacheManager.getAllRecords();
                LogUtils.debug("TAG", "total cache size  " + (list != null ? list.size() : 0));
                long size = Listener.t5TemplateMatcher.Size();
                if (list != null && list.size() > 0) {

                    if (list.size() > size) {

                        for (FaceRecord record : list) {
                            try {
                                //Listener.t5TemplateMatcher.InsertFace(record.id, record.template);
                                // LogUtils.debug("TAG", "face inserted " + record.id);
                                users.put(new JSONObject().put("empid",
                                        record.id).put("errorstring",
                                        "success"));
                            } catch (Exception e) { }
                        }
                    }
                }
                LogUtils.debug("TAG", "total enrollments " + Listener.t5TemplateMatcher.Size());
                localCacheManager.closeDbConnection();
            }else{
                users.put(new JSONObject().put("empid","").put("errorstring","error").put("msg","face sdk not init."));
            }
        }catch (Exception er){
            try {
                users.put(new JSONObject().put("empid", "").put("errorstring", "error").put("msg", er.getMessage()));
            }catch (Exception err){}
        }
        if (users.length()==0){
            try {
                users.put(new JSONObject().put("empid", "").put("errorstring", "error").put("msg", "No users found"));
            }catch (Exception err){}
        }
        return users;
    }

    public static void enrollFromCache(Context context) {

        if (Listener.isSDKInitialized) {
            LocalCacheManager localCacheManager = new LocalCacheManager(context);
            List<FaceRecord> list = localCacheManager.getAllRecords();
            LogUtils.debug("TAG", "total cache size  " + (list != null ? list.size() : 0));
            long size = Listener.t5TemplateMatcher.Size();
            if (list != null && list.size() > 0) {

                if (list.size() > size) {

                    for (FaceRecord record : list) {
                        try {

                            Listener.t5TemplateMatcher.InsertFace(record.id, record.template);
                            // LogUtils.debug("TAG", "face inserted " + record.id);
                        } catch (Exception e) {

                            e.printStackTrace();
                        }
                    }
                }
            }
            LogUtils.debug("TAG", "total enrollments " + Listener.t5TemplateMatcher.Size());
            localCacheManager.closeDbConnection();
        }
    }


    public static void appendLog(String text) {


        try {

            if (!BuildConfig.DEBUG){
                return;
            }
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "T5Face";
            File root = new File(rootPath);
            if (!root.exists()) {
                root.mkdirs();
            }

            File logFile = new File(rootPath + File.separator + new SimpleDateFormat("dd_MMM_yyyy").format(new Date()) + "log_file.txt");
            if (!logFile.exists()) {
                try {
                    logFile.createNewFile();
                } catch (IOException e) {

                   Utility.printStack(e);
                }
            }

            text = new Date().toString() + "  " + text;
            //BufferedWriter for performance, true to set append to file flag
            BufferedWriter buf = new BufferedWriter(new FileWriter(logFile, true));
            buf.append(text);
            buf.newLine();
            buf.close();
        } catch (IOException e) {

           Utility.printStack(e);
        }
    }


    public static byte[] getByteArrayFromUri(Context context, Uri uri) throws Exception {
        InputStream inputStream = null;
        ByteArrayOutputStream byteBuffer = null;

        try {

            inputStream = context.getContentResolver().openInputStream(uri);
            byteBuffer = new ByteArrayOutputStream();
            int bufferSize = 1024;
            byte[] buffer = new byte[bufferSize];

            int len = 0;
            while ((len = inputStream.read(buffer)) != -1) {
                byteBuffer.write(buffer, 0, len);
            }
            return byteBuffer.toByteArray();
        } catch (FileNotFoundException fn) {
            throw fn;


        } catch (IOException io) {
            throw io;


        } catch (Exception e) {
            throw e;
        } finally {

            context = null;
            try {
                if (inputStream != null) {
                    inputStream.close();
                }

                if (byteBuffer != null) {
                    byteBuffer.close();
                }
            } catch (Exception e) {

            }
        }


    }


    public static String getFileNameFromUri(ContentResolver resolver, Uri uri) {

        try {
            Cursor returnCursor =
                    resolver.query(uri, null, null, null, null);
            assert returnCursor != null;
            int nameIndex = returnCursor.getColumnIndex(OpenableColumns.DISPLAY_NAME);
            returnCursor.moveToFirst();
            String name = returnCursor.getString(nameIndex);
            returnCursor.close();
            return name;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String removeFileExtension(String name) {
        if (name.contains(".")) {

            if (name.indexOf(".") > 0)
                name = name.substring(0, name.lastIndexOf("."));
        }

        return name;
    }


    public static void writeTemplateToFile(byte[] capturedBytes, String fileName) {

        try {
            //fileName = removeFileExtension(fileName);
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + "/tech5Templates/";
            File root = new File(rootPath);
            if (!root.exists()) {
                root.mkdirs();
            }
            File f = new File(rootPath + fileName + "_" + System.currentTimeMillis() + ".dat");
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

    public static void writeToFile(byte[] capturedBytes, String fileName) {
        try {
            // fileName = removeFileExtension(fileName);
            String rootPath = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "CaraFaces";
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

    private static String matchFiles[]={"/face_detector/210.bin","/builder/211.bin"};

    public static boolean loadBinFiles(Context context, String destDir, String zipFileName) {
        //String destDir = "/storage/emulated/0/share/face_sdk";
        // String destDir = destPath;
        LogUtils.debug("TAG", "loading bin files destDir " + destDir);
        LogUtils.debug("TAG", "loading bin files zipFileName " + zipFileName);
        File f = new File(destDir);
        boolean isfreshLoad=false;

        if (f.exists()){
            for (int file=0;file<matchFiles.length;file++){
                if (!new File(destDir+matchFiles[file]).exists()){
                    isfreshLoad=true;
                }
            }
        }

        if (isfreshLoad || !f.exists()) {
            try {
                deleteSDKDir(destDir);
                InputStream inputStream = context.getAssets().open(zipFileName);
                String faceSdkLocation = Environment.getExternalStorageDirectory().getAbsolutePath() + File.separator + "share";
                unzipNew(inputStream, faceSdkLocation);
                Log.d("TAG","new files created in share");
                return true;
            } catch (Exception e) {
                Utility.printStack(e);
                return false;
            }
        } else {
            LogUtils.debug("TAG", "bin files already exists() ");
        }
        return false;
    }


    private static boolean unZip(final String filePath,final String fileOutputPath){
        InputStream is;
        ZipInputStream zis;
        try
        {
            String filename;
            is = new FileInputStream(filePath);
            zis = new ZipInputStream(new BufferedInputStream(is));
            ZipEntry ze;
            byte[] buffer = new byte[1024];
            int count;

            while ((ze = zis.getNextEntry()) != null)
            {
                filename = ze.getName();
                File fmd=null;
                FileOutputStream fout;

                //Log.d(getClass().getSimpleName(),fileOutputPath+""+filename);
                fmd = new File(fileOutputPath + "/" + filename);

                if (!fmd.getParentFile().exists()) {
                    if (fmd.getParentFile().mkdirs()){
                        Log.d("unZip","Directory created "+fmd.getParentFile());
                    }else {
                        Log.d("unZip","Failed to create Directory "+fmd.getParentFile());
                    }
                }else{
                    if(fmd.exists()){
                        fmd.getAbsoluteFile().delete();
                        Log.d("unZip","File deleted "+fmd.getAbsolutePath());
                    }
                }

                if (fmd.exists()){
                    Log.d("unZip","File present "+fmd.getAbsolutePath());
                }else {
                    Log.d("unZip","File not present "+fmd.getAbsolutePath());
                    if (ze.isDirectory()){
                        fmd.mkdir();
                        Log.d("unZip","File is directory "+fmd.getAbsolutePath());
                    }
                }
                if (!fmd.isDirectory()) {
                    fout = new FileOutputStream(fmd);
                    while ((count = zis.read(buffer)) != -1) {
                        fout.write(buffer, 0, count);
                    }
                    fout.close();
                    zis.closeEntry();
                }
            }

            if (is!=null){
                if(new File(filePath).exists()){
                    //new File(filePath).delete();
                }
            }
            zis.close();
        } catch(Exception e) {   //e.printStackTrace();
            Utility.printStack(e);
        }
        return true;
    }

    private static int count=0;
    private static long total = 0;
    private static int lenghtOfFile=0;
    private static String remoteSdkBin="sdk";
    public static boolean startDownload(Uri.Builder urlstring, final String srcPath,final String destPath){

        try {
            String api=urlstring.appendPath("caralic").appendPath(remoteSdkBin).build().toString()+File.separator+srcPath;
            //Log.d("TECH5SDK",api);
            URL url = new URL(api);
            URLConnection conection = url.openConnection();
            conection.connect();
            // getting file length
            lenghtOfFile = conection.getContentLength();
            String ctype=conection.getContentType();
            // input stream to read file - with 8k buffer
            InputStream input = new BufferedInputStream(url.openStream(), 8192);

            if (!new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmp/").exists()){
                new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmp/").mkdir();
            }

            File f = new File(Environment.getExternalStorageDirectory().getAbsolutePath()+"/tmp/"+srcPath);
            if (f.exists()) {
                unZip(f.getAbsolutePath(),destPath);

                if (!isLibs("share")){
                    f.delete();
                }else {
                    return false;
                }
            }

            FileOutputStream output = new FileOutputStream(f);
            byte data[] = new byte[1024];
            count=0;
            total=0;
            while ((count = input.read(data)) != -1) {
                total += count;
                // writing data to file
                output.write(data, 0, count);
                //Log.d("TAG","File write -> "+total);
            }

            // flushing output
            output.flush();
            // closing streams
            output.close();
            input.close();

            //unzip the files
            //unzipNew(new FileInputStream(f),destPath);
            unZip(f.getAbsolutePath(),destPath);

            Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(),"version_code",BuildConfig.VERSION_CODE+"");

            LogUtils.debug("TAG", "File write successful, version -> "+CaraManager.getInstance().getSharedPreferences().getString("version_code","000"));
            return true;
            //f.delete();
        }catch (Exception er){
            Utility.printStack(er);
        }
        return false;

    }


    private static void unzipNew(InputStream inputStream, String destDirectory) throws IOException {
        File destDir = new File(destDirectory);
        if (!destDir.exists()) {
            destDir.mkdirs();
        }
        ZipInputStream zipIn = new ZipInputStream(inputStream);
        ZipEntry entry = zipIn.getNextEntry();
        // iterates over entries in the zip file
        while (entry != null) {
            String filePath = destDirectory + File.separator + entry.getName();
            if (!entry.isDirectory()) {
                // if the entry is a file, extracts it
                extractFile(zipIn, filePath);
            } else {
                // if the entry is a directory, make the directory
                File dir = new File(filePath);
                if (!dir.exists())
                dir.mkdirs();
            }
            zipIn.closeEntry();
            entry = zipIn.getNextEntry();
        }
        zipIn.close();
    }

    private static void extractFile(ZipInputStream zipIn, String filePath) {
        try {
            //if (new File(filePath).exists()) {
                BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(filePath));
                byte[] bytesIn = new byte[4096];
                int read = 0;
                while ((read = zipIn.read(bytesIn)) != -1) {
                    bos.write(bytesIn, 0, read);
                }
                bos.close();
           // }
        }catch (Exception er){
            Utility.printStack(er);
        }
    }

    private static String[] sdkFiles={"/storage/emulated/0/share/face_sdk_211/builder_211.bin",
            "/storage/emulated/0/share/face_sdk_211/face_detector_211.bin",
            "/storage/emulated/0/share/face_sdk_211/icao_100.bin",
            "/storage/emulated/0/share/face_sdk_211/matcher/211_in.bin",
            "/storage/emulated/0/share/face_sdk_211/quality/100_glass_smile_occlusion.bin", //glasses for 1.8.2 +
            "/storage/emulated/0/share/face_sdk_211/quality/100_mask_light.bin"};
    private static int totalFiles = 6;
    private static int filesCount = 0;
    private static boolean getAllDirs(File[] dir){
        if (dir.length>0) {
            for (int f=0;f<dir.length;f++){
                if (dir[f].isDirectory()){
                    getAllDirs(dir[f].listFiles());
                }else if (dir[f].isFile()){
                    String filePath=dir[f].getParent()+"_"+dir[f].getName();
                    //Log.d("TECH5SDK",filePath);
                    for (int i=0;i<sdkFiles.length;i++){
                        if (sdkFiles[i].equalsIgnoreCase(filePath)){
                            filesCount++;
                        }else{
                            Log.d("TAG","file not found "+filePath);
                        }
                    }
                }
            }
        }
        Log.d("TAG","total files "+filesCount);
        return (filesCount==totalFiles);
    }

    public static boolean isLibs(String dirName){
        filesCount=0;
        String destDir = Environment.getExternalStorageDirectory() + File.separator + dirName;
        File dir = new File(destDir);
        if (dir.exists()) {
           return getAllDirs(dir.listFiles());
        }
        return false;
    }

    public static  boolean deleteSDKDir(String dirName) {

        try {
            String filepath = Environment.getExternalStorageDirectory()+File.separator+".caratemp/liveness.dat";
            File spoof = new File(filepath);
            if (spoof.exists()) {
                spoof.delete();
            }
        }catch (Exception er){}

        try {
            String filepath = Environment.getExternalStorageDirectory()+File.separator+".caratemp/liveness.bin";
            File liveness = new File(filepath);
            if (liveness.exists()) {
                liveness.delete();
            }
        }catch (Exception er){}

        String destDir = Environment.getExternalStorageDirectory() + File.separator + dirName;
        File dir = new File(destDir);
        if (dir.exists()) {
            try {
                return AppUtils.deleteFileOrDirectory(dir);
            } catch (Exception e) {
                Utility.printStack(e);
                return false;
            }
        }

        return false;
    }


    public static String getSystemArchitecture(){

        String arch = System.getProperty("os.arch");

        return arch;
    }

    public static void copyFilesFromAssets(Context context,String srcDir, String distDir) throws IOException
    {
        //File sd_path = Environment.getExternalStorageDirectory();
        //String dest_dir_path = sd_path + addLeadingSlash(distDir);

        File dest_dir = new File(distDir);

       if (!dest_dir.exists()){
           if (!dest_dir.isDirectory()){
               dest_dir.mkdir();
               Log.d("TAG","file created");
           }
       }

        AssetManager asset_manager = context.getApplicationContext().getAssets();
        String[] files = asset_manager.list(srcDir);

        for (int i = 0; i < files.length; i++)
        {
              Log.d("TAG",files[i]);
        }

    }


    public void copyAssetFile(String assetFilePath, String destinationFilePath,Context context) throws IOException
    {
        InputStream in = context.getApplicationContext().getAssets().open(assetFilePath);
        OutputStream out = new FileOutputStream(destinationFilePath);

        byte[] buf = new byte[1024];
        int len;
        while ((len = in.read(buf)) > 0)
            out.write(buf, 0, len);
        in.close();
        out.close();
    }

    public String addTrailingSlash(String path)
    {
        if (path.charAt(path.length() - 1) != '/')
        {
            path += "/";
        }
        return path;
    }

    public String addLeadingSlash(String path)
    {
        if (path.charAt(0) != '/')
        {
            path = "/" + path;
        }
        return path;
    }

    public static void createDir(File dir) throws IOException
    {
        if (dir.exists())
        {
            if (!dir.isDirectory())
            {
                throw new IOException("Can't create directory, a file is in the way");
            }
        } else
        {
            dir.mkdirs();
            if (!dir.isDirectory())
            {
                throw new IOException("Unable to create directory");
            }
        }
    }

}
