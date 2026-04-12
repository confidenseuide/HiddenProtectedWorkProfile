package background.work.around;

import java.util.*;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.media.*;
import android.os.*;
import android.provider.*;
import android.os.storage.*;
import java.util.*;
import android.app.admin.*;
import android.hardware.usb.UsbManager;


public class RiderService extends Service {
    private boolean isRunning = false;
	private static final String CH_ID = "GuardChan";
    private BroadcastReceiver receiver;
    private BroadcastReceiver usbReceiver;
    private long startTime;

	private void EndLessWL() {	
	new Thread(() -> {
	android.os.PowerManager pm = (android.os.PowerManager) getSystemService(android.content.Context.POWER_SERVICE);
	android.os.PowerManager.WakeLock[] wl = new android.os.PowerManager.WakeLock[10]; 
	int i = 0;
	while (true) {
	try {
	if (i<0) i=10;
	if (i<10) wl[i%10] = pm.newWakeLock(android.os.PowerManager.PARTIAL_WAKE_LOCK, "BackgroundWorkAround"+String.valueOf(i%10)+"::WakeLock"+String.valueOf(i%10));
	wl[i%10].acquire(9000); 
	i++;
	} catch (Throwable t) {}
	android.os.SystemClock.sleep(3000); }
	}).start(); }
	
	private void startForegroundAlarm() {
    new Thread(() -> {
        Context ctx = getApplicationContext();

        try {
            AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
            if (am == null) return;

			Intent serviceIntent = new Intent(getPackageName() + ".ALARM");
            serviceIntent.setPackage(getPackageName());            
            
            PendingIntent operation = PendingIntent.getBroadcast(
                    ctx, 
                    333, 
                    serviceIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            Intent showIntent = new Intent(ctx, MainActivity.class); 
            PendingIntent showAction = PendingIntent.getActivity(
                    ctx, 
                    0, 
                    showIntent, 
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            long triggerAt = System.currentTimeMillis() + 30000;

            AlarmManager.AlarmClockInfo alarmClockInfo = new AlarmManager.AlarmClockInfo(
                    triggerAt, 
                    showAction 
			);
           
			am.setAlarmClock(alarmClockInfo, operation);

        } catch (Throwable t) {
            t.printStackTrace();
        }
    }).start(); 
	}


	private void startWatchdogThread() {
    new Thread(() -> {
        Context ctx = getApplicationContext();

        while (true) {
            try {
                AlarmManager am = (AlarmManager) ctx.getSystemService(Context.ALARM_SERVICE);
                
                Intent intent = new Intent(ctx.getPackageName() + ".START");
                intent.setPackage(ctx.getPackageName());

                PendingIntent pi = PendingIntent.getBroadcast(
                        ctx, 
                        777, 
                        intent, 
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );

                if (am != null) {
               am.setExact(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 60000, pi);
                }
            } catch (Throwable t) {
              
            } 
            android.os.SystemClock.sleep(30000);
        }
    }).start();
	}

	private void serviceMainVoid() {
		startTime = System.currentTimeMillis();
		
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
        PowerManager pm = (PowerManager) getSystemService(Context.POWER_SERVICE);

        if ((km != null && km.isKeyguardLocked()) || (pm != null && !pm.isInteractive())) {
			            Context context = this;
			            DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(DEVICE_POLICY_SERVICE);
			            UserManager um = (UserManager) getSystemService(USER_SERVICE);
						int a = 0;
						try{if("mounted".equalsIgnoreCase(((StorageManager)context.getSystemService(Context.STORAGE_SERVICE)).getPrimaryStorageVolume().getState())){a=1;}}
						catch(Throwable t){}
                        if (a==1 || um.isUserUnlocked(android.os.Process.myUserHandle())) {    
                        if (dpm != null) {
                            ComponentName admin = new ComponentName(context, hidden.protectedwp.safespace.MyDeviceAdminReceiver.class);
                            setAppsVisibility(false);

							// Profile protection code
                            int flag = 1;
                            try {
                                flag = DevicePolicyManager.class.getField("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY").getInt(null);
                            } catch (Throwable t01) {}
							try {
							dpm.lockNow(flag);
							} catch (Throwable t02) {}
							if (flag != 1) {
								try {
								dpm.lockNow(1);
								} catch (Throwable t03) {}
							}
							// Profile protection code

                        }
					}
		}


        if (usbReceiver == null) {
        usbReceiver = new BroadcastReceiver() {
			@Override
			public void onReceive(Context context, Intent intent) {
				if (isInitialStickyBroadcast()) return;
				hidden.protectedwp.safespace.wipe.wipe(RiderService.this);
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
				            hidden.protectedwp.safespace.wipe.wipe(RiderService.this);
					} 	
                    if (intent != null && Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                        UserManager um = (UserManager) getSystemService(USER_SERVICE);
						int a = 0;
						try{if("mounted".equalsIgnoreCase(((StorageManager)context.getSystemService(Context.STORAGE_SERVICE)).getPrimaryStorageVolume().getState())){a=1;}}
						catch(Throwable t){}
                        if (a==1 || um.isUserUnlocked(android.os.Process.myUserHandle())) {    
                        if (dpm != null) {
                            ComponentName admin = new ComponentName(context, hidden.protectedwp.safespace.MyDeviceAdminReceiver.class);
                            setAppsVisibility(false);

							// Profile protection code
                            int flag = 1;
                            try {
                                flag = DevicePolicyManager.class.getField("FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY").getInt(null);
                            } catch (Throwable t01) {}
							try {
							dpm.lockNow(flag);
							} catch (Throwable t02) {}
							if (flag != 1) {
								try {
								dpm.lockNow(1);
								} catch (Throwable t03) {}
							}
							// Profile protection code

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

	private void DestroyPanic() {
		Intent intent = new Intent(getPackageName() + ".START");
        intent.setPackage(getPackageName());            
        sendBroadcast(intent);
	}
	
	private void DestroyCleaner() {
		if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) {}
            receiver = null;
        }
		if (usbReceiver != null) {
            try { unregisterReceiver(usbReceiver); } catch (Exception ignored) {}
			usbReceiver = null;
        }
	}

	private void setAppsVisibility(final boolean visible) {
    final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    final ComponentName admin = new ComponentName(this, hidden.protectedwp.safespace.MyDeviceAdminReceiver.class);
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
	
    private void startEnforcedService() {
	Context context = this;
    DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
    NotificationManager nm = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
    String pkg = context.getPackageName();

	Intent intent = new Intent(context, hidden.protectedwp.safespace.MainActivity.class); intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
		
	if (dpm.getPermissionGrantState(new ComponentName(this, hidden.protectedwp.safespace.MyDeviceAdminReceiver.class), context.getPackageName(), android.Manifest.permission.POST_NOTIFICATIONS) != DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED) {
    dpm.setPermissionGrantState(
                    new ComponentName(this, hidden.protectedwp.safespace.MyDeviceAdminReceiver.class),
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
        nch.setSound(null, null);
		nch.enableVibration(false);
		nm.createNotificationChannel(nch);
    }

    Notification notif = new Notification.Builder(context, activeId)
            .setContentTitle("ProtectedWorkProfile 🔥")
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

	private void TryStartEnforcedService() {
		try {startEnforcedService();} 
        catch (Throwable t) {}
	}

	
    private void initBindAndStart() {
	   if (!isRunning) {
        isRunning = true;
		forceBindAndStart();
		startForegroundAlarm();
		startWatchdogThread();
		serviceMainVoid();
		TryStartEnforcedService();
		EndLessWL();
        }
	}

	private void forceBindAndStart() {
    Intent intent = new Intent(this, HelperService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT | Context.BIND_ABOVE_CLIENT);
    try {startService(intent);} 
    catch (Throwable t) {}
    }
    
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public void onServiceConnected(ComponentName name, IBinder service) {}
        @Override
        public void onServiceDisconnected(ComponentName name) {
            forceBindAndStart();
        }
    };

    @Override
    public IBinder onBind(Intent intent) {
        initBindAndStart();
		return new Binder();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
    initBindAndStart();
	TryStartEnforcedService();
    return START_STICKY;
    }

    @Override
    public void onDestroy() {
        DestroyPanic();
		DestroyCleaner();
        super.onDestroy();
    }
}
