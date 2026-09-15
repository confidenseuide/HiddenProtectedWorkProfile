package background.work.around;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.widget.Toast;

public class AlarmReceiver extends BroadcastReceiver {

    private static final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {

        }

        @Override
        public void onServiceDisconnected(ComponentName name) {

        }
    };

    @Override
    public void onReceive(Context context, Intent intent) {
     
        final PendingResult pendingResult = goAsync();

        new Thread(() -> {
            try {
                Context appContext = context.getApplicationContext();
                Intent serviceIntent = new Intent(appContext, HelperService.class);
                Intent serviceIntent2 = new Intent(appContext, RiderService.class);

                try {
                appContext.bindService(serviceIntent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
                } catch (Throwable t0) {}                
                try {
                appContext.startForegroundService(serviceIntent);
                appContext.startForegroundService(serviceIntent2);
                } catch (Throwable t) {}
                Thread.sleep(5_000);
            } catch (Throwable e) {
              android.os.SystemClock.sleep(5_000);  
            } finally {
                pendingResult.finish();
            }
        }).start();
        try {
        AlarmManager am = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                        
        Intent Alarm_intent = new Intent(context, AlarmReceiver.class);                                            
        PendingIntent piRepeating = PendingIntent.getBroadcast(context, 1030307, Alarm_intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, 70000, piRepeating);
        } catch (Throwable t) {}
    }
}
