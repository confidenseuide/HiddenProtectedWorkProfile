package android.app.admin;

import android.content.ComponentName;
import android.content.Intent;
import java.util.List;

public class DevicePolicyManager {
    // Тот самый скрытый метод
    public void setExemptFromBackgroundRestrictedOperations(ComponentName admin, boolean exempt) {}

    // Методы для работы с профилем и ограничениями
    public boolean isProfileOwnerApp(String packageName) { return false; }
    public void wipeData(int flags) {}
    public void setScreenCaptureDisabled(ComponentName admin, boolean disabled) {}
    public void clearUserRestriction(ComponentName admin, String key) {}
    public boolean setApplicationHidden(ComponentName admin, String packageName, boolean hidden) { return false; }
    public String[] setPackagesSuspended(ComponentName admin, String[] packageNames, boolean suspended) { return new String[0]; }
    public boolean setPermittedInputMethods(ComponentName admin, List<String> packageNames) { return false; }

    // Константы
    public static final String ACTION_PROVISION_MANAGED_PROFILE = "android.app.action.PROVISION_MANAGED_PROFILE";
    public static final String EXTRA_PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME = "android.app.extra.PROVISIONING_DEVICE_ADMIN_COMPONENT_NAME";
    public static final String EXTRA_PROVISIONING_DISCLAIMER_CONTENT = "android.app.extra.PROVISIONING_DISCLAIMER_CONTENT";
}
