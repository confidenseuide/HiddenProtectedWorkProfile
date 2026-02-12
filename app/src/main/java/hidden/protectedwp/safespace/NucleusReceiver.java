package protectedwp.safespace;

import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import java.lang.reflect.*;
import java.util.*;

public class NucleusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
            action.equals(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)) {

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);

            if (!dpm.isProfileOwnerApp(context.getPackageName())) return;

            Intent serviceIntent = new Intent(context, WatcherService.class);
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
