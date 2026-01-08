package com.example.hider;

import android.os.*;
import android.content.*;
import android.content.pm.*;
import android.app.*;
import android.app.admin.*;
import android.view.*;
import android.view.inputmethod.*;
import android.widget.*;
import java.util.*;

public class SelectActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private SharedPreferences prefs;
    
    private static final String PREFS_NAME = "HiderPrefs";
    private static final String KEY_HIDDEN_PACKAGES = "hidden_pkgs";

	@Override
    protected void onPause() {
	super.onPause();
	finish();
	}
	
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
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Select Keyboard");
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
        InputMethodManager imm = (InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE);
        PackageManager pm = getPackageManager();

        Set<String> allPackages = new HashSet<>();
        
        List<InputMethodInfo> enabledImes = imm.getInputMethodList();
        for (InputMethodInfo imi : enabledImes) {
            allPackages.add(imi.getPackageName());
        }

        Set<String> previouslyHidden = prefs.getStringSet(KEY_HIDDEN_PACKAGES, new HashSet<>());
        allPackages.addAll(previouslyHidden);

        List<String> listData = new ArrayList<>(allPackages);
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, listData);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String selectedPkg = listData.get(position);
            
            processKeyboardSelection(selectedPkg, allPackages);
            
            Toast.makeText(this, "Activated: " + selectedPkg, Toast.LENGTH_SHORT).show();
            renderList(listView);
        });
    }

    private void processKeyboardSelection(String targetPkg, Set<String> allPackages) {
        Set<String> nowHidden = new HashSet<>();

        dpm.setApplicationHidden(adminComponent, targetPkg, false);
        dpm.setPackagesSuspended(adminComponent, new String[]{targetPkg}, false);
        
        for (String pkg : allPackages) {
            if (!pkg.equals(targetPkg)) {
                
                dpm.setApplicationHidden(adminComponent, pkg, true);
                dpm.setPackagesSuspended(adminComponent, new String[]{pkg}, true);
                nowHidden.add(pkg);
				finish();
            }
        }

        prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, nowHidden).apply();
        dpm.setPermittedInputMethods(adminComponent, List.of(targetPkg));
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
                            new ComponentName(getPackageName(), SelectActivity.class.getName()), 
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
