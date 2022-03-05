package android.wyse.face;

import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;

public class CaraApp extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        CaraManager.getInstance().setTimerTone(RingtoneManager.getRingtone(getApplicationContext(),
                Uri.parse("android.resource://" + getPackageName() + "/raw/beep")));
        CaraManager.getInstance().setShutterTone(RingtoneManager.getRingtone(getApplicationContext(),
                Uri.parse("android.resource://" + getPackageName() + "/raw/shutter")));


        if(Build.VERSION.SDK_INT<=24){
            //set last in and out status
            CaraManager.getInstance().setSharedPreferences(getSharedPreferences(CaraManager.PREF_FILE_NAME, Context.MODE_PRIVATE));

            CaraManager.getInstance().setLastInout_resetTime(Long.valueOf(CaraManager.getInstance().getSharedPreferences().getString("inout_reset", System.currentTimeMillis() + "")));
        }

        CaraManager.getInstance().setCaraDb(new Database(getApplicationContext()));


        CaraManager.getInstance().initWhiteDns();

        //CaraManager.getInstance().initUsers();
        CaraManager.getInstance().setTaj(false);
        CaraManager.getInstance().setLicVersion(true);
        CaraManager.getInstance().setProd(!BuildConfig.DEBUG);

    }

    @Override
    public void onTerminate() {
        super.onTerminate();
    }

}



