package protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class ActionsActivity extends Activity {

    private Map<String, String> labelToClass = new LinkedHashMap<>();
    private static final String CLOSE_APP_LABEL = "CloseApp";
    private static final String RESET_LABEL = "ShowApps&SetUp";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. ФЛАГИ СТРОГО ДО SUPER
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        

            setShowWhenLocked(true);
            setTurnScreenOn(true);
        
        
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED 
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON 
                | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        

        super.onCreate(savedInstanceState);

        // UI
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        contentBox.setGravity(Gravity.CENTER_HORIZONTAL);
        contentBox.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        ((LinearLayout.LayoutParams)contentBox.getLayoutParams()).setMargins(60, 0, 60, 0);

        TextView title = new TextView(this);
        title.setText("What to do?");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 40);
        title.setGravity(Gravity.CENTER);
        contentBox.addView(title);

        ListView listView = new ListView(this);
        listView.setLayoutParams(new LinearLayout.LayoutParams(-1, -2));
        contentBox.addView(listView);

        root.addView(contentBox);
        setContentView(root);

        labelToClass.put(CLOSE_APP_LABEL, "ACTION_CLOSE");
        loadActivities();

        List<String> labels = new ArrayList<>(labelToClass.keySet());
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, labels);
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String label = labels.get(position);
            String className = labelToClass.get(label);

            if (label.equals(CLOSE_APP_LABEL)) {
                Intent home = new Intent(Intent.ACTION_MAIN);
                home.addCategory(Intent.CATEGORY_HOME);
                home.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(home);
            } else if (label.equals(RESET_LABEL)) {
                unlock();
            } else {
                try {
                    Intent i = new Intent();
                    i.setComponent(new ComponentName(getPackageName(), className));
                    i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    startActivity(i);
                } catch (Exception ignored) {}
            }
        });
    }

    private void unlock() {
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);

        // Если профиль уже разблокирован
        if (um.isUserUnlocked()) {
            savePrefsAndRestart();
            return;
        }

        // Запрашиваем интерактивное снятие блокировки
        km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
            @Override
            public void onDismissSucceeded() {
                savePrefsAndRestart();
            }

            @Override
            public void onDismissError() {
                // Если KM тупит, пробуем форсировать через флаг
                getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                // Костыль: проверяем через секунду, не разблокировался ли юзер сам
                new Handler(Looper.getMainLooper()).postDelayed(() -> {
                    if (um.isUserUnlocked()) savePrefsAndRestart();
                }, 1000);
            }

            @Override
            public void onDismissCancelled() {
                // Юзер отменил
            }
        });
    }

    private void savePrefsAndRestart() {
        // commit() для мгновенной синхронной записи
        this.createDeviceProtectedStorageContext()
            .getSharedPreferences("prefs", Context.MODE_PRIVATE)
            .edit()
            .putBoolean("isDone", false)
            .commit();

        Intent i1 = new Intent(this, MainActivity.class);
        i1.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(i1);
    }

    private void loadActivities() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);

            for (ActivityInfo info : pi.activities) {
                if (info.name.equals(this.getClass().getName())) continue;

                String label;
                if (info.name.endsWith("MainActivity")) {
                    label = RESET_LABEL;
                } else {
                    label = info.loadLabel(pm).toString();
                    if (label.equals(info.name) || label.isEmpty() || label.equals("ProtectedWorkProfile")) {
                        label = RESET_LABEL;
                    }
                }
                labelToClass.put(label, info.name);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        // Флаги безопасности должны быть активны всегда
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        super.onResume();
        
        if (isWorkProfileContext()) {
            hideSystemUI();
        } else if (hasWorkProfile()) {
            launchWorkProfileDelayed();
        }
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY 
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION 
                | View.SYSTEM_UI_FLAG_FULLSCREEN
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
    }

    private boolean isWorkProfileContext() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isProfileOwnerApp(getPackageName());
    }

    private boolean hasWorkProfile() {
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        return um.getUserProfiles().size() > 1;
    }

    private void launchWorkProfileDelayed() {
        LauncherApps la = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
        UserManager um = (UserManager) getSystemService(Context.USER_SERVICE);
        for (UserHandle profile : um.getUserProfiles()) {
            if (um.getSerialNumberForUser(profile) != 0) {
                la.startMainActivity(new ComponentName(getPackageName(), ActionsActivity.class.getName()), profile, null, null);
                break;
            }
        }
    }
}
