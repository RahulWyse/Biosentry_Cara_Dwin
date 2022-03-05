package android.wyse.face;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;

public class CaraAdmin extends android.app.admin.DeviceAdminReceiver{


    public static ComponentName getComponentName(Context context) {
        return new ComponentName(context.getPackageName(),
                context.getPackageName() + ".CaraAdmin");
    }

    @Override
    public void onEnabled(Context context, Intent intent) {
        super.onEnabled(context, intent);
    }

    @Override
    public void onDisabled(Context context, Intent intent) {
        super.onDisabled(context, intent);
    }

    @Override
    public CharSequence onDisableRequested(Context context, Intent intent) {
        // TODO Auto-generated method stub
        return super.onDisableRequested(context, intent);
    }

}

