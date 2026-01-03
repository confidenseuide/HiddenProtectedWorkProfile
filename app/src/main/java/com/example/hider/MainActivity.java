package com.example.hider;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;

import android.os.Process;

public class MainActivity extends Activity {

	private void setAppsVisibility(final boolean visible) {

    final TextView tv = new TextView(this);
    tv.setBackgroundColor(0xFF000000);
    tv.setTextColor(0xFFFFFFFF);
    tv.setTextSize(120);
    tv.setGravity(17);
    setContentView(tv);
    getWindow().getDecorView().setSystemUiVisibility(5894);

    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

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

        @Override
        public void run() {
            if (seconds > 0) {
                if (seconds == 9) {
                    Intent intent = new Intent(MainActivity.this, WatcherService.class);
                    startForegroundService(intent);
                }
                if (seconds == 8) {
                    getPackageManager().setComponentEnabledSetting(
                        new ComponentName(MainActivity.this, NucleusReceiver.class),
                        PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                        PackageManager.DONT_KILL_APP);
                }
                if (seconds == 7) {
                    ComponentName admin = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);
                    try {
                        if (Build.VERSION.SDK_INT >= 30) {
                            dpm.setUserControlDisabledPackages(admin, java.util.Collections.singletonList(getPackageName()));
                        }
                        java.lang.reflect.Method method = dpm.getClass().getMethod("setAdminExemptFromBackgroundRestrictedOperations", ComponentName.class, boolean.class);
                        method.invoke(dpm, admin, true);
                    } catch (Exception ignored) {}
                }

                tv.setText(String.valueOf(seconds--));
                // Повторный запуск через 1 секунду
                new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
            } else {
                tv.setText("✅");
                moveTaskToBack(true);
            }
        } // Конец метода run()
    }); // Конец анонимного класса Runnable и метода post()
} // Конец метода setAppsVisibility


	
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        if (dpm.isProfileOwnerApp(getPackageName())) {
            setAppsVisibility(false);
            return;
        }

        
        if (hasWorkProfile()) {
            launchWorkProfileDelayed();
        } else {
            
            Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
            intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, 
                            new ComponentName(this, MyDeviceAdminReceiver.class));
			intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DISCLAIMER_CONTENT, "This application is intended to test what happens if you hide system and other components in a work profile. You create a work profile only for testing, not for regular use, as it may become unsuitable for that, and if the result does not please you, you can delete the work profile through the main profile using the Account Settings.");
            startActivityForResult(intent, 100);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
            launchWorkProfileDelayed();
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
                List<UserHandle> profiles = userManager.getUserProfiles();

                for (UserHandle profile : profiles) {
                    if (!profile.equals(Process.myUserHandle())) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), MainActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        break;
                    }
                }
            }
        }, 1300); 
    }
}
