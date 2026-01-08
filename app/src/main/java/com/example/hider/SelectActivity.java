package com.example.hider;

import android.app.Activity;
import android.view.*;
import android.content.Intent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.view.inputmethod.InputMethodInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class SelectActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private SharedPreferences prefs;
    
    private static final String PREFS_NAME = "HiderPrefs";
    private static final String KEY_HIDDEN_PACKAGES = "hidden_pkgs";
    

    @Override
    protected void onResume() {
        super.onResume();
		dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);		
		if (!dpm.isProfileOwnerApp(getPackageName())) {
			Intent intent = new Intent(context, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			context.startActivity(intent);
			getPackageManager().setComponentEnabledSetting(
            new ComponentName(this, SelectActivity.class),
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP);
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
            }
        }

        prefs.edit().putStringSet(KEY_HIDDEN_PACKAGES, nowHidden).apply();
        dpm.setPermittedInputMethods(adminComponent, List.of(targetPkg));
    }
}
