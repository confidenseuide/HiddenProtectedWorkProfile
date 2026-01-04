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

		Intent launch = new Intent(context, MainActivity.class);
        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        context.startActivity(launch);
			
    }
}
