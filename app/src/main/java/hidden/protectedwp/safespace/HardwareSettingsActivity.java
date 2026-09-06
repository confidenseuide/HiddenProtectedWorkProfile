package hidden.protectedwp.safespace;

import android.app.Activity;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.Context;
import android.os.Bundle;
import android.os.UserManager;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.Switch;

public class HardwareSettingsActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(54, 54, 54, 54);
        layout.setGravity(android.view.Gravity.TOP | android.view.Gravity.CENTER_HORIZONTAL);
                
        Switch wipeSwitch = new Switch(this);
        wipeSwitch.setText("Wipe work profile data on any incorrect password entry attempt on primary user lock screen (this feature can't work if app is stopped, work profile is paused, and in safe mode)");
        wipeSwitch.setTextSize(15);

        LinearLayout.LayoutParams wipeParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        wipeParams.topMargin = 48;
        wipeSwitch.setLayoutParams(wipeParams);

        layout.addView(wipeSwitch);
        setContentView(layout);

        Context deviceProtectedContext = createDeviceProtectedStorageContext();
        android.content.SharedPreferences prefs = deviceProtectedContext.getSharedPreferences("prefs", Context.MODE_PRIVATE);

        wipeSwitch.setChecked(prefs.getBoolean("x1337", false));

        wipeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            prefs.edit().putBoolean("x1337", isChecked).commit();
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY

            | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN

            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
            | View.SYSTEM_UI_FLAG_FULLSCREEN
        );
    }
}
