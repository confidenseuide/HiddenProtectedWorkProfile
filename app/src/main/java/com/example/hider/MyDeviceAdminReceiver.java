package com.example.hider;

import android.app.admin.*;
import android.content.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {
	
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
    // Получаем ID профиля. 0 — это всегда основной владелец (Main User)
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
