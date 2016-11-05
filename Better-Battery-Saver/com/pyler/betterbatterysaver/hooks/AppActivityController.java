package com.pyler.betterbatterysaver.hooks;

import android.app.Activity;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppActivityController {
    public static final String KEY = "disable_app_launch";
    public static final String TAG = "AppActivityController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.AppActivityController.1 */
    static class C00191 extends XC_MethodHook {
        C00191() {
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            param.thisObject.finish();
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                XposedBridge.hookAllMethods(Activity.class, "onStart", new C00191());
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
