package com.pyler.betterbatterysaver.hooks;

import com.pyler.betterbatterysaver.util.Constants;
import com.pyler.betterbatterysaver.util.Logger;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class SelfHook {
    public static final String TAG = "SelfHook";

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (Constants.PACKAGE_NAME.equals(lpparam.packageName)) {
            try {
                XposedHelpers.findAndHookMethod("com.pyler.betterbatterysaver.activities.PreferencesActivity", lpparam.classLoader, "isXposedModuleEnabled", new Object[]{XC_MethodReplacement.returnConstant(Boolean.valueOf(true))});
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
