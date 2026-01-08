package com.example.hider;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class DisableActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private SharedPreferences prefs;
    
    private static final String PREFS_NAME = "DisablePrefs";
    private static final String KEY_DISABLED_PACKAGES = "disabled_pkgs";

	    @Override
    protected void onResume() {
        super.onResume();
		dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);		
		if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		if (!isWorkProfileContext() && !hasWorkProfile()) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Disable Apps");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 20);
        layout.addView(title);

        ListView listView = new ListView(this);
        layout.addView(listView);
        setContentView(layout);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class); 
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        renderList(listView);
    }

    private void renderList(ListView listView) {
        PackageManager pm = getPackageManager();
        
        
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        
        Set<String> disabledInPrefs = new HashSet<>(prefs.getStringSet(KEY_DISABLED_PACKAGES, new HashSet<>()));
        List<String> displayList = new ArrayList<>();

        for (ApplicationInfo app : allApps) {
               if (!app.packageName.equals(getPackageName())) {
                String status = disabledInPrefs.contains(app.packageName) ? " [HIDDEN]" : "";
                displayList.add(app.packageName + status);
            }
        }

        
        Collections.sort(displayList);

        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, displayList);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String rawText = displayList.get(position);
            String pkgName = rawText.replace(" [HIDDEN]", "");
            
            togglePackageState(pkgName);
            renderList(listView);
        });
    }

    private void togglePackageState(String pkgName) {
        Set<String> disabledInPrefs = new HashSet<>(prefs.getStringSet(KEY_DISABLED_PACKAGES, new HashSet<>()));
        boolean isCurrentlyDisabled = disabledInPrefs.contains(pkgName);

        try {
            if (isCurrentlyDisabled) {
                
                dpm.setApplicationHidden(adminComponent, pkgName, false);
                dpm.setPackagesSuspended(adminComponent, new String[]{pkgName}, false);
                disabledInPrefs.remove(pkgName);
                Toast.makeText(this, pkgName + " restored", Toast.LENGTH_SHORT).show();
            } else {

                dpm.setApplicationHidden(adminComponent, pkgName, true);
                dpm.setPackagesSuspended(adminComponent, new String[]{pkgName}, true);
                disabledInPrefs.add(pkgName);
                Toast.makeText(this, pkgName + " frozen", Toast.LENGTH_SHORT).show();
            }

            
            prefs.edit().putStringSet(KEY_DISABLED_PACKAGES, disabledInPrefs).apply();

        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
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
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            
            if (launcherApps != null && userManager != null) {
                List<UserHandle> profiles = userManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                   if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), DisableActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        }
    }, 1000);
	}
}
