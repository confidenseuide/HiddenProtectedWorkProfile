package com.example.hider;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import java.util.*;
import android.widget.*;
import android.view.*;
import android.os.Process;

public class MainActivity extends Activity {

private void restart() {
    if (getIntent().getBooleanExtra("restarted", false)) {
        return;
    }

	
       // 1. Первый сон — даем системе осознать статус провижнинга
    try { Thread.sleep(300); } catch (Exception e) {}
    
    moveTaskToBack(true);

    // 2. Второй сон — ждем, пока окно "улетит" за горизонт
    try { Thread.sleep(300); } catch (Exception e) {}

    finish();

    // 3. Третий сон — удерживаем процесс от закрытия, пока система чистит стек
    try { Thread.sleep(300); } catch (Exception e) {}

    // 3. Запускаем новый поток, который выживет после finish()
    // и подождет, пока труп активити остынет
    new Thread(new Runnable() {
        @Override
        public void run() {
			// Перед тем как уйти в спячку, даем процессу "бронежилет"
        Thread.currentThread().setPriority(Thread.MAX_PRIORITY);
            try {
                // Спим 1 секунду. За это время finish() точно отработает,
                // и Provisioning Manager закроется.
                Thread.sleep(500);
            } catch (InterruptedException ignored) {}


			Context app = getApplicationContext();
            Intent intent = new Intent(app, MyDeviceAdminReceiver.class);
            intent.setAction("ACTION_REBIRTH_STAGE_2");
            app.sendBroadcast(intent);
			
            // 4. Используем Context приложения (не Активити!), чтобы запустить новую задачу
            

            // 5. Жестко убиваем старый процесс, если нужно всё очистить
            // android.os.Process.killProcess(android.os.Process.myPid());
        }
    }).start();
}



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        final DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);

        final TextView tv = new TextView(this);
        tv.setBackgroundColor(0xFF000000);
        tv.setTextColor(0xFFFFFFFF);
        tv.setTextSize(120);
        tv.setGravity(17);
        setContentView(tv);
        getWindow().getDecorView().setSystemUiVisibility(5894);
        
        if (dpm.isProfileOwnerApp(getPackageName())) {
            if (Build.VERSION.SDK_INT >= 33) {
                dpm.setPermissionGrantState(
                    new ComponentName(this, MyDeviceAdminReceiver.class),
                    getPackageName(),
                    android.Manifest.permission.POST_NOTIFICATIONS,
                    DevicePolicyManager.PERMISSION_GRANT_STATE_GRANTED
                );
            }
        
            new Handler(Looper.getMainLooper()).post(new Runnable() {
                int seconds = 10;
                public void run() {
                    if (seconds > 0) {            
                        if (seconds == 9) {
                            Intent intent = new Intent(MainActivity.this, WatcherService.class);
                            startForegroundService(intent);
                        }
                        if (seconds == 8) {
                            getPackageManager().setComponentEnabledSetting(
                                new ComponentName(MainActivity.this, NucleusReceiver.class),
                                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                                PackageManager.DONT_KILL_APP);
                        }
                        if (seconds == 7) {
								ComponentName admin = new ComponentName(MainActivity.this, MyDeviceAdminReceiver.class);

							    try {if (Build.VERSION.SDK_INT >= 30) {
									dpm.setUserControlDisabledPackages(admin, java.util.Collections.singletonList(getPackageName()));
								}} catch (Exception ignored) {}
							    try {
								    java.lang.reflect.Method method = dpm.getClass().getMethod("setAdminExemptFromBackgroundRestrictedOperations", ComponentName.class, boolean.class);
								    method.invoke(dpm, admin, true);
							    }catch (Exception ignored) {}
							
							}
                        tv.setText(String.valueOf(seconds--));
                        new Handler(Looper.getMainLooper()).postDelayed(this, 1000);
                    } else {
                        tv.setText("✅");
                        moveTaskToBack(true);
                    }
                }
            });
        return;
        } else {
            if (hasWorkProfile()) {
                launchWorkProfileDelayed();
            } else {
                

				Intent intent = new Intent(DevicePolicyManager.ACTION_PROVISION_MANAGED_PROFILE);
                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME, 
                                new ComponentName(this, MyDeviceAdminReceiver.class));
                intent.putExtra(DevicePolicyManager.EXTRA_PROVISIONING_DISCLAIMER_CONTENT, "This app creates a temporary work profile. It will be reset when the screen is turned off or when you reboot your phone.");
                startActivityForResult(intent, 100);



			}
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		getWindow().getDecorView().setKeepScreenOn(true);
        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == 100 && resultCode == Activity.RESULT_OK) {
			restart();
		}
    }

    private boolean isWorkProfileContext() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isProfileOwnerApp(getPackageName());
    }

    private boolean hasWorkProfile() {
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        return userManager.getUserProfiles().size() > 1;
    }

    private void launchWorkProfileDelayed() {
    new Handler(Looper.getMainLooper()).postDelayed(new Runnable() {
        @Override
        public void run() {
            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            
            if (launcherApps != null && userManager != null) {
                List<UserHandle> profiles = userManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                    // Проверяем ID: если не 0, то это рабочий профиль
                    if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), MainActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        // Закрываем текущую активити
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        }
    }, 1000);
}

}
