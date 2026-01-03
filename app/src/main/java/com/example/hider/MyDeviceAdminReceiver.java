package com.example.hider;

import android.app.admin.*;
import android.content.*;

public class MyDeviceAdminReceiver extends DeviceAdminReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
		super.onReceive(context, intent);
        String action = intent.getAction();
        if (action == null) {return;}		
		if (action.equals(DevicePolicyManager.ACTION_DEVICE_ADMIN_ENABLED)) {      
            return;}	
		
		if (action.equals(DevicePolicyManager.ACTION_PROFILE_PROVISIONING_COMPLETE) ||
        action.equals(DevicePolicyManager.ACTION_DEVICE_ADMIN_ENABLED)) {
            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);    
            dpm.setProfileEnabled(admin);
            dpm.setProfileName(admin, "Ephemeral WP");
            dpm.enableSystemApp(admin, context.getPackageName());
            return;}
		
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_SHUTDOWN) || 
            action.equals(Intent.ACTION_SCREEN_OFF) ||
            action.equals("android.intent.action.QUICKBOOT_POWEROFF")) {

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
			
            try {             
                dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
            } catch (Exception e) {
                try {
                    dpm.wipeData(0);
                } catch (Exception ignored) {}
            }
        }
    }

}
