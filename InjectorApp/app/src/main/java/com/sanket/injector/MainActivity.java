package com.sanket.injector;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.CompoundButton;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import com.sanket.injector.utils.RootUtils;
import com.sanket.injector.engine.InjectionEngine;
import com.sanket.injector.utils.ProcessUtils;

public class MainActivity extends AppCompatActivity {

    private Button btnInject;
    private Button btnStop;
    private Button btnCheckRoot;
    private Button btnCheckProcess;
    private TextView tvStatus;
    private TextView tvProcessInfo;
    private ProgressBar progressBar;
    private Switch switchAutoInject;
    private TextView tvLog;

    private InjectionEngine injectionEngine;
    private Handler mainHandler;
    private boolean isInjecting = false;
    private boolean isInjected = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        initViews();
        initEngine();
        checkRootAccess();
        checkTargetProcess();
    }

    private void initViews() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Sanket FF Injector");
        }

        btnInject = findViewById(R.id.btnInject);
        btnStop = findViewById(R.id.btnStop);
        btnCheckRoot = findViewById(R.id.btnCheckRoot);
        btnCheckProcess = findViewById(R.id.btnCheckProcess);
        tvStatus = findViewById(R.id.tvStatus);
        tvProcessInfo = findViewById(R.id.tvProcessInfo);
        progressBar = findViewById(R.id.progressBar);
        switchAutoInject = findViewById(R.id.switchAutoInject);
        tvLog = findViewById(R.id.tvLog);
        progressBar.setVisibility(View.GONE);
        btnStop.setEnabled(false);

        btnInject.setOnClickListener(v -> startInjection());
        btnStop.setOnClickListener(v -> stopInjection());
        btnCheckRoot.setOnClickListener(v -> checkRootAccess());
        btnCheckProcess.setOnClickListener(v -> checkTargetProcess());

        switchAutoInject.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                startAutoInject();
            } else {
                stopAutoInject();
            }
        });

        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void initEngine() {
        injectionEngine = new InjectionEngine(this);
        injectionEngine.setCallback(new InjectionEngine.InjectionCallback() {
            @Override
            public void onInjectionStart() {
                runOnUiThread(() -> {
                    updateStatus("Injecting...", false);
                    btnInject.setEnabled(false);
                    btnStop.setEnabled(true);
                    progressBar.setVisibility(View.VISIBLE);
                    appendLog("Starting injection...");
                });
            }

            @Override
            public void onInjectionSuccess(String message) {
                runOnUiThread(() -> {
                    updateStatus("Injected Successfully!", true);
                    isInjected = true;
                    isInjecting = false;
                    btnInject.setEnabled(false);
                    btnStop.setEnabled(true);
                    progressBar.setVisibility(View.GONE);
                    appendLog("SUCCESS: " + message);
                });
            }

            @Override
            public void onInjectionFailed(String error) {
                runOnUiThread(() -> {
                    updateStatus("Injection Failed", false);
                    isInjecting = false;
                    btnInject.setEnabled(true);
                    btnStop.setEnabled(false);
                    progressBar.setVisibility(View.GONE);
                    appendLog("ERROR: " + error);
                    Toast.makeText(MainActivity.this, "Injection Failed: " + error, Toast.LENGTH_LONG).show();
                });
            }

            @Override
            public void onLog(String message) {
                runOnUiThread(() -> appendLog(message));
            }
        };
    }

    private void checkRootAccess() {
        appendLog("Checking root access...");
        new Thread(() -> {
            boolean hasRoot = RootUtils.isRootAvailable();
            runOnUiThread(() -> {
                if (RootUtils.isRootAvailable()) {
                    tvStatus.setText("Status: Root Access ✓");
                    tvStatus.setTextColor(getColor(android.R.color.holo_green_dark));
                    appendLog("✓ Root access granted");
                    btnInject.setEnabled(true);
                } else {
                    tvStatus.setText("Status: No Root Access ✗");
                    tvStatus.setTextColor(getColor(android.R.color.holo_red_dark));
                    appendLog("✗ Root access DENIED");
                    btnInject.setEnabled(false);
                    toast("Root access required!");
                }
            });
        }).start();
    }

    private void checkTargetProcess() {
        appendLog("Checking target process...");
        new Thread(() -> {
            int pid = ProcessUtils.findPidByName("com.garena.game.ffi");
            runOnUiThread(() -> {
                if (pid > 0) {
                    tvProcessInfo.setText("Process: com.garena.game.ffi (PID: " + pid + ")");
                    tvProcessInfo.setTextColor(getColor(android.R.color.holo_green_dark));
                    appendLog("✓ Target process found: PID " + pid);
                } else {
                    tvProcessInfo.setText("Process: Not Running");
                    tvProcessInfo.setTextColor(getColor(android.R.color.holo_red_dark));
                    appendLog("✗ Target process not found");
                }
            });
        }).start();
    }

    private void startInjection() {
        if (isInjecting) return;

        if (!RootUtils.isRootAvailable()) {
            toast("Root access required!");
            return;
        }

        isInjecting = true;
        injectionEngine.inject();
    }

    private void stopInjection() {
        if (!isInjected) return;

        injectionEngine.stop();
        isInjected = false;
        isInjecting = false;
        runOnUiThread(() -> {
            updateStatus("Stopped", false);
            btnInject.setEnabled(true);
            btnStop.setEnabled(false);
            appendLog("Injection stopped");
        });
    }

    private void startAutoInject() {
        appendLog("Auto-inject enabled");
        // Start monitoring service
    }

    private void stopAutoInject() {
        appendLog("Auto-inject disabled");
    }

    private void updateStatus(String status, boolean success) {
        tvStatus.setText("Status: " + status);
        tvStatus.setTextColor(success ?
            ContextCompat.getColor(this, android.R.color.holo_green_dark) :
            ContextCompat.getColor(this, android.R.color.holo_red_dark));
    }

    private void appendLog(String message) {
        String timestamp = new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date());
        String logEntry = "[" + new java.text.SimpleDateFormat("HH:mm:ss").format(new java.util.Date()) + "] " + message + "\n";
        tvLog.append(logEntry);
        // Auto-scroll
        tvLog.post(() -> {
            tvLog.scrollTo(0, tvLog.getBottom());
        });
    }

    private void toast(String message) {
        runOnUiThread(() -> Toast.makeText(this, message, Toast.LENGTH_SHORT).show());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (injectionEngine != null) {
            injectionEngine.stop();
        }
    }
}