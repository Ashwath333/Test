package com.pyler.betterbatterysaver.util;

import android.app.AndroidAppHelper;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.preference.Preference;
import android.provider.Settings.System;
import de.robv.android.xposed.XSharedPreferences;
import de.robv.android.xposed.callbacks.XC_LoadPackage.LoadPackageParam;
import eu.chainfire.libsuperuser.Shell.SU;
import java.io.File;
import java.util.HashSet;
import java.util.Set;

public class Utils {
    private Context mContext;
    private SharedPreferences mPrefs;

    public Utils(Context context) {
        SharedPreferences sharedPreferences;
        this.mContext = context;
        if (context != null) {
            sharedPreferences = context.getSharedPreferences(Constants.PREFS_NAME, VERSION.SDK_INT < 24 ? 1 : 0);
        } else {
            sharedPreferences = null;
        }
        this.mPrefs = sharedPreferences;
    }

    public void setPrefsFileWorldReadable() {
        if (this.mContext != null) {
            File prefsFile = new File(new File(this.mContext.getApplicationInfo().dataDir, "shared_prefs"), "com.pyler.betterbatterysaver_preferences.xml");
            if (prefsFile.exists()) {
                prefsFile.setReadable(true, false);
            }
        }
    }

    public boolean hasRoot() {
        return SU.available();
    }

    public boolean getBatterBatterySaver() {
        if (this.mPrefs == null) {
            return false;
        }
        return this.mPrefs.getBoolean("better_battery_saver", false);
    }

    public void setBetterBatterySaver(boolean mode) {
        if (this.mPrefs != null) {
            this.mPrefs.edit().putBoolean("better_battery_saver", mode).apply();
        }
    }

    public void setChargingMode(boolean mode) {
        if (this.mPrefs != null) {
            this.mPrefs.edit().putBoolean("charging", mode).apply();
        }
    }

    public boolean isCharging() {
        if (this.mPrefs == null) {
            return false;
        }
        return this.mPrefs.getBoolean("charging", false);
    }

    public void setBatteryLevel(int level) {
        if (this.mPrefs != null) {
            this.mPrefs.edit().putInt("battery_level", level).apply();
        }
    }

    public int getBatteryLevel() {
        if (this.mPrefs == null) {
            return -1;
        }
        return this.mPrefs.getInt("battery_level", -1);
    }

    public int getBatteryLevelThreshold() {
        if (this.mPrefs == null) {
            return -1;
        }
        return this.mPrefs.getInt("battery_level_threshold", 15);
    }

    public int getBatteryLevelThreshold(String packageName) {
        if (this.mPrefs == null) {
            return -1;
        }
        return this.mPrefs.getInt(getKeyForPackage(packageName, "battery_level_threshold"), 15);
    }

    public Set<String> getStartMode() {
        if (this.mPrefs == null) {
            HashSet hashSet = new HashSet();
        }
        return this.mPrefs.getStringSet("start_mode", new HashSet());
    }

    public Set<String> getExitMode() {
        if (this.mPrefs == null) {
            HashSet hashSet = new HashSet();
        }
        return this.mPrefs.getStringSet("exit_mode", new HashSet());
    }

    public boolean areHeadsUpNotificationsEnabled() {
        if (this.mPrefs == null) {
            return false;
        }
        return this.mPrefs.getBoolean("headsup_notifications", false);
    }

    public boolean areInfoNotificationsEnabled() {
        if (this.mPrefs == null) {
            return false;
        }
        return this.mPrefs.getBoolean("info_notifications", false);
    }

    public boolean getBooleanPreference(String key) {
        if (this.mPrefs == null) {
            return false;
        }
        return this.mPrefs.getBoolean(key, false);
    }

    public int getIntPreference(String key) {
        if (this.mPrefs == null) {
            return -1;
        }
        return this.mPrefs.getInt(key, -1);
    }

    public boolean canWriteSystemSettings() {
        if (VERSION.SDK_INT >= 23) {
            return System.canWrite(this.mContext);
        }
        return true;
    }

    public String getKeyForPackage(String packageName, String key) {
        return packageName + "_" + key;
    }

    public String getCurrentPackageName() {
        String packageName = AndroidAppHelper.currentPackageName();
        if (packageName == null || packageName.isEmpty()) {
            return "android";
        }
        return packageName;
    }

    public boolean shouldHook(XSharedPreferences prefs, String packageName, String key) {
        if (prefs == null) {
            return false;
        }
        prefs.reload();
        String packageKey = getKeyForPackage(packageName, key);
        int currentBatteryLevel = prefs.getInt("battery_level", -1);
        boolean isCharging = prefs.getBoolean("charging", false);
        int batteryLevelThreshold = prefs.getInt(getKeyForPackage(packageName, "battery_level_threshold"), 15);
        if (currentBatteryLevel == -1 || isCharging || currentBatteryLevel > batteryLevelThreshold) {
            return false;
        }
        return prefs.getBoolean(packageKey, false);
    }

    public boolean shouldHook(XSharedPreferences prefs, LoadPackageParam lpparam, String key) {
        return shouldHook(prefs, lpparam.packageName, key);
    }

    public void setValueSeekBarPreference(Preference preference, int value) {
        try {
            Class.forName("android.preference.SeekBarPreference").getMethod("setProgress", new Class[]{Integer.TYPE}).invoke(preference, new Object[]{Integer.valueOf(value)});
        } catch (Exception e) {
        }
    }
}
