package com.pyler.betterbatterysaver.hooks;

import android.app.Activity;
import android.view.WindowManager.LayoutParams;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AppBrightnessController {
    public static final String KEY = "set_lowest_app_brightness";
    public static final String TAG = "AppBrightnessController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.AppBrightnessController.1 */
    static class C00201 extends XC_MethodHook {
        C00201() {
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Activity activity = param.thisObject;
            LayoutParams lp = activity.getWindow().getAttributes();
            lp.screenBrightness = 0.0f;
            activity.getWindow().setAttributes(lp);
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                XposedBridge.hookAllMethods(Activity.class, "onResume", new C00201());
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
