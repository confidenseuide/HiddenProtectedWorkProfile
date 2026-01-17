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

public class AdditionalOptionsActivity extends Activity {

    private static final String PREFS_NAME = "HiderPrefs";
    private static final String KEY_WIPE_ENABLED = "wipe_on_failed_pwd";

    @Override
    protected void onResume() {
        super.onResume();
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
        final Context safeContext = createDeviceProtectedStorageContext();
        safeContext.moveSharedPreferencesFrom(this, PREFS_NAME);
        
        final SharedPreferences prefs = safeContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setBackgroundColor(Color.WHITE);
        int p = (int) TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, 24, getResources().getDisplayMetrics());
        root.setPadding(p, p, p, p);

        TextView title = new TextView(this);
        title.setText("Additional options");
        title.setTextSize(22);
        title.setTextColor(Color.BLACK);
        title.setPadding(0, 0, 0, p);
        root.addView(title);

        LinearLayout row = new LinearLayout(this);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setGravity(Gravity.CENTER_VERTICAL);

        TextView label = new TextView(this);
        label.setText("Wipe profile on 1 password filed attempt. This is useful for situations where you go to an unusual place where people might meet you and force you to unblock your profile.");
        label.setTextSize(16);
        label.setTextColor(Color.parseColor("#333333"));
        row.addView(label, new LinearLayout.LayoutParams(0, -2, 1.0f));

        final Switch sw = new Switch(this);
        sw.setChecked(prefs.getBoolean(KEY_WIPE_ENABLED, false));
        
        sw.setOnCheckedChangeListener((btn, isChecked) -> {
            new Thread(() -> {
                final boolean success = prefs.edit().putBoolean(KEY_WIPE_ENABLED, isChecked).commit();
                if (!success) {
                    runOnUiThread(() -> {
                        Toast.makeText(AdditionalOptionsActivity.this, "Memory error! Try again!", Toast.LENGTH_SHORT).show();
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
