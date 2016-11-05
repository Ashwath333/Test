package com.pyler.betterbatterysaver.activities;

import android.annotation.TargetApi;
import android.app.AlertDialog.Builder;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnClickListener;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.Preference.OnPreferenceClickListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.pyler.betterbatterysaver.BuildConfig;
import com.pyler.betterbatterysaver.C0000R;
import com.pyler.betterbatterysaver.util.Utils;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class PreferencesActivity extends PreferenceActivity {
    public static Context mContext;
    public static SharedPreferences mPrefs;
    public static Utils mUtils;

    public static class Settings extends PreferenceFragment {

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.11 */
        class AnonymousClass11 implements OnPreferenceChangeListener {
            final /* synthetic */ Preference val$screenTimeoutOff;

            AnonymousClass11(Preference preference) {
                this.val$screenTimeoutOff = preference;
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                this.val$screenTimeoutOff.setEnabled(((Boolean) newValue).booleanValue());
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.12 */
        class AnonymousClass12 implements OnPreferenceChangeListener {
            final /* synthetic */ Preference val$screenTimeoutOn;

            AnonymousClass12(Preference preference) {
                this.val$screenTimeoutOn = preference;
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                this.val$screenTimeoutOn.setEnabled(((Boolean) newValue).booleanValue());
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.1 */
        class C00051 implements OnPreferenceChangeListener {
            C00051() {
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                Settings.this.reloadAppsList();
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.2 */
        class C00062 implements OnPreferenceChangeListener {
            C00062() {
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                PreferencesActivity.mPrefs.edit().putBoolean("show_system_apps", ((Boolean) newValue).booleanValue()).apply();
                Settings.this.reloadAppsList();
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.3 */
        class C00073 implements OnPreferenceChangeListener {
            C00073() {
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int batteryLevel = ((Integer) newValue).intValue();
                preference.setTitle(Settings.this.getString(C0000R.string.battery_level_threshold, new Object[]{Integer.valueOf(batteryLevel)}));
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.4 */
        class C00084 implements OnPreferenceChangeListener {
            C00084() {
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int screenTimeout = ((Integer) newValue).intValue();
                preference.setTitle(Settings.this.getString(C0000R.string.screen_timeout, new Object[]{Integer.valueOf(screenTimeout)}));
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.5 */
        class C00095 implements OnPreferenceChangeListener {
            final /* synthetic */ PreferenceScreen val$appBatterySavingSettings;
            final /* synthetic */ PreferenceCategory val$appSettings;

            C00095(PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory) {
                this.val$appBatterySavingSettings = preferenceScreen;
                this.val$appSettings = preferenceCategory;
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                if (((Boolean) newValue).booleanValue()) {
                    this.val$appBatterySavingSettings.addPreference(this.val$appSettings);
                } else {
                    this.val$appBatterySavingSettings.removePreference(this.val$appSettings);
                }
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.6 */
        class C00106 implements OnPreferenceClickListener {
            C00106() {
            }

            public boolean onPreferenceClick(Preference preference) {
                PreferencesActivity.mPrefs.edit().clear().apply();
                Settings.this.getActivity().recreate();
                return false;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.7 */
        class C00117 implements OnPreferenceClickListener {
            C00117() {
            }

            public boolean onPreferenceClick(Preference preference) {
                Settings.this.openLink("http://forum.xda-developers.com/xposed/modules/mod-battery-saver-t3419064");
                return false;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.8 */
        class C00128 implements OnPreferenceClickListener {
            C00128() {
            }

            public boolean onPreferenceClick(Preference preference) {
                Settings.this.openLink("https://www.paypal.com/cgi-bin/webscr?cmd=_s-xclick&hosted_button_id=6NTYA2HMPQHVW");
                return false;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.9 */
        class C00139 implements OnClickListener {
            C00139() {
            }

            public void onClick(DialogInterface dialog, int id) {
                dialog.cancel();
                Intent writeSystemSettings = new Intent("android.settings.action.MANAGE_WRITE_SETTINGS");
                writeSystemSettings.setData(Uri.parse("package:com.pyler.betterbatterysaver"));
                writeSystemSettings.addFlags(268435456);
                try {
                    Settings.this.startActivity(writeSystemSettings);
                } catch (ActivityNotFoundException e) {
                }
            }
        }

        public class LoadApps extends AsyncTask<Void, Void, Void> {
            PreferenceCategory appReceiverSettings;
            PreferenceCategory appServiceSettings;
            PreferenceCategory appSettings;
            List<ApplicationInfo> packages;
            PackageManager pm;

            /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.LoadApps.1 */
            class C00141 implements Comparator<String[]> {
                C00141() {
                }

                public int compare(String[] entry1, String[] entry2) {
                    return entry1[1].compareToIgnoreCase(entry2[1]);
                }
            }

            /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.LoadApps.2 */
            class C00152 implements OnPreferenceClickListener {
                final /* synthetic */ String val$appName;
                final /* synthetic */ String val$packageName;

                C00152(String str, String str2) {
                    this.val$packageName = str;
                    this.val$appName = str2;
                }

                public boolean onPreferenceClick(Preference preference) {
                    Intent openAppSettings = new Intent(PreferencesActivity.mContext, AppSettingsActivity.class);
                    openAppSettings.putExtra("package", this.val$packageName);
                    openAppSettings.putExtra("app", this.val$appName);
                    Settings.this.startActivity(openAppSettings);
                    return false;
                }
            }

            /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.LoadApps.3 */
            class C00163 implements OnPreferenceClickListener {
                final /* synthetic */ String val$appName;
                final /* synthetic */ String val$packageName;

                C00163(String str, String str2) {
                    this.val$packageName = str;
                    this.val$appName = str2;
                }

                public boolean onPreferenceClick(Preference preference) {
                    Intent openAppSettings = new Intent(PreferencesActivity.mContext, AppServiceSettingsActivity.class);
                    openAppSettings.putExtra("package", this.val$packageName);
                    openAppSettings.putExtra("app", this.val$appName);
                    Settings.this.startActivity(openAppSettings);
                    return false;
                }
            }

            /* renamed from: com.pyler.betterbatterysaver.activities.PreferencesActivity.Settings.LoadApps.4 */
            class C00174 implements OnPreferenceClickListener {
                final /* synthetic */ String val$appName;
                final /* synthetic */ String val$packageName;

                C00174(String str, String str2) {
                    this.val$packageName = str;
                    this.val$appName = str2;
                }

                public boolean onPreferenceClick(Preference preference) {
                    Intent openAppSettings = new Intent(PreferencesActivity.mContext, AppReceiverSettingsActivity.class);
                    openAppSettings.putExtra("package", this.val$packageName);
                    openAppSettings.putExtra("app", this.val$appName);
                    Settings.this.startActivity(openAppSettings);
                    return false;
                }
            }

            public LoadApps() {
                this.appSettings = (PreferenceCategory) Settings.this.findPreference("app_settings");
                this.appServiceSettings = (PreferenceCategory) Settings.this.findPreference("app_service_settings");
                this.appReceiverSettings = (PreferenceCategory) Settings.this.findPreference("app_receiver_settings");
                this.pm = PreferencesActivity.mContext.getPackageManager();
                this.packages = this.pm.getInstalledApplications(128);
            }

            protected Void doInBackground(Void... arg0) {
                List<String[]> sortedApps = new ArrayList();
                if (this.appSettings != null) {
                    this.appSettings.removeAll();
                }
                if (this.appServiceSettings != null) {
                    this.appServiceSettings.removeAll();
                }
                if (this.appReceiverSettings != null) {
                    this.appReceiverSettings.removeAll();
                }
                for (ApplicationInfo app : this.packages) {
                    if (Settings.this.isAllowedApp(app)) {
                        sortedApps.add(new String[]{((ApplicationInfo) i$.next()).packageName, ((ApplicationInfo) i$.next()).loadLabel(PreferencesActivity.mContext.getPackageManager()).toString()});
                    }
                }
                Collections.sort(sortedApps, new C00141());
                for (int i = 0; i < sortedApps.size(); i++) {
                    String appName = ((String[]) sortedApps.get(i))[1];
                    String packageName = ((String[]) sortedApps.get(i))[0];
                    Preference appPreference = new Preference(PreferencesActivity.mContext);
                    Preference servicePreference = new Preference(PreferencesActivity.mContext);
                    Preference receiverPreference = new Preference(PreferencesActivity.mContext);
                    appPreference.setTitle(appName);
                    servicePreference.setTitle(appName);
                    receiverPreference.setTitle(appName);
                    appPreference.setSummary(packageName);
                    servicePreference.setSummary(packageName);
                    receiverPreference.setSummary(packageName);
                    appPreference.setOnPreferenceClickListener(new C00152(packageName, appName));
                    if (this.appSettings != null) {
                        this.appSettings.addPreference(appPreference);
                    }
                    servicePreference.setOnPreferenceClickListener(new C00163(packageName, appName));
                    if (this.appServiceSettings != null) {
                        this.appServiceSettings.addPreference(servicePreference);
                    }
                    receiverPreference.setOnPreferenceClickListener(new C00174(packageName, appName));
                    if (this.appReceiverSettings != null) {
                        this.appReceiverSettings.addPreference(receiverPreference);
                    }
                }
                return null;
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (VERSION.SDK_INT < 24) {
                getPreferenceManager().setSharedPreferencesMode(1);
            }
            addPreferencesFromResource(C0000R.xml.preferences);
            PreferencesActivity.mPrefs = PreferenceManager.getDefaultSharedPreferences(PreferencesActivity.mContext);
            PreferenceScreen mainSettings = (PreferenceScreen) findPreference("better_battery_saver");
            findPreference("show_system_apps").setOnPreferenceChangeListener(new C00051());
            OnPreferenceChangeListener c00062 = new C00062();
            Preference showSystemServices = findPreference("show_system_services");
            if (showSystemServices != null) {
                showSystemServices.setOnPreferenceChangeListener(c00062);
            }
            Preference showSystemReceivers = findPreference("show_system_receivers");
            if (showSystemReceivers != null) {
                showSystemReceivers.setOnPreferenceChangeListener(c00062);
            }
            PreferenceScreen appBatterySavingSettings = (PreferenceScreen) findPreference("app_battery_saving_settings");
            if (!PreferencesActivity.isXposedModuleEnabled()) {
                mainSettings.removePreference(appBatterySavingSettings);
            }
            PreferenceCategory batterySaverOn = (PreferenceCategory) findPreference("battery_saver_on");
            PreferenceCategory batterySaverOff = (PreferenceCategory) findPreference("battery_saver_off");
            PreferenceScreen appServiceManager = (PreferenceScreen) findPreference("app_services_manager");
            PreferenceScreen appReceiverManager = (PreferenceScreen) findPreference("app_receivers_manager");
            PreferenceScreen settings = (PreferenceScreen) findPreference("settings");
            if (VERSION.SDK_INT < 21) {
                batterySaverOn.removePreference(findPreference("turn_android_saver_off"));
                batterySaverOff.removePreference(findPreference("turn_android_saver_on"));
                settings.removePreference(findPreference("headsup_notifications"));
            }
            if (VERSION.SDK_INT < 23) {
                batterySaverOn.removePreference(findPreference("turn_doze_off"));
                batterySaverOff.removePreference(findPreference("turn_doze_on"));
            }
            if (!PreferencesActivity.mUtils.hasRoot()) {
                batterySaverOn.removePreference(findPreference("turn_device_off"));
                batterySaverOn.removePreference(findPreference("turn_screen_off"));
                batterySaverOn.removePreference(findPreference("turn_mobile_data_off"));
                batterySaverOn.removePreference(findPreference("turn_airplane_mode_on"));
                batterySaverOn.removePreference(findPreference("turn_nfc_off"));
                batterySaverOn.removePreference(findPreference("turn_gps_off"));
                batterySaverOff.removePreference(findPreference("turn_screen_on"));
                batterySaverOff.removePreference(findPreference("turn_mobile_data_on"));
                batterySaverOff.removePreference(findPreference("turn_airplane_mode_off"));
                batterySaverOff.removePreference(findPreference("turn_nfc_on"));
                batterySaverOff.removePreference(findPreference("turn_gps_on"));
                if (VERSION.SDK_INT >= 21) {
                    batterySaverOn.removePreference(findPreference("turn_android_saver_on"));
                    batterySaverOff.removePreference(findPreference("turn_android_saver_off"));
                }
                if (VERSION.SDK_INT >= 23) {
                    batterySaverOn.removePreference(findPreference("turn_doze_on"));
                    batterySaverOff.removePreference(findPreference("turn_doze_off"));
                }
                mainSettings.removePreference(appServiceManager);
                mainSettings.removePreference(appReceiverManager);
            }
            reloadAppsList();
            Preference batteryLevelThreshold = findPreference("battery_level_threshold");
            batteryLevelThreshold.setTitle(getString(C0000R.string.battery_level_threshold, new Object[]{Integer.valueOf(PreferencesActivity.mUtils.getBatteryLevelThreshold())}));
            batteryLevelThreshold.setOnPreferenceChangeListener(new C00073());
            boolean setScreenTimeoutOffValue = PreferencesActivity.mUtils.getBooleanPreference("set_screen_timeout_off");
            Preference screenTimeoutOff = findPreference("screen_timeout_off");
            int screenTimeoutOffValue = PreferencesActivity.mUtils.getIntPreference("screen_timeout_off");
            screenTimeoutOff.setTitle(getString(C0000R.string.screen_timeout, new Object[]{Integer.valueOf(screenTimeoutOffValue)}));
            screenTimeoutOff.setEnabled(setScreenTimeoutOffValue);
            boolean setScreenTimeoutOnValue = PreferencesActivity.mUtils.getBooleanPreference("set_screen_timeout_on");
            Preference screenTimeoutOn = findPreference("screen_timeout_on");
            int screenTimeoutOnValue = PreferencesActivity.mUtils.getIntPreference("screen_timeout_on");
            String screenTimeoutOnTitle = getString(C0000R.string.screen_timeout, new Object[]{Integer.valueOf(screenTimeoutOnValue)});
            screenTimeoutOn.setEnabled(setScreenTimeoutOnValue);
            screenTimeoutOn.setTitle(screenTimeoutOnTitle);
            OnPreferenceChangeListener listener = new C00084();
            screenTimeoutOff.setOnPreferenceChangeListener(listener);
            screenTimeoutOn.setOnPreferenceChangeListener(listener);
            Preference useAppBatterySaving = findPreference("app_battery_saving");
            if (useAppBatterySaving != null) {
                PreferenceCategory appSettings = (PreferenceCategory) findPreference("app_settings");
                if (PreferencesActivity.mUtils.getBooleanPreference("app_battery_saving")) {
                    appBatterySavingSettings.addPreference(appSettings);
                    reloadAppsList();
                } else {
                    appBatterySavingSettings.removePreference(appSettings);
                }
                useAppBatterySaving.setOnPreferenceChangeListener(new C00095(appBatterySavingSettings, appSettings));
            }
            C00106 c00106 = new C00106();
            findPreference("reset_all_settings").setOnPreferenceClickListener(c00106);
            findPreference("version").setSummary(BuildConfig.VERSION_NAME);
            findPreference("support").setOnPreferenceClickListener(new C00117());
            C00128 c00128 = new C00128();
            findPreference("donate").setOnPreferenceClickListener(c00128);
        }

        private void openLink(String url) {
            Intent openUrl = new Intent("android.intent.action.VIEW");
            openUrl.setData(Uri.parse(url));
            openUrl.addFlags(268435456);
            try {
                startActivity(openUrl);
            } catch (ActivityNotFoundException e) {
            }
        }

        @TargetApi(23)
        private void openManageWriteSettings() {
            if (VERSION.SDK_INT >= 23) {
                Builder dialog = new Builder(getActivity());
                dialog.setTitle(C0000R.string.modify_system_settings);
                dialog.setMessage(C0000R.string.grant_write_system_settings_permission_message);
                dialog.setCancelable(true);
                dialog.setPositiveButton(C0000R.string.grant_write_system_settings_permission, new C00139());
                dialog.create().show();
            }
        }

        public void onResume() {
            super.onResume();
            Preference brightnessOff = findPreference("turn_brightness_off");
            Preference brightnessOn = findPreference("turn_brightness_on");
            Preference setScreenTimeoutOff = findPreference("set_screen_timeout_off");
            Preference setScreenTimeoutOn = findPreference("set_screen_timeout_on");
            Preference wifiApOff = findPreference("turn_wifi_ap_off");
            Preference wifiApOn = findPreference("turn_wifi_ap_on");
            if (PreferencesActivity.mUtils.canWriteSystemSettings()) {
                brightnessOff.setOnPreferenceChangeListener(null);
                brightnessOn.setOnPreferenceChangeListener(null);
                wifiApOff.setOnPreferenceChangeListener(null);
                wifiApOn.setOnPreferenceChangeListener(null);
                Preference screenTimeoutOff = findPreference("screen_timeout_off");
                Preference screenTimeoutOn = findPreference("screen_timeout_on");
                setScreenTimeoutOff.setOnPreferenceChangeListener(new AnonymousClass11(screenTimeoutOff));
                setScreenTimeoutOn.setOnPreferenceChangeListener(new AnonymousClass12(screenTimeoutOn));
                return;
            }
            OnPreferenceChangeListener listener = new OnPreferenceChangeListener() {
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    Settings.this.openManageWriteSettings();
                    return false;
                }
            };
            brightnessOff.setOnPreferenceChangeListener(listener);
            brightnessOn.setOnPreferenceChangeListener(listener);
            setScreenTimeoutOff.setOnPreferenceChangeListener(listener);
            setScreenTimeoutOn.setOnPreferenceChangeListener(listener);
            wifiApOff.setOnPreferenceChangeListener(listener);
            wifiApOn.setOnPreferenceChangeListener(listener);
        }

        public void onPause() {
            super.onPause();
            PreferencesActivity.mUtils.setPrefsFileWorldReadable();
        }

        public void reloadAppsList() {
            new LoadApps().execute(new Void[0]);
        }

        public boolean isAllowedApp(ApplicationInfo appInfo) {
            boolean showSystemApps = PreferencesActivity.mUtils.getBooleanPreference("show_system_apps");
            if ((appInfo.flags & 1) == 0 || showSystemApps) {
                return true;
            }
            return false;
        }
    }

    public static boolean isXposedModuleEnabled() {
        return false;
    }

    protected void onCreate(Bundle savedInstanceState) {
        mUtils = new Utils(this);
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        getFragmentManager().beginTransaction().replace(16908290, new Settings()).commit();
    }
}
