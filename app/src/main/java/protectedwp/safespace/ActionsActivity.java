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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setGravity(Gravity.CENTER);
        root.setBackgroundColor(Color.BLACK);

        LinearLayout contentBox = new LinearLayout(this);
        contentBox.setOrientation(LinearLayout.VERTICAL);
        contentBox.setGravity(Gravity.CENTER_HORIZONTAL);
        LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        boxParams.setMargins(60, 0, 60, 0);
        contentBox.setLayoutParams(boxParams);

        TextView title = new TextView(this);
        title.setText("What to do?");
        title.setTextSize(24);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.WHITE);
        title.setPadding(0, 0, 0, 40);
        title.setGravity(Gravity.CENTER);
        contentBox.addView(title);

        ListView listView = new ListView(this);
        LinearLayout.LayoutParams listParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT);
        listView.setLayoutParams(listParams);
        contentBox.addView(listView);

        root.addView(contentBox);
        setContentView(root);

        labelToClass.put(CLOSE_APP_LABEL, "ACTION_CLOSE");
        loadNonLauncherActivities();

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
            } else {
                try {
					if (label.equals("ProtectedWorkProfile") || label.equals("ShowApps&SetUp")) {
					unlock();
					}
					if (!label.equals("ProtectedWorkProfile") && !label.equals("ShowApps&SetUp")) {
                    Intent i = new Intent();
                    i.setComponent(new ComponentName(getPackageName(), className));
                    startActivity(i);}
                } catch (Exception ignored) {}
            }
        });
    }

	private void unlock() {
		KeyguardManager km = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
		km.requestDismissKeyguard(this, new KeyguardManager.KeyguardDismissCallback() {
    @Override
    public void onDismissSucceeded() {

		ActionsActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("isDone", false).apply();
	
		Intent i1 = new Intent();
                    i1.setComponent(new ComponentName(getPackageName(), MainActivity.class));
                    startActivity(i1);
	}

    @Override
    public void onDismissCancelled() {
        unlock();
    }

    @Override
    public void onDismissError() {
        unlock();
    }
});
		
	}

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                | View.SYSTEM_UI_FLAG_FULLSCREEN);
    }

    private void loadNonLauncherActivities() {
        try {
            PackageManager pm = getPackageManager();
            PackageInfo pi = pm.getPackageInfo(getPackageName(), PackageManager.GET_ACTIVITIES);
            
            Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
            mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);
            mainIntent.setPackage(getPackageName());
            List<android.content.pm.ResolveInfo> launcherApps = pm.queryIntentActivities(mainIntent, 0);
            
            List<String> launcherClassNames = new ArrayList<>();
            for (android.content.pm.ResolveInfo ri : launcherApps) {
                launcherClassNames.add(ri.activityInfo.name);
            }

            for (ActivityInfo info : pi.activities) {
                if (!info.name.equals(this.getClass().getName()) && !launcherClassNames.contains(info.name)) {
                    String label = info.loadLabel(pm).toString();

					if (label.equals(info.name) || label.isEmpty()) {
                        String[] parts = info.name.split("\\.");
                        label = parts[parts.length - 1];
                    }
					if (label.equals("ProtectedWorkProfile")) {
						label = "ShowApps&SetUp";
						
					}
                    labelToClass.put(label, info.name);
                }
            }
        } catch (Exception ignored) {}
    }
    
    @Override
    protected void onResume() {
        super.onResume();
		if (isWorkProfileContext()) {
			getWindow().getDecorView().setKeepScreenOn(true);
			hideSystemUI();
		}	
		
		if (!isWorkProfileContext() && hasWorkProfile()) {
            launchWorkProfileDelayed();
		}
		
		if (!isWorkProfileContext() && !hasWorkProfile()) {
			Intent intent = new Intent(this, MainActivity.class);
			intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
			startActivity(intent);
			finish();
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

            LauncherApps launcherApps = (LauncherApps) getSystemService(Context.LAUNCHER_APPS_SERVICE);
            UserManager userManager = (UserManager) getSystemService(Context.USER_SERVICE);
            
            if (launcherApps != null && userManager != null) {
                List<UserHandle> profiles = userManager.getUserProfiles();
                for (UserHandle profile : profiles) {
                   if (userManager.getSerialNumberForUser(profile) != 0) {
                        launcherApps.startMainActivity(
                            new ComponentName(getPackageName(), ActionsActivity.class.getName()), 
                            profile, null, null
                        );
                        
                        finishAndRemoveTask();
                        break;
                    }
                }
            }
        }
}
