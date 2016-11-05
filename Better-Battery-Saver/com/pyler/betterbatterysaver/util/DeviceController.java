package com.pyler.betterbatterysaver.util;

import android.bluetooth.BluetoothAdapter;
import android.content.ContentResolver;
import android.content.Context;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.Build.VERSION;
import android.os.PowerManager;
import android.provider.Settings.Secure;
import android.provider.Settings.System;
import eu.chainfire.libsuperuser.Shell.SU;

public class DeviceController {
    private Context mContext;
    private Utils mUtils;

    public DeviceController(Context context) {
        this.mContext = context;
        this.mUtils = new Utils(this.mContext);
    }

    public void setMobileDataMode(boolean mode) {
        SystemSettingsEditor.setBoolean(SystemSettingsEditor.GLOBAL, SystemSettingsEditor.MOBILE_DATA, mode);
    }

    public boolean isPowerSaveMode() {
        if (this.mContext == null) {
            return false;
        }
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (VERSION.SDK_INT >= 21) {
            return pm.isPowerSaveMode();
        }
        return false;
    }

    public boolean isDozeMode() {
        if (this.mContext == null) {
            return false;
        }
        PowerManager pm = (PowerManager) this.mContext.getSystemService("power");
        if (VERSION.SDK_INT >= 23) {
            return pm.isDeviceIdleMode();
        }
        return false;
    }

    public void setPowerSaveMode(boolean mode) {
        SystemSettingsEditor.setBoolean(SystemSettingsEditor.GLOBAL, SystemSettingsEditor.LOW_POWER, mode);
    }

    public void setWiFiMode(boolean mode) {
        if (this.mContext != null) {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            if (wifiManager != null) {
                wifiManager.setWifiEnabled(mode);
            }
        }
    }

    public void setGPSMode(boolean mode) {
        if (this.mContext != null) {
            String newSetting;
            String oldSetting = Secure.getString(this.mContext.getContentResolver(), "location_providers_allowed");
            if (oldSetting.isEmpty()) {
                newSetting = "gps";
            } else {
                newSetting = oldSetting + ",gps";
            }
            if (mode) {
                SystemSettingsEditor.setString(SystemSettingsEditor.SECURE, SystemSettingsEditor.MOBILE_DATA, newSetting);
            } else {
                SystemSettingsEditor.clear(SystemSettingsEditor.SECURE, SystemSettingsEditor.MOBILE_DATA);
            }
        }
    }

    public void setBluetoothMode(boolean mode) {
        BluetoothAdapter btAdapter = BluetoothAdapter.getDefaultAdapter();
        if (btAdapter != null) {
            if (mode) {
                btAdapter.enable();
            } else {
                btAdapter.disable();
            }
        }
    }

    public void setNFCMode(boolean mode) {
        if (!SU.available()) {
            return;
        }
        if (mode) {
            if (VERSION.SDK_INT >= 19) {
                SU.run("service call nfc 6");
            } else {
                SU.run("service call nfc 5");
            }
        } else if (VERSION.SDK_INT >= 19) {
            SU.run("service call nfc 5");
        } else {
            SU.run("service call nfc 4");
        }
    }

    public void setAutoSyncMode(boolean mode) {
        ContentResolver.setMasterSyncAutomatically(mode);
    }

    public void setBrightnessMode(boolean mode) {
        if (this.mContext == null || !this.mUtils.canWriteSystemSettings()) {
            return;
        }
        if (mode) {
            System.putInt(this.mContext.getContentResolver(), "screen_brightness_mode", 1);
            return;
        }
        System.putInt(this.mContext.getContentResolver(), "screen_brightness", 0);
        System.putInt(this.mContext.getContentResolver(), "screen_brightness", 10);
    }

    public void turnDeviceOff() {
        if (SU.available()) {
            SU.run("am start -a android.intent.action.ACTION_REQUEST_SHUTDOWN");
        }
    }

    public void setScreenMode(boolean mode) {
        if (this.mContext != null && SU.available()) {
            boolean isScreenOn = ((PowerManager) this.mContext.getSystemService("power")).isScreenOn();
            if (mode) {
                if (!isScreenOn) {
                    SU.run("input keyevent 26");
                }
            } else if (isScreenOn) {
                SU.run("input keyevent 26");
            }
        }
    }

    public void setScreenTimeout(boolean mode) {
        if (this.mContext == null || !this.mUtils.canWriteSystemSettings()) {
            return;
        }
        if (mode) {
            System.putInt(this.mContext.getContentResolver(), "screen_off_timeout", this.mUtils.getIntPreference("screen_timeout_on"));
            return;
        }
        System.putInt(this.mContext.getContentResolver(), "screen_off_timeout", this.mUtils.getIntPreference("screen_timeout_off"));
    }

    public void setAirplaneMode(boolean mode) {
        if (SU.available()) {
            SystemSettingsEditor.setBoolean(SystemSettingsEditor.GLOBAL, SystemSettingsEditor.AIRPLANE_MODE, mode);
            String enabled = mode ? "true" : "false";
            SU.run(String.format("am broadcast -a android.intent.action.AIRPLANE_MODE --ez state %s", new Object[]{enabled}));
        }
    }

    public void setServiceMode(String name, boolean mode) {
        if (!SU.available()) {
            return;
        }
        if (mode) {
            SU.run("pm enable " + name);
        } else {
            SU.run("pm disable " + name);
        }
    }

    public void setDozeMode(boolean mode) {
        if (VERSION.SDK_INT >= 23 && SU.available()) {
            boolean enabled = "1".equals(SU.run("dumpsys deviceidle enabled").toString());
            if (mode) {
                SU.run("dumpsys deviceidle enable");
                SU.run("dumpsys deviceidle force-idle");
            } else if (enabled) {
                SU.run("dumpsys deviceidle disable");
            }
        }
    }

    public void setWiFiApMode(boolean mode) {
        if (this.mContext != null && this.mUtils.canWriteSystemSettings()) {
            WifiManager wifiManager = (WifiManager) this.mContext.getSystemService("wifi");
            if (wifiManager != null) {
                try {
                    WifiManager.class.getMethod("setWifiApEnabled", new Class[]{WifiConfiguration.class, Boolean.TYPE}).invoke(wifiManager, new Object[]{null, Boolean.valueOf(mode)});
                } catch (Exception e) {
                }
            }
        }
    }
}
