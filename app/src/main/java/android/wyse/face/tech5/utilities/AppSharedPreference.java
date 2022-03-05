package android.wyse.face.tech5.utilities;

import android.content.Context;
import android.content.SharedPreferences;
import android.preference.PreferenceManager;

public class AppSharedPreference {
    private SharedPreferences sharedPref;
    private SharedPreferences.Editor editor;
    private String IS_RESOURCE_DELETED = "IS_RESOURCE_DELETED";
    private String IS_FIRST_TIME_INSTALLED = "IS_FIRST_TIME_INSTALLED";
    private String BASE_URL = "BASE_URL";

    private static final String FACE_THRESHOLD = "FACE_THRESHOLD";
    private static final String BUILDER = "builder";
    private static final float DEFAULT_FACE_THRESHOLD = 6.0f;
    private static final float DEFAULT_DETECTOR_CONFIDENCE = 0.9f;
    private static final String DETECTOR_CONFIDENCE = "DETECTOR_CONFIDENCE";
    private Context context = null;
    private String APP_VERSION_CODE="APP_VERSION_CODE";

    public AppSharedPreference(Context context) {
        this.context = context;
        sharedPref = PreferenceManager.getDefaultSharedPreferences(context);
    }

    public void setResourceDeleted(boolean isResourceDeleted) {
        editor = sharedPref.edit();
        editor.putBoolean(IS_RESOURCE_DELETED, isResourceDeleted);
        editor.apply();
    }

    public boolean isResourceDeleted() {
        return (sharedPref.getBoolean(IS_RESOURCE_DELETED, false));
    }

    public void setFirstTimeInstalled(boolean isFirstTimeInstalled) {
        editor = sharedPref.edit();
        editor.putBoolean(IS_FIRST_TIME_INSTALLED, isFirstTimeInstalled);
        editor.apply();
    }

    public boolean isFirstTimeInstalled() {
        return (sharedPref.getBoolean(IS_FIRST_TIME_INSTALLED, false));
    }

    public void setBaseUrl(String baseUrl) {
        editor = sharedPref.edit();
        editor.putString(BASE_URL, baseUrl);
        editor.apply();
    }


    public void setFaceThreshold(float faceThreshold) {

        editor = sharedPref.edit();
        editor.putFloat(FACE_THRESHOLD, faceThreshold);
        editor.apply();

    }

    public float getDetectorConfidence() {
        return sharedPref.getFloat(DETECTOR_CONFIDENCE, DEFAULT_DETECTOR_CONFIDENCE);
    }

    public void setDetectorConfidence(float detectorConfidence) {

        editor = sharedPref.edit();
        editor.putFloat(DETECTOR_CONFIDENCE, detectorConfidence);
        editor.apply();

    }

    public float getFaceThreshold() {
        return sharedPref.getFloat(FACE_THRESHOLD, DEFAULT_FACE_THRESHOLD);
    }


    public void setBuilder(String builder){

        editor = sharedPref.edit();
        editor.putString(BUILDER, builder);
        editor.apply();

    }
    public String getBuilder() {
        String builderDefault= "211";
        return sharedPref.getString(BUILDER, builderDefault);
    }



    public int getAppVersionCode(){
        return (sharedPref.getInt(APP_VERSION_CODE, 0));
    }

    public void setVersionCode(int versionCode){
        editor = sharedPref.edit();
        editor.putInt(APP_VERSION_CODE, versionCode);
        editor.apply();
    }


    public void clear() {
        editor = sharedPref.edit();
        editor.clear();
        editor.apply();
    }
}
