package android.wyse.face.tech5.utilities;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.hardware.Camera;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Base64;
import android.view.Surface;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;


public class AppUtils {
    private Context context;

    private AppUtils() {
    }

    public AppUtils(Context context) {
        this.context = context;
    }

    public static byte[] getByteArray(String inputBase64) {
        return Base64.decode(inputBase64, Base64.DEFAULT);
    }

    public static String getBase64(byte[] inputByteArray) {
        return Base64.encodeToString(inputByteArray,
                Base64.DEFAULT);
    }

    public static byte[] flip(byte[] image) {
        //android:scaleX="-1" To flip horizontally or
        //android:scaleY="-1" To flip vertically
        // create new matrix for transformation
        Bitmap src=convertByteArrayToBitmap(image);
        Matrix matrix = new Matrix();
        matrix.preScale(-1.0f, 1.0f);
        // return transformed image
        return convertBitmapToByteArray(Bitmap.createBitmap(src, 0, 0, src.getWidth(), src.getHeight(), matrix, true));
    }

    public static Bitmap convertBase64ToBitmap(String b64) {
        byte[] imageAsBytes = Base64.decode(b64.getBytes(), Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }

    public static byte[] convertBitmapToByteArray(Bitmap bitmap) {
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        byte[] byteArray = stream.toByteArray();
        bitmap.recycle();
        return byteArray;
    }

    public static Bitmap convertByteArrayToBitmap(byte[] imageAsBytes) {
        return BitmapFactory.decodeByteArray(imageAsBytes, 0, imageAsBytes.length);
    }

    public static void showMessage(Context context, String message) {
        ((Activity) context).runOnUiThread(() -> Toast.makeText(context, message, Toast.LENGTH_LONG).show());
    }

    public static String getDateTime() {
        DateFormat dateFormat = new SimpleDateFormat("yyyy_MM_dd_HH_mm_ss", Locale.US);
        Date date = new Date();
        return dateFormat.format(date);
    }

    public String getUserDirectoryName() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getString("Tech5_user", "anonymous") + "_" + sharedPreferences.getString("Tech5_user_created_ts", "anonymous");
    }

    public int incrementUserEnrollCount() {
        int c = getUserEnrollCount();
        c += 1;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putInt("enroll_c", c).apply();
        return c;
    }

    public int getUserEnrollCount() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("enroll_c", -1);
    }

    public int incrementFailedImagesCount() {
        int c = getFailedImagesCount();
        c += 1;
        SharedPreferences pref = PreferenceManager.getDefaultSharedPreferences(context);
        pref.edit().putInt("FAILED_IMAGES_c", c).apply();
        return c;
    }

    public int getFailedImagesCount() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(context);
        return sharedPreferences.getInt("FAILED_IMAGES_c", -1);
    }

    public static boolean isValidEmail(String email) {
        return !TextUtils.isEmpty(email) && android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches();
    }

//    public static int[] getOptimalDimensions(float mediaWidth, float mediaHeight, int width, int height) {
//        int layoutWidth = width;
//        int layoutHeight = height;
//        float ratioWidth = layoutWidth / mediaWidth;
//        float ratioHeight = layoutHeight / mediaHeight;
//        float aspectRatio = mediaWidth / mediaHeight;
//        if (ratioWidth > ratioHeight) {
//            layoutWidth = (int) (layoutHeight * aspectRatio);
//        } else {
//            layoutHeight = (int) (layoutWidth / aspectRatio);
//        }
//        Log.i(TAG, "layoutWidth: " + layoutWidth);
//        Log.i(TAG, "layoutHeight: " + layoutHeight);
//        Log.i(TAG, "aspectRatio: " + aspectRatio);
//        return new int[]{layoutWidth, layoutHeight};
//    }
//
//    public static byte[] rotateBitmap(byte[] inputByteArray) throws IOException {
//        Matrix mat = new Matrix();
//        mat.postRotate(getOrientation(inputByteArray));
//        Bitmap bitmap = BitmapFactory.decodeByteArray(inputByteArray, 0, inputByteArray.length);
//        bitmap = Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), mat, true);
//        ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, outputStream);
//        return outputStream.toByteArray();
//    }

    public static byte[] rotateImageData(Activity activity, byte[] data, int cameraId) throws Exception {
        Bitmap imageBitmap = null;
        // COnverting ByteArray to Bitmap - >Rotate and Convert back to Data
        if (data != null) {
            imageBitmap = BitmapFactory.decodeByteArray(data, 0, (data != null) ? data.length : 0);
            if (activity.getResources().getConfiguration().orientation == Configuration.ORIENTATION_PORTRAIT) {
                Matrix mtx = new Matrix();
                int cameraEyeValue = setPhotoOrientation(activity, cameraId); // CameraID = 1 : front 0:back
                if (cameraId == 1) { // As Front camera is Mirrored so Fliping the Orientation
                    if (cameraEyeValue == 270) {
                        mtx.postRotate(90);
                    } else if (cameraEyeValue == 90) {
                        mtx.postRotate(270);
                    }
                } else {
                    mtx.postRotate(cameraEyeValue); // cameraEyeValue is default to Display Rotation
                }
                imageBitmap = Bitmap.createBitmap(imageBitmap, 0, 0, imageBitmap.getWidth(), imageBitmap.getHeight(), mtx, true);
            } else {// LANDSCAPE MODE
                //No need to reverse width and height
                Bitmap scaled = Bitmap.createScaledBitmap(imageBitmap, imageBitmap.getWidth(), imageBitmap.getHeight(), true);
                imageBitmap = scaled;
            }
        }
        // Converting the Die photo to Bitmap
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        imageBitmap.compress(Bitmap.CompressFormat.JPEG, 100, stream);
        return stream.toByteArray();
    }

    private static int setPhotoOrientation(Activity activity, int cameraId) {
        android.hardware.Camera.CameraInfo info = new android.hardware.Camera.CameraInfo();
        android.hardware.Camera.getCameraInfo(cameraId, info);
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
        // do something for phones running an SDK before lollipop
        if (info.facing == Camera.CameraInfo.CAMERA_FACING_FRONT) {
            result = (info.orientation + degrees) % 360;
            result = (360 - result) % 360; // compensate the mirror
        } else { // back-facing
            result = (info.orientation - degrees + 360) % 360;
        }

        return result;
    }

    public static boolean deleteFileOrDirectory(File fileOrDirectory) {
        if (fileOrDirectory.isDirectory()) {
            for (File child : fileOrDirectory.listFiles()) {
                deleteFileOrDirectory(child);
            }
        }
        return fileOrDirectory.delete();
        //return true;
    }

}
