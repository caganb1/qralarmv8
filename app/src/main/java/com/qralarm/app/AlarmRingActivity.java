package com.qralarm.app;

import android.app.KeyguardManager;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * True lock-screen takeover: this activity shows directly over the keyguard
 * (not as a notification the user must tap) and forces maximum screen
 * brightness regardless of the device's current brightness setting.
 */
public class AlarmRingActivity extends AppCompatActivity {

    public static final String EXTRA_ALARM_ID = "extra_alarm_id";

    private int alarmId = -1;
    private Alarm alarm;
    private final ExecutorService db = Executors.newSingleThreadExecutor();

    // Reaction-time tracking: recorded the instant this screen first appears.
    private long ringStartTimeMillis = -1L;

    // ── Lifecycle ─────────────────────────────────────────────────────────────

    @Override
    protected void onCreate(Bundle saved) {
        super.onCreate(saved);

        ringStartTimeMillis = System.currentTimeMillis();

        applyLockScreenBypass();
        applyMaxBrightness();   // MUST run before setContentView()

        setContentView(R.layout.activity_alarm_ring);

        alarmId = getIntent().getIntExtra(EXTRA_ALARM_ID, -1);
        if (alarmId == -1) { finish(); return; }

        loadAndBind();
        findViewById(R.id.btn_scan_qr).setOnClickListener(v -> startScan());
    }

    /**
     * Forces this activity to render directly over the lock screen and
     * actively requests the keyguard to dismiss itself (where the device
     * has no secure lock set — e.g. no PIN/pattern). On devices with a
     * secure lock, the system still shows the ring UI above the keyguard
     * via setShowWhenLocked(true), matching standard alarm-clock behaviour.
     */
    private void applyLockScreenBypass() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);

            KeyguardManager km = (KeyguardManager) getSystemService(KEYGUARD_SERVICE);
            if (km != null) {
                km.requestDismissKeyguard(this, null);
            }
        } else {
            getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED |
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON |
                WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
        }
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    /** Forces absolute maximum screen brightness, independent of the system setting. */
    private void applyMaxBrightness() {
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = 1.0f;
        getWindow().setAttributes(lp);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        int id = intent.getIntExtra(EXTRA_ALARM_ID, -1);
        if (id != -1) {
            alarmId = id;
            ringStartTimeMillis = System.currentTimeMillis();
            loadAndBind();
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Restore system brightness to the user's normal setting.
        WindowManager.LayoutParams lp = getWindow().getAttributes();
        lp.screenBrightness = WindowManager.LayoutParams.BRIGHTNESS_OVERRIDE_NONE;
        getWindow().setAttributes(lp);
        db.shutdown();
    }

    // Block hardware back so alarm can only be dismissed via QR.
    @Override public void onBackPressed() { /* intentionally empty */ }
    @Override public boolean onKeyDown(int k, KeyEvent e) {
        return k == KeyEvent.KEYCODE_BACK || super.onKeyDown(k, e);
    }

    // ── UI ────────────────────────────────────────────────────────────────────

    private void loadAndBind() {
        db.execute(() -> {
            alarm = AlarmDatabase.getInstance(this).alarmDao().getAlarmById(alarmId);
            runOnUiThread(() -> {
                if (alarm == null) { finish(); return; }
                ((TextView) findViewById(R.id.tv_ring_time)).setText(alarm.getTimeString());
                String lbl = alarm.label != null ? alarm.label : "";
                ((TextView) findViewById(R.id.tv_ring_label)).setText(lbl);

                TextView tvInstr = findViewById(R.id.tv_ring_instruction);
                TextView tvQr    = findViewById(R.id.tv_ring_qr_label);
                View     btnFree = findViewById(R.id.btn_dismiss_no_qr);

                if (alarm.hasQrCode()) {
                    tvInstr.setText(R.string.scan_to_dismiss);
                    String qLabel = alarm.qrCodeLabel != null
                            ? alarm.qrCodeLabel : getString(R.string.your_qr_code);
                    tvQr.setText(getString(R.string.required_code, qLabel));
                    tvQr.setVisibility(View.VISIBLE);
                    if (btnFree != null) btnFree.setVisibility(View.GONE);
                } else {
                    tvInstr.setText(R.string.no_qr_configured);
                    tvQr.setVisibility(View.GONE);
                    if (btnFree != null) {
                        btnFree.setVisibility(View.VISIBLE);
                        btnFree.setOnClickListener(v -> dismiss());
                    }
                }
            });
        });
    }

    // ── QR scan ───────────────────────────────────────────────────────────────

    private void startScan() {
        IntentIntegrator ii = new IntentIntegrator(this);
        ii.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        ii.setPrompt(getString(R.string.scan_prompt));
        ii.setBeepEnabled(false);
        ii.setOrientationLocked(true);
        ii.initiateScan();
    }

    @Override
    protected void onActivityResult(int req, int res, Intent data) {
        IntentResult r = IntentIntegrator.parseActivityResult(req, res, data);
        if (r != null) {
            if (r.getContents() == null) {
                Toast.makeText(this, R.string.scan_cancelled, Toast.LENGTH_SHORT).show();
            } else {
                validate(r.getContents());
            }
        } else {
            super.onActivityResult(req, res, data);
        }
    }

    private void validate(String scanned) {
        if (alarm == null) return;
        if (!alarm.hasQrCode() || scanned.trim().equals(alarm.qrCodeValue.trim())) {
            dismiss();
        } else {
            Toast.makeText(this, R.string.wrong_code, Toast.LENGTH_LONG).show();
        }
    }

    private void dismiss() {
        AlarmService.stop(this);

        long nowMillis = System.currentTimeMillis();
        int reactionSeconds = (int) Math.max(0, (nowMillis - ringStartTimeMillis) / 1000L);

        if (alarm != null) {
            db.execute(() -> {
                // Record wake-up stat (gamification / weekly trend chart).
                new WakeStatRepository(this).insert(new WakeStat(ringStartTimeMillis, reactionSeconds));

                if (!alarm.repeats()) {
                    AlarmDatabase.getInstance(this).alarmDao().setEnabled(alarmId, false);
                } else {
                    AlarmScheduler.schedule(this, alarm);
                }
            });
        }

        // Optionally launch the post-alarm wind-down routine.
        if (AppPrefs.isRoutineFeatureEnabled(this)) {
            new RoutineRepository(this).getSelectedRoutine(routine -> runOnUiThread(() -> {
                if (routine != null) {
                    Intent i = new Intent(this, RoutineActivity.class);
                    i.putExtra(RoutineActivity.EXTRA_ROUTINE_NAME, routine.name);
                    i.putExtra(RoutineActivity.EXTRA_ROUTINE_SECONDS, routine.durationSeconds);
                    startActivity(i);
                }
                finish();
            }));
        } else {
            finish();
        }
    }
}
