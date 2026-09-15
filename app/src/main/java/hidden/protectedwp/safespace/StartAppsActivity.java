package hidden.protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.Drawable;
import android.os.*;
import android.view.*;
import android.text.*;
import android.widget.*;
import android.content.pm.LauncherActivityInfo;
import android.content.pm.LauncherApps;
import java.util.*;

public class StartAppsActivity extends Activity {

    private List<AppEntry> allApps = new ArrayList<>();
    private List<String> filteredNames = new ArrayList<>();
    private ArrayAdapter<String> adapter;
    private PackageManager pm;
    private LauncherApps launcherApps;
    private DevicePolicyManager dpm;

    private static class AppEntry implements Comparable<AppEntry> {
        String pkgName;
        String appName;
        Drawable icon;
        ComponentName launcherActivity;
        List<ActivityInfo> allExportedActivities;
        boolean fromLauncherApps;

        AppEntry(
            String pkgName,
            String appName,
            Drawable icon,
            ComponentName launcherActivity,
            List<ActivityInfo> all,
            boolean fromLauncherApps
        ) {
            this.pkgName = pkgName;
            this.appName = appName;
            this.icon = icon;
            this.launcherActivity = launcherActivity;
            this.allExportedActivities = all;
            this.fromLauncherApps = fromLauncherApps;
        }

        @Override
        public int compareTo(AppEntry other) {
            if (this.fromLauncherApps != other.fromLauncherApps) {
                return this.fromLauncherApps ? -1 : 1;
            }

            return this.appName.compareToIgnoreCase(other.appName);
        }
    }

    @Override
    protected void onResume() {
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        pm = getPackageManager();
        launcherApps = (LauncherApps) getSystemService(LAUNCHER_APPS_SERVICE);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(20, 20, 20, 20);

        EditText searchBar = new EditText(this);
        searchBar.setHint("Search package");
        searchBar.setHintTextColor(Color.GRAY);
        searchBar.setTextColor(Color.WHITE);
        searchBar.setBackgroundColor(Color.parseColor("#222222"));
        layout.addView(searchBar);

        ListView listView = new ListView(this);
        layout.addView(listView);

        setContentView(layout);

        adapter = new ArrayAdapter<String>(
            this,
            android.R.layout.simple_list_item_1,
            filteredNames
        ) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                LinearLayout row = new LinearLayout(StartAppsActivity.this);
                row.setOrientation(LinearLayout.HORIZONTAL);
                row.setPadding(30, 25, 30, 25);
                row.setGravity(Gravity.CENTER_VERTICAL);
                row.setBackgroundColor(Color.BLACK);

                AppEntry app = findEntry(filteredNames.get(position));

                ImageView iconView = new ImageView(StartAppsActivity.this);
                iconView.setImageDrawable(app.icon);
                row.addView(iconView, new LinearLayout.LayoutParams(100, 100));

                TextView tv = new TextView(StartAppsActivity.this);
                tv.setText(app.appName + " [" + app.pkgName + "]");
                tv.setTextColor(Color.WHITE);
                tv.setTextSize(15);
                tv.setPadding(30, 0, 10, 0);
                tv.setLayoutParams(
                    new LinearLayout.LayoutParams(0, -2, 1.0f)
                );
                row.addView(tv);

                return row;
            }
        };

        listView.setAdapter(adapter);

        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void onTextChanged(
                CharSequence s,
                int start,
                int before,
                int count
            ) {
                filter(s.toString());
            }

            @Override
            public void beforeTextChanged(
                CharSequence s,
                int start,
                int count,
                int after
            ) {}

            @Override
            public void afterTextChanged(Editable s) {}
        });

        listView.setOnItemClickListener(
            (parent, view, position, id) -> {
                String key = filteredNames.get(position);
                AppEntry entry = findEntry(key);

                if (entry != null) {
                    if (entry.launcherActivity != null) {
                        if (!launchWithLauncherApps(entry)) {
                            showActivitySelectionDialog(entry);
                        }
                    } else {
                        showActivitySelectionDialog(entry);
                    }
                } else {
                    Toast.makeText(
                        StartAppsActivity.this,
                        "App entry not found",
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        );

        listView.setOnItemLongClickListener(
            (parent, view, position, id) -> {
                String key = filteredNames.get(position);
                AppEntry entry = findEntry(key);

                if (entry != null && entry.launcherActivity == null) {
                    showActivitySelectionDialog(entry);
                } else if (entry == null) {
                    Toast.makeText(
                        StartAppsActivity.this,
                        "App entry not found",
                        Toast.LENGTH_LONG
                    ).show();
                }

                return true;
            }
        );

        loadAppsAsync();
    }

    private void showActivitySelectionDialog(AppEntry entry) {
        List<String> actNames = new ArrayList<>();

        for (ActivityInfo a : entry.allExportedActivities) {
            String shortName = a.name.replace(entry.pkgName, "");

            actNames.add(
                shortName.isEmpty()
                    ? a.name
                    : shortName
            );
        }

        if (actNames.isEmpty()) {
            Toast.makeText(
                this,
                "No exported activities: " + entry.pkgName,
                Toast.LENGTH_LONG
            ).show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(
            this,
            AlertDialog.THEME_DEVICE_DEFAULT_DARK
        );

        builder.setTitle(
            "Select Activity for " + entry.pkgName
        );

        builder.setItems(
            actNames.toArray(new String[0]),
            (dialog, which) -> {
                ActivityInfo selected =
                    entry.allExportedActivities.get(which);

                if (!launchActivity(selected)) {
                    Toast.makeText(
                        this,
                        "Failed to launch: " + selected.name,
                        Toast.LENGTH_LONG
                    ).show();
                }
            }
        );

        AlertDialog dialog = builder.create();
        dialog.show();

        if (dialog.getWindow() != null) {
            WindowManager.LayoutParams lp =
                dialog.getWindow().getAttributes();

            lp.gravity = Gravity.CENTER;
            lp.y = 0;

            dialog.getWindow().setAttributes(lp);
        }
    }

    private boolean launchWithLauncherApps(AppEntry entry) {
        try {
            UserHandle user =
                android.os.Process.myUserHandle();

            launcherApps.startMainActivity(
                entry.launcherActivity,
                user,
                null,
                null
            );

            return true;

        } catch (Exception e) {
            Toast.makeText(
                this,
                "Launcher launch error: "
                    + e.getClass().getSimpleName()
                    + ": "
                    + String.valueOf(e.getMessage()),
                Toast.LENGTH_LONG
            ).show();

            return false;
        }
    }

    private boolean launchActivity(ActivityInfo act) {
        try {
            Intent i = new Intent(Intent.ACTION_MAIN);

            i.setComponent(
                new ComponentName(
                    act.packageName,
                    act.name
                )
            );

            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);

            startActivity(i);

            return true;

        } catch (Exception e) {
            try {
                Intent fallback = new Intent();

                fallback.setComponent(
                    new ComponentName(
                        act.packageName,
                        act.name
                    )
                );

                fallback.addFlags(
                    Intent.FLAG_ACTIVITY_NEW_TASK
                );

                startActivity(fallback);

                return true;

            } catch (Exception e2) {
                Toast.makeText(
                    this,
                    "Activity launch error: "
                        + e2.getClass().getSimpleName()
                        + ": "
                        + String.valueOf(e2.getMessage()),
                    Toast.LENGTH_LONG
                ).show();

                return false;
            }
        }
    }

    private void loadAppsAsync() {
        ProgressDialog pd = new ProgressDialog(this);
        pd.setMessage("Loading...");
        pd.show();

        if (pd.getWindow() != null) {
            WindowManager.LayoutParams lp =
                pd.getWindow().getAttributes();

            lp.gravity = Gravity.CENTER;

            pd.getWindow().setAttributes(lp);
        }

        new Thread(() -> {
            List<AppEntry> loadedApps =
                new ArrayList<>();

            try {
                UserHandle user =
                    android.os.Process.myUserHandle();

                List<LauncherActivityInfo>
                    launcherActivities =
                        launcherApps.getActivityList(
                            null,
                            user
                        );

                for (LauncherActivityInfo info :
                     launcherActivities) {

                    ComponentName componentName =
                        info.getComponentName();

                    String pkgName =
                        componentName.getPackageName();

                    if (pkgName.equals(getPackageName())) {
                        continue;
                    }

                    String label =
                        info.getLabel() != null
                            ? info.getLabel().toString()
                            : pkgName;

                    String appName = label;

                    Drawable icon =
                        info.getIcon(0);

                    loadedApps.add(
                        new AppEntry(
                            pkgName,
                            appName,
                            icon,
                            componentName,
                            new ArrayList<ActivityInfo>(),
                            true
                        )
                    );
                }

            } catch (Exception e) {
                String error =
                    "LauncherApps load error: "
                    + e.getClass().getSimpleName()
                    + ": "
                    + String.valueOf(e.getMessage());

                runOnUiThread(() ->
                    Toast.makeText(
                        StartAppsActivity.this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                );
            }

            try {
                List<PackageInfo> packages =
                    pm.getInstalledPackages(
                        PackageManager.GET_ACTIVITIES
                        | PackageManager.MATCH_UNINSTALLED_PACKAGES
                    );

                Set<String> launcherPackages =
                    new HashSet<>();

                for (AppEntry entry : loadedApps) {
                    if (entry.launcherActivity != null) {
                        launcherPackages.add(
                            entry.pkgName
                        );
                    }
                }

                for (PackageInfo pkg : packages) {
                    if (pkg.packageName.equals(getPackageName())) {
                        continue;
                    }

                    if (launcherPackages.contains(pkg.packageName)) {
                        continue;
                    }

                    if (pkg.activities == null) {
                        continue;
                    }

                    List<ActivityInfo> exported =
                        new ArrayList<>();

                    for (ActivityInfo act : pkg.activities) {
                        if (!act.exported) {
                            continue;
                        }

                        exported.add(act);
                    }

                    if (!exported.isEmpty()) {
                        String appName =
                            pkg.applicationInfo
                                .loadLabel(pm)
                                .toString();

                        Drawable icon =
                            pkg.applicationInfo
                                .loadIcon(pm);

                        loadedApps.add(
                            new AppEntry(
                                pkg.packageName,
                                appName,
                                icon,
                                null,
                                exported,
                                false
                            )
                        );
                    }
                }

            } catch (Exception e) {
                String error =
                    "Package load error: "
                    + e.getClass().getSimpleName()
                    + ": "
                    + String.valueOf(e.getMessage());

                runOnUiThread(() ->
                    Toast.makeText(
                        StartAppsActivity.this,
                        error,
                        Toast.LENGTH_LONG
                    ).show()
                );
            }

            Collections.sort(loadedApps);

            allApps.clear();
            allApps.addAll(loadedApps);

            runOnUiThread(() -> {
                filter("");

                if (pd.isShowing()) {
                    pd.dismiss();
                }
            });

        }).start();
    }

    private void filter(String query) {
        filteredNames.clear();

        String q = query.toLowerCase();

        for (int i = 0; i < allApps.size(); i++) {
            AppEntry e = allApps.get(i);

            if (e.pkgName.toLowerCase().contains(q)
                    || e.appName.toLowerCase().contains(q)) {

                filteredNames.add(
                    e.pkgName + "|" + i
                );
            }
        }

        adapter.notifyDataSetChanged();
    }

    private AppEntry findEntry(String key) {
        int separator =
            key.lastIndexOf("|");

        if (separator >= 0) {
            try {
                int index = Integer.parseInt(
                    key.substring(separator + 1)
                );

                if (index >= 0 &&
                    index < allApps.size()) {

                    return allApps.get(index);
                }

            } catch (Exception e) {
                Toast.makeText(
                    this,
                    "Entry parse error: "
                        + e.getClass().getSimpleName()
                        + ": "
                        + String.valueOf(e.getMessage()),
                    Toast.LENGTH_LONG
                ).show();
            }
        }

        for (AppEntry e : allApps) {
            if (e.pkgName.equals(key)) {
                return e;
            }
        }

        return null;
    }
}
