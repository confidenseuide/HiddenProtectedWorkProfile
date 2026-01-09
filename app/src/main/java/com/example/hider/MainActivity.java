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

							    try {if (Build.VERSION.SDK_INT >= 30) {
									dpm.setUserControlDisabledPackages(admin, java.util.Collections.singletonList(getPackageName()));
								}} catch (Exception ignored) {}
							    try {
								    java.lang.reflect.Method method = dpm.getClass().getMethod("setAdminExemptFromBackgroundRestrictedOperations", ComponentName.class, boolean.class);
								    method.invoke(dpm, admin, true);
							    }catch (Exception ignored) {}
							
							}
						if (seconds == 7) {
							Thread loader = new Thread(() -> {
								android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
								Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
								ComponentName admin = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
								int flags = PackageManager.GET_ACTIVITIES | PackageManager.MATCH_UNINSTALLED_PACKAGES; 
								List<PackageInfo> packages = getPackageManager().getInstalledPackages(flags);
								for (PackageInfo pkg : packages) {
									if (pkg.packageName.equals(getPackageName())) continue;     
									if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
									try {
										dpm.enableSystemApp(admin, pkg.packageName);
									} catch (Exception ignored) {}
									}
								}
							});
							loader.start();
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
		WORKAROUND: Outrunning Provisioning Manager Focus-Reset Logic.
		* On many OEM ROMs (Xiaomi, Samsung, etc.), the ProvisioningManager often 
		* fails or force-closes the app if a Work Profile activity is launched 
		* immediately from onActivityResult. This happens due to a race condition 
		* where the system UserManagerService hasn't fully propagated the new user 
		* state across all system services.
		* We deliberately freeze the UI Thread to hold the Task Focus. This prevents 
		* the system from returning to the Home Launcher while the "Zombie Thread" 
		* performs a high-priority launch of the Work Profile Activity.
		* Process.killProcess() is used as a final synchronization barrier to 
		* ensure the frozen Main Profile process is completely detached from the 
		* task stack once the Work Profile activity has taken over the foreground.
		*/
        Thread zombie = new Thread(() -> {
            try { Thread.sleep(1000); } catch (InterruptedException ignored) {}
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
            try { Thread.sleep(1500); } catch (InterruptedException ignored) {}
             android.os.Process.killProcess(android.os.Process.myPid());
        });

        zombie.setPriority(Thread.MAX_PRIORITY);
        zombie.start();

        try {
            Thread.sleep(4900); 
        } catch (InterruptedException ignored) {}
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
