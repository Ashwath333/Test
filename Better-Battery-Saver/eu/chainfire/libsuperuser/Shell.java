package eu.chainfire.libsuperuser;

import android.os.Build.VERSION;
import android.os.Handler;
import android.os.Looper;
import eu.chainfire.libsuperuser.StreamGobbler.OnLineListener;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Map.Entry;
import java.util.UUID;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

public class Shell {
    protected static String[] availableTestCommands;

    public static class Builder {
        private boolean autoHandler;
        private List<Command> commands;
        private Map<String, String> environment;
        private Handler handler;
        private OnLineListener onSTDERRLineListener;
        private OnLineListener onSTDOUTLineListener;
        private String shell;
        private boolean wantSTDERR;
        private int watchdogTimeout;

        public Builder() {
            this.handler = null;
            this.autoHandler = true;
            this.shell = "sh";
            this.wantSTDERR = false;
            this.commands = new LinkedList();
            this.environment = new HashMap();
            this.onSTDOUTLineListener = null;
            this.onSTDERRLineListener = null;
            this.watchdogTimeout = 0;
        }

        public Builder setHandler(Handler handler) {
            this.handler = handler;
            return this;
        }

        public Builder setAutoHandler(boolean autoHandler) {
            this.autoHandler = autoHandler;
            return this;
        }

        public Builder setShell(String shell) {
            this.shell = shell;
            return this;
        }

        public Builder useSH() {
            return setShell("sh");
        }

        public Builder useSU() {
            return setShell("su");
        }

        public Builder setWantSTDERR(boolean wantSTDERR) {
            this.wantSTDERR = wantSTDERR;
            return this;
        }

        public Builder addEnvironment(String key, String value) {
            this.environment.put(key, value);
            return this;
        }

        public Builder addEnvironment(Map<String, String> addEnvironment) {
            this.environment.putAll(addEnvironment);
            return this;
        }

        public Builder addCommand(String command) {
            return addCommand(command, 0, null);
        }

        public Builder addCommand(String command, int code, OnCommandResultListener onCommandResultListener) {
            return addCommand(new String[]{command}, code, onCommandResultListener);
        }

        public Builder addCommand(List<String> commands) {
            return addCommand((List) commands, 0, null);
        }

        public Builder addCommand(List<String> commands, int code, OnCommandResultListener onCommandResultListener) {
            return addCommand((String[]) commands.toArray(new String[commands.size()]), code, onCommandResultListener);
        }

        public Builder addCommand(String[] commands) {
            return addCommand(commands, 0, null);
        }

        public Builder addCommand(String[] commands, int code, OnCommandResultListener onCommandResultListener) {
            this.commands.add(new Command(commands, code, onCommandResultListener, null));
            return this;
        }

        public Builder setOnSTDOUTLineListener(OnLineListener onLineListener) {
            this.onSTDOUTLineListener = onLineListener;
            return this;
        }

        public Builder setOnSTDERRLineListener(OnLineListener onLineListener) {
            this.onSTDERRLineListener = onLineListener;
            return this;
        }

        public Builder setWatchdogTimeout(int watchdogTimeout) {
            this.watchdogTimeout = watchdogTimeout;
            return this;
        }

        public Builder setMinimalLogging(boolean useMinimal) {
            Debug.setLogTypeEnabled(6, !useMinimal);
            return this;
        }

        public Interactive open() {
            return new Interactive(null, null);
        }

        public Interactive open(OnCommandResultListener onCommandResultListener) {
            return new Interactive(onCommandResultListener, null);
        }
    }

    private static class Command {
        private static int commandCounter;
        private final int code;
        private final String[] commands;
        private final String marker;
        private final OnCommandLineListener onCommandLineListener;
        private final OnCommandResultListener onCommandResultListener;

        static {
            commandCounter = 0;
        }

        public Command(String[] commands, int code, OnCommandResultListener onCommandResultListener, OnCommandLineListener onCommandLineListener) {
            this.commands = commands;
            this.code = code;
            this.onCommandResultListener = onCommandResultListener;
            this.onCommandLineListener = onCommandLineListener;
            StringBuilder append = new StringBuilder().append(UUID.randomUUID().toString());
            Object[] objArr = new Object[1];
            int i = commandCounter + 1;
            commandCounter = i;
            objArr[0] = Integer.valueOf(i);
            this.marker = append.append(String.format("-%08x", objArr)).toString();
        }
    }

    public static class Interactive {
        private StreamGobbler STDERR;
        private DataOutputStream STDIN;
        private StreamGobbler STDOUT;
        private final boolean autoHandler;
        private volatile List<String> buffer;
        private final Object callbackSync;
        private volatile int callbacks;
        private volatile boolean closed;
        private volatile Command command;
        private final List<Command> commands;
        private final Map<String, String> environment;
        private final Handler handler;
        private volatile boolean idle;
        private final Object idleSync;
        private volatile int lastExitCode;
        private volatile String lastMarkerSTDERR;
        private volatile String lastMarkerSTDOUT;
        private final OnLineListener onSTDERRLineListener;
        private final OnLineListener onSTDOUTLineListener;
        private Process process;
        private volatile boolean running;
        private final String shell;
        private final boolean wantSTDERR;
        private ScheduledThreadPoolExecutor watchdog;
        private volatile int watchdogCount;
        private int watchdogTimeout;

        /* renamed from: eu.chainfire.libsuperuser.Shell.Interactive.2 */
        class C00282 implements Runnable {
            C00282() {
            }

            public void run() {
                Interactive.this.handleWatchdog();
            }
        }

        /* renamed from: eu.chainfire.libsuperuser.Shell.Interactive.3 */
        class C00293 implements Runnable {
            final /* synthetic */ String val$fLine;
            final /* synthetic */ OnLineListener val$fListener;

            C00293(OnLineListener onLineListener, String str) {
                this.val$fListener = onLineListener;
                this.val$fLine = str;
            }

            public void run() {
                try {
                    this.val$fListener.onLine(this.val$fLine);
                } finally {
                    Interactive.this.endCallback();
                }
            }
        }

        /* renamed from: eu.chainfire.libsuperuser.Shell.Interactive.4 */
        class C00304 implements Runnable {
            final /* synthetic */ Command val$fCommand;
            final /* synthetic */ int val$fExitCode;
            final /* synthetic */ List val$fOutput;

            C00304(Command command, List list, int i) {
                this.val$fCommand = command;
                this.val$fOutput = list;
                this.val$fExitCode = i;
            }

            public void run() {
                try {
                    if (!(this.val$fCommand.onCommandResultListener == null || this.val$fOutput == null)) {
                        this.val$fCommand.onCommandResultListener.onCommandResult(this.val$fCommand.code, this.val$fExitCode, this.val$fOutput);
                    }
                    if (this.val$fCommand.onCommandLineListener != null) {
                        this.val$fCommand.onCommandLineListener.onCommandResult(this.val$fCommand.code, this.val$fExitCode);
                    }
                    Interactive.this.endCallback();
                } catch (Throwable th) {
                    Interactive.this.endCallback();
                }
            }
        }

        /* renamed from: eu.chainfire.libsuperuser.Shell.Interactive.5 */
        class C00315 implements OnLineListener {
            C00315() {
            }

            public void onLine(String line) {
                synchronized (Interactive.this) {
                    if (Interactive.this.command == null) {
                        return;
                    }
                    String contentPart = line;
                    String markerPart = null;
                    int markerIndex = line.indexOf(Interactive.this.command.marker);
                    if (markerIndex == 0) {
                        contentPart = null;
                        markerPart = line;
                    } else if (markerIndex > 0) {
                        contentPart = line.substring(0, markerIndex);
                        markerPart = line.substring(markerIndex);
                    }
                    if (contentPart != null) {
                        Interactive.this.addBuffer(contentPart);
                        Interactive.this.processLine(contentPart, Interactive.this.onSTDOUTLineListener);
                        Interactive.this.processLine(contentPart, Interactive.this.command.onCommandLineListener);
                    }
                    if (markerPart != null) {
                        try {
                            Interactive.this.lastExitCode = Integer.valueOf(markerPart.substring(Interactive.this.command.marker.length() + 1), 10).intValue();
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        Interactive.this.lastMarkerSTDOUT = Interactive.this.command.marker;
                        Interactive.this.processMarker();
                    }
                }
            }
        }

        /* renamed from: eu.chainfire.libsuperuser.Shell.Interactive.6 */
        class C00326 implements OnLineListener {
            C00326() {
            }

            public void onLine(String line) {
                synchronized (Interactive.this) {
                    if (Interactive.this.command == null) {
                        return;
                    }
                    String contentPart = line;
                    int markerIndex = line.indexOf(Interactive.this.command.marker);
                    if (markerIndex == 0) {
                        contentPart = null;
                    } else if (markerIndex > 0) {
                        contentPart = line.substring(0, markerIndex);
                    }
                    if (contentPart != null) {
                        if (Interactive.this.wantSTDERR) {
                            Interactive.this.addBuffer(contentPart);
                        }
                        Interactive.this.processLine(contentPart, Interactive.this.onSTDERRLineListener);
                    }
                    if (markerIndex >= 0) {
                        Interactive.this.lastMarkerSTDERR = Interactive.this.command.marker;
                        Interactive.this.processMarker();
                    }
                }
            }
        }

        /* renamed from: eu.chainfire.libsuperuser.Shell.Interactive.1 */
        class C00331 implements OnCommandResultListener {
            final /* synthetic */ Builder val$builder;
            final /* synthetic */ OnCommandResultListener val$onCommandResultListener;

            C00331(Builder builder, OnCommandResultListener onCommandResultListener) {
                this.val$builder = builder;
                this.val$onCommandResultListener = onCommandResultListener;
            }

            public void onCommandResult(int commandCode, int exitCode, List<String> output) {
                if (exitCode == 0 && !Shell.parseAvailableResult(output, SU.isSU(Interactive.this.shell))) {
                    exitCode = -4;
                }
                Interactive.this.watchdogTimeout = this.val$builder.watchdogTimeout;
                this.val$onCommandResultListener.onCommandResult(0, exitCode, output);
            }
        }

        private Interactive(Builder builder, OnCommandResultListener onCommandResultListener) {
            this.process = null;
            this.STDIN = null;
            this.STDOUT = null;
            this.STDERR = null;
            this.watchdog = null;
            this.running = false;
            this.idle = true;
            this.closed = true;
            this.callbacks = 0;
            this.idleSync = new Object();
            this.callbackSync = new Object();
            this.lastExitCode = 0;
            this.lastMarkerSTDOUT = null;
            this.lastMarkerSTDERR = null;
            this.command = null;
            this.buffer = null;
            this.autoHandler = builder.autoHandler;
            this.shell = builder.shell;
            this.wantSTDERR = builder.wantSTDERR;
            this.commands = builder.commands;
            this.environment = builder.environment;
            this.onSTDOUTLineListener = builder.onSTDOUTLineListener;
            this.onSTDERRLineListener = builder.onSTDERRLineListener;
            this.watchdogTimeout = builder.watchdogTimeout;
            if (Looper.myLooper() != null && builder.handler == null && this.autoHandler) {
                this.handler = new Handler();
            } else {
                this.handler = builder.handler;
            }
            if (onCommandResultListener != null) {
                this.watchdogTimeout = 60;
                this.commands.add(0, new Command(Shell.availableTestCommands, 0, new C00331(builder, onCommandResultListener), null));
            }
            if (!open() && onCommandResultListener != null) {
                onCommandResultListener.onCommandResult(0, -3, null);
            }
        }

        protected void finalize() throws Throwable {
            if (this.closed || !Debug.getSanityChecksEnabledEffective()) {
                super.finalize();
            } else {
                Debug.log(ShellNotClosedException.EXCEPTION_NOT_CLOSED);
                throw new ShellNotClosedException();
            }
        }

        public void addCommand(String command) {
            addCommand(command, 0, (OnCommandResultListener) null);
        }

        public void addCommand(String command, int code, OnCommandResultListener onCommandResultListener) {
            addCommand(new String[]{command}, code, onCommandResultListener);
        }

        public void addCommand(String command, int code, OnCommandLineListener onCommandLineListener) {
            addCommand(new String[]{command}, code, onCommandLineListener);
        }

        public void addCommand(List<String> commands) {
            addCommand((List) commands, 0, (OnCommandResultListener) null);
        }

        public void addCommand(List<String> commands, int code, OnCommandResultListener onCommandResultListener) {
            addCommand((String[]) commands.toArray(new String[commands.size()]), code, onCommandResultListener);
        }

        public void addCommand(List<String> commands, int code, OnCommandLineListener onCommandLineListener) {
            addCommand((String[]) commands.toArray(new String[commands.size()]), code, onCommandLineListener);
        }

        public void addCommand(String[] commands) {
            addCommand(commands, 0, (OnCommandResultListener) null);
        }

        public synchronized void addCommand(String[] commands, int code, OnCommandResultListener onCommandResultListener) {
            this.commands.add(new Command(commands, code, onCommandResultListener, null));
            runNextCommand();
        }

        public synchronized void addCommand(String[] commands, int code, OnCommandLineListener onCommandLineListener) {
            this.commands.add(new Command(commands, code, null, onCommandLineListener));
            runNextCommand();
        }

        private void runNextCommand() {
            runNextCommand(true);
        }

        private synchronized void handleWatchdog() {
            if (this.watchdog != null) {
                if (this.watchdogTimeout != 0) {
                    int exitCode;
                    if (isRunning()) {
                        int i = this.watchdogCount;
                        this.watchdogCount = i + 1;
                        if (i >= this.watchdogTimeout) {
                            exitCode = -1;
                            Debug.log(String.format("[%s%%] WATCHDOG_EXIT", new Object[]{this.shell.toUpperCase(Locale.ENGLISH)}));
                        }
                    } else {
                        exitCode = -2;
                        Debug.log(String.format("[%s%%] SHELL_DIED", new Object[]{this.shell.toUpperCase(Locale.ENGLISH)}));
                    }
                    postCallback(this.command, exitCode, this.buffer);
                    this.command = null;
                    this.buffer = null;
                    this.idle = true;
                    this.watchdog.shutdown();
                    this.watchdog = null;
                    kill();
                }
            }
        }

        private void startWatchdog() {
            if (this.watchdogTimeout != 0) {
                this.watchdogCount = 0;
                this.watchdog = new ScheduledThreadPoolExecutor(1);
                this.watchdog.scheduleAtFixedRate(new C00282(), 1, 1, TimeUnit.SECONDS);
            }
        }

        private void stopWatchdog() {
            if (this.watchdog != null) {
                this.watchdog.shutdownNow();
                this.watchdog = null;
            }
        }

        private void runNextCommand(boolean notifyIdle) {
            boolean running = isRunning();
            if (!running) {
                this.idle = true;
            }
            if (running && this.idle && this.commands.size() > 0) {
                Command command = (Command) this.commands.get(0);
                this.commands.remove(0);
                this.buffer = null;
                this.lastExitCode = 0;
                this.lastMarkerSTDOUT = null;
                this.lastMarkerSTDERR = null;
                if (command.commands.length > 0) {
                    try {
                        if (command.onCommandResultListener != null) {
                            this.buffer = Collections.synchronizedList(new ArrayList());
                        }
                        this.idle = false;
                        this.command = command;
                        startWatchdog();
                        for (String write : command.commands) {
                            Debug.logCommand(String.format("[%s+] %s", new Object[]{this.shell.toUpperCase(Locale.ENGLISH), write}));
                            this.STDIN.write((write + "\n").getBytes("UTF-8"));
                        }
                        this.STDIN.write(("echo " + command.marker + " $?\n").getBytes("UTF-8"));
                        this.STDIN.write(("echo " + command.marker + " >&2\n").getBytes("UTF-8"));
                        this.STDIN.flush();
                    } catch (IOException e) {
                    }
                } else {
                    runNextCommand(false);
                }
            } else if (!running) {
                while (this.commands.size() > 0) {
                    postCallback((Command) this.commands.remove(0), -2, null);
                }
            }
            if (this.idle && notifyIdle) {
                synchronized (this.idleSync) {
                    this.idleSync.notifyAll();
                }
            }
        }

        private synchronized void processMarker() {
            if (this.command.marker.equals(this.lastMarkerSTDOUT) && this.command.marker.equals(this.lastMarkerSTDERR)) {
                postCallback(this.command, this.lastExitCode, this.buffer);
                stopWatchdog();
                this.command = null;
                this.buffer = null;
                this.idle = true;
                runNextCommand();
            }
        }

        private synchronized void processLine(String line, OnLineListener listener) {
            if (listener != null) {
                if (this.handler != null) {
                    String fLine = line;
                    OnLineListener fListener = listener;
                    startCallback();
                    this.handler.post(new C00293(fListener, fLine));
                } else {
                    listener.onLine(line);
                }
            }
        }

        private synchronized void addBuffer(String line) {
            if (this.buffer != null) {
                this.buffer.add(line);
            }
        }

        private void startCallback() {
            synchronized (this.callbackSync) {
                this.callbacks++;
            }
        }

        private void postCallback(Command fCommand, int fExitCode, List<String> fOutput) {
            if (fCommand.onCommandResultListener != null || fCommand.onCommandLineListener != null) {
                if (this.handler == null) {
                    if (!(fCommand.onCommandResultListener == null || fOutput == null)) {
                        fCommand.onCommandResultListener.onCommandResult(fCommand.code, fExitCode, fOutput);
                    }
                    if (fCommand.onCommandLineListener != null) {
                        fCommand.onCommandLineListener.onCommandResult(fCommand.code, fExitCode);
                        return;
                    }
                    return;
                }
                startCallback();
                this.handler.post(new C00304(fCommand, fOutput, fExitCode));
            }
        }

        private void endCallback() {
            synchronized (this.callbackSync) {
                this.callbacks--;
                if (this.callbacks == 0) {
                    this.callbackSync.notifyAll();
                }
            }
        }

        private synchronized boolean open() {
            boolean z;
            Debug.log(String.format("[%s%%] START", new Object[]{this.shell.toUpperCase(Locale.ENGLISH)}));
            try {
                if (this.environment.size() == 0) {
                    this.process = Runtime.getRuntime().exec(this.shell);
                } else {
                    Map<String, String> newEnvironment = new HashMap();
                    newEnvironment.putAll(System.getenv());
                    newEnvironment.putAll(this.environment);
                    int i = 0;
                    String[] env = new String[newEnvironment.size()];
                    for (Entry<String, String> entry : newEnvironment.entrySet()) {
                        env[i] = ((String) entry.getKey()) + "=" + ((String) entry.getValue());
                        i++;
                    }
                    this.process = Runtime.getRuntime().exec(this.shell, env);
                }
                this.STDIN = new DataOutputStream(this.process.getOutputStream());
                this.STDOUT = new StreamGobbler(this.shell.toUpperCase(Locale.ENGLISH) + "-", this.process.getInputStream(), new C00315());
                this.STDERR = new StreamGobbler(this.shell.toUpperCase(Locale.ENGLISH) + "*", this.process.getErrorStream(), new C00326());
                this.STDOUT.start();
                this.STDERR.start();
                this.running = true;
                this.closed = false;
                runNextCommand();
                z = true;
            } catch (IOException e) {
                z = false;
            }
            return z;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public void close() {
            /*
            r7 = this;
            r5 = 1;
            r6 = 0;
            r0 = r7.isIdle();
            monitor-enter(r7);
            r2 = r7.running;	 Catch:{ all -> 0x002f }
            if (r2 != 0) goto L_0x000d;
        L_0x000b:
            monitor-exit(r7);	 Catch:{ all -> 0x002f }
        L_0x000c:
            return;
        L_0x000d:
            r2 = 0;
            r7.running = r2;	 Catch:{ all -> 0x002f }
            r2 = 1;
            r7.closed = r2;	 Catch:{ all -> 0x002f }
            monitor-exit(r7);	 Catch:{ all -> 0x002f }
            if (r0 != 0) goto L_0x0032;
        L_0x0016:
            r2 = eu.chainfire.libsuperuser.Debug.getSanityChecksEnabledEffective();
            if (r2 == 0) goto L_0x0032;
        L_0x001c:
            r2 = eu.chainfire.libsuperuser.Debug.onMainThread();
            if (r2 == 0) goto L_0x0032;
        L_0x0022:
            r2 = "Application attempted to wait for a non-idle shell to close on the main thread";
            eu.chainfire.libsuperuser.Debug.log(r2);
            r2 = new eu.chainfire.libsuperuser.ShellOnMainThreadException;
            r3 = "Application attempted to wait for a non-idle shell to close on the main thread";
            r2.<init>(r3);
            throw r2;
        L_0x002f:
            r2 = move-exception;
            monitor-exit(r7);	 Catch:{ all -> 0x002f }
            throw r2;
        L_0x0032:
            if (r0 != 0) goto L_0x0037;
        L_0x0034:
            r7.waitForIdle();
        L_0x0037:
            r2 = r7.STDIN;	 Catch:{ IOException -> 0x007b, InterruptedException -> 0x008b }
            r3 = "exit\n";
            r4 = "UTF-8";
            r3 = r3.getBytes(r4);	 Catch:{ IOException -> 0x007b, InterruptedException -> 0x008b }
            r2.write(r3);	 Catch:{ IOException -> 0x007b, InterruptedException -> 0x008b }
            r2 = r7.STDIN;	 Catch:{ IOException -> 0x007b, InterruptedException -> 0x008b }
            r2.flush();	 Catch:{ IOException -> 0x007b, InterruptedException -> 0x008b }
        L_0x0049:
            r2 = r7.process;	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2.waitFor();	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2 = r7.STDIN;	 Catch:{ IOException -> 0x008d, InterruptedException -> 0x008b }
            r2.close();	 Catch:{ IOException -> 0x008d, InterruptedException -> 0x008b }
        L_0x0053:
            r2 = r7.STDOUT;	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2.join();	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2 = r7.STDERR;	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2.join();	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r7.stopWatchdog();	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2 = r7.process;	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r2.destroy();	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
        L_0x0065:
            r2 = "[%s%%] END";
            r3 = new java.lang.Object[r5];
            r4 = r7.shell;
            r5 = java.util.Locale.ENGLISH;
            r4 = r4.toUpperCase(r5);
            r3[r6] = r4;
            r2 = java.lang.String.format(r2, r3);
            eu.chainfire.libsuperuser.Debug.log(r2);
            goto L_0x000c;
        L_0x007b:
            r1 = move-exception;
            r2 = r1.getMessage();	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            r3 = "EPIPE";
            r2 = r2.contains(r3);	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
            if (r2 != 0) goto L_0x0049;
        L_0x0088:
            throw r1;	 Catch:{ IOException -> 0x0089, InterruptedException -> 0x008b }
        L_0x0089:
            r2 = move-exception;
            goto L_0x0065;
        L_0x008b:
            r2 = move-exception;
            goto L_0x0065;
        L_0x008d:
            r2 = move-exception;
            goto L_0x0053;
            */
            throw new UnsupportedOperationException("Method not decompiled: eu.chainfire.libsuperuser.Shell.Interactive.close():void");
        }

        public synchronized void kill() {
            this.running = false;
            this.closed = true;
            try {
                this.STDIN.close();
            } catch (IOException e) {
            }
            try {
                this.process.destroy();
            } catch (Exception e2) {
            }
            this.idle = true;
            synchronized (this.idleSync) {
                this.idleSync.notifyAll();
            }
        }

        public boolean isRunning() {
            if (this.process == null) {
                return false;
            }
            try {
                this.process.exitValue();
                return false;
            } catch (IllegalThreadStateException e) {
                return true;
            }
        }

        public synchronized boolean isIdle() {
            if (!isRunning()) {
                this.idle = true;
                synchronized (this.idleSync) {
                    this.idleSync.notifyAll();
                }
            }
            return this.idle;
        }

        /* JADX WARNING: inconsistent code. */
        /* Code decompiled incorrectly, please refer to instructions dump. */
        public boolean waitForIdle() {
            /*
            r4 = this;
            r1 = 0;
            r2 = eu.chainfire.libsuperuser.Debug.getSanityChecksEnabledEffective();
            if (r2 == 0) goto L_0x001a;
        L_0x0007:
            r2 = eu.chainfire.libsuperuser.Debug.onMainThread();
            if (r2 == 0) goto L_0x001a;
        L_0x000d:
            r1 = "Application attempted to wait for a shell to become idle on the main thread";
            eu.chainfire.libsuperuser.Debug.log(r1);
            r1 = new eu.chainfire.libsuperuser.ShellOnMainThreadException;
            r2 = "Application attempted to wait for a shell to become idle on the main thread";
            r1.<init>(r2);
            throw r1;
        L_0x001a:
            r2 = r4.isRunning();
            if (r2 == 0) goto L_0x0060;
        L_0x0020:
            r2 = r4.idleSync;
            monitor-enter(r2);
        L_0x0023:
            r3 = r4.idle;	 Catch:{ all -> 0x005c }
            if (r3 != 0) goto L_0x0030;
        L_0x0027:
            r3 = r4.idleSync;	 Catch:{ InterruptedException -> 0x002d }
            r3.wait();	 Catch:{ InterruptedException -> 0x002d }
            goto L_0x0023;
        L_0x002d:
            r0 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x005c }
        L_0x002f:
            return r1;
        L_0x0030:
            monitor-exit(r2);	 Catch:{ all -> 0x005c }
            r2 = r4.handler;
            if (r2 == 0) goto L_0x0060;
        L_0x0035:
            r2 = r4.handler;
            r2 = r2.getLooper();
            if (r2 == 0) goto L_0x0060;
        L_0x003d:
            r2 = r4.handler;
            r2 = r2.getLooper();
            r3 = android.os.Looper.myLooper();
            if (r2 == r3) goto L_0x0060;
        L_0x0049:
            r2 = r4.callbackSync;
            monitor-enter(r2);
        L_0x004c:
            r3 = r4.callbacks;	 Catch:{ all -> 0x0059 }
            if (r3 <= 0) goto L_0x005f;
        L_0x0050:
            r3 = r4.callbackSync;	 Catch:{ InterruptedException -> 0x0056 }
            r3.wait();	 Catch:{ InterruptedException -> 0x0056 }
            goto L_0x004c;
        L_0x0056:
            r0 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x0059 }
            goto L_0x002f;
        L_0x0059:
            r1 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x0059 }
            throw r1;
        L_0x005c:
            r1 = move-exception;
            monitor-exit(r2);	 Catch:{ all -> 0x005c }
            throw r1;
        L_0x005f:
            monitor-exit(r2);	 Catch:{ all -> 0x0059 }
        L_0x0060:
            r1 = 1;
            goto L_0x002f;
            */
            throw new UnsupportedOperationException("Method not decompiled: eu.chainfire.libsuperuser.Shell.Interactive.waitForIdle():boolean");
        }

        public boolean hasHandler() {
            return this.handler != null;
        }
    }

    private interface OnResult {
        public static final int SHELL_DIED = -2;
        public static final int SHELL_EXEC_FAILED = -3;
        public static final int SHELL_RUNNING = 0;
        public static final int SHELL_WRONG_UID = -4;
        public static final int WATCHDOG_EXIT = -1;
    }

    public static class SH {
        public static List<String> run(String command) {
            return Shell.run("sh", new String[]{command}, null, false);
        }

        public static List<String> run(List<String> commands) {
            return Shell.run("sh", (String[]) commands.toArray(new String[commands.size()]), null, false);
        }

        public static List<String> run(String[] commands) {
            return Shell.run("sh", commands, null, false);
        }
    }

    public static class SU {
        private static Boolean isSELinuxEnforcing;
        private static String[] suVersion;

        static {
            isSELinuxEnforcing = null;
            suVersion = new String[]{null, null};
        }

        public static List<String> run(String command) {
            return Shell.run("su", new String[]{command}, null, false);
        }

        public static List<String> run(List<String> commands) {
            return Shell.run("su", (String[]) commands.toArray(new String[commands.size()]), null, false);
        }

        public static List<String> run(String[] commands) {
            return Shell.run("su", commands, null, false);
        }

        public static boolean available() {
            return Shell.parseAvailableResult(run(Shell.availableTestCommands), true);
        }

        public static synchronized String version(boolean internal) {
            String str;
            int idx = 0;
            synchronized (SU.class) {
                if (!internal) {
                    idx = 1;
                }
                if (suVersion[idx] == null) {
                    String version = null;
                    List<String> ret = Shell.run(internal ? "su -V" : "su -v", new String[]{"exit"}, null, false);
                    if (ret != null) {
                        for (String line : ret) {
                            if (internal) {
                                try {
                                    if (Integer.parseInt(line) > 0) {
                                        version = line;
                                        break;
                                    }
                                } catch (NumberFormatException e) {
                                }
                            } else if (!line.trim().equals(BuildConfig.FLAVOR)) {
                                version = line;
                                break;
                            }
                        }
                    }
                    suVersion[idx] = version;
                }
                str = suVersion[idx];
            }
            return str;
        }

        public static boolean isSU(String shell) {
            int pos = shell.indexOf(32);
            if (pos >= 0) {
                shell = shell.substring(0, pos);
            }
            pos = shell.lastIndexOf(47);
            if (pos >= 0) {
                shell = shell.substring(pos + 1);
            }
            return shell.equals("su");
        }

        public static String shell(int uid, String context) {
            String shell = "su";
            if (context != null && isSELinuxEnforcing()) {
                String display = version(false);
                String internal = version(true);
                if (display != null && internal != null && display.endsWith("SUPERSU") && Integer.valueOf(internal).intValue() >= 190) {
                    shell = String.format(Locale.ENGLISH, "%s --context %s", new Object[]{shell, context});
                }
            }
            if (uid <= 0) {
                return shell;
            }
            return String.format(Locale.ENGLISH, "%s %d", new Object[]{shell, Integer.valueOf(uid)});
        }

        public static String shellMountMaster() {
            if (VERSION.SDK_INT >= 17) {
                return "su --mount-master";
            }
            return "su";
        }

        public static synchronized boolean isSELinuxEnforcing() {
            boolean z;
            synchronized (SU.class) {
                if (isSELinuxEnforcing == null) {
                    Boolean enforcing = null;
                    if (VERSION.SDK_INT >= 17) {
                        if (new File("/sys/fs/selinux/enforce").exists()) {
                            InputStream is;
                            try {
                                is = new FileInputStream("/sys/fs/selinux/enforce");
                                if (is.read() == 49) {
                                    z = true;
                                } else {
                                    z = false;
                                }
                                enforcing = Boolean.valueOf(z);
                                is.close();
                            } catch (Exception e) {
                            } catch (Throwable th) {
                                is.close();
                            }
                        }
                        if (enforcing == null) {
                            if (VERSION.SDK_INT >= 19) {
                                z = true;
                            } else {
                                z = false;
                            }
                            enforcing = Boolean.valueOf(z);
                        }
                    }
                    if (enforcing == null) {
                        enforcing = Boolean.valueOf(false);
                    }
                    isSELinuxEnforcing = enforcing;
                }
                z = isSELinuxEnforcing.booleanValue();
            }
            return z;
        }

        public static synchronized void clearCachedResults() {
            synchronized (SU.class) {
                isSELinuxEnforcing = null;
                suVersion[0] = null;
                suVersion[1] = null;
            }
        }
    }

    public interface OnCommandLineListener extends OnResult, OnLineListener {
        void onCommandResult(int i, int i2);
    }

    public interface OnCommandResultListener extends OnResult {
        void onCommandResult(int i, int i2, List<String> list);
    }

    @Deprecated
    public static List<String> run(String shell, String[] commands, boolean wantSTDERR) {
        return run(shell, commands, null, wantSTDERR);
    }

    /* JADX WARNING: inconsistent code. */
    /* Code decompiled incorrectly, please refer to instructions dump. */
    public static java.util.List<java.lang.String> run(java.lang.String r20, java.lang.String[] r21, java.lang.String[] r22, boolean r23) {
        /*
        r15 = java.util.Locale.ENGLISH;
        r0 = r20;
        r12 = r0.toUpperCase(r15);
        r15 = eu.chainfire.libsuperuser.Debug.getSanityChecksEnabledEffective();
        if (r15 == 0) goto L_0x0021;
    L_0x000e:
        r15 = eu.chainfire.libsuperuser.Debug.onMainThread();
        if (r15 == 0) goto L_0x0021;
    L_0x0014:
        r15 = "Application attempted to run a shell command from the main thread";
        eu.chainfire.libsuperuser.Debug.log(r15);
        r15 = new eu.chainfire.libsuperuser.ShellOnMainThreadException;
        r16 = "Application attempted to run a shell command from the main thread";
        r15.<init>(r16);
        throw r15;
    L_0x0021:
        r15 = "[%s%%] START";
        r16 = 1;
        r0 = r16;
        r0 = new java.lang.Object[r0];
        r16 = r0;
        r17 = 0;
        r16[r17] = r12;
        r15 = java.lang.String.format(r15, r16);
        eu.chainfire.libsuperuser.Debug.logCommand(r15);
        r15 = new java.util.ArrayList;
        r15.<init>();
        r11 = java.util.Collections.synchronizedList(r15);
        if (r22 == 0) goto L_0x00c8;
    L_0x0041:
        r9 = new java.util.HashMap;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r9.<init>();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = java.lang.System.getenv();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r9.putAll(r15);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r22;
        r0 = r0.length;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = r0;
        r15 = 0;
    L_0x0053:
        r0 = r16;
        if (r15 >= r0) goto L_0x007d;
    L_0x0057:
        r6 = r22[r15];	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r17 = "=";
        r0 = r17;
        r13 = r6.indexOf(r0);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        if (r13 < 0) goto L_0x007a;
    L_0x0063:
        r17 = 0;
        r0 = r17;
        r17 = r6.substring(r0, r13);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r18 = r13 + 1;
        r0 = r18;
        r18 = r6.substring(r0);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r17;
        r1 = r18;
        r9.put(r0, r1);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
    L_0x007a:
        r15 = r15 + 1;
        goto L_0x0053;
    L_0x007d:
        r8 = 0;
        r15 = r9.size();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = new java.lang.String[r15];	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r22 = r0;
        r15 = r9.entrySet();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = r15.iterator();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
    L_0x008e:
        r15 = r16.hasNext();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        if (r15 == 0) goto L_0x00c8;
    L_0x0094:
        r7 = r16.next();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r7 = (java.util.Map.Entry) r7;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r17 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r17.<init>();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r7.getKey();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = (java.lang.String) r15;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r17;
        r15 = r0.append(r15);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r17 = "=";
        r0 = r17;
        r17 = r15.append(r0);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r7.getValue();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = (java.lang.String) r15;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r17;
        r15 = r0.append(r15);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r22[r8] = r15;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r8 = r8 + 1;
        goto L_0x008e;
    L_0x00c8:
        r15 = java.lang.Runtime.getRuntime();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r20;
        r1 = r22;
        r10 = r15.exec(r0, r1);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r3 = new java.io.DataOutputStream;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r10.getOutputStream();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r3.<init>(r15);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r4 = new eu.chainfire.libsuperuser.StreamGobbler;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15.<init>();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r15.append(r12);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = "-";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r15.toString();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = r10.getInputStream();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r16;
        r4.<init>(r15, r0, r11);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r2 = new eu.chainfire.libsuperuser.StreamGobbler;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15.<init>();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = r15.append(r12);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = "*";
        r15 = r15.append(r16);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = r15.toString();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r17 = r10.getErrorStream();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        if (r23 == 0) goto L_0x016f;
    L_0x0116:
        r15 = r11;
    L_0x0117:
        r0 = r16;
        r1 = r17;
        r2.<init>(r0, r1, r15);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r4.start();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r2.start();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r0 = r21;
        r0 = r0.length;	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r16 = r0;
        r15 = 0;
    L_0x012a:
        r0 = r16;
        if (r15 >= r0) goto L_0x0171;
    L_0x012e:
        r14 = r21[r15];	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r17 = "[%s+] %s";
        r18 = 2;
        r0 = r18;
        r0 = new java.lang.Object[r0];	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r18 = r0;
        r19 = 0;
        r18[r19] = r12;	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r19 = 1;
        r18[r19] = r14;	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r17 = java.lang.String.format(r17, r18);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        eu.chainfire.libsuperuser.Debug.logCommand(r17);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r17 = new java.lang.StringBuilder;	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r17.<init>();	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r0 = r17;
        r17 = r0.append(r14);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r18 = "\n";
        r17 = r17.append(r18);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r17 = r17.toString();	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r18 = "UTF-8";
        r17 = r17.getBytes(r18);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r0 = r17;
        r3.write(r0);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r3.flush();	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r15 = r15 + 1;
        goto L_0x012a;
    L_0x016f:
        r15 = 0;
        goto L_0x0117;
    L_0x0171:
        r15 = "exit\n";
        r16 = "UTF-8";
        r15 = r15.getBytes(r16);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r3.write(r15);	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
        r3.flush();	 Catch:{ IOException -> 0x01bf, InterruptedException -> 0x01d0 }
    L_0x017f:
        r10.waitFor();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r3.close();	 Catch:{ IOException -> 0x01d3, InterruptedException -> 0x01d0 }
    L_0x0185:
        r4.join();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r2.join();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r10.destroy();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r15 = eu.chainfire.libsuperuser.Shell.SU.isSU(r20);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        if (r15 == 0) goto L_0x019f;
    L_0x0194:
        r15 = r10.exitValue();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = 255; // 0xff float:3.57E-43 double:1.26E-321;
        r0 = r16;
        if (r15 != r0) goto L_0x019f;
    L_0x019e:
        r11 = 0;
    L_0x019f:
        r15 = "[%s%%] END";
        r16 = 1;
        r0 = r16;
        r0 = new java.lang.Object[r0];
        r16 = r0;
        r17 = 0;
        r18 = java.util.Locale.ENGLISH;
        r0 = r20;
        r1 = r18;
        r18 = r0.toUpperCase(r1);
        r16[r17] = r18;
        r15 = java.lang.String.format(r15, r16);
        eu.chainfire.libsuperuser.Debug.logCommand(r15);
        return r11;
    L_0x01bf:
        r5 = move-exception;
        r15 = r5.getMessage();	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        r16 = "EPIPE";
        r15 = r15.contains(r16);	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
        if (r15 != 0) goto L_0x017f;
    L_0x01cc:
        throw r5;	 Catch:{ IOException -> 0x01cd, InterruptedException -> 0x01d0 }
    L_0x01cd:
        r5 = move-exception;
        r11 = 0;
        goto L_0x019f;
    L_0x01d0:
        r5 = move-exception;
        r11 = 0;
        goto L_0x019f;
    L_0x01d3:
        r15 = move-exception;
        goto L_0x0185;
        */
        throw new UnsupportedOperationException("Method not decompiled: eu.chainfire.libsuperuser.Shell.run(java.lang.String, java.lang.String[], java.lang.String[], boolean):java.util.List<java.lang.String>");
    }

    static {
        availableTestCommands = new String[]{"echo -BOC-", "id"};
    }

    protected static boolean parseAvailableResult(List<String> ret, boolean checkForRoot) {
        if (ret == null) {
            return false;
        }
        boolean echo_seen = false;
        for (String line : ret) {
            if (line.contains("uid=")) {
                if (!checkForRoot || line.contains("uid=0")) {
                    return true;
                }
                return false;
            } else if (line.contains("-BOC-")) {
                echo_seen = true;
            }
        }
        return echo_seen;
    }
}
