package com.pyler.betterbatterysaver.hooks;

import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WakelockController {
    public static final String KEY = "disable_wakelocks";
    public static final String SERVICE = "com.android.server.power.PowerManagerService";
    public static final String TAG = "WakelockController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.WakelockController.1 */
    static class C00251 extends XC_MethodHook {
        final /* synthetic */ XSharedPreferences val$prefs;

        C00251(XSharedPreferences xSharedPreferences) {
            this.val$prefs = xSharedPreferences;
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Utils utils = new Utils();
            if (utils.shouldHook(this.val$prefs, utils.getCurrentPackageName(), WakelockController.KEY)) {
                param.setResult(null);
            }
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            XC_MethodHook hook = new C00251(prefs);
            try {
                XposedBridge.hookAllMethods(XposedHelpers.findClass(SERVICE, lpparam.classLoader), "acquireWakeLockInternal", hook);
                XposedBridge.hookAllMethods(XposedHelpers.findClass(SERVICE, lpparam.classLoader), "releaseWakeLockInternal", hook);
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
