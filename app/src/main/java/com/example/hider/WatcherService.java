package com.example.hider;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.os.Build;
import android.os.IBinder;

public class WatcherService extends Service {
    private static final String CH_ID = "GuardChan";
    private BroadcastReceiver receiver;
    private long startTime;

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
                .setContentText("Data will be wiped on screen off.")       
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
                            try {
                                dpm.wipeData(0);
                            } catch (Exception e) {
                                dpm.wipeData(0);
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
