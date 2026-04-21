package hidden.protectedwp.safespace;

import android.app.Activity;
import android.app.AlertDialog;
import android.view.*;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.content.Intent;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.WindowManager;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Switch;

public class HighEfficiencyModeActivity extends Activity {

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
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_SECURE);
        
        super.onCreate(savedInstanceState);

        DisplayMetrics dm = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(dm);
        int screenHeight = dm.heightPixels;
        int screenWidth = dm.widthPixels;

        ScrollView scrollView = new ScrollView(this);
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);

        int topMargin = (int) (screenHeight * 0.07);
        int sidePadding = (int) (16 * dm.density);
        layout.setPadding(sidePadding, topMargin, sidePadding, sidePadding);

        final Switch toggle = new Switch(this);
        
        toggle.setText("Enable High-Efficiency Mode (enabled by default) ? In this mode, the app will use alarms and receivers to continuously restart itself and maintain a high process priority, to make it harder for aggressive firmware to kill it and ensure its restart after death. The app will also launch 10 short WakeLocks, which will be constantly updated to prevent the phone from entering Doze Mode. This is necessary for the app to respond quickly to threats, as USB event broadcasts may arrive slowly in Doze Mode. Furthermore, when the main service restarts, not just via screen-off receiver the app will check if the screen is off or locked and the profile is not encrypted. in simple words, it will check if the app has missed the screen turning off moment which occurred when the receiver was inactive and app didn't have trigger to evict keys, for example because it was killed at that moment and only then restarted. in this case, it will evict them using the same mechanism DevicePolicyManager.lockNow(FLAG_EVICT_CREDENTIAL_ENCRYPTION_KEY).\n\n"+
                "This mode is suitable if you have a low-spec phone or a phone with aggressive battery optimization, if you’re running a resource-intensive game, or if you simply want the app to respond more quickly to USB events, even in Doze Mode.\n\n"+                        
                "The high-efficiency mode state saves in the app’s memory, but changes to this state only take effect after the app’s process has been restarted. To restart the app’s process, it is recommended to just restart the phone.\n\n"+                
                "If you see a snowflake ❄ after text in the app notification title, it is normal mode. If you see a flame 🔥, it is high-efficiency mode.\n\n"+             
                "High-efficiency mode may consume more battery power.");

        if (HighEfficiencyModeActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).getBoolean("isHighEfficiencyModeEnabled", true)) {
            toggle.setChecked(true);
        } else {
            toggle.setChecked(false);
        }

        toggle.setOnClickListener(v -> {
            boolean currentlyChecked = toggle.isChecked();
             
            if (!currentlyChecked) {
                toggle.setChecked(true);

                AlertDialog dialog = new AlertDialog.Builder(HighEfficiencyModeActivity.this)
                        .setTitle("Are You Sure?")
                        .setMessage("Want to disable High-Efficiency Mode? Changes will be applied only after reboot.")
                        .setPositiveButton("Yes, disable", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface d, int which) {
                                HighEfficiencyModeActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("isHighEfficiencyModeEnabled", false).apply();
                                toggle.setChecked(false);
                            }
                        })
                        .setNegativeButton("Cancel", null)
                        .create();

                dialog.show();
                
                WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
                lp.copyFrom(dialog.getWindow().getAttributes());
                lp.width = (int) (screenWidth * 0.9);
                lp.gravity = Gravity.CENTER;
                lp.x = 0;
                lp.y = 0; 
                dialog.getWindow().setAttributes(lp);

            } else {
                HighEfficiencyModeActivity.this.createDeviceProtectedStorageContext().getSharedPreferences("prefs", Context.MODE_PRIVATE).edit().putBoolean("isHighEfficiencyModeEnabled", true).apply();
                background.work.around.Start.RunService(HighEfficiencyModeActivity.this);
                try {
                Intent intentRider = new Intent(HighEfficiencyModeActivity.this, background.work.around.RiderService.class);
                startForegroundService(intentRider);
                } catch (Throwable t) {}
            }
        });

        layout.addView(toggle);
        scrollView.addView(layout);
        setContentView(scrollView);
    }
}
