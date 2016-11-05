package eu.chainfire.libsuperuser;

import eu.chainfire.libsuperuser.Shell.Interactive;
import eu.chainfire.libsuperuser.Shell.SU;
import java.util.ArrayList;
import java.util.List;

public abstract class Policy {
    private static final int MAX_POLICY_LENGTH = 4064;
    private static volatile Boolean canInject;
    private static volatile boolean injected;
    private static final Object synchronizer;

    protected abstract String[] getPolicies();

    static {
        synchronizer = new Object();
        canInject = null;
        injected = false;
    }

    public static boolean haveInjected() {
        return injected;
    }

    public static void resetInjected() {
        synchronized (synchronizer) {
            injected = false;
        }
    }

    public static boolean canInject() {
        boolean booleanValue;
        synchronized (synchronizer) {
            if (canInject != null) {
                booleanValue = canInject.booleanValue();
            } else {
                canInject = Boolean.valueOf(false);
                List<String> result = Shell.run("sh", new String[]{"supolicy"}, null, false);
                if (result != null) {
                    for (String line : result) {
                        if (line.contains("supolicy")) {
                            canInject = Boolean.valueOf(true);
                            break;
                        }
                    }
                }
                booleanValue = canInject.booleanValue();
            }
        }
        return booleanValue;
    }

    public static void resetCanInject() {
        synchronized (synchronizer) {
            canInject = null;
        }
    }

    protected List<String> getInjectCommands() {
        return getInjectCommands(true);
    }

    protected List<String> getInjectCommands(boolean allowBlocking) {
        List<String> list = null;
        synchronized (synchronizer) {
            if (!SU.isSELinuxEnforcing()) {
            } else if (allowBlocking && !canInject()) {
            } else if (injected) {
            } else {
                String[] policies = getPolicies();
                if (policies == null || policies.length <= 0) {
                } else {
                    list = new ArrayList();
                    String command = BuildConfig.FLAVOR;
                    for (String policy : policies) {
                        if (command.length() == 0 || (command.length() + policy.length()) + 3 < MAX_POLICY_LENGTH) {
                            command = command + " \"" + policy + "\"";
                        } else {
                            list.add("supolicy --live" + command);
                            command = BuildConfig.FLAVOR;
                        }
                    }
                    if (command.length() > 0) {
                        list.add("supolicy --live" + command);
                    }
                }
            }
        }
        return list;
    }

    public void inject() {
        synchronized (synchronizer) {
            List commands = getInjectCommands();
            if (commands != null && commands.size() > 0) {
                SU.run(commands);
            }
            injected = true;
        }
    }

    public void inject(Interactive shell, boolean waitForIdle) {
        synchronized (synchronizer) {
            List commands = getInjectCommands(waitForIdle);
            if (commands != null && commands.size() > 0) {
                shell.addCommand(commands);
                if (waitForIdle) {
                    shell.waitForIdle();
                }
            }
            injected = true;
        }
    }
}
