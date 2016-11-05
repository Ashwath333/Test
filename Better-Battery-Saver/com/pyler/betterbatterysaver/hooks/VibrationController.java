package com.pyler.betterbatterysaver.hooks;

import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class VibrationController {
    public static final String KEY = "disable_vibrations";
    public static final String SERVICE = "com.android.server.VibratorService";
    public static final String TAG = "VibrationController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.VibrationController.1 */
    static class C00241 extends XC_MethodHook {
        final /* synthetic */ XSharedPreferences val$prefs;

        C00241(XSharedPreferences xSharedPreferences) {
            this.val$prefs = xSharedPreferences;
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Utils utils = new Utils();
            if (utils.shouldHook(this.val$prefs, utils.getCurrentPackageName(), VibrationController.KEY)) {
                param.setResult(null);
            }
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            try {
                XposedBridge.hookAllMethods(XposedHelpers.findClass(SERVICE, lpparam.classLoader), "vibrate", new C00241(prefs));
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
