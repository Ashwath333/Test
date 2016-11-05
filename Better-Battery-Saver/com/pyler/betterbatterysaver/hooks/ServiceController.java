package com.pyler.betterbatterysaver.hooks;

import android.os.Build.VERSION;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ServiceController {
    public static final String KEY = "disable_services";
    public static final String SERVICE = "com.android.server.am.ActiveServices";
    public static final String SERVICE_OLD = "com.android.server.am.ActivityManagerService";
    public static final String TAG = "ServiceController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.ServiceController.1 */
    static class C00231 extends XC_MethodHook {
        final /* synthetic */ XSharedPreferences val$prefs;

        C00231(XSharedPreferences xSharedPreferences) {
            this.val$prefs = xSharedPreferences;
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            Utils utils = new Utils();
            if (utils.shouldHook(this.val$prefs, utils.getCurrentPackageName(), ServiceController.KEY)) {
                param.setResult(null);
            }
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if ("android".equals(lpparam.packageName)) {
            XC_MethodHook hook = new C00231(prefs);
            try {
                if (VERSION.SDK_INT >= 18) {
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(SERVICE, lpparam.classLoader), "startServiceLocked", hook);
                } else {
                    XposedBridge.hookAllMethods(XposedHelpers.findClass(SERVICE_OLD, lpparam.classLoader), "startServiceLocked", hook);
                }
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
