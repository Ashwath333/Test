package com.pyler.betterbatterysaver.activities;

import android.app.ActionBar;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceFragment;
import android.preference.PreferenceGroup;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.pyler.betterbatterysaver.C0000R;
import com.pyler.betterbatterysaver.util.Utils;
import java.util.ArrayList;
import java.util.Iterator;

public class AppSettingsActivity extends PreferenceActivity {
    private static String appName;
    public static Context mContext;
    public static SharedPreferences mPrefs;
    public static Utils mUtils;
    private static String packageName;

    public static class Settings extends PreferenceFragment {

        /* renamed from: com.pyler.betterbatterysaver.activities.AppSettingsActivity.Settings.1 */
        class C00031 implements OnPreferenceChangeListener {
            final /* synthetic */ PreferenceScreen val$appSettings;
            final /* synthetic */ Preference val$batteryLevelThreshold;
            final /* synthetic */ PreferenceCategory val$batterySavingSettings;

            C00031(Preference preference, PreferenceScreen preferenceScreen, PreferenceCategory preferenceCategory) {
                this.val$batteryLevelThreshold = preference;
                this.val$appSettings = preferenceScreen;
                this.val$batterySavingSettings = preferenceCategory;
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                boolean set = ((Boolean) newValue).booleanValue();
                this.val$batteryLevelThreshold.setEnabled(set);
                if (set) {
                    this.val$appSettings.addPreference(this.val$batterySavingSettings);
                } else {
                    this.val$appSettings.removePreference(this.val$batterySavingSettings);
                }
                return true;
            }
        }

        /* renamed from: com.pyler.betterbatterysaver.activities.AppSettingsActivity.Settings.2 */
        class C00042 implements OnPreferenceChangeListener {
            C00042() {
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                int batteryLevel = ((Integer) newValue).intValue();
                preference.setTitle(Settings.this.getString(C0000R.string.battery_level_threshold, new Object[]{Integer.valueOf(batteryLevel)}));
                return true;
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (VERSION.SDK_INT < 24) {
                getPreferenceManager().setSharedPreferencesMode(1);
            }
            addPreferencesFromResource(C0000R.xml.app_settings);
            AppSettingsActivity.mPrefs = PreferenceManager.getDefaultSharedPreferences(AppSettingsActivity.mContext);
            PreferenceScreen appSettings = (PreferenceScreen) findPreference("app_settings");
            Iterator i$ = getPreferenceList(appSettings, new ArrayList()).iterator();
            while (i$.hasNext()) {
                Preference p = (Preference) i$.next();
                String newKey = getKeyForPackage(p.getKey());
                p.setKey(newKey);
                if (p instanceof CheckBoxPreference) {
                    ((CheckBoxPreference) p).setChecked(AppSettingsActivity.mPrefs.getBoolean(newKey, false));
                } else {
                    AppSettingsActivity.mUtils.setValueSeekBarPreference(p, AppSettingsActivity.mPrefs.getInt(getKeyForPackage("battery_level_threshold"), 15));
                }
            }
            Preference batterySaving = findPreference(getKeyForPackage("battery_saving"));
            Preference batteryLevelThreshold = findPreference(getKeyForPackage("battery_level_threshold"));
            PreferenceCategory batterySavingSettings = (PreferenceCategory) findPreference("battery_saving_settings");
            boolean batterySavingValue = AppSettingsActivity.mUtils.getBooleanPreference(getKeyForPackage("battery_saving"));
            batteryLevelThreshold.setEnabled(batterySavingValue);
            if (!batterySavingValue) {
                appSettings.removePreference(batterySavingSettings);
            }
            batterySaving.setOnPreferenceChangeListener(new C00031(batteryLevelThreshold, appSettings, batterySavingSettings));
            int batteryLevel = AppSettingsActivity.mUtils.getBatteryLevelThreshold(AppSettingsActivity.packageName);
            batteryLevelThreshold.setTitle(getString(C0000R.string.battery_level_threshold, new Object[]{Integer.valueOf(batteryLevel)}));
            batteryLevelThreshold.setOnPreferenceChangeListener(new C00042());
        }

        public void onPause() {
            super.onPause();
            AppSettingsActivity.mUtils.setPrefsFileWorldReadable();
        }

        private String getKeyForPackage(String key) {
            return AppSettingsActivity.mUtils.getKeyForPackage(AppSettingsActivity.packageName, key);
        }

        private ArrayList<Preference> getPreferenceList(Preference p, ArrayList<Preference> list) {
            if ((p instanceof PreferenceCategory) || (p instanceof PreferenceScreen)) {
                PreferenceGroup pGroup = (PreferenceGroup) p;
                int pCount = pGroup.getPreferenceCount();
                for (int i = 0; i < pCount; i++) {
                    getPreferenceList(pGroup.getPreference(i), list);
                }
            } else {
                list.add(p);
            }
            return list;
        }
    }

    protected void onCreate(Bundle savedInstanceState) {
        mUtils = new Utils(this);
        super.onCreate(savedInstanceState);
        mContext = getApplicationContext();
        Intent intent = getIntent();
        if (intent != null) {
            packageName = intent.getStringExtra("package");
            appName = intent.getStringExtra("app");
            ActionBar actionBar = getActionBar();
            if (actionBar != null) {
                actionBar.setTitle(appName);
            }
            getFragmentManager().beginTransaction().replace(16908290, new Settings()).commit();
        }
    }
}
