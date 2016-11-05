package com.pyler.betterbatterysaver.hooks;

import android.hardware.Camera;
import android.hardware.camera2.CameraAccessException;
import android.hardware.camera2.CameraManager;
import android.os.Build.VERSION;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodHook.MethodHookParam;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.XposedBridge;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;

public class CameraController {
    public static final String KEY = "disable_camera_control";
    public static final String TAG = "CameraController";

    /* renamed from: com.pyler.betterbatterysaver.hooks.CameraController.1 */
    static class C00221 extends XC_MethodHook {
        C00221() {
        }

        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (VERSION.SDK_INT >= 21) {
                param.setThrowable(new CameraAccessException(1));
            }
        }
    }

    public static void init(XSharedPreferences prefs, LoadPackageParam lpparam) {
        if (new Utils().shouldHook(prefs, lpparam, KEY)) {
            try {
                if (VERSION.SDK_INT >= 21) {
                    XposedBridge.hookAllMethods(CameraManager.class, "openCamera", new C00221());
                }
                XposedBridge.hookAllMethods(Camera.class, "open", XC_MethodReplacement.returnConstant(null));
            } catch (Throwable t) {
                Logger.m0i(TAG, t.getMessage());
            }
        }
    }
}
