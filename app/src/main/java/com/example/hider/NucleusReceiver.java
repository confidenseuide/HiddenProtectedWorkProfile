package com.example.hider;

import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import java.lang.reflect.*;
import java.util.*;

public class NucleusReceiver extends BroadcastReceiver {
    
    private void setAppsVisibility(Context context, final boolean visible) {
    // Получаем сервисы через пришедший context
    final DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    final ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);
    final PackageManager pm = context.getPackageManager();

    // Заменяем getPackageName() на context.getPackageName()
    if (!dpm.isProfileOwnerApp(context.getPackageName())) return;

    // MATCH_UNINSTALLED_PACKAGES позволяет видеть скрытые приложения
    List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);

    for (ApplicationInfo app : packages) {
        String pkg = app.packageName;

        if (pkg.equals(context.getPackageName())) continue;

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launcherIntent.setPackage(pkg);

        // Проверка на наличие Launcher активности
        List<ResolveInfo> activities = pm.queryIntentActivities(launcherIntent, 
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES);

        if (activities != null && !activities.isEmpty()) {
            try {
                dpm.setApplicationHidden(admin, pkg, !visible);
            } catch (Exception ignored) {
                // Пропускаем критические пакеты, которые Android запрещает скрывать
            }
        }
    }
}

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        
        if (action.equals(Intent.ACTION_BOOT_COMPLETED) || 
            action.equals(Intent.ACTION_LOCKED_BOOT_COMPLETED) ||
            action.equals(Intent.ACTION_MANAGED_PROFILE_UNLOCKED)) {

            DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
            
            setAppsVisibility(context, false); 

            Intent serviceIntent = new Intent(context, WatcherService.class);
            try {
                context.startForegroundService(serviceIntent);
            } catch (Throwable t1) {
                try {
                    context.startService(serviceIntent);
                } catch (Throwable t2) {
              
                }
            }

        }
    }
}
