package protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.os.*;
import android.view.*;
import android.widget.*;
import android.graphics.*;
import android.util.*;
import java.util.*;

public class AdditionalOptionsActivity extends Activity {

    private static final String PREFS_NAME = "HiderPrefs";
    private static final String KEY_WIPE_ENABLED = "wipe_on_failed_pwd";

	private void showWipeLimitDialog() {
    final android.widget.EditText input = new android.widget.EditText(this);
    input.setInputType(android.text.InputType.TYPE_CLASS_NUMBER);
    android.widget.FrameLayout container = new android.widget.FrameLayout(this);
    android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(-1, -2);
    int margin = (int) android.util.TypedValue.applyDimension(android.util.TypedValue.COMPLEX_UNIT_DIP, 20, getResources().getDisplayMetrics());
    params.leftMargin = margin; params.rightMargin = margin;
    input.setLayoutParams(params);
    container.addView(input);
    final android.app.AlertDialog dialog = new android.app.AlertDialog.Builder(this).setMessage("Set limit of failed system password attempts to wipe profile data. Even if profile paused (app stopped), when trying to enter profile, this limit continues to work at system level. If set limit to 1, this may become more reliable replacement for Duress Password, since instead of it just enter incorrect password and the system itself performs wipe. But in such a case, high risk of accidental wipe. Set this limit (1) when going outside home and don't intend to use profile. + On street higher risk be forced to enter password. If work profile doesn't have separate password from main profile, this limit will be applied to screen unlock. And most balanced option for work in HOME - is 3. Don't set 0. 0 - no limit.").setView(container).setCancelable(false).setPositiveButton("OK", new android.content.DialogInterface.OnClickListener() {
        @Override
        public void onClick(android.content.DialogInterface dialog, int which) {
            String val = input.getText().toString();
            if (val.isEmpty()) {showWipeLimitDialog(); return;}
            try {
                int limit = Integer.parseInt(val);
                android.app.admin.DevicePolicyManager dpm = (android.app.admin.DevicePolicyManager) getSystemService(android.content.Context.DEVICE_POLICY_SERVICE);
                android.content.ComponentName adminComponent = new android.content.ComponentName(AdditionalOptionsActivity.this, MyDeviceAdminReceiver.class);
                dpm.setMaximumFailedPasswordsForWipe(adminComponent, limit);
                int factLimit = dpm.getMaximumFailedPasswordsForWipe(adminComponent);
				if (limit==0){android.widget.Toast.makeText(AdditionalOptionsActivity.this, "Please don't set 0. Is - no limit.", android.widget.Toast.LENGTH_LONG).show();}
				else{
                android.widget.Toast.makeText(AdditionalOptionsActivity.this, "Password failed attempts for wipe: " + factLimit + ".", android.widget.Toast.LENGTH_LONG).show();}
				showWipeLimitDialog();
				try {
					if (limit!=0){
					Context appContext2 = getApplicationContext();
					Intent actions2 = new Intent(appContext2, ActionsActivity.class);
					actions2.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_MULTIPLE_TASK);
					appContext2.startActivity(actions2);}
				} catch (Throwable tirex) {}
				return;
			} catch (Throwable t) {
                android.widget.TextView errorView = new android.widget.TextView(AdditionalOptionsActivity.this);
                errorView.setText(t.getMessage()); errorView.setTextIsSelectable(true); errorView.setPadding(60, 40, 60, 0);
                new android.app.AlertDialog.Builder(AdditionalOptionsActivity.this).setTitle("Err:").setView(errorView).setPositiveButton("OK", null).show();
            }
        }
    }).create();
    dialog.getWindow().addFlags(android.view.WindowManager.LayoutParams.FLAG_SECURE);
    dialog.getWindow().getDecorView().setSystemUiVisibility(android.view.View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY | android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION | android.view.View.SYSTEM_UI_FLAG_FULLSCREEN);
    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    
	android.view.Window window = dialog.getWindow();
	if (window != null) {
    window.setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    android.view.WindowManager.LayoutParams lp = window.getAttributes();
    lp.gravity = android.view.Gravity.CENTER;
    lp.y = 0; 
    lp.width = android.view.WindowManager.LayoutParams.MATCH_PARENT; 
    window.setAttributes(lp);
	}
	dialog.show();
	input.requestFocus();
    input.requestFocus();
	}

    @Override
    protected void onResume() {
        super.onResume();
		getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
		getWindow().getDecorView().setKeepScreenOn(true);
        getWindow().getDecorView().setSystemUiVisibility(
			View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
			| View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_FULLSCREEN
			| View.SYSTEM_UI_FLAG_LAYOUT_STABLE
			| View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
			| View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );
		if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		if (!isWorkProfileContext() && !hasWorkProfile()) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
		}}
	
	private boolean isWorkProfileContext() {
        DevicePolicyManager dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        return dpm.isProfileOwnerApp(getPackageName());
    }

    private boolean hasWorkProfile() {
        UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
        return userManager.getUserProfiles().size() > 1;
    }

    private void launchWorkProfileDelayed() {

            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            
            if (launcherApps != null && userManager != null) {
                List<UserHandle> profiles = userManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                   if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), AdditionalOptionsActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        }
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        super.onCreate(savedInstanceState);
        showWipeLimitDialog();
    }
}
