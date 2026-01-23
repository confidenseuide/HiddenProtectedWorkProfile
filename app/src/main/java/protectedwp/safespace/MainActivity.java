package protectedwp.safespace;

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

	private static volatile String ucd_is_work="";
	
	private void showPasswordPrompt() {
    final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Material_Light_Dialog);
	if (dialog.getWindow() != null) {dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    
    android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
    layout.setOrientation(android.widget.LinearLayout.VERTICAL);
    layout.setBackgroundColor(0xFFFFFFFF);
    int padding = (int) (24 * getResources().getDisplayMetrics().density);
    layout.setPadding(padding, padding, padding, padding);

    android.widget.TextView tv = new android.widget.TextView(this);
    tv.setText("Please set password for profile if you haven't done so yet");
    tv.setTextSize(18);
    tv.setTextColor(0xFF000000);
    tv.setPadding(0, 0, 0, padding);
    layout.addView(tv);

    android.widget.LinearLayout.LayoutParams buttonParams = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    buttonParams.setMargins(0, 8, 0, 8);

    android.graphics.drawable.GradientDrawable greenShape = new android.graphics.drawable.GradientDrawable();
    greenShape.setCornerRadius(8f);
    greenShape.setColor(0xFF4CAF50);

    android.widget.Button btnSet = new android.widget.Button(this);
    btnSet.setText("SET PASSWORD");
    btnSet.setTextColor(0xFFFFFFFF);
    btnSet.setBackground(greenShape);
    btnSet.setLayoutParams(buttonParams);
    btnSet.setOnClickListener(v -> {
        try {
            android.content.Intent intent = new android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivity(intent);
        } catch (Exception ignored) {}
    });
    layout.addView(btnSet);

    android.graphics.drawable.GradientDrawable redShape = new android.graphics.drawable.GradientDrawable();
    redShape.setCornerRadius(8f);
    redShape.setColor(0xFFF44336);

    android.widget.Button btnClose = new android.widget.Button(this);
    btnClose.setText("CLOSE THE APP");
    btnClose.setTextColor(0xFFFFFFFF);
    btnClose.setBackground(redShape);
    btnClose.setLayoutParams(buttonParams);
    btnClose.setOnClickListener(v -> {
        try {
            android.content.Intent homeIntent = new android.content.Intent(android.content.Intent.ACTION_MAIN);
            homeIntent.addCategory(android.content.Intent.CATEGORY_HOME);
            homeIntent.setFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(homeIntent);
        } catch (Exception ignored) {}
    });
    layout.addView(btnClose);
    dialog.setContentView(layout);
	android.view.Window window = dialog.getWindow();
    if (window != null) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.y = 0;
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        window.setAttributes(lp);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);    
    dialog.show();
	}


	private void setAppsVisibility(final boolean visible) {
    final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    final ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
    final PackageManager pm = getPackageManager();

    if (!dpm.isProfileOwnerApp(getPackageName())) return;

    List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);

    for (ApplicationInfo app : packages) {
        String pkg = app.packageName;

        if (pkg.equals(getPackageName())) {continue;}
		if (!pm.queryIntentServices(new Intent("android.view.InputMethod").setPackage(pkg), 0).isEmpty()) {continue;}

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launcherIntent.setPackage(pkg);

		//We get ALL packages in the current profile, including hidden (uninstalled) ones.
        List<ResolveInfo> activities = pm.queryIntentActivities(launcherIntent, 
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES);

        if (activities != null && !activities.isEmpty()) {
            try {
                dpm.setApplicationHidden(admin, pkg, !visible);
            } catch (Exception ignored) {
            }
        }
    }
}

	private void showOnboarding() {
    final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_NoTitleBar_Fullscreen);
    android.view.Window window = dialog.getWindow();
    if (window != null) {
		window.addFlags(WindowManager.LayoutParams.FLAG_SECURE); 
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                | android.view.WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        window.getDecorView().setSystemUiVisibility(
                android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN
                | android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
    }

    android.util.DisplayMetrics dm = getResources().getDisplayMetrics();
    float scaleFactor = (float) Math.sqrt(dm.widthPixels * dm.heightPixels);
    float textPx = scaleFactor * 0.025f;
    int pX = (int) (dm.widthPixels * 0.08f);

    android.widget.LinearLayout root = new android.widget.LinearLayout(this);
    root.setOrientation(android.widget.LinearLayout.VERTICAL);
    root.setBackgroundColor(0xFFFFFFFF);

    android.widget.LinearLayout headerContainer = new android.widget.LinearLayout(this);
    headerContainer.setOrientation(android.widget.LinearLayout.VERTICAL);
    headerContainer.setBackgroundColor(0xFF7484B0);
    android.widget.LinearLayout.LayoutParams hParams = new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f);
    
    android.view.View spacer = new android.view.View(this);
    headerContainer.addView(spacer, new android.widget.LinearLayout.LayoutParams(-1, 0, 1.0f));

    android.widget.TextView titleTv = new android.widget.TextView(this);
    titleTv.setText("ProtectedWorkProfile");
    titleTv.setTextColor(0xFFFFFFFF);
    titleTv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, scaleFactor * 0.035f);
    titleTv.setPadding(pX, 0, pX, (int)(pX * 0.5f));
    headerContainer.addView(titleTv);
    
    root.addView(headerContainer, hParams);

    android.widget.ScrollView scroll = new android.widget.ScrollView(this);
    android.widget.LinearLayout.LayoutParams sParams = new android.widget.LinearLayout.LayoutParams(-1, 0, 2.0f);
    
    android.widget.TextView tv = new android.widget.TextView(this);
    tv.setPadding(pX, (int)(pX * 0.8f), pX, pX);
    tv.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textPx);
    tv.setTextColor(0xFF333333);
    tv.setLineSpacing(0, 1.2f);
	tv.setTypeface(null, android.graphics.Typeface.BOLD); 
    tv.setText("Hello! This is ProtectedWorkProfile app.\n" +
            "The app creates work profile that hide work apps and that will be frozen when your screen turns off.\n\n" +
            "Just click start -> next -> next ->... to create profile.\n\n" +
            "When profile created, the app starts autoconfiguration:\n" +
            "1. App starts service and receiver for screen off / reboot listen.\n" +
            "2. App tries to ignore battery optimization and disable package control to prevent stop-signals from system.\n" +
            "3. App adds \"safest\" system browser to profile (with less permissions from ours blacklist).\n" +
            "4. App disables screenshots in profile (safety), allows apps install and accounts management (user freedom).\n" +
            "5. App selects \"safest\" system keyboard and freezes others.\n"+
		    "6. App asks you to set profile password to protect data. It is also recommended to set a password for your main phone, not just your profile.\n"+
			"7. When screen turns off profile will be frozen and profie apps hidden (except this app)\n"+
			"8. To unhide apps just click to \"Set​🌱​⁢ProtectedWorkProfile\" shortcut and wait for the timer.\n\n");
    scroll.addView(tv);
    root.addView(scroll, sParams);

    android.view.View divider = new android.view.View(this);
    divider.setBackgroundColor(0xFFDCDCDC);
    root.addView(divider, new android.widget.LinearLayout.LayoutParams(-1, 3));
		
    android.widget.RelativeLayout bottomBar = new android.widget.RelativeLayout(this);
    bottomBar.setBackgroundColor(0xFFF5F5F5);
    bottomBar.setPadding(pX, (int)(pX * 0.2f), pX, (int)(pX * 0.2f));

    android.widget.Button btn = new android.widget.Button(this);
    btn.setText("START >");
    btn.setTextColor(0xFF333333);
    btn.setBackgroundColor(0);
    btn.setTypeface(null, android.graphics.Typeface.BOLD);
    btn.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX, textPx * 0.9f);

    android.widget.RelativeLayout.LayoutParams btnParams = new android.widget.RelativeLayout.LayoutParams(-2, -2);
    btnParams.addRule(android.widget.RelativeLayout.ALIGN_PARENT_RIGHT);
    bottomBar.addView(btn, btnParams);

    root.addView(bottomBar, new android.widget.LinearLayout.LayoutParams(-1, -2));

    btn.setOnClickListener(v -> {
        
		Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
        intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, new ComponentName(this, MyDeviceAdminReceiver.class));
		startActivityForResult(intent, 100);
		
        dialog.dismiss();
    });

    dialog.setContentView(root);
    dialog.setCancelable(false);
    dialog.show();
}

	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
		super.onCreate(savedInstanceState);
		final UserManager um = (UserManager) getSystemService(USER_SERVICE);
        if (um.isUserUnlocked(android.os.Process.myUserHandle())) {
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
			Thread t = new Thread(() -> {
				android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
				try {
					AppOpsManager ops = (AppOpsManager) getSystemService(Context.APP_OPS_SERVICE);
					int uid = getApplicationInfo().uid;
					String pkg = getPackageName();
					java.lang.reflect.Method setMode = ops.getClass().getMethod("setMode", int.class, int.class, String.class, int.class);
					/*
					Fix for Xiaomi devices: allows Boot receivers and background start
					to ensure wipe profile data on phone reboot works as expected. 
					*/
					for (int code = 10008; code <= 10009; code++) {
						try {
							setMode.invoke(ops, code, uid, pkg, 0);
						} catch (Throwable ignore) {}
					}} catch (Throwable ignore) {}
			});
			t.setPriority(Thread.MAX_PRIORITY);
			t.start();
			
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
				Why do we use a timer to setup?: 
				This app creates a temporary work profile that is deleted when the screen turns off. 
				The user can delete and recreate it multiple times in some situations. 
				Auto-configuration allows doing it fast. 
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
									// App is added to userControlDisabled packages. This will not apply to real user control ⸻ as a profile owner the app can't be stopped by user click in settings anyway. This option is important for the system. On some aggressive firmwares, the system simulates a user stop signal to terminate background apps. Direct signal not blocked like button in settings. But UserControlDisabled packages may not receive this signal. We must work constantly for the critical function of wiping data when the screen is off or the phone reboots.
								}} catch (Throwable t) {}
							    try {
								    java.lang.reflect.Method method = dpm.getClass().getMethod("setAdminExemptFromBackgroundRestrictedOperations", ComponentName.class, boolean.class);
								    method.invoke(dpm, admin, true);
							    }catch (Throwable t) {}
							}

					
							if (seconds == 7) {
							Thread loader777 = new Thread(() -> {   
							android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_URGENT_DISPLAY);
							setAppsVisibility(true);
							});
							loader777.setPriority(Thread.MAX_PRIORITY);
							loader777.start();
						}

						if (seconds == 6) {
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
								} catch (Throwable t) {}        
							});
							loader.start();
						}
						
						if (seconds == 5) {
						dpm.setScreenCaptureDisabled(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), true);
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_UNKNOWN_SOURCES);	
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_INSTALL_APPS);		
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_UNINSTALL_APPS);					
						dpm.clearUserRestriction(new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class), UserManager.DISALLOW_MODIFY_ACCOUNTS);	
						}
						
						if (seconds == 3) {
							
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
										}} catch (Throwable t) {}
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
						MainActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("isDone", true).apply();
						showPasswordPrompt();
                    }
                }
            });
        return;
        } else {
            if (hasWorkProfile()) {
                launchWorkProfileDelayed();
            } else {
				showOnboarding();
				
			}
        }
    }}

    @Override
    protected void onResume() {
        super.onResume();

        if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
		getWindow().getDecorView().setKeepScreenOn(true);
        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
		if (isWorkProfileContext()) {
		if (MainActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("isDone", false)) {
					Context appContext1 = getApplicationContext();
					Intent actions1 = new Intent(appContext1, ActionsActivity.class);
					actions1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
					appContext1.startActivity(actions1);
		}
		MainActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("isDone", true).apply();
		}
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
		​super.onActivityResult is not used here on purpose. The system must not know about onActivityResult using.
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
                    } catch (Throwable t) {}
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

	@Override
	public void onBackPressed() {}

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
                            new ComponentName(getPackageName(), ZeroActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        
}

}
