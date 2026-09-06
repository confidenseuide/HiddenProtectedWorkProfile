package hidden.protectedwp.safespace;

import android.content.Intent;
import android.widget.Toast;
import android.widget.Button;
import android.content.pm.LauncherApps;
import android.os.UserHandle;
import java.util.List;
import android.provider.Settings;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.admin.DeviceAdminInfo;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.UserManager;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import java.util.Locale;

public class CopeActivity extends Activity {

    @Override
    protected void onResume() {
        super.onResume();
        getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_LAYOUT_STABLE | android.view.View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);  
    }

    private TextView customInputDisplay;
    private StringBuilder currentInput = new StringBuilder();

    private TextView text;
    private AlertDialog copeOwnerDialog;
    private AlertDialog usbWarningDialog;
    private LinearLayout buttonBox;

    private boolean isEn() { return !Locale.getDefault().getLanguage().equals("ru"); }

        
    private boolean isCopeOwner() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        boolean isCOPE = dpm != null && android.os.Build.VERSION.SDK_INT >= 30 && dpm.isOrganizationOwnedDeviceWithManagedProfile() && dpm.isProfileOwnerApp(getPackageName());
        return isCOPE;
    }

    private void setButtonState(Button b, boolean enabled) {
    b.setEnabled(enabled);

    GradientDrawable shape = new GradientDrawable();
    shape.setShape(GradientDrawable.RECTANGLE);
    shape.setColor(
            enabled
                    ? Color.parseColor("#34495e")
                    : Color.parseColor("#4a6278")
    );
    shape.setCornerRadius(6f);

    b.setBackground(shape);
    }


     private void showCopeOwnerInstruction() {
                                            
            String pkg = getPackageName();
            long userId = ((UserManager) getSystemService(Context.USER_SERVICE)).getSerialNumberForUser(android.os.Process.myUserHandle()); 
            String admin = " " + pkg + "/.MyDeviceAdminReceiver";

            String universalCommand =                     
                    "adb shell dpm mark-profile-owner-on-organization-owned-device --user " + userId + admin;         
                     
        final String message = isEn()
                ? "These features are available only if you have COPE rights, grant them via the ADB command:\n" + universalCommand
                : "Эти функции доступны только при наличии прав COPE, выдайте их через ADB комманду:\n" + universalCommand;


        copeOwnerDialog = new AlertDialog.Builder(this)
                .setMessage(message)
                .setPositiveButton("OK", (dialog, which) -> copeOwnerDialog = null)
                .create();

        copeOwnerDialog.setOnShowListener(d -> {
            TextView messageView = copeOwnerDialog.findViewById(android.R.id.message);
            if (messageView != null) {
                messageView.setTextIsSelectable(true);
            }
        });

        copeOwnerDialog.setOnDismissListener(dialog -> copeOwnerDialog = null);
        copeOwnerDialog.show();

        Window window = copeOwnerDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.x = 0;
            params.y = 0;
            window.setAttributes(params);
        }
    }
    
    private void showUsbWarningAlert() {
        if (usbWarningDialog != null && usbWarningDialog.isShowing()) return;

        String alertTitle = isEn() ? "Warning:" : "Предупреждение:";
        String alertMsg = isEn()
                ? "Disabling USB functions occurs at the operating system level and does not affect the low-level logic of the USB port, meaning it does not provide 100% protection.\nThis is just a step towards security.\nIf you want the ability to completely disable the USB port, it is better to use the GrapheneOS operating system."
                : "Отключение USB функций происходит на уровне операционной системы и не затрагивает низкоуровневую логику USB порта, тоесть не даёт 100-процентной защиты.\nЭто лишь шаг к безопасности.\nЕсли вы хотите возможность полного отключения USB порта, лучше использовать операционную систему GrapheneOS.";

        usbWarningDialog = new AlertDialog.Builder(this)
                .setTitle(alertTitle)
                .setMessage(alertMsg)
                .setPositiveButton("OK", (dialog, which) -> usbWarningDialog = null)
                .create();

        Window window = usbWarningDialog.getWindow();
        if (window != null) {
            WindowManager.LayoutParams params = window.getAttributes();
            params.gravity = Gravity.CENTER;
            params.x = 0;
            params.y = 0;
            window.setAttributes(params);
        }

        usbWarningDialog.setOnShowListener(dialog -> {
            TextView titleView = usbWarningDialog.findViewById(getResources().getIdentifier("alertTitle", "id", "android"));
            if (titleView != null) {
                titleView.setTextColor(Color.parseColor("#ff5555"));
            }
            TextView msgView = usbWarningDialog.findViewById(android.R.id.message);
            if (msgView != null) {
                msgView.setTextIsSelectable(true);
            }
        });

        usbWarningDialog.setOnDismissListener(dialog -> usbWarningDialog = null);
        usbWarningDialog.show();
    }

    @Override
    protected void onCreate(Bundle b) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(b);        
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        ScrollView scrollView = new ScrollView(this);
        scrollView.setFillViewport(true);
        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setPadding(64, 64, 64, 64);

        text = new TextView(this);
        text.setGravity(Gravity.CENTER_HORIZONTAL);
        text.setTextSize(16f);
        text.setTextColor(Color.WHITE);

        buttonBox = new LinearLayout(this);
        buttonBox.setOrientation(LinearLayout.VERTICAL);
        buttonBox.setGravity(Gravity.CENTER);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(0, 64, 0, 0);
        buttonBox.setLayoutParams(boxParams);

        root.addView(text);
        root.addView(buttonBox);
        scrollView.addView(root);
        setContentView(scrollView);

        renderMainSettingsMenu();
    }

    private void render(String textValue) { text.setText(textValue); }


    private void renderMainSettingsMenu() {
    buttonBox.removeAllViews();

    DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
    ComponentName adminName = new ComponentName(this, MyDeviceAdminReceiver.class);
    boolean isCO = isCopeOwner();
    DevicePolicyManager parentDpm = isCopeOwner() ? dpm.getParentProfileInstance(adminName) : null;
    
    if (Build.VERSION.SDK_INT >= 31) {
        CheckBox cbUsbAndDebug = new CheckBox(this);
        cbUsbAndDebug.setText(isEn() ? "Disallow mount physical media, USB-connetions and debugging features on the entire device" : "Запретить монтирование физических носителей, USB-подключения и функции отладки на всем устройстве");
        cbUsbAndDebug.setTextColor(Color.WHITE);
        cbUsbAndDebug.setTextSize(16f);

        if (isCO) {
            boolean usbDataDisabled = !dpm.isUsbDataSignalingEnabled();    
            boolean mountMediaDisabled = parentDpm.getUserRestrictions(adminName).getBoolean(UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA, false);         
            boolean usbFileTransferDisabled = parentDpm.getUserRestrictions(adminName).getBoolean(UserManager.DISALLOW_USB_FILE_TRANSFER, false);
            boolean adbDisabled = parentDpm.getUserRestrictions(adminName).getBoolean(UserManager.DISALLOW_DEBUGGING_FEATURES, false);         
            
            cbUsbAndDebug.setChecked(usbDataDisabled && usbFileTransferDisabled && adbDisabled && mountMediaDisabled);
        } else {
            cbUsbAndDebug.setChecked(false);
            cbUsbAndDebug.setAlpha(0.5f);
        }

        cbUsbAndDebug.setOnClickListener(v -> {
            if (!isCO) {
                cbUsbAndDebug.setChecked(false);
                showCopeOwnerInstruction();
                return;
            }
            if (cbUsbAndDebug.isChecked()) {             
                    dpm.setUsbDataSignalingEnabled(false);                         
                    parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER);             
                    parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES); 
                    parentDpm.addUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);                     
                    showUsbWarningAlert();                                                   
            } else {
                dpm.setUsbDataSignalingEnabled(true);               
                parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_USB_FILE_TRANSFER);    
                parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_DEBUGGING_FEATURES);
                parentDpm.clearUserRestriction(adminName, UserManager.DISALLOW_MOUNT_PHYSICAL_MEDIA);                                                                    
            }
        });
        buttonBox.addView(cbUsbAndDebug);
    }

    
    boolean isGranted = dpm != null && dpm.hasGrantedPolicy(new ComponentName(this, MyDeviceAdminReceiver.class), DeviceAdminInfo.USES_POLICY_DISABLE_KEYGUARD_FEATURES);

    CheckBox cbRestrictions2 = new CheckBox(this);
    cbRestrictions2.setText(isEn() ? "Disallow trust agents, biometric unlock, and notifications on the lock screen on main profile" : "Запретить агентов доверия, разблокировку по биометрии, и уведомления на экране блокировки в основном профиле");
    cbRestrictions2.setTextColor(Color.WHITE);
    cbRestrictions2.setTextSize(16f);

    if (isCO && isGranted) {
        int disabledFeatures = parentDpm.getKeyguardDisabledFeatures(adminName);
        boolean trustAgentsDisabled = (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS) != 0;
        boolean biometricsDisabled = (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS) != 0;
		boolean secureNotificationsDisabled = (disabledFeatures & DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS) != 0;

        cbRestrictions2.setChecked(trustAgentsDisabled && biometricsDisabled && secureNotificationsDisabled);      
    } else {
        cbRestrictions2.setChecked(false);
        cbRestrictions2.setAlpha(0.5f);
    }

    cbRestrictions2.setOnClickListener(v -> {
        if (!isCO || !isGranted) {
            cbRestrictions2.setChecked(false);
            if (!isCO) showCopeOwnerInstruction();
            return;
        }
        int currentFeatures = dpm.getKeyguardDisabledFeatures(adminName);
        if (cbRestrictions2.isChecked()) {
            int pcur = parentDpm.getKeyguardDisabledFeatures(adminName);    
                parentDpm.setKeyguardDisabledFeatures(adminName, pcur 
                    | DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS 
                    | DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS
                    | DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);
            
        } else {
           int pcur = parentDpm.getKeyguardDisabledFeatures(adminName);
                parentDpm.setKeyguardDisabledFeatures(adminName, pcur 
                    & ~DevicePolicyManager.KEYGUARD_DISABLE_TRUST_AGENTS 
                    & ~DevicePolicyManager.KEYGUARD_DISABLE_BIOMETRICS
                    & ~DevicePolicyManager.KEYGUARD_DISABLE_SECURE_NOTIFICATIONS);                     
        }
    });
    buttonBox.addView(cbRestrictions2);

	CheckBox cbCamAndScr = new CheckBox(this);
		cbCamAndScr.setText(isEn()
        ? "Disallow camera and screenshots in work profile"
        : "Запретить камеру и скриншоты в рабочем профиле");
		cbCamAndScr.setTextColor(Color.WHITE);
		cbCamAndScr.setTextSize(15f);

		boolean hasNoCam = dpm.getCameraDisabled(adminName);
		boolean hasNoScr = dpm.getScreenCaptureDisabled(adminName);                

		cbCamAndScr.setChecked(hasNoCam && hasNoScr);

		cbCamAndScr.setOnClickListener(v -> {
   
		boolean disable = cbCamAndScr.isChecked();
   
		dpm.setCameraDisabled(adminName, disable);
  
		dpm.setScreenCaptureDisabled(adminName, disable);
        });

        buttonBox.addView(cbCamAndScr);
	
	
    CheckBox cbCameraAndCapture = new CheckBox(this);
        cbCameraAndCapture.setText(isEn()
        ? "Disallow camera and screenshots on main profile"
        : "Запретить камеру и скриншоты в основном профиле");
        cbCameraAndCapture.setTextColor(Color.WHITE);
        cbCameraAndCapture.setTextSize(15f);
        
        if (isCO) {    
            
           boolean isCamDisabled = parentDpm.getCameraDisabled(adminName);
        
           boolean isScrDisabled = parentDpm.getScreenCaptureDisabled(adminName);                
    
           cbCameraAndCapture.setChecked(isCamDisabled && isScrDisabled);

        } else {    
            cbCameraAndCapture.setChecked(false);    
            cbCameraAndCapture.setAlpha(0.5f);
        }


        cbCameraAndCapture.setOnClickListener(v -> {
    
            if (!isCO) {     
                cbCameraAndCapture.setChecked(false);      
                showCopeOwnerInstruction();         
                return;    
            }

    boolean shouldDisable = cbCameraAndCapture.isChecked();
	
    parentDpm.setCameraDisabled(adminName, shouldDisable);
    parentDpm.setScreenCaptureDisabled(adminName, shouldDisable);
            
    });
				
    buttonBox.addView(cbCameraAndCapture);

    
    Button btnSetWorkPassword = new Button(this);
    btnSetWorkPassword.setText(
        isEn() ? "Set password for work profile"
               : "Установить пароль для рабочего профиля"
    );

    GradientDrawable shape = new GradientDrawable();
    shape.setShape(GradientDrawable.RECTANGLE);
    shape.setColor(Color.parseColor("#34495e"));
    shape.setCornerRadius(6f);

    btnSetWorkPassword.setBackground(shape);
    btnSetWorkPassword.setTextColor(Color.WHITE);
    btnSetWorkPassword.setPadding(32, 32, 32, 32);

    LinearLayout.LayoutParams btnParams = new LinearLayout.LayoutParams(
        LinearLayout.LayoutParams.MATCH_PARENT,
        LinearLayout.LayoutParams.WRAP_CONTENT
    );
    btnParams.setMargins(0, 16, 0, 16);
    btnSetWorkPassword.setLayoutParams(btnParams);

    btnSetWorkPassword.setOnClickListener(v -> {        
            Intent intent = new Intent(DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivity(intent);  
            finish();
    });

    buttonBox.addView(btnSetWorkPassword);

     boolean isSeparate = false;
     try {
        DevicePolicyManager ldpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        ComponentName ladminName = new ComponentName(this, MyDeviceAdminReceiver.class);     
        isSeparate = !ldpm.isUsingUnifiedPassword(ladminName);
    } catch (Throwable t) {
        isSeparate = false;
    }
                    
    if (isSeparate) {
    Button btnSetWorkAttempts = new Button(this);

        btnSetWorkAttempts.setText(
        isEn()
                ? "Set unlock attempts limit for work profile"
                : "Установить лимит попыток разблокировки рабочего профиля"
        );

        GradientDrawable attemptsShape = new GradientDrawable();
        attemptsShape.setShape(GradientDrawable.RECTANGLE);
        attemptsShape.setColor(Color.parseColor("#34495e"));
        attemptsShape.setCornerRadius(6f);

        btnSetWorkAttempts.setBackground(attemptsShape);
        btnSetWorkAttempts.setTextColor(Color.WHITE);
        btnSetWorkAttempts.setPadding(32, 32, 32, 32);

        LinearLayout.LayoutParams attemptsParams =
        new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );


        attemptsParams.setMargins(0, 15, 0, 15);
        btnSetWorkAttempts.setLayoutParams(attemptsParams);
        
        btnSetWorkAttempts.setOnClickListener(v -> {    
            render(
            isEn()
                    ? "Set the maximum number of failed unlock attempts before reset for the work profile. This reset can wipe main profile too if you used the ADB command specified in the toggles."
                    : "Задайте максимальное количество неверных попыток разблокировки до сброса для рабочего профиля. Этот сброс может стереть и основной профиль если вы использовали ADB команду указанную в переключателях."    
            );

          renderWorkProfileAttemptsInput();
        });

        buttonBox.addView(btnSetWorkAttempts);
            
    }

        Button btnBack = new Button(this);
        btnBack.setText(isEn() ? "Main menu" : "Главное меню");
        GradientDrawable backShape = new GradientDrawable();
        backShape.setShape(GradientDrawable.RECTANGLE);
        backShape.setColor(Color.parseColor("#34495e"));
        backShape.setCornerRadius(6f);
        btnBack.setBackground(backShape);
        btnBack.setTextColor(Color.WHITE);
        btnBack.setPadding(32, 32, 32, 32);

        LinearLayout.LayoutParams backParams = new LinearLayout.LayoutParams(   
            LinearLayout.LayoutParams.MATCH_PARENT,  
            LinearLayout.LayoutParams.WRAP_CONTENT
        );

        backParams.setMargins(0, 15, 0, 15);
        btnBack.setLayoutParams(backParams);
        btnBack.setOnClickListener(v -> startActivity(new Intent(this, ActionsActivity.class)));
        buttonBox.addView(btnBack);
    }

    @Override
    protected void onDestroy() {
        if (copeOwnerDialog != null && copeOwnerDialog.isShowing()) {
            copeOwnerDialog.dismiss();
        }
        copeOwnerDialog = null;

        if (usbWarningDialog != null && usbWarningDialog.isShowing()) {
            usbWarningDialog.dismiss();
        }
        usbWarningDialog = null;

        super.onDestroy();
    }

    private void renderWorkProfileAttemptsInput() {
    buttonBox.removeAllViews();
    currentInput.setLength(0);

    TextView customInputDisplay = new TextView(this);
    customInputDisplay.setGravity(Gravity.CENTER);
    customInputDisplay.setTextSize(22f);
    customInputDisplay.setTextColor(Color.WHITE);
    customInputDisplay.setText("");
    customInputDisplay.setPadding(32, 32, 32, 32);

    GradientDrawable bgShape = new GradientDrawable();
    bgShape.setShape(GradientDrawable.RECTANGLE);
    bgShape.setColor(Color.parseColor("#2c3e50"));
    bgShape.setCornerRadius(8f);
    bgShape.setStroke(2, Color.parseColor("#7f8c8d"));

    customInputDisplay.setBackground(bgShape);

    LinearLayout.LayoutParams displayParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    displayParams.setMargins(0, 0, 0, 16);
    customInputDisplay.setLayoutParams(displayParams);

    buttonBox.addView(customInputDisplay);

    LinearLayout keypadBox = new LinearLayout(this);
    keypadBox.setOrientation(LinearLayout.VERTICAL);
    keypadBox.setGravity(Gravity.CENTER);

    LinearLayout.LayoutParams keypadParams =
            new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
            );

    keypadParams.setMargins(0, 16, 0, 16);
    keypadBox.setLayoutParams(keypadParams);

    final Button[] okBtnRef = new Button[1];

    String[][] keys = {
            {"1", "2", "3"},
            {"4", "5", "6"},
            {"7", "8", "9"},
            {"⌫", "0", "OK"}
    };

    for (String[] rowKeys : keys) {

        LinearLayout rowLayout = new LinearLayout(this);
        rowLayout.setOrientation(LinearLayout.HORIZONTAL);
        rowLayout.setGravity(Gravity.CENTER);

        LinearLayout.LayoutParams rowParams =
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );

        rowParams.setMargins(0, 4, 0, 4);
        rowLayout.setLayoutParams(rowParams);

        for (String key : rowKeys) {

            Button keyBtn = new Button(this);
            keyBtn.setText(key);

            GradientDrawable keyShape = new GradientDrawable();
            keyShape.setShape(GradientDrawable.RECTANGLE);

            boolean isOk = key.equals("OK");

            if (isOk) {
                keyBtn.setEnabled(false);
                keyShape.setColor(Color.parseColor("#4a6278"));
                okBtnRef[0] = keyBtn;
            } else {
                keyShape.setColor(Color.parseColor("#34495e"));
            }

            keyShape.setCornerRadius(6f);

            keyBtn.setBackground(keyShape);
            keyBtn.setTextColor(Color.WHITE);
            keyBtn.setTextSize(20f);
            keyBtn.setPadding(16, 24, 16, 24);

            LinearLayout.LayoutParams keyParams =
                    new LinearLayout.LayoutParams(
                            0,
                            LinearLayout.LayoutParams.WRAP_CONTENT,
                            1.0f
                    );

            keyParams.setMargins(4, 0, 4, 0);
            keyBtn.setLayoutParams(keyParams);

            keyBtn.setOnClickListener(v -> {

                if (key.equals("⌫")) {

                    if (currentInput.length() > 0) {
                        currentInput.deleteCharAt(
                                currentInput.length() - 1
                        );
                    }

                } else if (isOk) {

                    if (!keyBtn.isEnabled()) {
                        return;
                    }

                    try {
                        int attempts = Integer.parseInt(
                                currentInput.toString()
                        );

                        if (attempts <= 0) {
                            return;
                        }

                        DevicePolicyManager dpm =
                                (DevicePolicyManager) getSystemService(
                                        Context.DEVICE_POLICY_SERVICE
                                );

                        ComponentName adminName =
                                new ComponentName(
                                        this,
                                        MyDeviceAdminReceiver.class
                                );

                                            
                            dpm.setMaximumFailedPasswordsForWipe(
                                    adminName,
                                    attempts
                            );

                            Toast.makeText(
                                    this,
                                    isEn()
                                            ? "Unlock attempts limit applied"
                                            : "Лимит попыток разблокировки применён",
                                    Toast.LENGTH_SHORT
                            ).show();

                            renderMainSettingsMenu();
                        

                    } catch (NumberFormatException ignored) {
                    }

                } else {

                    if (currentInput.length() < 9) {
                        currentInput.append(key);
                    }
                }

                customInputDisplay.setText(
                        currentInput.toString()
                );

                boolean valid = false;

                if (currentInput.length() > 0) {
                    try {
                        long value = Long.parseLong(
                                currentInput.toString()
                        );

                        valid =
                                value > 0
                                        && value <= Integer.MAX_VALUE;

                    } catch (NumberFormatException ignored) {
                        valid = false;
                    }
                }

                setButtonState(
                        okBtnRef[0],
                        valid
                );
            });

            rowLayout.addView(keyBtn);
        }

        keypadBox.addView(rowLayout);
    }

    buttonBox.addView(keypadBox);

    }

   
    
}
