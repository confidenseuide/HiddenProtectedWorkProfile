package hidden.protectedwp.safespace;

import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;

public class wipe {

    public static void wipe(Context context) {
        Context c = context.getApplicationContext();

        try {
            ((DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE)).wipeData(0);
            return;
        } catch (Throwable tee1) {}

        try {
            c.getSystemService(Context.USER_SERVICE).getClass().getMethod("removeUser",UserHandle.class).invoke(c.getSystemService(Context.USER_SERVICE),Process.myUserHandle());
            return;
        } catch (Throwable tee2) {}

        try {
            ((DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE)).clearProfileOwner(new ComponentName(c,MyDeviceAdminReceiver.class));
            return;
        } catch (Throwable tee3) {}
    }
}
