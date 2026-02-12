package protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import java.util.*;

public class SetPasswordActivity extends Activity {

	private DevicePolicyManager dpm;

	private void showPasswordPrompt() {
    final android.app.Dialog dialog = new android.app.Dialog(this, android.R.style.Theme_Material_Light_Dialog);
	if (dialog.getWindow() != null) {dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);}
    dialog.requestWindowFeature(android.view.Window.FEATURE_NO_TITLE);
    
    android.widget.LinearLayout layout = new android.widget.LinearLayout(this);
    layout.setOrientation(android.widget.LinearLayout.VERTICAL);
    layout.setBackgroundColor(0xFFFFFFFF);
    int padding = (int) (24 * getResources().getDisplayMetrics().density);
    layout.setPadding(padding, padding, padding, padding);

    android.widget.TextView tv = new android.widget.TextView(this);
    tv.setText("Please set password for profile if you haven't done so yet. Use a password that is different from the password of the main profile. Don't use face unlock and fingerprints (it's not secure). If error on trying set system password, just wipe profile and create again. If you don't want wipe it, reboot phone.");
    tv.setTextSize(18);
    tv.setTextColor(0xFF000000);
    tv.setPadding(0, 0, 0, padding);
    layout.addView(tv);

    android.widget.LinearLayout.LayoutParams buttonParams = new android.widget.LinearLayout.LayoutParams(
            android.view.ViewGroup.LayoutParams.MATCH_PARENT, 
            android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
    buttonParams.setMargins(0, 8, 0, 8);

    android.graphics.drawable.GradientDrawable greenShape = new android.graphics.drawable.GradientDrawable();
    greenShape.setCornerRadius(8f);
    greenShape.setColor(0xFF4CAF50);

    android.widget.Button btnSet = new android.widget.Button(this);
    btnSet.setText("SET SYSTEM PASSWORD");
    btnSet.setTextColor(0xFFFFFFFF);
    btnSet.setBackground(greenShape);
    btnSet.setLayoutParams(buttonParams);
    btnSet.setOnClickListener(v -> {
        try {
            android.content.Intent intent = new android.content.Intent(android.app.admin.DevicePolicyManager.ACTION_SET_NEW_PASSWORD);
            startActivity(intent);
        } catch (Exception ignored) {}
    });
    layout.addView(btnSet);

	android.graphics.drawable.GradientDrawable blueShape = new android.graphics.drawable.GradientDrawable();
    blueShape.setCornerRadius(8f);
    blueShape.setColor(0xFF2196F3);
	android.widget.Button btnSecurity = new android.widget.Button(this);
	btnSecurity.setText("SET Show(Unhide) APPS PASSWORD");
	btnSecurity.setTextColor(0xFFFFFFFF);
	btnSecurity.setBackground(blueShape);
	btnSecurity.setLayoutParams(buttonParams);
	btnSecurity.setOnClickListener(v -> {
    try {
        Context appContext7 = getApplicationContext();
        Intent actions7 = new Intent(appContext7, SecurityActivity.class);
        actions7.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
        appContext7.startActivity(actions7);
    } catch (Exception ignored) {}
	});
	layout.addView(btnSecurity);

    android.graphics.drawable.GradientDrawable redShape = new android.graphics.drawable.GradientDrawable();
    redShape.setCornerRadius(8f);
    redShape.setColor(0xFFF44336);

    android.widget.Button btnClose = new android.widget.Button(this);
    btnClose.setText("ADDITIONAL ACTIONS / GO BACK");
    btnClose.setTextColor(0xFFFFFFFF);
    btnClose.setBackground(redShape);
    btnClose.setLayoutParams(buttonParams);
    btnClose.setOnClickListener(v -> {
        try {
            Context appContext2 = getApplicationContext();
			Intent actions2 = new Intent(appContext2, ActionsActivity.class);
			actions2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
			appContext2.startActivity(actions2);
        } catch (Exception ignored) {}
    });
    layout.addView(btnClose);
    dialog.setContentView(layout);
	android.view.Window window = dialog.getWindow();
    if (window != null) {
        WindowManager.LayoutParams lp = window.getAttributes();
        lp.gravity = Gravity.CENTER;
        lp.y = 0;
        lp.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.9);
        window.setAttributes(lp);
        window.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(android.graphics.Color.TRANSPARENT));
    }
    dialog.setCancelable(false);
    dialog.setCanceledOnTouchOutside(false);    
    dialog.show();
	}
	
    @Override
    protected void onResume() {
        super.onResume();
		dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);		
		if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		if (!isWorkProfileContext() && !hasWorkProfile()) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
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
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
		if (isWorkProfileContext()) {
			showPasswordPrompt();
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
                   if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), SetPasswordActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        }
    }, 1000);
	}
}
