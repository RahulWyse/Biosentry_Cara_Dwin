package android.wyse.face;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ObjectAnimator;
import android.animation.PropertyValuesHolder;
import android.animation.ValueAnimator;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Matrix;
import android.graphics.Paint;
import android.graphics.PixelFormat;
import android.graphics.RectF;
import android.graphics.Shader;
import android.net.ConnectivityManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.util.Base64;
import android.view.Gravity;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.AccelerateInterpolator;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.DecelerateInterpolator;
import android.widget.ImageView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.security.NoSuchAlgorithmException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;

public class Utility {


  private static WindowManager manager;
  private static customViewGroup bottomView;

  public Utility(){}

  public static void disablePullNotificationTouch(Context context) {
    try {
              manager = ((WindowManager) context.getSystemService(Context.WINDOW_SERVICE));
              WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
              localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
              localLayoutParams.gravity = Gravity.TOP;
              localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
                // this is to enable the notification to recieve touch events
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
                // Draws over status bar
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;

              localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;
              localLayoutParams.height = (int) (25 * context.getResources().getDisplayMetrics().scaledDensity);
              localLayoutParams.format = PixelFormat.TRANSPARENT;

              bottomView = new customViewGroup(context);
              //        manager.addView(view, localLayoutParams);
              try {
                manager.addView(bottomView, localLayoutParams);
              } catch (Exception er) { }
    }catch(Exception er){}

  }

  //added by sonu auti on 18/08/2018 to remove added views from main screen
  public void disableTop(){
    try {
      if (manager != null && bottomView != null) {
        manager.removeViewImmediate(bottomView);
      }
    }catch (Exception er){}
  }

  //Add this class in your project
  public static class customViewGroup extends ViewGroup {
    public customViewGroup(Context context) {
      super(context);
    }

    @Override
    public void onLayout(boolean changed, int l, int t, int r, int b) {
    }

    @Override
    public boolean onInterceptTouchEvent(MotionEvent ev) {
      return true;
    }}

  private static WindowManager manager_;
  private static customViewGroup view_;

  public static void disableBottomButtons(Context context){
    manager_ = ((WindowManager)   context.getSystemService(Context.WINDOW_SERVICE));
    WindowManager.LayoutParams localLayoutParams_ = new WindowManager.LayoutParams();
    if (Build.VERSION.SDK_INT>=28) {
      localLayoutParams_.type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY;
    }else {
      localLayoutParams_.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
    }
    localLayoutParams_.gravity = Gravity.BOTTOM;
    localLayoutParams_.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE |
      // this is to enable the notification to recieve touch events
      WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL |
      // Draws over status bar
      WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN | WindowManager.LayoutParams.FLAG_FULLSCREEN;

    localLayoutParams_.width = WindowManager.LayoutParams.MATCH_PARENT;
    localLayoutParams_.height = (int) (40 * context.getResources().getDisplayMetrics().scaledDensity);
    localLayoutParams_.format = PixelFormat.TRANSPARENT;
    view_ = new customViewGroup(context);
    manager_.addView(view_, localLayoutParams_);
  }

  /*public static SharedPreferences getSharedPref(Context context){
      return context.getSharedPreferences(CaraManager.PREF_FILE_NAME,Context.MODE_PRIVATE);
  }*/

  public static boolean writeSharedPref(SharedPreferences sharedPref,String key,String value){
      SharedPreferences.Editor editor = sharedPref.edit();
      editor.putString(key, value);
      return editor.commit();
  }

  public static void setFadeAnimation(View view) {

    Animation fadeIn = new AlphaAnimation(0, 1);
    fadeIn.setInterpolator(new DecelerateInterpolator()); //add this
    fadeIn.setDuration(500);

    Animation fadeOut = new AlphaAnimation(1, 0);
    fadeOut.setInterpolator(new AccelerateInterpolator()); //and this
    fadeOut.setStartOffset(500);
    fadeOut.setDuration(500);

    view.startAnimation(fadeOut);
    view.startAnimation(fadeIn);

  }

  public static ArrayList<String> getLanguages(){
    ArrayList<String> langs= new ArrayList<>();
    langs.add("SELECT LANGUAGE");
    langs.add("English");
    langs.add("Hindi");
    return langs;
  }

  public static String getUUID(Context context){
    return Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
  }

  public static void printStack(Exception er){
      //writeToFile(er.getLocalizedMessage()+"\n","log.txt",true);
      if (!CaraManager.getInstance().isProd())
      er.printStackTrace();
  }

  public static void removeBottomButtons(){
    try {
      if (view_ != null && manager_ != null) {
        //manager_.removeView(view_);
        manager_.removeViewImmediate(view_);
      }
    }catch (Exception er){}
  }

  public static byte[] decodeBase64(String data){
    return Base64.decode(data,Base64.DEFAULT);
  }

  public static String encodeBase64(byte[] data){
    return Base64.encodeToString(data, 0);
  }

  public static String encodeBase64(String data){
    // encode data on your side using BASE64
    // System.out.println("ecncoded value is " + bytesEncoded);
    return Base64.encodeToString(data.getBytes(), 0);
  }

  public static void animateObject(View view,float value){
    ObjectAnimator textViewAnimator = ObjectAnimator.ofFloat(view, "translationY",value);
    textViewAnimator.setDuration(1000);
    textViewAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    textViewAnimator.start();
  }

  public static void FadeView(View view){
    ObjectAnimator textViewAnimator =ObjectAnimator.ofFloat(view, "alpha", 0.0F, 1.0F);
      textViewAnimator.setDuration(300L);
      textViewAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
      textViewAnimator.start();
  }

  public static void FadeOutView(View view){
    ObjectAnimator textViewAnimator =ObjectAnimator.ofFloat(view, "alpha", 0.0F, 4.0F);
    textViewAnimator.setDuration(1500L);
    textViewAnimator.setInterpolator(new AccelerateDecelerateInterpolator());
    textViewAnimator.start();
  }


  public static String encodeImage(byte[] imageByteArray) {
    return Base64.encodeToString(imageByteArray, Base64.DEFAULT);
  }



  public static String getFilePath(String fileName){
    try {
      String root = Environment.getExternalStorageDirectory().getAbsolutePath();
      File myDir = new File(root + dirPath);
      return new File(myDir,fileName).getAbsolutePath();
    } catch (Exception e) {
      //e.printStackTrace();
    }
    return "";
  }

  public static String getFilePath(String fileName,String dirPath){
    try {
      return new File(dirPath,fileName).getAbsolutePath();
    } catch (Exception e) {
        Utility.printStack(e);
    }
    return "";
  }

  private static final String dirPath="/faces";
  public static void writeToFile(String data, String name,boolean isAppend){
    try {
      String root = Environment.getExternalStorageDirectory().getAbsolutePath();
      File myDir = new File(root + dirPath);
      if (!myDir.exists()){  myDir.mkdir();  }
      File file = new File(myDir, name);
      if (!file.exists()){file.createNewFile();  }
      FileOutputStream fileOutputStream = new FileOutputStream(file,isAppend);
      OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
      writer.append(data);
      writer.close();
      fileOutputStream.close();
    } catch (Exception e) {
      Utility.printStack(e);
    }
  }

  public static String convertToBase64(Bitmap bitmap,int type,int compress) {
    ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
    bitmap.compress(Bitmap.CompressFormat.JPEG, compress, byteArrayOutputStream);
    byte[] byteArray = byteArrayOutputStream.toByteArray();
    return Base64.encodeToString(byteArray, type);
  }

  public static byte[] convertBitmapToByteArray(Context context, Bitmap bitmap) {
    ByteArrayOutputStream buffer = new ByteArrayOutputStream(bitmap.getWidth() * bitmap.getHeight());
    bitmap.compress(Bitmap.CompressFormat.JPEG, 100, buffer);
    return buffer.toByteArray();
  }

  public static boolean isInternetConnected(Context context) {
      ConnectivityManager connectivityManager = (ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
      return connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
  }

  public static String getCurrentTimeStamp() {
    try {
      SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
      return dateFormat.format(new Date());
    } catch (Exception var2) {
      var2.printStackTrace();
      return null;
    }
  }



  public static Bitmap getScaledDownBitmap(Bitmap bitmap, int threshold, boolean isNecessaryToKeepOrig) {
    int width = bitmap.getWidth();
    int height = bitmap.getHeight();
    int newWidth = width;
    int newHeight = height;
    if (width > height && width > threshold) {
      newWidth = threshold;
      newHeight = (int)((float)height * (float)threshold / (float)width);
    }

    if (width > height && width <= threshold) {
      return bitmap;
    } else {
      if (width < height && height > threshold) {
        newHeight = threshold;
        newWidth = (int)((float)width * (float)threshold / (float)height);
      }

      if (width < height && height <= threshold) {
        return bitmap;
      } else {
        if (width == height && width > threshold) {
          newWidth = threshold;
          newHeight = threshold;
        }

        return width == height && width <= threshold ? bitmap : getResizedBitmap(bitmap, newWidth, newHeight, isNecessaryToKeepOrig);
      }
    }
  }

  public static Bitmap getResizedBitmap(Bitmap bm, int newWidth, int newHeight, boolean isNecessaryToKeepOrig) {
    int width = bm.getWidth();
    int height = bm.getHeight();
    float scaleWidth = (float)newWidth / (float)width;
    float scaleHeight = (float)newHeight / (float)height;
    Matrix matrix = new Matrix();
    matrix.postScale(scaleWidth, scaleHeight);
    Bitmap resizedBitmap = Bitmap.createBitmap(bm, 0, 0, width, height, matrix, false);
    if (!isNecessaryToKeepOrig) {
      bm.recycle();
    }

    return resizedBitmap;
  }

  public static void animateScale(View view,float value){
    ObjectAnimator scaleDown = ObjectAnimator.ofPropertyValuesHolder(view,
      PropertyValuesHolder.ofFloat("scaleX", value),
      PropertyValuesHolder.ofFloat("scaleY", value));
    scaleDown.setDuration(500);
    scaleDown.start();
  }
}
