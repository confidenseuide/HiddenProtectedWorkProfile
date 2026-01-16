package com.example.hider;

import android.app.*;
import java.util.*;
import java.lang.reflect.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;

public class WatcherService extends Service {
    private static final String CH_ID = "GuardChan";
    private BroadcastReceiver receiver;
    private long startTime;


    private void setAppsVisibility(final boolean visible) {
    final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    final ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
    final PackageManager pm = getPackageManager();

    if (!dpm.isProfileOwnerApp(getPackageName())) return;

    // Получаем ВООБЩЕ ВСЕ пакеты в текущем профиле, включая скрытые (uninstalled)
    List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);

    for (ApplicationInfo app : packages) {
        String pkg = app.packageName;

        // Себя не трогаем
        if (pkg.equals(getPackageName())) continue;

        // ПРОВЕРКА: является ли приложение лаунчерным (есть ли у него MAIN + LAUNCHER)
        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launcherIntent.setPackage(pkg);

        // Ищем активность именно в этом пакете, учитывая скрытые компоненты
        List<ResolveInfo> activities = pm.queryIntentActivities(launcherIntent, 
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES);

        // Если список не пуст — значит это лаунчер-приложение
        if (activities != null && !activities.isEmpty()) {
            try {
                // Если visible = true, то hidden = false (показываем)
                dpm.setApplicationHidden(admin, pkg, !visible);
            } catch (Throwable t00) {
                // Системный хлам, который нельзя скрыть, просто пропускаем
            }
        }
    }
}
    
    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        startTime = System.currentTimeMillis();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null && nm.getNotificationChannel(CH_ID) == null) {
            NotificationChannel channel = new NotificationChannel(
                CH_ID, "Security System", NotificationManager.IMPORTANCE_LOW);
            nm.createNotificationChannel(channel);
        }

        
        Notification notif = new Notification.Builder(this, CH_ID)
                .setContentTitle("Profile Protected")
                .setContentText("it will be frozen on screen off and apps will be hidden.")       
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
        } else {
            startForeground(1, notif);
        }

       if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (System.currentTimeMillis() - startTime < 3000) return;

                    if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                        if (dpm != null) {
                            ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);

                            setAppsVisibility(false);
                            try {
                                int flag = DevicePolicyManager.class.getField("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY").getInt(null);
                                dpm.lockNow(flag);
                            } catch (Throwable t01) {
                                try {
                                dpm.lockNow(1);
                                } catch (Throwable t08) {}
                            }
                            UserManager um = (UserManager) getSystemService(USER_SERVICE);
                            if (um.isUserUnlocked(android.os.Process.myUserHandle())) {
                                try {
                                dpm.lockNow(1);
                                } catch (Throwable t07) {}
                            }

                            try {
                                int userId = android.os.Process.myUserHandle().hashCode();
                                Object sm = context.getSystemService("storage");
                                java.lang.reflect.Method lockMethod = sm.getClass().getMethod("lockUserKey", int.class);
                                lockMethod.invoke(sm, userId);
                            } catch (Throwable t02) {
                                
                            }

                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            if (Build.VERSION.SDK_INT >= 34) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(receiver, filter);
            }
        }

        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) {}
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
