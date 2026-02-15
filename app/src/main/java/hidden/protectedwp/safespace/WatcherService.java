package hidden.protectedwp.safespace;

import android.app.*;
import android.os.storage.*;
import java.util.*;
import java.lang.reflect.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.hardware.usb.UsbManager;

public class WatcherService extends DeviceAdminService {
    private static final String CH_ID = "GuardChan";
    private BroadcastReceiver receiver;
    private BroadcastReceiver usbReceiver;
    private long startTime;
	
	private void startEnforcedService() {
	Context context = this;
    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    String pkg = context.getPackageName();

	Intent intent = new Intent(context, MainActivity.class); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		
	if (dpm.getPermissionGrantState(new ComponentName(this, MyDeviceAdminReceiver.class), context.getPackageName(), android.Manifest.permission.POST_NOTIFICATIONS) != DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED) {
    dpm.setPermissionGrantState(
                    new ComponentName(this, MyDeviceAdminReceiver.class),
                    getPackageName(),
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                );}

    List<NotificationChannel> channels = nm.getNotificationChannels();
    String activeId = null;
    boolean needNew = false;

    for (NotificationChannel ch : channels) {
        if (ch.getImportance() == NotificationManager.IMPORTANCE_NONE) {
            nm.deleteNotificationChannel(ch.getId());
            needNew = true;
        } else if (activeId == null) {
            activeId = ch.getId();
        }
    }

    if (needNew || activeId == null) {
        activeId = "protectedwp.safespace" + Long.toHexString(new java.security.SecureRandom().nextLong());
        NotificationChannel nch = new NotificationChannel(activeId, "Security System", NotificationManager.IMPORTANCE_DEFAULT);
        nm.createNotificationChannel(nch);
    }

    Notification notif = new Notification.Builder(context, activeId)
            .setContentTitle("ProtectedWorkProfile")
            .setContentText("Tap here to start")
            .setSmallIcon(android.R.drawable.ic_lock_lock)
            .setOngoing(true)
	        .setContentIntent(pendingIntent)
		    .setAutoCancel(false)
            .build();

    if (android.os.Build.VERSION.SDK_INT >= 34) {
        startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_SYSTEM_EXEMPTED);
    } else {
        startForeground(1, notif);
    }
	}

	
    private void setAppsVisibility(final boolean visible) {
    final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    final ComponentName admin = new ComponentName(this, MyDeviceAdminReceiver.class);
    final PackageManager pm = getPackageManager();

    if (!dpm.isProfileOwnerApp(getPackageName())) return;
    //  We get ALL packages in the current profile, including hidden (uninstalled) ones.
    List<ApplicationInfo> packages = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);

    for (ApplicationInfo app : packages) {
        String pkg = app.packageName;

        if (pkg.equals(getPackageName())) {continue;}
        if (!pm.queryIntentServices(new Intent("android.view.InputMethod").setPackage(pkg), 0).isEmpty()) {continue;}

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN, null);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);
        launcherIntent.setPackage(pkg);

        List<ResolveInfo> activities = pm.queryIntentActivities(launcherIntent, 
                PackageManager.MATCH_DISABLED_COMPONENTS | PackageManager.MATCH_UNINSTALLED_PACKAGES);

        if (activities != null && !activities.isEmpty()) {
            try {
                dpm.setApplicationHidden(admin, pkg, !visible);
            } catch (Throwable t00) {
            }
        }
    }
}
    
      @Override
    public void onCreate() {
        super.onCreate();
        startTime = System.currentTimeMillis();

		startEnforcedService();
        if (usbReceiver == null) {
        usbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (isInitialStickyBroadcast()) return;
				wipe.wipe(WatcherService.this);
			}
		};
        if (Build.VERSION.SDK_INT >= 34) {
		registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"),Context.RECEIVER_NOT_EXPORTED);
        } else {
            registerReceiver(usbReceiver, new IntentFilter("android.hardware.usb.action.USB_STATE"));
        }
        }

       if (receiver == null) {
            receiver = new BroadcastReceiver() {
                @Override
                public void onReceive(Context context, Intent intent) {
                    if (isInitialStickyBroadcast()) return;
                    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
                    if (intent != null && UsbManager.ACTION_USB_DEVICE_ATTACHED.equals(intent.getAction())) {
				            wipe.wipe(WatcherService.this);
					} 	
                    if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        UserManager um = (UserManager) getSystemService(USER_SERVICE);
						int a = 0;
						try{if("mounted".equalsIgnoreCase(((StorageManager)context.getSystemService(Context.STORAGE_SERVICE)).getPrimaryStorageVolume().getState())){a=1;}}
						catch(Throwable t){}
                        if (a==1 || um.isUserUnlocked(android.os.Process.myUserHandle())) {    
                        if (dpm != null) {
                            ComponentName admin = new ComponentName(context, MyDeviceAdminReceiver.class);
                            setAppsVisibility(false);
                            
                            try {
                                int flag = DevicePolicyManager.class.getField("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY").getInt(null);
                                dpm.lockNow(flag);
                            } catch (Throwable t01) {}

                            try {
                                int userId = android.os.Process.myUserHandle().hashCode();
                                Object sm = context.getSystemService("storage");
                                java.lang.reflect.Method lockMethod = sm.getClass().getMethod("lockUserKey", int.class);
                                lockMethod.invoke(sm, userId);
                            } catch (Throwable t02) {}

							try {
                                dpm.lockNow(1);
                                } catch (Throwable t03) {}

                        }
					}
                    }
                }
            };

            IntentFilter filter = new IntentFilter();
           filter.addAction(Intent.ACTION_SCREEN_OFF);
           filter.addAction(UsbManager.ACTION_USB_DEVICE_ATTACHED);
            if (Build.VERSION.SDK_INT >= 34) {
                registerReceiver(receiver, filter, Context.RECEIVER_NOT_EXPORTED);
            } else {
                registerReceiver(receiver, filter);
            }
        }

        
    }

    @Override
    public void onDestroy() {
        if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) {}
            receiver = null;
        }
		if (usbReceiver != null) {
            try { unregisterReceiver(usbReceiver); } catch (Exception ignored) {}
			usbReceiver = null;
        }
        super.onDestroy();
    }

    
}
