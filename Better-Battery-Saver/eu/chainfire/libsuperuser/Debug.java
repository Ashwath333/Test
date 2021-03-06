package eu.chainfire.libsuperuser;

import android.os.Looper;
import android.util.Log;

public class Debug {
    public static final int LOG_ALL = 65535;
    public static final int LOG_COMMAND = 2;
    public static final int LOG_GENERAL = 1;
    public static final int LOG_NONE = 0;
    public static final int LOG_OUTPUT = 4;
    public static final String TAG = "libsuperuser";
    private static boolean debug;
    private static OnLogListener logListener;
    private static int logTypes;
    private static boolean sanityChecks;

    public interface OnLogListener {
        void onLog(int i, String str, String str2);
    }

    static {
        debug = false;
        logTypes = LOG_ALL;
        logListener = null;
        sanityChecks = true;
    }

    public static void setDebug(boolean enable) {
        debug = enable;
    }

    public static boolean getDebug() {
        return debug;
    }

    private static void logCommon(int type, String typeIndicator, String message) {
        if (!debug || (logTypes & type) != type) {
            return;
        }
        if (logListener != null) {
            logListener.onLog(type, typeIndicator, message);
            return;
        }
        String str = TAG;
        StringBuilder append = new StringBuilder().append("[libsuperuser][").append(typeIndicator).append("]");
        String str2 = (message.startsWith("[") || message.startsWith(" ")) ? BuildConfig.FLAVOR : " ";
        Log.d(str, append.append(str2).append(message).toString());
    }

    public static void log(String message) {
        logCommon(LOG_GENERAL, "G", message);
    }

    public static void logCommand(String message) {
        logCommon(LOG_COMMAND, "C", message);
    }

    public static void logOutput(String message) {
        logCommon(LOG_OUTPUT, "O", message);
    }

    public static void setLogTypeEnabled(int type, boolean enable) {
        if (enable) {
            logTypes |= type;
        } else {
            logTypes &= type ^ -1;
        }
    }

    public static boolean getLogTypeEnabled(int type) {
        return (logTypes & type) == type;
    }

    public static boolean getLogTypeEnabledEffective(int type) {
        return getDebug() && getLogTypeEnabled(type);
    }

    public static void setOnLogListener(OnLogListener onLogListener) {
        logListener = onLogListener;
    }

    public static OnLogListener getOnLogListener() {
        return logListener;
    }

    public static void setSanityChecksEnabled(boolean enable) {
        sanityChecks = enable;
    }

    public static boolean getSanityChecksEnabled() {
        return sanityChecks;
    }

    public static boolean getSanityChecksEnabledEffective() {
        return getDebug() && getSanityChecksEnabled();
    }

    public static boolean onMainThread() {
        return Looper.myLooper() != null && Looper.myLooper() == Looper.getMainLooper();
    }
}
