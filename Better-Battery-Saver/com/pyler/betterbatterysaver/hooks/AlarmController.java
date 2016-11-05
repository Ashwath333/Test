package com.pyler.betterbatterysaver.hooks;

import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class AlarmController {
    public static final String KEY = "disable_alarms";
    public static final String SERVICE = "com.android.server.AlarmManagerService";
    public static final String TAG = "AlarmController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.AlarmController.1 */
    static class C00181 extends XC_MethodHook {
        final /* synthetic */ XSharedPreferences val$prefs;

        C00181(XSharedPreferences xSharedPreferences) {
            this.val$prefs = xSharedPreferences;
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Utils utils = new Utils();
            if (utils.shouldHook(this.val$prefs, utils.getCurrentPackageName(), AlarmController.KEY)) {
                param.setResult(null);
            }
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            try {
                XposedBridge.hookAllMethods(XposedHelpers.findClass(SERVICE, lpparam.classLoader), "triggerAlarmsLocked", new C00181(prefs));
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
