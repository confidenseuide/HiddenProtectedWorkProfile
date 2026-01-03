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
        
        // 1. Создаем канал уведомлений для Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID, "Secure Guard", NotificationManager.IMPORTANCE_LOW);
            getSystemService(NotificationManager.class).createNotificationChannel(channel);
        }

        // 2. Запускаем Foreground (уведомление будет висеть в профиле)
        Notification notification = new Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Защита профиля активна")
                .setContentText("Данные будут удалены при выключении экрана")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .build();
        
        startForeground(1, notification);

        // 3. Регистрируем динамический ресивер для SCREEN_OFF
        registerReceiver(screenReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    }

    private void wipeEverything(Context context) {
        DevicePolicyManager dpm = (DevicePolicyManager) context.getSystemService(Context.DEVICE_POLICY_SERVICE);
        try {
            // Флаг WIPE_EXTERNAL_STORAGE чистит и файлы
            dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
        } catch (Exception e) {
            dpm.wipeData(0);
        }
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        return START_STICKY; // Перезапуск сервиса системой, если он упадет
    }

    @Override
    public void onDestroy() {
        unregisterReceiver(screenReceiver);
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
