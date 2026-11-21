package com.sanket.injector.engine;

import android.content.Context;
import android.util.Log;

import com.sanket.injector.utils.ProcessUtils;
import com.sanket.injector.utils.RootUtils;

public class InjectionEngine {

    private static final String TAG = "InjectionEngine";
    private static final String TARGET_PACKAGE = "com.garena.game.ffi";
    private static final String TARGET_LIB = "libil2cpp.so";

    private Context context;
    private InjectionCallback callback;
    private boolean isRunning = false;
    private Thread injectionThread;
    private ProcessInjectionThread injectionThreadObj;

    public interface InjectionCallback {
        void onInjectionStart();
        void onInjectionSuccess(String message);
        void onInjectionFailed(String error);
        void onLog(String message);
    }

    public InjectionEngine(Context context) {
        this.context = context;
    }

    public void setCallback(InjectionCallback callback) {
        this.callback = callback;
    }

    public void inject() {
        if (injectionThread != null && injectionThread.isAlive()) {
            log("Injection already in progress");
            return;
        }

        injectionThreadObj = new ProcessInjectionThread();
        injectionThread = new Thread(injectionThreadObj);
        injectionThread.start();
    }

    public void stop() {
        if (injectionThreadObj != null) {
            injectionThreadObj.stopInjection();
        }
        if (injectionThread != null) {
            injectionThread.interrupt();
        }
    }

    private void log(String message) {
        logCallback("LOG: " + message);
    }

    private void logCallback(String message) {
        if (callback != null) {
            callback.onLog(message);
        }
    }

    private void successCallback(String message) {
        if (callback != null) {
            callback.onInjectionSuccess(message);
        }
    }

    private void errorCallback(String error) {
        if (callback != null) {
            callback.onInjectionFailed(message);
        }
    }

    private void startCallback() {
        if (callback != null) {
            callback.onInjectionStart();
        }
    }

    private class ProcessInjectionThread implements Runnable {
        private volatile boolean running = true;
        private int targetPid = -1;

        @Override
        public void run() {
            logCallback("Starting injection process...");

            try {
                // Step 1: Verify root
                log("Checking root access...");
                if (!RootUtils.isRootAvailable()) {
                    error("Root access required!");
                    return;
                }
                log("Root access confirmed");

                // Step 2: Find target process
                log("Finding target process: com.garena.game.ffi");
                int pid = findTargetProcess();
                if (pid <= 0) {
                    error("Target process not found: com.garena.game.ffi");
                    return;
                }
                log("Found target PID: " + targetPid);

                // Step 2: Check if process is accessible
                if (!isProcessAccessible(targetPid)) {
                    error("Cannot access target process. Check SELinux status.");
                    return;
                }

                // Step 3: Perform injection
                log("Starting injection...");
                boolean success = performInjection(targetPid);

                if (success) {
                    callback.onInjectionSuccess("Injection successful! Mod menu active.");
                } else {
                    error("Injection failed");
                }

            } catch (Exception e) {
                logError("Injection error: " + e.getMessage());
                error("Injection failed: " + e.getMessage());
            }
        }

        private boolean performInjection(int pid) {
            try {
                log("Injecting into PID: " + pid);

                // Method 1: Frida Gadget Injection (recommended)
                if (injectFridaGadget()) {
                    log("Frida Gadget injection successful");
                    return true;
                }

                // Method 2: Direct library injection
                if (injectLibrary()) {
                    log("Library injection successful");
                    return true;
                }

                // Method 3: ptrace injection
                if (injectViaPtrace()) {
                    log("Ptrace injection successful");
                    return true;
                }

                logError("All injection methods failed");
                return false;

            } catch (Exception e) {
                logError("Injection error: " + e.getMessage());
                return false;
            }
        }

        private boolean injectFridaGadget() {
            try {
                log("Attempting Frida Gadget injection...");

                // Check if frida-gadget is available
                String checkCmd = "ls /data/local/tmp/frida-gadget.so 2>/dev/null || ls /data/local/tmp/frida-server 2>/dev/null";
                String result = RootUtils.runCommand("ls /data/local/tmp/frida-gadget.so 2>/dev/null || ls /data/local/tmp/frida-server 2>/dev/null || echo 'NOT_FOUND'");

                if (result.contains("NOT_FOUND")) {
                    log("Frida gadget not found. Attempting to download...");
                    if (!downloadFridaGadget()) {
                        logError("Failed to download Frida gadget");
                        return false;
                    }
                }

                // Inject frida-gadget using ptrace
                String injectCmd = String.format(
                    "su -c 'cd /data/local/tmp && ./frida-gadget.so -f com.garena.game.ffi --listen=0.0.0.0:27042 --runtime=v8 &'",
                    ""
                );

                String result = RootUtils.runCommand("cd /data/local/tmp && chmod +x frida-gadget.so && ./frida-gadget.so -f com.garena.game.ffi --listen=0.0.0.0:27042 --runtime=v8 &");

                if (result.contains("Listening") || result.contains("attached") || result.contains("resumed")) {
                    log("Frida injection successful");
                    return true;
                }

                // Alternative: Use frida-server
                String serverResult = RootUtils.runCommand("frida-server -D &");
                if (!serverResult.contains("error") && !serverResult.isEmpty()) {
                    log("Frida server started");
                    return true;
                }

                return false;

            } catch (Exception e) {
                logError("Frida injection error: " + e.getMessage());
                return false;
            }
        }

        private boolean injectLibrary() {
            // Inject custom .so library
            try {
                String libPath = "/data/local/tmp/libunknownmodz.so";
                File libFile = new File(libPath);

                if (!new File(libPath).exists()) {
                    logError("Library not found: " + libPath);
                    return false;
                }

                // Use ptrace to inject
                String cmd = String.format(
                    "su -c 'ptrace_attach %d && dlopen(\"%s\", RTLD_NOW) && ptrace_detach'",
                    getTargetPid(), "/data/local/tmp/libunknownmodz.so"
                );

                String result = RootUtils.runCommand("su -c '" + String.format(
                    "gdb -p %d -ex 'call (void*)dlopen(\\\"/data/local/tmp/libunknownmodz.so\\\", 2)' -ex detach -ex quit",
                    getTargetPid()) + "'");

                return result != null && !result.contains("error");

            } catch (Exception e) {
                logError("Library injection error: " + e.getMessage());
                return false;
            }
        }

        private boolean injectViaPtrace() {
            try {
                // Use gdb to inject
                String cmd = String.format(
                    "su -c 'gdb -p %d -ex \"call (void*)dlopen(\\\"/data/local/tmp/libunknownmodz.so\\\", 2)\" -ex detach -ex quit'",
                    getTargetPid()
                );

                String result = RootUtils.runCommand("su -c '" + String.format(
                    "gdb -p %d -ex 'call (void*)dlopen(\\\"/data/local/tmp/libunknownmodz.so\\\", 2)' -ex detach -ex quit",
                    getTargetPid()) + "'");

                return result != null && !result.contains("error") && !result.contains("Error");
            } catch (Exception e) {
                logError("Ptrace injection error: " + e.getMessage());
                return false;
            }
        }

        private boolean downloadFridaGadget() {
            try {
                String arch = System.getProperty("os.arch").contains("64") ? "arm64" : "arm";
                String url = "https://github.com/frida/frida/releases/download/16.1.7/frida-gadget-16.1.7-android-" + arch + ".so.xz";

                String cmd = String.format(
                    "su -c 'cd /data/local/tmp && wget -q %s -O frida-gadget.so.xz && xz -d frida-gadget.so.xz && chmod +x frida-gadget.so'",
                    "https://github.com/frida/frida/releases/download/16.1.7/frida-gadget-16.1.7-android-arm64.so.xz"
                );

                String result = RootUtils.runCommand("su -c 'cd /data/local/tmp && wget -q https://github.com/frida/frida/releases/download/16.1.7/frida-gadget-16.1.7-android-arm64.so.xz -O frida-gadget.so.xz && xz -d frida-gadget.so.xz && chmod +x frida-gadget.so'");

                File gadget = new File("/data/local/tmp/frida-gadget.so");
                return gadget.exists() && gadget.length() > 0;
            } catch (Exception e) {
                logError("Download failed: " + e.getMessage());
                return false;
            }
        }

        private void stopInjection() {
            running = false;
        }
    }

    // Native injection via JNI
    private native boolean nativeInject(int pid, String libPath);
    private native boolean nativeHook(int pid, String methodName, long hookAddr);
    private native boolean nativeUnhook(int pid, String methodName);

    // JNI Methods would be implemented in injector_jni.cpp
    // static {
    //     System.loadLibrary("injector");
    // }

    // Helper methods
    private int getTargetPid() {
        return ProcessUtils.findPidByName("com.garena.game.ffi");
    }

    private boolean isProcessAlive(int pid) {
        return new File("/proc/" + pid).exists();
    }

    private void log(String message) {
        if (callback != null) callback.onLog(message);
    }

    private void logError(String message) {
        if (callback != null) callback.onLog("ERROR: " + message);
    }

    private void error(String error) {
        if (callback != null) callback.onInjectionFailed(message);
    }
}