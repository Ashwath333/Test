package com.pyler.betterbatterysaver.receivers;

import android.app.Notification.Action;
import android.app.Notification.Builder;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.drawable.Icon;
import android.os.Build.VERSION;
import com.pyler.betterbatterysaver.C0000R;
import com.pyler.betterbatterysaver.activities.PreferencesActivity;
import com.pyler.betterbatterysaver.services.BatteryMonitorService;
import com.pyler.betterbatterysaver.util.Constants;
import com.pyler.betterbatterysaver.util.DeviceController;
import com.pyler.betterbatterysaver.util.Logger;
import com.pyler.betterbatterysaver.util.Utils;
import java.util.Set;

public class BetterBatterySaverReceiver extends BroadcastReceiver {
    private static final int CONFIRM_NOTIFICATION_ID = 0;
    private static final int INFO_CONFIRMATION_ID = 1;
    private static final String TAG = "BetterBatterySaverReceiver";
    private Context mContext;
    private Utils mUtils;

    public void onReceive(Context context, Intent intent) {
        this.mContext = context;
        this.mUtils = new Utils(context);
        if ("android.intent.action.BOOT_COMPLETED".equals(intent.getAction())) {
            context.startService(new Intent(context, BatteryMonitorService.class));
            Logger.m0i(TAG, "BatteryMonitorService has started");
        } else if ("android.os.action.POWER_SAVE_MODE_CHANGED".equals(intent.getAction())) {
            if (new DeviceController(context).isPowerSaveMode()) {
                if (this.mUtils.getStartMode().contains("android_saver")) {
                    setBetterBatterySaver(true);
                    Logger.m0i(TAG, "Bettery Battery Saver started since Android Battery Saver was turned on");
                }
            } else if (this.mUtils.getExitMode().contains("android_saver")) {
                setBetterBatterySaver(false);
            }
        } else if ("android.os.action.DEVICE_IDLE_MODE_CHANGED".equals(intent.getAction())) {
            if (new DeviceController(context).isDozeMode()) {
                Set<String> startMode = this.mUtils.getStartMode();
                if (startMode.contains("auto") && startMode.contains("doze_mode")) {
                    manageBetterBatterySaver(true);
                    Logger.m0i(TAG, "Bettery Battery Saver started since Doze mode was turned on");
                    return;
                }
                return;
            }
            Set<String> exitMode = this.mUtils.getExitMode();
            if (exitMode.contains("auto") && exitMode.contains("doze_mode")) {
                manageBetterBatterySaver(false);
            }
        } else if (Constants.INTENT_BETTER_BATTERY_SAVER_START.equals(intent.getAction())) {
            cancelConfirmStartNotification();
            manageBetterBatterySaver(true);
            Logger.m0i(TAG, "INTENT_BETTER_BATTERY_SAVE_START: Turning on");
        } else if (Constants.INTENT_BETTER_BATTERY_SAVER_STOP.equals(intent.getAction())) {
            cancelConfirmStartNotification();
            manageBetterBatterySaver(false);
            Logger.m0i(TAG, "INTENT_BETTER_BATTERY_SAVE_START: Turning off");
        } else if (!"android.intent.action.BATTERY_CHANGED".equals(intent.getAction())) {
        } else {
            if (intent.getExtras() == null) {
                Logger.m0i(TAG, "ACTION_BATTERY_CHANGED intent has no extras");
                return;
            }
            boolean wasCharging = this.mUtils.isCharging();
            int previousBatteryLevel = this.mUtils.getBatteryLevel();
            int plugged = intent.getIntExtra("plugged", -1);
            boolean isCharging = isCharging(plugged);
            this.mUtils.setChargingMode(isCharging);
            int batteryLevel = intent.getIntExtra("level", -1);
            this.mUtils.setBatteryLevel(batteryLevel);
            Logger.m0i(TAG, "Battery level " + batteryLevel + ", charging " + isCharging);
            if (batteryLevel != previousBatteryLevel || isCharging != wasCharging) {
                int batteryLevelThreshold = this.mUtils.getBatteryLevelThreshold();
                if (batteryLevel == batteryLevelThreshold) {
                    setBetterBatterySaver(!isCharging(plugged));
                } else if (batteryLevel > batteryLevelThreshold) {
                    if (this.mUtils.getBatterBatterySaver()) {
                        setBetterBatterySaver(false);
                    }
                } else if (batteryLevel < batteryLevelThreshold && !this.mUtils.getBatterBatterySaver()) {
                    setBetterBatterySaver(true);
                }
            }
        }
    }

    private boolean isCharging(int plugged) {
        return plugged == INFO_CONFIRMATION_ID || plugged == 2;
    }

    public void setBetterBatterySaver(boolean mode) {
        Set<String> runMode;
        if (mode) {
            runMode = this.mUtils.getStartMode();
        } else {
            runMode = this.mUtils.getExitMode();
        }
        if (runMode.contains("auto")) {
            manageBetterBatterySaver(mode);
        } else if (runMode.contains("notification")) {
            showConfirmStartNotification(mode);
        }
    }

    private void manageBetterBatterySaver(boolean mode) {
        this.mUtils.setBetterBatterySaver(mode);
        DeviceController device = new DeviceController(this.mContext);
        if (mode) {
            if (this.mUtils.getBooleanPreference("turn_wifi_off")) {
                device.setWiFiMode(false);
                Logger.m0i(TAG, "Wi-Fi turned off");
            }
            if (this.mUtils.getBooleanPreference("turn_bluetooth_off")) {
                device.setBluetoothMode(false);
                Logger.m0i(TAG, "Bluetooth turned off");
            }
            if (this.mUtils.getBooleanPreference("turn_auto_sync_off")) {
                device.setAutoSyncMode(false);
                Logger.m0i(TAG, "Auto Sync turned off");
            }
            if (this.mUtils.getBooleanPreference("set_screen_timeout_off")) {
                device.setScreenTimeout(false);
                Logger.m0i(TAG, "Screen timeout turned off");
            }
            if (this.mUtils.getBooleanPreference("turn_brightness_off")) {
                device.setBrightnessMode(false);
                Logger.m0i(TAG, "Brightness turned off");
            }
            if (this.mUtils.getBooleanPreference("turn_wifi-ap_off")) {
                device.setWiFiApMode(false);
                Logger.m0i(TAG, "Wi-Fi AP turned off");
            }
            if (this.mUtils.hasRoot()) {
                if (this.mUtils.getBooleanPreference("turn_gps_off")) {
                    device.setGPSMode(false);
                    Logger.m0i(TAG, "GPS turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_nfc_off")) {
                    device.setNFCMode(false);
                    Logger.m0i(TAG, "NFC turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_device_off")) {
                    device.turnDeviceOff();
                    Logger.m0i(TAG, "Device turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_screen_off")) {
                    device.setScreenMode(false);
                    Logger.m0i(TAG, "Screen turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_mobile_data_off")) {
                    device.setMobileDataMode(false);
                    Logger.m0i(TAG, "Mobile data turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_android_saver_on")) {
                    device.setPowerSaveMode(true);
                    Logger.m0i(TAG, "Android Battery Saver turned on");
                }
                if (this.mUtils.getBooleanPreference("turn_airplane_mode_on")) {
                    device.setAirplaneMode(true);
                    Logger.m0i(TAG, "Airplane mode turned on");
                }
                if (this.mUtils.getBooleanPreference("turn_doze_on")) {
                    device.setDozeMode(true);
                    Logger.m0i(TAG, "Doze turned on");
                }
            }
        } else {
            if (this.mUtils.getBooleanPreference("turn_wifi_on")) {
                device.setWiFiMode(true);
                Logger.m0i(TAG, "Wi-Fi turned on");
            }
            if (this.mUtils.getBooleanPreference("turn_bluetooth_on")) {
                device.setBluetoothMode(true);
                Logger.m0i(TAG, "Bluetooth turned on");
            }
            if (this.mUtils.getBooleanPreference("turn_auto_sync_on")) {
                device.setAutoSyncMode(true);
                Logger.m0i(TAG, "Auto Sync turned on");
            }
            if (this.mUtils.getBooleanPreference("set_screen_timeout_on")) {
                device.setScreenTimeout(true);
                Logger.m0i(TAG, "Screen timeout turned on");
            }
            if (this.mUtils.getBooleanPreference("turn_brightness_on")) {
                device.setBrightnessMode(true);
                Logger.m0i(TAG, "Brightness turned on");
            }
            if (this.mUtils.getBooleanPreference("turn_wifi_ap_on")) {
                device.setWiFiApMode(true);
                Logger.m0i(TAG, "Wi-Fi AP turned on");
            }
            if (this.mUtils.hasRoot()) {
                if (this.mUtils.getBooleanPreference("turn_gps_on")) {
                    device.setGPSMode(true);
                    Logger.m0i(TAG, "GPS turned on");
                }
                if (this.mUtils.getBooleanPreference("turn_nfc_on")) {
                    device.setNFCMode(true);
                    Logger.m0i(TAG, "NFC turned on");
                }
                if (this.mUtils.getBooleanPreference("turn_screen_on")) {
                    device.setScreenMode(true);
                    Logger.m0i(TAG, "Screen turned on");
                }
                if (this.mUtils.getBooleanPreference("turn_mobile_data_on")) {
                    device.setMobileDataMode(true);
                    Logger.m0i(TAG, "Mobile data turned on");
                }
                if (this.mUtils.getBooleanPreference("turn_android_saver_off")) {
                    device.setPowerSaveMode(false);
                    Logger.m0i(TAG, "Android Battery Saver turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_airplane_mode_off")) {
                    device.setAirplaneMode(false);
                    Logger.m0i(TAG, "Airplane mode turned off");
                }
                if (this.mUtils.getBooleanPreference("turn_doze_off")) {
                    device.setDozeMode(false);
                    Logger.m0i(TAG, "Doze turned off");
                }
            }
        }
        showInfoNotification(mode);
    }

    private void showConfirmStartNotification(boolean mode) {
        String title;
        String action;
        PendingIntent run;
        Intent start = new Intent(Constants.INTENT_BETTER_BATTERY_SAVER_START);
        Intent stop = new Intent(Constants.INTENT_BETTER_BATTERY_SAVER_STOP);
        Intent app = new Intent(this.mContext, PreferencesActivity.class);
        app.addFlags(268435456);
        PendingIntent openApp = PendingIntent.getActivity(this.mContext, (int) System.currentTimeMillis(), app, CONFIRM_NOTIFICATION_ID);
        PendingIntent turnOn = PendingIntent.getBroadcast(this.mContext, (int) System.currentTimeMillis(), start, CONFIRM_NOTIFICATION_ID);
        PendingIntent turnOff = PendingIntent.getBroadcast(this.mContext, (int) System.currentTimeMillis(), stop, CONFIRM_NOTIFICATION_ID);
        if (mode) {
            title = this.mContext.getString(C0000R.string.turn_on_message);
            action = this.mContext.getString(C0000R.string.turn_on);
            run = turnOn;
        } else {
            title = this.mContext.getString(C0000R.string.turn_off_message);
            action = this.mContext.getString(C0000R.string.turn_off);
            run = turnOff;
        }
        Builder builder = new Builder(this.mContext).setContentText(title).setSmallIcon(C0000R.drawable.ic_notification).setContentIntent(openApp).setAutoCancel(true);
        if (VERSION.SDK_INT >= 23) {
            builder.addAction(new Action.Builder(Icon.createWithResource(this.mContext, C0000R.drawable.ic_notification_turn_on), action, run).build());
        } else {
            builder.addAction(C0000R.drawable.ic_notification_turn_on, action, run);
        }
        if (VERSION.SDK_INT <= 23) {
            builder.setContentTitle(this.mContext.getString(C0000R.string.app_name));
        }
        if (this.mUtils.areHeadsUpNotificationsEnabled()) {
            builder.setPriority(INFO_CONFIRMATION_ID).setVibrate(new long[CONFIRM_NOTIFICATION_ID]);
        }
        ((NotificationManager) this.mContext.getSystemService("notification")).notify(CONFIRM_NOTIFICATION_ID, builder.build());
    }

    private void showInfoNotification(boolean mode) {
        if (this.mUtils.areInfoNotificationsEnabled()) {
            String title;
            if (mode) {
                title = this.mContext.getString(C0000R.string.battery_saver_enabled);
            } else {
                title = this.mContext.getString(C0000R.string.battery_saver_disabled);
            }
            Builder builder = new Builder(this.mContext).setContentText(title).setSmallIcon(C0000R.drawable.ic_notification).setAutoCancel(true);
            if (this.mUtils.areHeadsUpNotificationsEnabled()) {
                builder.setPriority(INFO_CONFIRMATION_ID).setVibrate(new long[CONFIRM_NOTIFICATION_ID]);
            }
            if (VERSION.SDK_INT <= 23) {
                builder.setContentTitle(this.mContext.getString(C0000R.string.app_name));
            }
            ((NotificationManager) this.mContext.getSystemService("notification")).notify(INFO_CONFIRMATION_ID, builder.build());
        }
    }

    public void cancelConfirmStartNotification() {
        ((NotificationManager) this.mContext.getSystemService("notification")).cancel(CONFIRM_NOTIFICATION_ID);
    }
}
