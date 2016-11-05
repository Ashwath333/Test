package com.pyler.betterbatterysaver.services;

import android.app.Service;
import android.content.BroadcastReceiver;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.IBinder;
import com.pyler.betterbatterysaver.receivers.BetterBatterySaverReceiver;
import com.pyler.betterbatterysaver.util.Constants;

public class BatteryMonitorService extends Service {
    private BroadcastReceiver receiver;

    public IBinder onBind(Intent intent) {
        return null;
    }

    public int onStartCommand(Intent intent, int flags, int startId) {
        return 1;
    }

    public void onCreate() {
        super.onCreate();
        IntentFilter filter = new IntentFilter();
        filter.addAction("android.intent.action.BATTERY_CHANGED");
        filter.addAction("android.os.action.POWER_SAVE_MODE_CHANGED");
        filter.addAction("android.os.action.DEVICE_IDLE_MODE_CHANGED");
        filter.addAction(Constants.INTENT_BETTER_BATTERY_SAVER_START);
        filter.addAction(Constants.INTENT_BETTER_BATTERY_SAVER_STOP);
        this.receiver = new BetterBatterySaverReceiver();
        registerReceiver(this.receiver, filter);
    }

    public void onDestroy() {
        super.onDestroy();
        unregisterReceiver(this.receiver);
    }
}
