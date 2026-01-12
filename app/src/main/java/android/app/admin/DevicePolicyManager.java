package android.app.admin;

import android.content.ComponentName;
import android.content.Intent;
import java.util.List;

public class DevicePolicyManager {
    // Скрытый метод (ради которого всё затевалось)
    public void setExemptFromBackgroundRestrictedOperations(ComponentName admin, boolean exempt) {}

    // ТОТ САМЫЙ ПОСЛЕДНИЙ МЕТОД:
    public boolean setPermissionGrantState(ComponentName admin, String packageName, String permission, int grantState) { return false; }

    // Остальные методы
    public void setProfileEnabled(ComponentName admin) {}
    public void setProfileName(ComponentName admin, String name) {}
    public void enableSystemApp(ComponentName admin, String packageName) {}
    public void setUserControlDisabledPackages(ComponentName admin, List<String> packages) {}
    public boolean isProfileOwnerApp(String packageName) { return false; }
    public void wipeData(int flags) {}
    public void setScreenCaptureDisabled(ComponentName admin, boolean disabled) {}
    public void clearUserRestriction(ComponentName admin, String key) {}
    public boolean setApplicationHidden(ComponentName admin, String packageName, boolean hidden) { return false; }
    public String[] setPackagesSuspended(ComponentName admin, String[] packageNames, boolean suspended) { return new String[0]; }
    public boolean setPermittedInputMethods(ComponentName admin, List<String> packageNames) { return false; }

    // Константы
    public static final String ACTION_SET_NEW_PASSWORD = "android.app.action.SET_NEW_PASSWORD";
    public static final int PERMISSION_GRANT_STATE_GRANTED = 1;
    public static final String ACTION_PROVISION_MANAGED_PROFILE = "android.app.action.PROVISION_MANAGED_PROFILE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_CONTENT = "android.app.extra.PROVISIONING_DISCLAIMER_CONTENT";
}
