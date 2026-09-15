package background.work.around;

import android.app.job.JobInfo;
import android.app.job.JobParameters;
import android.app.job.JobScheduler;
import android.app.job.JobService;
import java.util.*;
import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.provider.*;
import android.os.storage.*;

public class HelperService extends JobService {	

	private static final int PERIODIC_JOB_ID = 1001;
    private static final int DELAYED_JOB_ID = 1002;

    @Override
    public boolean onStartJob(JobParameters params) {
        scheduleJobs(getApplicationContext());        
        return false;
    }

    @Override
    public boolean onStopJob(JobParameters params) {        
        return false;
    }

    public static void scheduleJobs(Context context) {
		try {
        JobScheduler jobScheduler = (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
        if (jobScheduler == null) return;

        ComponentName componentName = new ComponentName(context, HelperService.class);

        boolean isPeriodicScheduled = false;
        for (JobInfo jobInfo : jobScheduler.getAllPendingJobs()) {
            if (jobInfo.getId() == PERIODIC_JOB_ID) {
                isPeriodicScheduled = true;
                break;
            }
        }

        if (!isPeriodicScheduled) {
            JobInfo.Builder periodicBuilder = new JobInfo.Builder(PERIODIC_JOB_ID, componentName)
                    .setPeriodic(JobInfo.getMinPeriodMillis())
                    .setPersisted(true)
                    .setRequiresCharging(false)
                    .setRequiresDeviceIdle(false);

            if (Build.VERSION.SDK_INT >= 26) {
                periodicBuilder.setRequiresBatteryNotLow(false);
                periodicBuilder.setRequiresStorageNotLow(false);
            }

            jobScheduler.schedule(periodicBuilder.build());
        }

        JobInfo.Builder delayedBuilder = new JobInfo.Builder(DELAYED_JOB_ID, componentName)
                .setMinimumLatency(15 * 1000L)                
                .setPersisted(true)
                .setRequiresCharging(false)
                .setRequiresDeviceIdle(false);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            delayedBuilder.setRequiresBatteryNotLow(false);
            delayedBuilder.setRequiresStorageNotLow(false);
        }

        jobScheduler.schedule(delayedBuilder.build());
		} catch (Throwable t) {}	
    }
	
			
		@Override
    public final void onCreate() {
        super.onCreate();
		TryStartEnforcedService();
		scheduleJobs(this);
		forceBindAndStart();			
		startWatchdogThread();
	}		
		

	private final void startWatchdogThread() {
		try {
        AlarmManager am = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
                        
        Intent intent = new Intent(this, AlarmReceiver.class);                                            
        PendingIntent piRepeating = PendingIntent.getBroadcast(this, 1030307, intent, PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);

        am.setRepeating(AlarmManager.RTC_WAKEUP, System.currentTimeMillis() + 30000, 70000, piRepeating);
        } catch (Throwable t) {}
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
        NotificationChannel nch = new NotificationChannel(activeId, " ", NotificationManager.IMPORTANCE_DEFAULT);
        nch.setSound(null, null);
		nch.enableVibration(false);
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
	

	private final void TryStartEnforcedService() {		
		try {startEnforcedService();} 
        catch (Throwable t) {}
	}    

	private final void forceBindAndStart() {
    Intent intent = new Intent(this, RiderService.class);
    bindService(intent, connection, Context.BIND_AUTO_CREATE | Context.BIND_IMPORTANT);
    try {startService(intent);} 
    catch (Throwable t) {}
    }
    
    private final ServiceConnection connection = new ServiceConnection() {
        @Override public final void onServiceConnected(ComponentName name, IBinder service) {}
        @Override
        public final void onServiceDisconnected(ComponentName name) {
            forceBindAndStart();
        }
    };


    @Override
    public final int onStartCommand(Intent intent, int flags, int startId) {    
	TryStartEnforcedService();
    return START_STICKY;
    }

    @Override
    public final void onDestroy() {		
        Start.RunService(this);		
        super.onDestroy();
    }
}
