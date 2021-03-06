package com.pyler.betterbatterysaver.hooks;

import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class ReceiverController {
    public static final String CLASS_CONTEXT_IMPL = "android.app.ContextImpl";
    public static final String KEY = "disable_receivers";
    public static final String TAG = "ReceiverController";

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                XposedBridge.hookAllMethods(XposedHelpers.findClass(CLASS_CONTEXT_IMPL, lpparam.classLoader), "registerReceiverInternal", XC_MethodReplacement.returnConstant(null));
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
