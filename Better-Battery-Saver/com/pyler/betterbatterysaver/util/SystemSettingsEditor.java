package com.pyler.betterbatterysaver.util;

import android.os.Build.VERSION;
import eu.chainfire.libsuperuser.BuildConfig;
import eu.chainfire.libsuperuser.Shell.SU;
import java.util.List;

public class SystemSettingsEditor {
    public static final String AIRPLANE_MODE = "airplane_mode_on";
    public static final String GLOBAL = "global";
    public static final String LOW_POWER = "low_power";
    public static final String MOBILE_DATA = "mobile_data";
    public static final String SECURE = "secure";
    private static final String TAG = "SystemSettingsEditor";

    public static void setBoolean(String namespace, String key, boolean state) {
        if (VERSION.SDK_INT >= 17) {
            if (SU.available()) {
                int value;
                if (state) {
                    value = 1;
                } else {
                    value = 0;
                }
                SU.run(String.format("settings put %s %s %d", new Object[]{namespace, key, Integer.valueOf(value)}));
                return;
            }
            Logger.m0i(TAG, "Cant set system settings, no root");
        }
    }

    public static void setString(String namespace, String key, String value) {
        if (VERSION.SDK_INT >= 17) {
            if (SU.available()) {
                SU.run(String.format("settings put %s %s %s", new Object[]{namespace, key, value}));
                return;
            }
            Logger.m0i(TAG, "Cant set system settings, no root");
        }
    }

    public static String get(String namespace, String key) {
        if (VERSION.SDK_INT < 17) {
            return null;
        }
        if (SU.available()) {
            List<String> out = SU.run(String.format("settings get %s %s", new Object[]{namespace, key}));
            if (out != null) {
                return out.toString();
            }
            return null;
        }
        Logger.m0i(TAG, "Cant get system settings, no root");
        return null;
    }

    public static void clear(String namespace, String key) {
        if (VERSION.SDK_INT >= 17) {
            if (SU.available()) {
                String command;
                if (VERSION.SDK_INT >= 21) {
                    command = String.format("settings delete %s %s", new Object[]{namespace, key});
                } else {
                    command = String.format("settings put %s %s %s", new Object[]{namespace, key, BuildConfig.FLAVOR});
                }
                SU.run(command);
                return;
            }
            Logger.m0i(TAG, "Cant clear system settings, no root");
        }
    }
}
