package com.sanket.injector.utils;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Process;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

public class ProcessUtils {

    public static int findPidByName(String packageName) {
        try {
            // Method 1: Using pidof
            String output = executeCommand("pidof " + packageName);
            if (!output.isEmpty()) {
                String[] pids = output.trim().split("\\s+");
                if (pids.length > 0) {
                    try {
                        return Integer.parseInt(pids[0].trim());
                    } catch (NumberFormatException e) {
                        // Try next method
                    }
                }
            }

            // Method 2: Using ps
            String psOutput = executeCommand("ps -A");
            if (!psOutput.isEmpty()) {
                String[] lines = psOutput.split("\n");
                for (String line : psOutput.split("\n")) {
                    if (line.contains(packageName)) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            try {
                                return Integer.parseInt(parts[1].trim());
                            } catch (NumberFormatException e) {
                                // Try next
                            }
                        }
                    }
                }
            }

            // Method 3: Using /proc
            File procDir = new File("/proc");
            File[] pidDirs = procDir.listFiles(File::isDirectory);
            if (pidDirs != null) {
                for (File pidDir : pidDirs) {
                    try {
                        int pid = Integer.parseInt(pidDir.getName());
                        File cmdlineFile = new File(pidDir, "cmdline");
                        if (cmdlineFile.exists()) {
                            String cmdline = readFile(cmdlineFile);
                            if (cmdline.contains(packageName)) {
                                return Integer.parseInt(pidDir.getName());
                            }
                        }
                    } catch (NumberFormatException ignored) {
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<Integer> findAllPidsByName(String packageName) {
        List<Integer> pids = new ArrayList<>();
        try {
            String output = executeCommand("ps -A");
            if (!output.isEmpty()) {
                String[] lines = output.split("\n");
                for (String line : output.split("\n")) {
                    if (line.contains(packageName)) {
                        String[] parts = line.trim().split("\\s+");
                        if (parts.length >= 2) {
                            try {
                                int pid = Integer.parseInt(parts[1].trim());
                                pids.add(pid);
                            } catch (NumberFormatException ignored) {
                            }
                        }
                    }
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return pids;
    }

    public static boolean isProcessRunning(int pid) {
        File procDir = new File("/proc/" + pid);
        return procDir.exists() && procDir.isDirectory();
    }

    public static String getProcessName(int pid) {
        File cmdlineFile = new File("/proc/" + pid + "/cmdline");
        if (cmdlineFile.exists()) {
            return readFile(new File("/proc/" + pid + "/cmdline"));
        }
        return "";
    }

    public static String getProcessCmdline(int pid) {
        File cmdlineFile = new File("/proc/" + pid + "/cmdline");
        return readFile(cmdlineFile);
    }

    public static long getProcessStartTime(int pid) {
        File statFile = new File("/proc/" + pid + "/stat");
        String content = readFile(statFile);
        if (!content.isEmpty()) {
            String[] parts = content.split(" ");
            if (parts.length > 21) {
                try {
                    return Long.parseLong(parts[21]);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return 0;
    }

    public static boolean isProcessAlive(int pid) {
        File procDir = new File("/proc/" + pid);
        return procDir.exists() && procDir.isDirectory();
    }

    public static String getProcessCmdlineFromStat(int pid) {
        File statFile = new File("/proc/" + pid + "/stat");
        String content = readFile(statFile);
        if (!content.isEmpty()) {
            int start = content.indexOf('(');
            int end = content.indexOf(')');
            if (start >= 0 && end > start) {
                return content.substring(start + 1, end);
            }
        }
        return "";
    }

    private static String executeCommand(String command) {
        try {
            Process process = Runtime.getRuntime().exec(new String[]{"su", "-c", command});
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream()));
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

    private static String readFile(File file) {
        try {
            BufferedReader reader = new BufferedReader(
                new InputStreamReader(new java.io.FileInputStream(file)));
            StringBuilder content = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                content.append(line);
            }
            reader.close();
            return content.toString();
        } catch (IOException e) {
            return "";
        }
    }

    public static String getProcessCmdline(int pid) {
        return readFile(new File("/proc/" + pid + "/cmdline"));
    }
}