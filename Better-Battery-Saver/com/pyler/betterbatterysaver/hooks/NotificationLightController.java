package com.pyler.betterbatterysaver.hooks;

import android.app.Notification.Builder;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class NotificationLightController {
    public static final String KEY = "disable_notification_lights";
    public static final String TAG = "NotificationLightController";

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                XposedBridge.hookAllMethods(Builder.class, "setLights", XC_MethodReplacement.returnConstant(null));
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
