package com.example.hider;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.os.Build;
import android.os.IBinder;

public class WatcherService extends Service {
    private static final String CHANNEL_ID = "SecureGuardChannel";

    private final BroadcastReceiver screenReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (Intent.ACTION_SCREEN_OFF.equals(intent.getAction())) {
                wipeEverything(context);
            }
        }
    };

    @Override
    public void onCreate() {
        super.onCreate();
        
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Secure Guard", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }
        
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Profile protection is active")
                .setContentText("The data will be deleted when the screen is turned off.")
                .setSmallIcon(android.R.drawable.ic_launcher)
                .build();
        
        startForeground(1, notification);

        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void wipeEverything(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            
            dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
        } catch (Exception e) {
            dpm.wipeData(0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY;
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
