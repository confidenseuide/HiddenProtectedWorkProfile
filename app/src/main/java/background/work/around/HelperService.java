package background.work.around;

import android.app.Service;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.media.MediaPlayer;
import android.os.Binder;
import android.os.IBinder;
import android.provider.Settings;

public class HelperService extends Service {
    private boolean isRunning = false;

	/* 
	Extremely important: a call of a static receiver increases the app priority, and a clogged queue is actually good, because as soon as all processes die, the system can revisit the broadcast queue and restart the application. The receiver is located in a separate process, so there is no need to worry that this will lead to failures in the service.
	*/
	private void startWatchdogThread() {
        new Thread(() -> {
            while (true) {
                DestroyPanic();
                android.os.SystemClock.sleep(7500);
				DestroyPanic2();
				android.os.SystemClock.sleep(7500);
            }
        }).start();
    }
	
	private void DestroyPanic() {
		try {
		Intent intent = new Intent(getPackageName() + ".START");
        intent.setPackage(getPackageName());            
        sendOrderedBroadcast(intent, null);
		} catch (Throwable t) {}
	}

	private void DestroyPanic2() {
		try {
		Intent intent2 = new Intent(getPackageName() + ".START2");
        intent2.setPackage(getPackageName());            
        sendOrderedBroadcast(intent2, null);
		} catch (Throwable t2) {}
	}
	
    private void DestroyCleaner() {
		isRunning = false;
	}
	

	private void initBindAndStart() {
	   if (!isRunning) {
        isRunning = true;
        forceBindAndStart();
		startWatchdogThread();
        }
	}

	private void forceBindAndStart() {
    Intent intent = new Intent(this, RiderService.class);
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
    return START_STICKY;
    }

    @Override
    public void onDestroy() {
        DestroyPanic();
        DestroyCleaner();
        super.onDestroy();
    }
}
