package com.example.hider;

import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
	
	@Override
	public void onPasswordFailed(Context context, Intent intent, UserHandle user) {
    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    dpm.wipeData(0);
	}
	
    @Override
    public void onProfileProvisioningComplete(Context context, Intent intent) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);    
  
        dpm.setProfileEnabled(admin);
        dpm.setProfileName(admin, "Ephemeral WP");
        dpm.enableSystemApp(admin, context.getPackageName());

		LauncherApps launcherApps = (LauncherApps) context.getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager userManager = (UserManager) context.getSystemService(Context.USER_SERVICE);
        List<UserHandle> profiles = userManager.getUserProfiles();

        for (UserHandle profile : profiles) {
        long userId = userManager.getSerialNumberForUser(profile);
    
         if (userId != 0) { 
        launcherApps.startMainActivity(
            new ComponentName(context.getPackageName(), MainActivity.class.getName()), 
            profile, null, null
        );
    }
}

    }
}
