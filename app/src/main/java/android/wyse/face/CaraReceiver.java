package android.wyse.face;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class CaraReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        try {


            if (intent.getAction().equals(Intent.ACTION_BOOT_COMPLETED)) {

                CaraManager.getInstance().setLastReboot(System.currentTimeMillis());
                Utility.writeSharedPref(CaraManager.getInstance().getSharedPreferences(), Helper.LAST_REBOOT,
                        CaraManager.getInstance().getLastReboot() + "");

                if (!CaraManager.getInstance().isUpload())
                    CaraManager.getInstance().uploadOfflinePunches(context);

                CaraManager.getInstance().reportHealth(context.getApplicationContext());
            }

            if (Helper.getBaseUri("").build().toString().equalsIgnoreCase("https://ws.tajhotels.com")) {
                CaraManager.getInstance().sendSignal(intent.getAction(), context.getApplicationContext());
            }
        }catch (Exception er){
            Utility.printStack(er);
        }

    }

}
