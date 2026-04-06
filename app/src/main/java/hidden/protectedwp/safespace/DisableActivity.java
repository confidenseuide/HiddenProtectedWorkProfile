package hidden.protectedwp.safespace;

import android.app.*;
import android.app.admin.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.os.*;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.*;
import android.widget.*;
import java.util.*;

public class DisableActivity extends Activity {

    private DevicePolicyManager dpm;
    private ComponentName adminComponent;
    private SharedPreferences prefs;
    
    private static final String PREFS_NAME = "DisablePrefs";
    private static final String KEY_DISABLED_PACKAGES = "disabled_pkgs";

    private ArrayAdapter<String> adapter;
    private List<String> originalList = new ArrayList<>();
    private List<String> filteredList = new ArrayList<>();
    private EditText searchField;

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
        
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setBackgroundColor(Color.BLACK);
        layout.setPadding(32, 32, 32, 32);

        TextView title = new TextView(this);
        title.setText("Disable Apps");
        title.setTextColor(Color.WHITE);
        title.setTextSize(20);
        title.setPadding(0, 0, 0, 10);
        layout.addView(title);
        
        searchField = new EditText(this);
        searchField.setHint("Search packages...");
        searchField.setHintTextColor(Color.GRAY);
        searchField.setTextColor(Color.WHITE);
        searchField.setBackgroundColor(Color.parseColor("#222222"));
        searchField.setPadding(20, 20, 20, 20);
        layout.addView(searchField);

        ListView listView = new ListView(this);
        layout.addView(listView);
        setContentView(layout);

        dpm = (DevicePolicyManager) getSystemService(Context.DEVICE_POLICY_SERVICE);
        adminComponent = new ComponentName(this, MyDeviceAdminReceiver.class); 
        prefs = getSharedPreferences(PREFS_NAME, MODE_PRIVATE);

        initList(listView);
        setupSearch();
    }

    private void initList(ListView listView) {
        PackageManager pm = getPackageManager();
        List<ApplicationInfo> allApps = pm.getInstalledApplications(PackageManager.MATCH_UNINSTALLED_PACKAGES);
        Set<String> disabledInPrefs = new HashSet<>(prefs.getStringSet(KEY_DISABLED_PACKAGES, new HashSet<>()));

        originalList.clear();
        for (ApplicationInfo app : allApps) {
            if (!app.packageName.equals(getPackageName())) {
                String status = disabledInPrefs.contains(app.packageName) ? " [HIDDEN]" : "";
                originalList.add(app.packageName + status);
            }
        }
        Collections.sort(originalList);
        filteredList.addAll(originalList);

        adapter = new ArrayAdapter<String>(this, android.R.layout.simple_list_item_1, filteredList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                View view = super.getView(position, convertView, parent);
                TextView tv = (TextView) view.findViewById(android.R.id.text1);
                tv.setTextColor(Color.WHITE);
                return view;
            }
        };
        listView.setAdapter(adapter);

        listView.setOnItemClickListener((parent, view, position, id) -> {
            String currentItemText = filteredList.get(position);
            String pkgName = currentItemText.replace(" [HIDDEN]", "");
            
            boolean newStateIsHidden = togglePackageState(pkgName);

            String newStatusText = pkgName + (newStateIsHidden ? " [HIDDEN]" : "");
            
            filteredList.set(position, newStatusText);
            
            for (int i = 0; i < originalList.size(); i++) {
                if (originalList.get(i).startsWith(pkgName)) {
                    originalList.set(i, newStatusText);
                    break;
                }
            }
            
            adapter.notifyDataSetChanged();
        });
    }

    private void setupSearch() {
        searchField.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().toLowerCase();
                filteredList.clear();
                for (String item : originalList) {
                    if (item.toLowerCase().contains(query)) {
                        filteredList.add(item);
                    }
                }
                adapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private boolean togglePackageState(String pkgName) {
        Set<String> disabledInPrefs = new HashSet<>(prefs.getStringSet(KEY_DISABLED_PACKAGES, new HashSet<>()));
        boolean isCurrentlyDisabled = disabledInPrefs.contains(pkgName);
        boolean newStateHidden = !isCurrentlyDisabled;

        try {
            if (isCurrentlyDisabled) {
                dpm.setApplicationHidden(adminComponent, pkgName, false);
                dpm.setPackagesSuspended(adminComponent, new String[]{pkgName}, false);
                disabledInPrefs.remove(pkgName);
                Toast.makeText(this, pkgName + " restored", Toast.LENGTH_SHORT).show();
            } else {
                dpm.setApplicationHidden(adminComponent, pkgName, true);
                dpm.setPackagesSuspended(adminComponent, new String[]{pkgName}, true);
                disabledInPrefs.add(pkgName);
                Toast.makeText(this, pkgName + " frozen", Toast.LENGTH_SHORT).show();
            }
            prefs.edit().putStringSet(KEY_DISABLED_PACKAGES, disabledInPrefs).apply();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
            return isCurrentlyDisabled;
        }
        return newStateHidden;
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
                                new ComponentName(getPackageName(), DisableActivity.class.getName()), 
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
