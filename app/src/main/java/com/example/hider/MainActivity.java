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
        final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        final ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
        PackageManager pm = getPackageManager();

        if (!dpm.isProfileOwnerApp(getPackageName())) return;

        final List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.GET_META_DATA);

        for (ApplicationInfo app : packages) {
            String pkg = app.packageName;
			
            if (pkg.equals("android") || pkg.equals("com.android.keychain") || pkg.equals("com.android.providers.settings") || pkg.equals("com.android.settings") || pkg.equals("com.android.systemui") || pkg.equals("com.android.framework") || pkg.equals("com.android.sf") || pkg.equals("com.android.externalstorage") || pkg.equals("com.android.server.telecom")  || pkg.equals("com.android.permissioncontroller")  || pkg.equals("com.google.android.permissioncontroller")) {
                try {
                    dpm.setApplicationHidden(admin, pkg, !visible);
                } catch (Exception e) {
                    
                }
            }
		}

		
  for (ApplicationInfo app : packages) {
       String pkg = app.packageName;

    if (!pkg.equals(getPackageName())) {
        try {
            dpm.setApplicationHidden(admin, pkg, !visible);
        } catch (Exception ignored) { 
			
        }
    }

  }
	
	}
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
