package android.wyse.face;

import static com.androidnetworking.common.Priority.MEDIUM;

import android.content.Context;
import android.content.res.AssetFileDescriptor;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;

import com.androidnetworking.AndroidNetworking;
import com.androidnetworking.BuildConfig;
import com.androidnetworking.error.ANError;
import com.androidnetworking.interfaces.JSONObjectRequestListener;

import org.apache.http.conn.ssl.StrictHostnameVerifier;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.UnknownHostException;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManagerFactory;


interface HelperCallback {
    void onResult(String result);
    void onError(String result);
}

public class Helper {

    private final static String TENENT_CODE = "";
    private final static String ACCESS_KEY = "";

    public static String NONCE = "";
    public static String SERVER_DNS = "ws.attendanceportal.com";
    public static String SERVER_IP = "115.124.109.47";
    public static int SERVER_PORT = 5004;
    public static String urlSchme = "https";
    public static String KIOSK_HASH = "";
    public static String POST_REQUEST = "POST";
    public static String GET_REQUEST = "GET";

    public static boolean SHOW_LOG = true;

    final static String SETTING_CAMERA = "camid";
    final static String SETTING_ACCESS_CONTROL = "isLatch";
    final static String SETTING_ACCESS_TIMEOUT = "timeout";
    final static String SETTING_PHOTO_TIMEOUT = "capturetime";
    final static String SETTING_LANG = "lang";
    final static String SETTING_IS_PRINTER = "isPrint";
    final static String SETTING_PRINTER_MAC_ID = "print_mac";
    final static String SETTING_LATCH_MAC = "latch_mac";
    final static String SETTING_AUDIO = "isAudio";
    final static String SETTING_IS_ACTIVE = "isActive";
    final static String SETTING_IS_LATCH = "isLatch";
    final static String SETTING_DNS = "dns";
    final static String SETTING_LATCHTYPE = "latch_type";
    final static String SETTING_PRINTER_TYPE = "printer_type";
    final static String SETTING_MAC = "device_mac";
    final static String SETTING_INMODE = "inmode";
    final static String SETTING_OUTMODE = "outmode";
    final static String LAST_REBOOT = "last_reboot";
    final static String SETTING_ISREBOOT = "isReboot";
    final static String SETTING_REBOOT_HHMM = "reboot_hhmm";
    final static String SETTING_ISQUALITY = "isquality";
    final static String SETTING_AUTOFLASH = "autoflash";
    final static String SETTING_THERMAL = "thermal_temp";
    final static String SETTING_ISDEGREE = "isdegree";
    final static String SETTING_MAXTEMP = "maxtemp";
    final static String SETTING_PLAYALARM = "playalarm";
    final static String SETTING_ISCENTER="isCenter";
    final static String SETTING_MINTEMP="mintemp";
    final static String SETTING_SCALE_TEMP = "scalevalue";
    final static String SETTING_ISLIVENESS="isliveness";
    final static String SETTING_URLTYPE="urlscheme";
    final static String SETTING_FACE_THR="face_thr";
    final static String SETTING_SHIFT_TIME="shifttime";
    final static String SETTING_RESET_TIME="resettime";
    final static String SETTING_FLIP_TIMEOUT="flip_timeout";
    final static String SETTING_ENROL_QUALITY="enrol_img_qlty";
    final static String SETTING_LIVENESS_THR="liveness_value";
    final static String TRUE = "true";
    final static String FALSE = "false";


    public static int CAMERA_CAPTURE_TIME = 40;
    public static int TIMEOUT_ACTIVITY = 15;

    private static String AURAL_LANG = "English";
    private static boolean isVoiceOver = true;

    final static String COMMAND_ADD = "add";
    final static String COMMAND_REMOVE = "remove";
    final static String COMMAND_HEALTH = "health";
    final static String COMMAND_REBOOT="reboot";
    final static String COMMAND_IMG_UPLOAD="upload_img_data";
    final static String COMMAND_FLUSH_TEMP = "flush_templates";
    final static String COMMAND_VERIFY_DNS = "verify_dns";
    final static String COMMAND_APP_UPDATE="app_update";
    final static String COMMAND_UPDATE_SETTINGS="update_settings";
    final static String COMMAND_GET_USERS="user_list";

    public static String getTech5Token() {
        return "b0a7c216e09938367fcd2f072285f62d53eefb2a48baf8835415b965e029ca20";
    }

    public String getDefaultResp(String value, String key, String msg) {
        try {
            JSONObject output = new JSONObject();
            output.put(key, value);
            output.put("msg", msg);
            return output.toString();
        } catch (Exception er) { }
        return "";
    }


    final static String AURAL_WELCOME = "WELCOME";
    final public static String AURAL_PHOTO = "PHOTO";
    final static String AURAL_GRANT = "GRANT";
    final static String AURAL_DENIED = "DENIED";
    final public static String AURAL_NOT_MATCHED = "AURAL_NOT_MATCHED";
    final public static String AURAL_TEMP_NORMAL = "TEMP_NORMAL";
    final public static String AURAL_TEMP_HIGH = "TEMP_HIGH";
    final public static String AURAL_COME_CLOSE = "AURAL_COME_CLOSE";
    final public static String AURAL_MARK_IN="AURAL_MARK_IN";
    final public static String AURAL_MARK_OUT="AURAL_MARK_OUT";

    public static void setAuralLang(String lang) {
        AURAL_LANG = lang;
    }

    public static String getAuralLang() {
        return AURAL_LANG;
    }

    public static void setVoiceOverOn(boolean flag) {
        isVoiceOver = flag;
    }

    private static boolean isIsVoiceOverOn() {
        return isVoiceOver;
    }


    private static void getMediaPlayer() {
        if (playtone == null) {
            playtone = new MediaPlayer();
            playtone.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
                @Override
                public void onPrepared(MediaPlayer mediaPlayer) {
                    if (playtone != null) {
                        try {
                            if (isIsVoiceOverOn()) {
                                mediaPlayer.setLooping(false);
                                mediaPlayer.start();
                            }
                        } catch (Exception er) {
                        }
                    }
                }
            });
        } else {
            if (playtone.isPlaying()) {
                playtone.stop();
                playtone.reset();
            } else {
                playtone.reset();
            }
        }
    }

    private static MediaPlayer playtone;
    public static void getAuralFeedback(Context context, String lang, String play_tag) {

        if (isIsVoiceOverOn()) {

            String indhin = "hindi/";
            String enus = "enus/";

            try {
                getMediaPlayer();
            } catch (Exception er) {
            }


            String AURAL_LANG = enus;
            switch (lang) {
                case "Hindi":
                    AURAL_LANG = indhin;
                    break;
                case "English":
                    AURAL_LANG = enus;
                    break;
            }

            AssetFileDescriptor afd;
            try {
                switch (play_tag) {
                    case AURAL_WELCOME:
                        afd = context.getAssets().openFd(AURAL_LANG + "welcome.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_NOT_MATCHED:
                        afd = context.getAssets().openFd(AURAL_LANG + "tryagain.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_PHOTO:
                        afd = context.getAssets().openFd(AURAL_LANG + "wait_for_photo.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_GRANT:
                        afd = context.getAssets().openFd(AURAL_LANG + "access_granted.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_TEMP_NORMAL:
                        afd = context.getAssets().openFd(AURAL_LANG + "temp_normal.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_TEMP_HIGH:
                        afd = context.getAssets().openFd(AURAL_LANG + "temp_high.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_DENIED:
                        afd = context.getAssets().openFd(AURAL_LANG + "access_denied.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_COME_CLOSE:
                        afd = context.getAssets().openFd(AURAL_LANG + "come_close.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_MARK_IN:
                        afd = context.getAssets().openFd(AURAL_LANG + "mark_in.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    case AURAL_MARK_OUT:
                        afd = context.getAssets().openFd(AURAL_LANG + "mark_out.m4a");
                        playtone.setDataSource(afd.getFileDescriptor(), afd.getStartOffset(), afd.getLength());
                        afd.close();
                        break;
                    default:
                        break;
                }
                playtone.prepare();
            } catch (IllegalStateException | IOException er) {
                //  er.printStackTrace();
                if (playtone != null) {
                    playtone.release();
                    playtone = null;
                }
            }
        }
        //return playtone;
    }

    public static void stopAuralFb() {
        try {
            if (playtone != null) {
                if (playtone.isPlaying()) {
                    playtone.stop();
                }
            }
        } catch (Exception er) {
            playtone = null;
        }
    }

    /**
     * @param context        - app context
     * @param method         - request method type GET, POST
     * @param requestType    - http or https
     * @param endpoint       - url to connect
     * @param jdata          - json object data in string format
     * @param helperCallback - result call back
     */
    public void networkRequst(final Context context, final String method, final String requestType,
                              final String endpoint, final String jdata, final HelperCallback helperCallback) {
        //Log.d("cara_logs","url ->"+endpoint);
       // Log.d("cara_logs","post data-> "+jdata);
        Thread networkTh = new Thread(new Runnable() {
            @Override
            public void run() {
                try {

                    if (Utility.isInternetConnected(context)) {

                        if (requestType.equals("https")) {
                            HttpsURLConnection urlConnection;

                            //Connect
                            urlConnection = (HttpsURLConnection) ((new URL(endpoint).openConnection()));

                            if (method.equals("POST")) {
                                urlConnection.setRequestProperty("Content-Type", "application/json");
                                urlConnection.setRequestProperty("Accept", "application/json");
                                urlConnection.setDoOutput(true);
                                urlConnection.setDoInput(true);
                            }

                            urlConnection.setConnectTimeout(10000);
                            urlConnection.setRequestMethod(method);

                            urlConnection.connect();


                            if (method.equals("POST")) {
                                // Log.d("requestMethod", urlConnection.getRequestMethod());
                                OutputStream os = urlConnection.getOutputStream();
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                                writer.write(jdata);
                                writer.flush();
                                writer.close();
                            }

                           // Log.d("cara_logs", endpoint + " - " + urlConnection.getResponseCode());

                            int respCode = urlConnection.getResponseCode();
                            if (respCode == HttpsURLConnection.HTTP_OK) {
                                InputStreamReader inputStream = new InputStreamReader(urlConnection.getInputStream());
                                BufferedReader br = new BufferedReader(inputStream);
                                StringBuilder builder = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    builder.append(line);
                                }
                                br.close();
                                final String response = builder.toString();
                                //Log.d("cara_logs", response);
                                if (!response.equals("")) {
                                    helperCallback.onResult(response.trim());
                                } else {
                                    helperCallback.onError("Error in request");
                                }
                            } else {
                                helperCallback.onError(respCode+"");
                            }
                        } else {
                            HttpURLConnection urlConnection;

                            //Connect
                            urlConnection = (HttpURLConnection) ((new URL(endpoint).openConnection()));
                            //urlConnection.setDoOutput(true);
                            urlConnection.setConnectTimeout(10000);
                            //urlConnection.setRequestProperty("Content-Type", "application/json");
                            //urlConnection.setRequestProperty("Accept", "application/json");
                            urlConnection.setRequestMethod(method);
                            //urlConnection.setDoInput(true);

                            if (method.equals("POST")) {
                                urlConnection.setRequestProperty("Content-Type", "application/json");
                                urlConnection.setRequestProperty("Accept", "application/json");
                                urlConnection.setDoOutput(true);
                                urlConnection.setDoInput(true);
                            }

                            urlConnection.connect();

                            if (method.equals("POST")) {
                                // Log.d("requestMethod", urlConnection.getRequestMethod());
                                OutputStream os = urlConnection.getOutputStream();
                                BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(os, "UTF-8"));
                                writer.write(jdata);
                                writer.flush();
                                writer.close();
                            }

                           //Log.d("cara_logs", endpoint + " - " + urlConnection.getResponseCode());

                            int respCode = urlConnection.getResponseCode();
                            if (respCode == HttpsURLConnection.HTTP_OK) {
                                InputStreamReader inputStream = new InputStreamReader(urlConnection.getInputStream());
                                BufferedReader br = new BufferedReader(inputStream);
                                StringBuilder builder = new StringBuilder();
                                String line;
                                while ((line = br.readLine()) != null) {
                                    builder.append(line);
                                }
                                br.close();
                                final String response = builder.toString();
                                //Log.d("cara_logs", response);
                                if (!response.equals("")) {
                                    helperCallback.onResult(response);
                                } else {
                                    helperCallback.onError("Error in request");
                                }
                            } else {
                                helperCallback.onError(respCode+"");
                            }
                        }

                        CaraManager.getInstance().setNetworkError(false);

                    } else {
                        CaraManager.getInstance().setNetworkError(true);
                        helperCallback.onError("No Internet connection !");
                    }
                } catch (UnknownHostException er) {
                    //er.printStackTrace();
                    helperCallback.onError(er.getMessage());
                    CaraManager.getInstance().setNetworkError(true);
                    Utility.printStack(er);
                }catch (Exception er){
                    helperCallback.onError(er.getMessage());
                   // CaraManager.getInstance().setNetworkError(true);
                    Utility.printStack(er);
                }finally {
                    //
                }
            }
        });
        networkTh.setPriority(Thread.NORM_PRIORITY);
        networkTh.start();

    }


    public  HttpsURLConnection setUpHttpConnection(Context context, String urlString) throws CertificateException, KeyStoreException, IOException, NoSuchAlgorithmException, KeyManagementException {
        URL url = new URL(urlString);
        Object urlConnection;
        if (Helper.urlSchme.contains("https")) {

            CertificateFactory cf = CertificateFactory.getInstance("X.509");
            InputStream caInput = new BufferedInputStream(context.getAssets().open("server.pem"));
            // InputStream caInput = new BufferedInputStream(context.getAssets().open("idemiapubkey.pub"));
            Certificate ca = cf.generateCertificate(caInput);
            String keyStoreType = KeyStore.getDefaultType();

            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
            keyStore.load(null, null);
            keyStore.setCertificateEntry("ca", ca);
            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
            tmf.init(keyStore);
            SSLContext sslContext = SSLContext.getInstance("TLS");
            sslContext.init(null, tmf.getTrustManagers(), null);

            urlConnection = url.openConnection();
            ((HttpsURLConnection) urlConnection).setSSLSocketFactory(sslContext.getSocketFactory());
            ((HttpsURLConnection) urlConnection).setHostnameVerifier(new StrictHostnameVerifier());
            HttpsURLConnection httpsURLConnection = ((HttpsURLConnection) urlConnection);

            httpsURLConnection.setRequestProperty("Content-Type", "application/json");
            httpsURLConnection.setRequestProperty("Accept", "application/json");
            httpsURLConnection.setRequestProperty("tenantCode", Helper.TENENT_CODE);
            httpsURLConnection.setRequestProperty("Authorization", Helper.ACCESS_KEY);

            //httpsURLConnection.setRequestProperty("OTT_TOKEN", NONCE);

            return httpsURLConnection;
        } else {
            urlConnection = url.openConnection();
        }

        ((HttpsURLConnection) urlConnection).setHostnameVerifier(new StrictHostnameVerifier());
        HttpsURLConnection httpsURLConnection = ((HttpsURLConnection) urlConnection);
        httpsURLConnection.setRequestProperty("Content-Type", "application/json");
        httpsURLConnection.setRequestProperty("Accept", "application/json");
        httpsURLConnection.setRequestProperty("tenantCode", Helper.TENENT_CODE);
        httpsURLConnection.setRequestProperty("Authorization", Helper.ACCESS_KEY);
        //httpsURLConnection.setRequestProperty("OTT_TOKEN", NONCE);

        return httpsURLConnection;
    }



    private final String dirPath = "/faces";

    public void writeToFile(String data, String name) {
        try {
            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myDir = new File(root + dirPath);
            if (!myDir.exists()) {
                myDir.mkdir();
            }
            File file = new File(myDir, name);
            if (!file.exists()) {
                file.createNewFile();
            }
            FileOutputStream fileOutputStream = new FileOutputStream(file, false);
            OutputStreamWriter writer = new OutputStreamWriter(fileOutputStream);
            writer.append(data);
            writer.close();
            fileOutputStream.close();
        } catch (IOException e) {
            //e.printStackTrace();
        }
    }

    public static File checkFile(String filepath) {

        String root = Environment.getExternalStorageDirectory().getAbsolutePath();
        String dirPathApk = "/.cara";
        File myDir = new File(root + dirPathApk);
        if (!myDir.exists()) {
            myDir.mkdirs();
        }
        return new File(myDir, filepath);
    }


    public String createMd5(String key) {
        StringBuffer sb = null;
        try {
            MessageDigest md = MessageDigest.getInstance("MD5");
            md.update(key.getBytes());
            byte byteData[] = md.digest();
            //convert the byte to hex format method 1
            sb = new StringBuffer();
            for (byte aByteData : byteData) {
                sb.append(Integer.toString((aByteData & 0xff) + 0x100, 16).substring(1));
            }
        } catch (Exception er) {
        }
        return sb.toString();
    }

    public static Uri.Builder getBaseUri(String path) {
        if (Helper.urlSchme.equals("null") || Helper.urlSchme.equals("")) {
            Helper.urlSchme = "https";
        }
        Uri.Builder builder = new Uri.Builder();
        builder.scheme(Helper.urlSchme);
        builder.encodedAuthority(Helper.SERVER_DNS);
        //builder.authority(Helper.SERVER_DNS);

        if (!path.equals("")) {
            builder.appendPath(path);
        }
        return builder;
    }

    private static boolean isDataActive = false;

    public boolean isNetworkConnected(Context context) {
        try {
            ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
            NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
            return activeNetwork != null && activeNetwork.isConnected();
        } catch (Exception conn) {
        }
        isDataActive = false;
        return false;
    }

    public static void secureScreen(Window window) {
        if (CaraManager.getInstance().isProd())
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE);
    }


    public JSONObject getDefaultSettings(Context context) {
        JSONObject deviceSetting = new JSONObject();
        try {
            String uuid = android.provider.Settings.Secure.getString(context.getContentResolver(), android.provider.Settings.Secure.ANDROID_ID);
            deviceSetting.put("uuid", uuid);
            deviceSetting.put("ver", BuildConfig.VERSION_NAME);
            deviceSetting.put("latch_mac", "00:1E:C0:3E:30:41");
            deviceSetting.put("print_mac", "00:1E:C0:3E:30:41");
            deviceSetting.put("device_mac", "");
            deviceSetting.put("dns", "");
            deviceSetting.put("isLatch", "false");
            deviceSetting.put("isPrint", "false");
            deviceSetting.put("isAudio", "false");
            deviceSetting.put("isActive", "true");
            deviceSetting.put("timeout", "3");
            deviceSetting.put("capturetime", "3");
            deviceSetting.put("camid", "3");
            deviceSetting.put("lang", "3");
            Helper.CAMERA_CAPTURE_TIME = 3;
            Helper.TIMEOUT_ACTIVITY = 3;
            return deviceSetting;
        } catch (Exception er) { }
        return deviceSetting;
    }

    public String readFile(String filePath) {
        String output = "";
        try {

            String root = Environment.getExternalStorageDirectory().getAbsolutePath();
            File myDir = new File(root + dirPath);

            File file = new File(myDir, filePath);
            FileInputStream is;
            BufferedReader reader;

            if (file.exists()) {
                is = new FileInputStream(file);
                reader = new BufferedReader(new InputStreamReader(is));
                output = reader.readLine();
            }
            // Log.d("data",output);
            return output;
        } catch (Exception er) {
            //er.printStackTrace();
        }
        return output;
    }

    /**
     * Used to log the test data.
     *
     * @param LogValue
     * @param LogKey
     * @param LogType
     */
    public void logData(String LogValue, String LogKey, String LogType) {
        String url = "";
        try {
            Uri.Builder builder = Helper.getBaseUri("");
            if (builder.build().toString().toLowerCase().contains(".com")) {
                url = builder.build().toString() + "/logger/api/log/transactions/insert?AuthKey=51BD02D9-065F-4E84-BC33-94C55BB4983D";
            }
            else{
                url = "https://ws.attendanceportal.com/logger/api/log/transactions/insert?AuthKey=51BD02D9-065F-4E84-BC33-94C55BB4983D";
            }
            String ApiKey = "6a813472-536e-4e84-b2da-bcc1e8d9212a";

            if (SHOW_LOG) {
                AndroidNetworking.post(url)
                        .addBodyParameter("ApiKey", ApiKey) // Security Key
                        .addBodyParameter("LogType", LogType) // Type of Event (E.g Excption, Info, Error , etc)
                        .addBodyParameter("LogValue", LogValue) // Information of Event (E.g {"Timetaken":"200mSec",Matchingresult:[{id:"",score:""},{id:"",score:""}]
                        .addBodyParameter("LogKey", LogKey) // LogKey is identifier of the event. (E.g IndentifyResponse)
                        .setTag("test")
                        .setPriority(MEDIUM)
                        .build()
                        .getAsJSONObject(new JSONObjectRequestListener() {
                            @Override
                            public void onResponse(JSONObject response) {
                            }

                            @Override
                            public void onError(ANError error) {
                                Log.d("Exception","Inside exception");
                            }
                        });
            }
        }
        catch (Exception ex)
        {

        }
    }

}
