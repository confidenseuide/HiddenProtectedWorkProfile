package com.example.hider;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;
import android.widget.*;
import android.view.*;
import android.view.inputmethod.*;
import android.os.Process;

public class MainActivity extends Activity {

	String a="";
	String b="";
	
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
				/*
				Why do we use a timer and not use switches?: 
				This app creates a temporary work profile that is deleted when the screen turns off. 
				The user can delete and recreate it multiple times in some situations. 
				Auto-configuration allows doing it fast. 
				At the end, we inform the user of what we did. 
				After the timer expires, the user will see the message before using the profile. 
				They can choose not to use the work profile or delete it by simply turning off the screen if they don't like that message. 
				Furthermore, work profile settings don't affect the main system settings.
				*/
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
									b="App is added from userControlDisabled packages. This will not change your experience. As a profile owner, app can't be stopped by user anyway, but this option is important for the system. On some aggressive devices, the system tries to force-stop apps to save battery. We need to stay active at all times for the critical function of wiping data when the screen is off or the phone reboots. ";
								}} catch (Exception ignored) {}
							    try {
								    java.lang.reflect.Method method = dpm.getClass().getMethod("setAdminExemptFromBackgroundRestrictedOperations", ComponentName.class, boolean.class);
								    method.invoke(dpm, admin, true);
									a="App excluded from battery restrictions for stable service and receivers work ";
							    }catch (Exception ignored) {}
							
							}

						if (seconds == 7) {
							Thread loader = new Thread(() -> {
								Integer current_int=null;
								Integer current_circle=null;
								String current_browser=null;
								android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
								Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
								ComponentName admin = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
								PackageManager pm = getPackageManager();
								
								int flags = PackageManager.GET_ACTIVITIES | PackageManager.GET_PERMISSIONS | PackageManager.MATCH_UNINSTALLED_PACKAGES; 
								List<PackageInfo> packages = pm.getInstalledPackages(flags);
								
								for (PackageInfo pkg : packages) {
									String pkgName = pkg.packageName;
									if (pkgName.equals(getPackageName())) {continue;}     
									
									if ((pkg.applicationInfo.flags & ApplicationInfo.FLAG_SYSTEM) != 0) {
										Intent bIntent = new Intent(Intent.ACTION_VIEW, android.net.Uri.parse("http://"));
										bIntent.addCategory(Intent.CATEGORY_BROWSABLE);
										bIntent.setPackage(pkgName);
										if (pm.queryIntentActivities(bIntent, PackageManager.MATCH_UNINSTALLED_PACKAGES).isEmpty()) {continue;}
										
										current_circle = 0;
										if (pkg.requestedPermissions != null) {
											for (String perm : pkg.requestedPermissions) {
												if (perm.equals("android.permission.PACKAGE_USAGE_STATS")){ current_circle += 500;}
												if (perm.equals("android.permission.MANAGE_EXTERNAL_STORAGE")) {current_circle += 500;}
												if (perm.equals("android.permission.SYSTEM_ALERT_WINDOW")){ current_circle += 500;}
												if (perm.equals("android.permission.WRITE_SETTINGS")) {current_circle += 500;}
												if (perm.equals("android.permission.SEND_SMS") || perm.equals("android.permission.RECEIVE_SMS") || perm.equals("android.permission.READ_SMS")) {current_circle += 500;}
												if (perm.equals("android.permission.CALL_PHONE") || perm.equals("android.permission.READ_PHONE_STATE")) {current_circle += 500;}
												if (perm.equals("android.permission.WAKE_LOCK")){ current_circle += 150;}
												if (perm.contains("TURN_SCREEN_ON") || perm.contains("WAKEUP_DEVICE")) {current_circle += 300; }
												if (perm.equals("android.permission.CHANGE_WIFI_STATE")){ current_circle += 200;}
												if (perm.equals("com.android.alarm.permission.SET_ALARM")) {current_circle += 200;}
												if (perm.equals("android.permission.SCHEDULE_EXACT_ALARM")) {current_circle += 200;}
											}
										}
										if (current_int == null) {
											current_int = current_circle;
											current_browser = pkgName;}
										if (current_circle < current_int) {
											current_int= current_circle;
											current_browser = pkgName;}
									}
								}
								try {
									if (current_browser != null) {
										dpm.enableSystemApp(admin, current_browser);}
								} catch (Exception ignored) {}        
							});
							loader.start();
						}
						
						if (seconds == 6) {
						dpm.setScreenCaptureDisabled(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), true);
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);	
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_APPS);		
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_UNINSTALL_APPS);					
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_MODIFY_ACCOUNTS);	
						}
						
						if (seconds == 5) {
							Thread loader = new Thread(() -> {
								InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
								PackageManager pm = getPackageManager();
								ComponentName admin = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
								SharedPreferences p = getSharedPreferences("HiderPrefs", MODE_PRIVATE);
								
								Set<String> allPackages = new HashSet<>();
								List<InputMethodInfo> enabledImes = imm.getInputMethodList();
								for (InputMethodInfo imi : enabledImes) {
									allPackages.add(imi.getPackageName());}
								
								Set<String> previouslyHidden = p.getStringSet("hidden_pkgs", new HashSet<>());
								allPackages.addAll(previouslyHidden);
								
								String current_keyboard = null;
								Integer current_int = null;
								
								for (String pkgName : allPackages) {
									try {
										PackageInfo pkg = pm.getPackageInfo(pkgName, PackageManager.GET_SERVICES | PackageManager.GET_PERMISSIONS);
										int current_circle = 0;
										boolean hasMainIme = false;
										for (InputMethodInfo imi : enabledImes) {
											if (imi.getPackageName().equals(pkgName)) {
												if (imi.getSubtypeCount() == 0) { hasMainIme = true; break; }
												for (int i = 0; i < imi.getSubtypeCount(); i++) {
													if (!imi.getSubtypeAt(i).isAuxiliary()) { hasMainIme = true; break; }
												}}
											if (hasMainIme) { break; }}
										if (!hasMainIme) { current_circle += 30000; }
										if (pkg.requestedPermissions != null) {
											for (String perm : pkg.requestedPermissions) {
												if (perm.equals("android.permission.AUTHENTICATE_ACCOUNTS")) current_circle += 500;
												if (perm.equals("android.permission.MANAGE_ACCOUNTS")) current_circle += 500;
												if (perm.equals("android.permission.USE_CREDENTIALS")) current_circle += 500;
												if (perm.equals("android.permission.READ_PROFILE")) current_circle += 500;
												if (perm.equals("android.permission.POST_NOTIFICATIONS")) current_circle += 500;
												if (perm.equals("android.permission.ACCESS_WIFI_STATE")) current_circle += 500;
												if (perm.equals("android.permission.BLUETOOTH_CONNECT")) current_circle += 500;
												if (perm.equals("com.google.android.c2dm.permission.RECEIVE")) current_circle += 500;
												if (perm.equals("com.google.android.gms.permission.AD_ID")) current_circle += 500;
												if (perm.equals("com.google.android.finsky.permission.BIND_GET_INSTALL_REFERRER_SERVICE")) current_circle += 500;
											}}
										if (current_int == null || current_circle < current_int) {
											current_int = current_circle;
											current_keyboard = pkgName;
										}} catch (Exception ignored) {}
								}
								if (current_keyboard != null) {
									Set<String> nowHidden = new HashSet<>();
									dpm.setApplicationHidden(admin, current_keyboard, false);
									dpm.setPackagesSuspended(admin, new String[]{current_keyboard}, false);
									
									for (String pkg : allPackages) {
										if (!pkg.equals(current_keyboard)) {
											dpm.setApplicationHidden(admin, pkg, true);
											dpm.setPackagesSuspended(admin, new String[]{pkg}, true);
											nowHidden.add(pkg);
										}}
									p.edit().putStringSet("hidden_pkgs", nowHidden).apply();
									dpm.setPermittedInputMethods(admin, java.util.Collections.singletonList(current_keyboard));
								}
							});
							loader.start();
						}

                        tv.setText(String.valueOf(seconds--));
                        new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                    } else {
						android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
                        float textPx = (float) Math.sqrt(dm.widthPixels * dm.heightPixels) * 0.021f;
                        tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textPx);
                        tv.setText("✅ Most safe browser among system apps added to the profile, most safe keyboard selected (unsafe hidden). \"Safe\" means less permissions. You can change keyboard in \"SelectKeyboard\" shortcut. Policy: install apps and manage accounts allowed for freedom, screenshots are disallowed for safety. Data will be wiped on screen Off and reboot phone / restart profile. Screen off listener service started. " + a + b + "✅");
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
		This is the code that auto-launches the work profile from OnActivityResult, bypassing the main thread. 
		Auto-launch is required for auto-start profile protection.
		Bypassing the main thread is necessary to prevent crashes, as on some OEM ROMs the system waits for OnActivityResult completion,
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
