package hidden.protectedwp.safespace;

import android.app.Activity;
import android.widget.Toast;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import java.util.Locale;

public class TipsActivity extends Activity {

    private boolean isRussian;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);       
		super.onCreate(savedInstanceState);

        String language = Locale.getDefault().getLanguage();
        isRussian = language.equals(new Locale("ru").getLanguage());

        ScrollView scrollView = new ScrollView(this);
        scrollView.setLayoutParams(new ViewGroup.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.MATCH_PARENT));

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(48, 48, 48, 48);
        scrollView.addView(layout);
        
        layout.addView(createTextView(getTextIntro(), true));

        layout.addView(createTextView(getTextTip1(), true));
        layout.addView(createButton(getBtnScreenLock(), v -> 
                openSettingsAction(android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PARENT_PROFILE_PASSWORD)));
        layout.addView(createTextView(getTextTip1Note(), false));

        layout.addView(createTextView(getTextTip2(), true));
        
        layout.addView(createTextView(getTextTip3(), true));
        layout.addView(createButton(getBtnNotifications(), v -> 
                openSettingsAction("android.settings.NOTIFICATION_SETTINGS")));

        layout.addView(createTextView(getTextTip4(), true));
        layout.addView(createButton(getBtnConnections(), v -> 
                openSettingsByClass("com.android.settings.Settings$IccLockSettingsActivity")));
        layout.addView(createTextView(getTextTip4Note(), false));

        setContentView(scrollView);
    }

    private TextView createTextView(String text, boolean addTopMargin) {
        TextView textView = new TextView(this);
        textView.setText(text);
        textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 16);
        textView.setTextColor(Color.parseColor("#E0E0E0")); 

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        if (addTopMargin) {
            params.setMargins(0, 48, 0, 16);
        } else {
            params.setMargins(0, 16, 0, 16);
        }
        textView.setLayoutParams(params);
        textView.setLineSpacing(0, 1.2f);
        return textView;
    }

    private Button createButton(String text, View.OnClickListener onClickListener) {
        Button button = new Button(this);
        button.setText(text);
        button.setTypeface(null, Typeface.BOLD);
        
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0, 16, 0, 32);
        button.setLayoutParams(params);
        
        button.setOnClickListener(onClickListener);
        return button;
    }

    private void openSettingsAction(String action) {
        try {
		Intent intent = new Intent(action);
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        startActivity(intent);
		} catch (Throwable e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }
    
	private void openSettingsByClass(String className) {
        try {
            Intent intent = new Intent();
            intent.setClassName("com.android.settings", className);
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
        } catch (Throwable e) {
            Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
        }
    }

    private String getTextIntro() {
        return isRussian 
            ? "Привет, это советы по безопасности. Тут вы можете увидеть советы по общей безопасности вашего устройства. Это не советы для конкретно этого профиля. Они могу звучать банально, но не все им следуют."
            : "Hi, these are security tips. Here you can see tips for the general security of your device. These are not tips specifically for this profile. They may sound basic, but not everyone follows them.";
    }

    private String getTextTip1() {
        return isRussian 
            ? "1. Пароль в основном профиле, а не только в рабочем, должен содержать не менее 15 случайных символов, чтобы его было максимально сложно подобрать за разумное время даже при полном контроле над железом. Если система позволяет задать больше символов, то используйте эту возможность."
            : "1. The password in the primary profile, not only in the work one, must contain at least 15 random characters to make it as difficult as possible to brute-force within a reasonable time, even in case of full control over the hardware. If the system allows you to set more characters, use this opportunity.";
    }


    private String getBtnScreenLock() {
        return isRussian ? "Настроить блокировку экрана основного профиля" : "Set up screen lock for primary profile";
    }

    private String getTextTip1Note() {
        return isRussian 
            ? "Помните, что это будет иметь смысл только если вы перезагрузили телефон или иным образом перевели пользователя в состояние BFU (до первой разблокировки), ведь иначе (в AFU) даже если экран заблокирован, взломать шифрование можно и без пароля.\n\nДля принудительной перезагрузки (если вендор блокирует обычную до подтверждения пароля) на большинстве устройств Android нужно зажать кнопку питания и ближайшую к ней кнопку громкости и подождать, пока она не произойдет, независимо от того, что происходит в этот момент."
            : "Remember that this will only make sense if you reboot the phone or in another way put the user into the BFU (Before First Unlock) state. Otherwise (in AFU), even if the screen is locked, encryption can be hacked without a password.\n\nFor a forced reboot (if the vendor blocks the standard one before the password confirmation), on most Android devices you need to press and hold the power button and the nearest volume button, and wait until it happens, regardless of what is happening at this moment.";
    }

    private String getTextTip2() {
        return isRussian 
            ? "2. Не используйте биометрическую разблокировку. Только пароль. Никаких отпечатков и лица. Их могут использовать для разблокировки против вашей воли. Пароль тоже могут если окажут давление, но это сделать сложнее. Чтобы защититься от подобного настройте \"Duress Password\", это функция которая поможет вам стереть данные введя фейковый пароль на том же экране блокировки. Она есть в операционной системе GrapheneOS, а также в приложениях DuressKeyboard и Duress (последний гарантий сброса не дает, ведь работает исключительно через спецвозможности, которые могут не сработать)."
            : "2. Do not use biometric unlock. Only a password. No fingerprints or face. They can be used to unlock the device against your will. A password too in case of pressure on you, but this is harder. To protect yourself aganist such situations, set up a \"Duress Password\". This is a feature that will help you wipe data by entering a fake password on the same lock screen. It is available in the GrapheneOS operating system, and also in the DuressKeyboard and Duress apps (the last one does not give any wipe guarantees, because it works only via accessibility services, which might not work).";
	}

    private String getTextTip3() {
        return isRussian 
            ? "3. Полностью отключите уведомления на экране блокировки. Сам факт что вам пришло уведомление посторонние люди уже могут использовать для давления на вас."
            : "3. Completely disable lock screen notifications. The mere fact that a notification has arrived can already be used by outsiders to pressure you.";
    }

    private String getBtnNotifications() {
        return isRussian ? "Настройки уведомлений" : "Notification settings";
    }

    private String getTextTip4() {
        return isRussian 
            ? "4. Многие забывают об этом. Но защиту нужно ставить не только на телефон, но и на SIM-карту. Если телефон украдут или отберут, то её могут использовать для кражи ваших аккаунтов или чтобы вас подставить. Если у вас физическая SIM-карта, то чтобы её защитить нужно установить на неё пин-код в настройках подключений."
            : "4. Many forget about this. But protection needs to be applied not only to the phone, but also to the SIM card. If the phone is stolen or taken away, SIM card can be used to steal your accounts or to frame you. If you have a physical SIM card, you need to set a PIN code for it in the connection settings to protect it.";
    }

    private String getBtnConnections() {
        return isRussian ? "Открыть настройки подключений" : "Open connection settings";
    }

    @Override
    protected void onResume() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onResume();
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

    private String getTextTip4Note() {
        return isRussian 
            ? "Это не сложно, но в некоторых случаях у вас перед установкой могут спросить пин-код оператора по умолчанию. Обычно это 0000. Но если сомневаетесь эту информацию легко найти в интернете почти под любого оператора. Код который вы зададите после этого не должен быть таким же простым!"
            : "It's not difficult, but in some cases, before setting it up you might be asked for the default carrier PIN. Usually, it's 0000. But if in doubt, this information is easy to find on the internet for almost any carrier. The code you set after this, shouldn't be as simple!";
    }
}
