package com.example.hider;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;
import android.widget.*;
import android.view.*;
import android.os.Process;

public class MainActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        final TextView tv = new TextView(this);
        tv.setBackgroundColor(0xFF000000);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(120);
        tv.setGravity(17);
        setContentView(tv);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        
        if (dpm.isProfileOwnerApp(getPackageName())) {
			
            getPackageManager().setComponentEnabledSetting(
            new ComponentName(MainActivity.this, NucleusReceiver.class),
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP);
			
            if (Build.VERSION.SDK_INT >= 33) {
                dpm.setPermissionGrantState(
                    new ComponentName(this, MyDeviceAdminReceiver.class),
                    getPackageName(),
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                );
            }
        
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                int seconds = 10;
                public void run() {
                    if (seconds > 0) {            
                        if (seconds == 9) {
                            Intent intent = new Intent(MainActivity.this, WatcherService.class);
                            startForegroundService(intent);
                        }
                        
                        if (seconds == 8) {
								ComponentName admin = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
							    dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_APPS_CONTROL);
							    try {if (Build.VERSION.SDK_INT >= 30) {
									dpm.setUserControlDisabledPackages(admin, java.util.Collections.singletonList(getPackageName()));
								}} catch (Exception ignored) {}
							    try {
								    java.lang.reflect.Method method = dpm.getClass().getMethod("setAdminExemptFromBackgroundRestrictedOperations", ComponentName.class, boolean.class);
								    method.invoke(dpm, admin, true);
							    }catch (Exception ignored) {}
							
							}
						
						if (seconds == 6) {	
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);	
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_APPS);		
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_UNINSTALL_APPS);					
						dpm.setPermittedAccessibilityServices(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), null);	
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_MODIFY_ACCOUNTS);	
						}

                        tv.setText(String.valueOf(seconds--));
                        new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                    } else {
                        tv.setText("✅");
						moveTaskToBack(true);
                    }
                }
            });
        return;
        } else {
            if (hasWorkProfile()) {
                launchWorkProfileDelayed();
            } else {

				Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, 
                                new ComponentName(this, MyDeviceAdminReceiver.class));
                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DISCLAIMER_CONTENT, "This app creates a temporary work profile. It will be reset when the screen is turned off or when you reboot your phone.");
                startActivityForResult(intent, 100);

			}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		getWindow().getDecorView().setKeepScreenOn(true);
        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

	@Override
	protected void onActivityResult(int requestCode, int resultCode, Intent data) {
	if (requestCode == 100) {
		/*
		This is the code that launches the work profile from OnActivityResult, bypassing the main thread. 
		This is necessary to prevent crashes, as on some OEM ROMs the system waits for OnActivityResult completion,
		and if you try to launch an Activity while it's running, an error message appears. 
		If you freeze the thread, there will be no error, as the method is suspended. 
		Killing the process is necessary to avoid the exit animation, as in a regular finish(), 
		as on some devices, the exit animation from the main Activity after launching the work profile can kick you out.
		*/
        Thread zombie = new Thread(() -> {
			android.os.SystemClock.sleep(1500); 
			Context app = getApplicationContext();
            UserManager um = (UserManager) app.getSystemService(Context.USER_SERVICE);
            LauncherApps la = (LauncherApps) app.getSystemService(Context.LAUNCHER_APPS_SERVICE);

            for (UserHandle profile : um.getUserProfiles()) {
                if (um.getSerialNumberForUser(profile) != 0) {
                     try {
                        la.startMainActivity(
                            new ComponentName(app.getPackageName(), MainActivity.class.getName()),
                            profile, null, null
                        );
                    } catch (Exception ignored) {}
                    break;
                }
            }
			android.os.SystemClock.sleep(1500); 
			android.os.Process.killProcess(android.os.Process.myPid());
        });

        zombie.setPriority(Thread.MAX_PRIORITY);
        zombie.start();

        android.os.SystemClock.sleep(4500);
    }
}


    private boolean isWorkProfileContext() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isProfileOwnerApp(getPackageName());
    }

    private boolean hasWorkProfile() {
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        return userManager.getUserProfiles().size() > 1;
    }

    private void launchWorkProfileDelayed() {
    
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            
            if (launcherApps != null && userManager != null) {
                List<UserHandle> profiles = userManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                   if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), MainActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        
}

}
