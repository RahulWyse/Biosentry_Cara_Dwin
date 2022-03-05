package android.wyse.face.tech5.utilities;

import android.util.Log;

public class LogUtils {
    public static void debug(String tag, String message) {
        if (tag == null) {
            tag = "SDKDemo";
        }
       // if (BuildConfig.DEBUG) {
            Log.d(tag, message);
          //  FL.d(tag, message);
            Utilities.appendLog(message);
       // }
    }
}
