package protectedwp.safespace;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.Switch;
import android.widget.TextView;
import android.widget.Toast;
import android.graphics.Color;
import android.util.TypedValue;

public class AdditionalOptionsActivity extends Activity {

    private static final String PREFS_NAME = "HiderPrefs";
    private static final String KEY_WIPE_ENABLED = "wipe_on_failed_pwd";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 1. Защита от скриншотов и скрытие из списка задач
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);

        // 2. Инициализируем защищенное хранилище (доступно в BFU режиме)
        // Мы НЕ делаем активити directBootAware в манифесте для безопасности,
        // но здесь принудительно используем правильный контекст.
        final Context safeContext = createDeviceProtectedStorageContext();
        
        // Переносим старые префы в защищенную область (на случай обновления с CE на DE хранилище)
        safeContext.moveSharedPreferencesFrom(this, PREFS_NAME);
        
        final SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        // 3. Создание UI (программно, без XML)
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        int p = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("Настройки защиты");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, p);
        root.addView(title);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("Удалить профиль после 3 ошибок пароля");
        label.setTextSize(16);
        label.setTextColor(Color.parseColor("#333333"));
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));

        final Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(KEY_WIPE_ENABLED, false));
        
        // 4. Логика сохранения (защита от ANR + Гарантия записи)
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            // Запускаем запись в фоновом потоке, чтобы исключить ANR на медленных дисках
            new Thread(() -> {
                // Используем .commit() для синхронной и гарантированной записи
                final boolean success = prefs.edit().putBoolean(KEY_WIPE_ENABLED, isChecked).commit();
                
                // Если запись не удалась (ошибка ФС), откатываем ползунок в UI-потоке
                if (!success) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdditionalOptionsActivity.this, "Ошибка записи! Попробуйте еще раз.", Toast.LENGTH_SHORT).show();
                        btn.setChecked(!isChecked);
                    });
                }
            }).start();
        });
        
        row.addView(sw);
        root.addView(row);

        setContentView(root);
    }
}
