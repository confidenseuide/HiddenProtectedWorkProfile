package protectedwp.safespace;

import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

	@Override
	public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    wipe.wipe(context);
	}
	
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);    
  
        dpm.setProfileEnabled(admin);
        dpm.setProfileName(admin, "Protected WP");
		try {dpm.enableSystemApp(admin, context.getPackageName());} 
		catch (Throwable t1) {}    

		LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        List<UserHandle> profiles = userManager.getUserProfiles();

        for (UserHandle profile : profiles) {
        long userId = userManager.getSerialNumberForUser(profile);
    
         if (userId != 0) { 
			try {
			launcherApps.startMainActivity(new ComponentName(context.getPackageName(), MainActivity.class.getName()), profile, null, null);
			} 
			catch (Throwable t2) {}    
		 }
		}
	}
}
