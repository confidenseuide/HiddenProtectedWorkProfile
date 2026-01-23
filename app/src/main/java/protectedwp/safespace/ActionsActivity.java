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
        // ФЛАГИ СУКА ДО SUPER
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        
        // Эти флаги ГОВОРЯТ СИСТЕМЕ: "Убери замок, если его нет или он простой"
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
        } else {
            getWindow().addFlags(0x00080000 | 0x00400000 | 0x00200000); 
            // FLAG_SHOW_WHEN_LOCKED | FLAG_DISMISS_KEYGUARD | FLAG_TURN_SCREEN_ON
        }

        super.onCreate(savedInstanceState);

        // UI (Оставил твой)
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
        
        // 1. ПРОВЕРКА ЧЕРЕЗ USER MANAGER
        // Если профиль заблокирован (Quiet Mode или Locked), то никакие интенты не пройдут
        if (Build.VERSION.SDK_INT >= 24) {
            if (um.isQuietModeEnabled(Process.myUserHandle())) {
                // Если включен тихий режим — просим систему ЕГО ВЫКЛЮЧИТЬ. 
                // Это вызовет системный UI разблокировки именно профиля.
                Intent intent = new Intent(Intent.ACTION_MAIN); // Заглушка
                // На самом деле тут надо дергать DPM
            }
        }

        // 2. СНЯТИЕ БЛОКИРОВКИ (DISMISS)
        if (Build.VERSION.SDK_INT >= 26) {
            km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
                @Override
                public void onDismissSucceeded() {
                    savePrefsAndRestart();
                }
            });
        } else {
            // Старый метод просто через флаг окна
            getWindow().addFlags(WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            savePrefsAndRestart();
        }
    }

    private void savePrefsAndRestart() {
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
                String label = info.name.endsWith("MainActivity") ? RESET_LABEL : info.loadLabel(pm).toString();
                labelToClass.put(label, info.name);
            }
        } catch (Exception ignored) {}
    }

    @Override
    protected void onResume() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        super.onResume();
    }
}
