package hidden.protectedwp.safespace;

import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import java.lang.reflect.*;
import java.util.*;

public class NucleusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) || Intent.ACTION_MANAGED_PROFILE_UNLOCKED.equals(action)) {

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

            if (!dpm.isProfileOwnerApp(context.getPackageName())) return;

            Intent serviceIntent=null;
            if (context.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("isHighEfficiencyModeEnabled", true)) {                     
            background.work.around.Start.RunService(context);
            serviceIntent = new Intent(context, background.work.around.RiderService.class);
            } else {
            serviceIntent = new Intent(context, WatcherService.class);
            }
            if (serviceIntent==null) return;
            try {
                context.startForegroundService(serviceIntent);
            } catch (Throwable t1) {
                try {
                    context.startService(serviceIntent);
                } catch (Throwable t2) {
              
                }
            }

        }
    }
}
