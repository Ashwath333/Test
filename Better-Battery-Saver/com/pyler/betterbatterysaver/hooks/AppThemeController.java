package com.pyler.betterbatterysaver.hooks;

import android.app.Activity;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppThemeController {
    public static final String KEY = "set_dark_app_theme";
    public static final String TAG = "AppThemeController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.AppThemeController.1 */
    static class C00211 extends XC_MethodHook {
        C00211() {
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.thisObject.setTheme(16974120);
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                XposedBridge.hookAllMethods(Activity.class, "onCreate", new C00211());
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
