package protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.view.*;
import android.text.*;
import android.widget.*;
import java.util.*;

public class StartAppsActivity extends Activity {

    private List<AppEntry> allApps = new ArrayList<>();
    private List<String> filteredNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private PackageManager pm;
	private DevicePolicyManager dpm;

    private static class AppEntry implements Comparable<AppEntry> {
        String pkgName;
        ActivityInfo bestActivity;
        List<ActivityInfo> allExportedActivities;

        AppEntry(String pkgName, ActivityInfo best, List<ActivityInfo> all) {
            this.pkgName = pkgName;
            this.bestActivity = best;
            this.allExportedActivities = all;
        }

        @Override
        public int compareTo(AppEntry other) {
            return this.pkgName.compareToIgnoreCase(other.pkgName);
        }
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
        pm = getPackageManager();

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(20, 20, 20, 20);

        EditText searchBar = new EditText(this);
        searchBar.setHint("Search package...");
        searchBar.setHintTextColor(Color.GRAY);
        searchBar.setTextColor(Color.WHITE);
        searchBar.setBackgroundColor(Color.parseColor("#222222"));
        layout.addView(searchBar);

        ListView listView = new ListView(this);
        layout.addView(listView);
        setContentView(layout);

        adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, filteredNames);
        listView.setAdapter(adapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filter(s.toString());
            }
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String pkgName = filteredNames.get(position);
            AppEntry entry = findEntry(pkgName);
            if (entry != null) {
                 if (!launchActivity(entry.bestActivity)) {
                    showActivitySelectionDialog(entry);
                }
            }
        });

     listView.setOnItemLongClickListener((parent, view, position, id) -> {
            String pkgName = filteredNames.get(position);
            AppEntry entry = findEntry(pkgName);
            if (entry != null) showActivitySelectionDialog(entry);
            return true;
        });

        loadAppsAsync();
    }

    private void showActivitySelectionDialog(AppEntry entry) {
        List<String> actNames = new ArrayList<>();
        for (ActivityInfo a : entry.allExportedActivities) {
             String shortName = a.name.replace(entry.pkgName, "");
            actNames.add(shortName.isEmpty() ? a.name : shortName);
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this, AlertDialog.THEME_DEVICE_DEFAULT_DARK);
        builder.setTitle("Select Activity for " + entry.pkgName);
        builder.setItems(actNames.toArray(new String[0]), (dialog, which) -> {
            ActivityInfo selected = entry.allExportedActivities.get(which);
            if (!launchActivity(selected)) {
                Toast.makeText(this, "Failed to launch this activity", Toast.LENGTH_SHORT).show();
            }
        });
        AlertDialog dialog = builder.create(); 
		dialog.show();
		if (dialog.getWindow() != null) {  
		WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
		lp.gravity = Gravity.CENTER;
		lp.y = 0;
		dialog.getWindow().setAttributes(lp);
		}
    }

    private boolean launchActivity(ActivityInfo act) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);
            i.setComponent(new ComponentName(act.packageName, act.name));
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            return true;
        } catch (Exception e) {
            try {
                Intent fallback = new Intent();
                fallback.setComponent(new ComponentName(act.packageName, act.name));
                fallback.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(fallback);
                return true;
            } catch (Exception e2) {
                return false;
            }
        }
    }

    private void loadAppsAsync() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Deep Scanning...");
        pd.show();
		if (pd.getWindow() != null) {
		WindowManager.LayoutParams lp = pd.getWindow().getAttributes();
		lp.gravity = Gravity.CENTER;
		pd.getWindow().setAttributes(lp);
		}

        new Thread(() -> {
            List<PackageInfo> packages = pm.getInstalledPackages(PackageManager.GET_ACTIVITIES | PackageManager.MATCH_UNINSTALLED_PACKAGES);
            for (PackageInfo pkg : packages) {
                if (pkg.packageName.equals(getPackageName())) continue;
                if (pkg.activities == null) continue;

                List<ActivityInfo> exported = new ArrayList<>();
                ActivityInfo best = null;
                int topScore = -1;
                String appLabel = pkg.applicationInfo.loadLabel(pm).toString();

                for (ActivityInfo act : pkg.activities) {
                    if (!act.exported) continue;
                    exported.add(act);

                    int score = calculateScore(act, appLabel);
                    if (score > topScore) {
                        topScore = score;
                        best = act;
                    }
                }
                if (!exported.isEmpty()) allApps.add(new AppEntry(pkg.packageName, best, exported));
            }
            Collections.sort(allApps);
            runOnUiThread(() -> { filter(""); pd.dismiss(); });
        }).start();
    }

    private int calculateScore(ActivityInfo act, String appLabel) {
        boolean hasMain = isAction(act, Intent.ACTION_MAIN);
        boolean hasLauncher = isCategory(act, Intent.CATEGORY_LAUNCHER);
        boolean noLabel = (act.labelRes == 0 && act.nonLocalizedLabel == null);
        boolean labelMatch = act.loadLabel(pm).toString().equalsIgnoreCase(appLabel);

        if (hasMain && hasLauncher) {
            if (noLabel) return 1000;
            if (labelMatch) return 900;
            return 800;
        }
        if (hasMain || hasLauncher) {
            int bonus = hasMain ? 100 : 0;
            if (noLabel) return 600 + bonus;
            if (labelMatch) return 500 + bonus;
            return 400 + bonus;
        }
        if (noLabel) return 300;
        if (labelMatch) return 200;
        return 100;
    }

    private void filter(String query) {
        filteredNames.clear();
        for (AppEntry e : allApps) {
            if (e.pkgName.toLowerCase().contains(query.toLowerCase())) filteredNames.add(e.pkgName);
        }
        adapter.notifyDataSetChanged();
    }

    private AppEntry findEntry(String pkg) {
        for (AppEntry e : allApps) if (e.pkgName.equals(pkg)) return e;
        return null;
    }

    private boolean isAction(ActivityInfo act, String action) {
        Intent i = new Intent(action).setPackage(act.packageName);
        List<ResolveInfo> rs = pm.queryIntentActivities(i, PackageManager.MATCH_ALL);
        for (ResolveInfo r : rs) if (r.activityInfo.name.equals(act.name)) return true;
        return false;
    }

    private boolean isCategory(ActivityInfo act, String cat) {
        Intent i = new Intent(Intent.ACTION_MAIN).addCategory(cat).setPackage(act.packageName);
        List<ResolveInfo> rs = pm.queryIntentActivities(i, PackageManager.MATCH_ALL);
        for (ResolveInfo r : rs) if (r.activityInfo.name.equals(act.name)) return true;
        return false;
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
                            new ComponentName(getPackageName(), StartAppsActivity.class.getName()), 
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
