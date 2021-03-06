package com.pyler.betterbatterysaver.hooks;

import android.net.wifi.WifiManager;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class WifiApController {
    public static final String KEY = "disable_wifi_ap_control";
    public static final String TAG = "WifiApController";

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                XposedBridge.hookAllMethods(WifiManager.class, "setWifiApEnabled", XC_MethodReplacement.returnConstant(Boolean.valueOf(false)));
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
