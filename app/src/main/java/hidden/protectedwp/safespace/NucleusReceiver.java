package hidden.protectedwp.safespace;

import android.hardware.usb.UsbManager;
import android.app.admin.*;
import android.content.*;
import android.os.UserManager;
import android.content.pm.*;
import java.util.*;

public class NucleusReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {

		if (context==null || intent == null) return;
		
		String action = intent.getAction();

		if (action == null) return;

		if (UsbManager.ACTION_USB_ACCESSORY_ATTACHED.equals(action) || UsbManager.ACTION_USB_ACCESSORY_DETACHED.equals(action) || UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(action) || UsbManager.ACTION_USB_DEVICE_DETACHED.equals(action)) wipe.wipe(context);
        
        if (Intent.ACTION_BOOT_COMPLETED.equals(action) || Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action) || Intent.ACTION_MANAGED_PROFILE_UNLOCKED.equals(action)) {

         DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
         if (!dpm.isProfileOwnerApp(context.getPackageName())) return;            

         if (Intent.ACTION_LOCKED_BOOT_COMPLETED.equals(action)) {
                        
            UserManager um = (UserManager) context.getSystemService(Context.USER_SERVICE);            
            if (!um.isUserUnlocked(android.os.Process.myUserHandle())) {    
                ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);                            
                SharedPreferences prefsDH = context.createDeviceProtectedStorageContext().getSharedPreferences("UPM", Context.MODE_PRIVATE);
                if (prefsDH.getBoolean("UPM", false)) {						
                    try {
                        final int Y = dpm.getCurrentFailedPasswordAttempts();
						int X = 1 + Y;  
						if (X > 3) X = 3;
						dpm.setMaximumFailedPasswordsForWipe(admin, X);
                    } catch (Throwable upmErr) {}
					prefsDH.edit().putBoolean("UPM1", true).commit();
                }
            }
        
         }
			
            background.work.around.Start.RunService(context);
			intent = new Intent(context, background.work.around.RiderService.class);							
            try {			
				context.startForegroundService(intent);
			} catch (Throwable t) {
				try {							
					context.startService(intent);			
				} catch (Throwable t1) {}
			}
			intent = new Intent(context, WatcherService.class);
			try {			
				context.startForegroundService(intent);
			} catch (Throwable t) {
				try {							
					context.startService(intent);			
				} catch (Throwable t1) {}
			}		

        }
    }
}
