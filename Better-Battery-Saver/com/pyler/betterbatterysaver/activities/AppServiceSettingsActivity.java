package com.pyler.betterbatterysaver.activities;

import android.app.ActionBar;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ServiceInfo;
import android.os.Build.VERSION;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import com.pyler.betterbatterysaver.C0000R;
import com.pyler.betterbatterysaver.util.DeviceController;
import com.pyler.betterbatterysaver.util.Utils;

public class AppServiceSettingsActivity extends PreferenceActivity {
    private static String appName;
    public static Context mContext;
    public static SharedPreferences mPrefs;
    public static Utils mUtils;
    private static String packageName;

    public static class Settings extends PreferenceFragment {

        /* renamed from: com.pyler.betterbatterysaver.activities.AppServiceSettingsActivity.Settings.1 */
        class C00021 implements OnPreferenceChangeListener {
            final /* synthetic */ boolean val$isServiceEnabled;
            final /* synthetic */ String val$serviceComponentName;

            C00021(boolean z, String str) {
                this.val$isServiceEnabled = z;
                this.val$serviceComponentName = str;
            }

            public boolean onPreferenceChange(Preference preference, Object newValue) {
                DeviceController device = new DeviceController(AppServiceSettingsActivity.mContext);
                if (((Boolean) newValue).booleanValue()) {
                    if (!this.val$isServiceEnabled) {
                        device.setServiceMode(this.val$serviceComponentName, true);
                    }
                } else if (this.val$isServiceEnabled) {
                    device.setServiceMode(this.val$serviceComponentName, false);
                }
                return true;
            }
        }

        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            if (VERSION.SDK_INT < 24) {
                getPreferenceManager().setSharedPreferencesMode(1);
            }
            addPreferencesFromResource(C0000R.xml.app_services);
            AppServiceSettingsActivity.mPrefs = PreferenceManager.getDefaultSharedPreferences(AppServiceSettingsActivity.mContext);
            PreferenceScreen appServices = (PreferenceScreen) findPreference("app_services");
            PackageManager pm = AppServiceSettingsActivity.mContext.getPackageManager();
            PackageInfo pi = null;
            try {
                pi = pm.getPackageInfo(AppServiceSettingsActivity.packageName, 4);
            } catch (NameNotFoundException e) {
                getActivity().finish();
            }
            ServiceInfo[] serviceList = pi.services;
            if (serviceList != null) {
                for (ServiceInfo service : serviceList) {
                    CheckBoxPreference serviceSetting = new CheckBoxPreference(AppServiceSettingsActivity.mContext);
                    String longName = service.name;
                    String shortName = longName.substring(longName.lastIndexOf(".") + 1, longName.length());
                    ComponentName cn = new ComponentName(AppServiceSettingsActivity.packageName, longName);
                    boolean isServiceEnabled = pm.getComponentEnabledSetting(cn) != 2;
                    String serviceComponentName = cn.flattenToShortString();
                    serviceSetting.setTitle(shortName);
                    serviceSetting.setSummary(longName);
                    serviceSetting.setChecked(isServiceEnabled);
                    serviceSetting.setOnPreferenceChangeListener(new C00021(isServiceEnabled, serviceComponentName));
                    appServices.addPreference(serviceSetting);
                }
            }
        }

        public void onPause() {
            super.onPause();
            AppServiceSettingsActivity.mUtils.setPrefsFileWorldReadable();
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
