package hidden.protectedwp.safespace;

import android.os.Build;
import android.content.SharedPreferences;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Process;
import android.os.UserHandle;

public class wipe {

    private static boolean isCopeOwner(Context c) {
        DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(c.getPackageName());
        return isCOPE;
    }

    private static boolean shouldWipeAllPhoneData(Context c) {
    Context deviceProtectedContext = c.createDeviceProtectedStorageContext();
    SharedPreferences devicePrefs = deviceProtectedContext.getSharedPreferences("prefs", Context.MODE_PRIVATE);
    return devicePrefs.getBoolean("wipe_all_phone_data", false);
    }

    public static void wipe(Context context) {
        Context c = context.getApplicationContext();

        if (shouldWipeAllPhoneData(c) && isCopeOwner(c)) {
            ComponentName adminName = new ComponentName(c, MyDeviceAdminReceiver.class);
            DevicePolicyManager dpm = (DevicePolicyManager) c.getSystemService(Context.DEVICE_POLICY_SERVICE);         
            DevicePolicyManager parentDpm = dpm.getParentProfileInstance(adminName);
          
            int flags = DevicePolicyManager.WIPE_RESET_PROTECTION_DATA 
              | DevicePolicyManager.WIPE_EUICC 
              | DevicePolicyManager.WIPE_EXTERNAL_STORAGE;

            if (Build.VERSION.SDK_INT >= 34) {    
            dpm.wipeDevice(flags);
            } else {
                parentDpm.wipeData(flags);
            }
            return;
        }

        try {
            ((DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE)).wipeData(DevicePolicyManager.WIPE_SILENTLY);
            return;
        } catch (Throwable tee1) {}

        try {
            c.getSystemService(Context.USER_SERVICE).getClass().getMethod("removeUser",UserHandle.class).invoke(c.getSystemService(Context.USER_SERVICE),Process.myUserHandle());
            return;
        } catch (Throwable tee2) {}

        try {
            ((DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE)).removeUser(new ComponentName(c, MyDeviceAdminReceiver.class), android.os.Process.myUserHandle());
            return;
        } catch (Throwable tee3) {}

        try {
            ((DevicePolicyManager)c.getSystemService(Context.DEVICE_POLICY_SERVICE)).clearProfileOwner(new ComponentName(c,MyDeviceAdminReceiver.class));
            return;
        } catch (Throwable tee4) {}
    }
}
