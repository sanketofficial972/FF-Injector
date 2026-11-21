package com.sanket.injector.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Build;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class RootUtils {

    public static boolean isRootAvailable() {
        return checkRootMethod1() || checkRootMethod2() || checkRootMethod3();
    }

    private static boolean checkRootMethod1() {
        String[] paths = {
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/system/xbin/.ext/.su",
            "/system/sbin/su",
            "/su/bin/su",
            "/system/sd/xbin/su",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/cache/su",
            "/data/su",
            "/system/bin/failsafe/su"
        };

        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }

    private static boolean checkRootMethod2() {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"/system/xbin/which", "su"});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && line.contains("su");
        } catch (IOException e) {
            return false;
        }
    }

    private static boolean checkRootMethod3() {
        try {
            Process process = Runtime.getRuntime().exec("su -c id");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return line != null && line.contains("uid=0");
        } catch (IOException e) {
            return false;
        }
    }

    public static boolean canRunAsRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            int exitCode = process.waitFor();
            return exitCode == 0;
        } catch (Exception e) {
            return false;
        }
    }

    public static String runAsRoot(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isSELinuxEnforcing() {
        try {
            Process process = Runtime.getRuntime().exec("getenforce");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            String line = reader.readLine();
            return "Enforcing".equalsIgnoreCase(line);
        } catch (Exception e) {
            return false;
        }
    }

    public static String getSELinuxMode() {
        try {
            Process process = Runtime.getRuntime().exec("getenforce");
            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static boolean isSELinuxPermissive() {
        String mode = getSELinuxMode();
        return "Permissive".equalsIgnoreCase(mode);
    }

    public static void setSELinuxPermissive() {
        executeCommand("setenforce 0");
    }

    public static void executeCommand(String command) {
        try {
            Runtime.getRuntime().exec(new String[]{"su", "-c", command});
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static String runCommand(String... commands) {
        try {
            ProcessBuilder pb = new ProcessBuilder();
            pb.command("su", "-c", String.join(" && ", commands));
            pb.redirectErrorStream(true);
            Process process = pb.start();

            BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
            StringBuilder output = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                output.append(line).append("\n");
            }
            process.waitFor();
            return output.toString();
        } catch (Exception e) {
            return "";
        }
    }

    public static boolean isMagiskInstalled() {
        return new File("/sbin/magisk").exists() ||
               new File("/system/bin/magisk").exists() ||
               new File("/system/xbin/magisk").exists() ||
               new File("/sbin/magisk").exists();
    }

    public static String getMagiskVersion() {
        try {
            Process process = Runtime.getRuntime().exec("magisk -v");
            java.io.BufferedReader reader = new java.io.BufferedReader(
                new java.io.InputStreamReader(process.getInputStream()));
            return reader.readLine();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public static boolean isZygiskEnabled() {
        File zygiskFile = new File("/data/adb/zygisk/enable");
        return zygiskFile.exists();
    }
}