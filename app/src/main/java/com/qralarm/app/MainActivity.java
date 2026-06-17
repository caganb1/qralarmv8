package com.qralarm.app;

import android.Manifest;
import android.app.AlarmManager;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.view.View;
import android.widget.TextView;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.android.material.snackbar.Snackbar;

import java.util.Calendar;

public class MainActivity extends AppCompatActivity implements AlarmAdapter.AlarmActionListener {

    private AlarmViewModel viewModel;
    private AlarmAdapter adapter;
    private TextView tvEmpty;

    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(new ActivityResultContracts.RequestMultiplePermissions(), result -> {
                // Check if exact alarm permission needed
                checkExactAlarmPermission();
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);


        tvEmpty = findViewById(R.id.tv_empty);
        RecyclerView recyclerView = findViewById(R.id.recycler_alarms);
        FloatingActionButton fab = findViewById(R.id.fab_add_alarm);

        viewModel = new ViewModelProvider(this).get(AlarmViewModel.class);

        adapter = new AlarmAdapter(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setAdapter(adapter);

        viewModel.getAllAlarms().observe(this, alarms -> {
            adapter.setAlarms(alarms);
            tvEmpty.setVisibility(alarms.isEmpty() ? View.VISIBLE : View.GONE);
        });

        fab.setOnClickListener(v -> openAddAlarm(-1));

        View btnRoutines = findViewById(R.id.btn_open_routines);
        if (btnRoutines != null) {
            btnRoutines.setOnClickListener(v ->
                    startActivity(new Intent(this, RoutineListActivity.class)));
        }

        View btnSavedQrs = findViewById(R.id.btn_open_saved_qrs);
        if (btnSavedQrs != null) {
            btnSavedQrs.setOnClickListener(v ->
                    startActivity(new Intent(this, SavedQRListActivity.class)));
        }

        requestPermissions();
        loadDashboardStats();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadDashboardStats(); // refresh average + chart after returning from an alarm
    }

    /** Loads "Average Wake-up Time" and the 7-day reaction chart. */
    private void loadDashboardStats() {
        TextView tvAvg = findViewById(R.id.tv_average_wake_time);
        WeeklyTrendChartView chart = findViewById(R.id.chart_weekly_trend);
        if (tvAvg == null || chart == null) return;

        Calendar weekStart = Calendar.getInstance();
        weekStart.set(Calendar.HOUR_OF_DAY, 0);
        weekStart.set(Calendar.MINUTE, 0);
        weekStart.set(Calendar.SECOND, 0);
        weekStart.set(Calendar.MILLISECOND, 0);
        // Roll back to Monday
        int dow = weekStart.get(Calendar.DAY_OF_WEEK); // 1=Sun..7=Sat
        int daysSinceMonday = (dow == Calendar.SUNDAY) ? 6 : dow - Calendar.MONDAY;
        weekStart.add(Calendar.DAY_OF_YEAR, -daysSinceMonday);
        long weekStartMillis = weekStart.getTimeInMillis();

        WakeStatRepository statsRepo = new WakeStatRepository(this);

        statsRepo.getAverageSince(weekStartMillis, avg -> runOnUiThread(() -> {
            if (avg == null) {
                tvAvg.setText(R.string.no_data_yet);
            } else {
                tvAvg.setText(getString(R.string.average_wake_time_value, formatAvg(avg)));
            }
        }));

        statsRepo.getStatsSince(weekStartMillis, stats -> runOnUiThread(() -> {
            Float[] dailyAverages = new Float[7]; // Mon..Sun
            int[]   counts        = new int[7];
            float[] sums          = new float[7];

            Calendar cal = Calendar.getInstance();
            for (WakeStat s : stats) {
                cal.setTimeInMillis(s.ringTimestamp);
                int d = cal.get(Calendar.DAY_OF_WEEK);
                int idx = (d == Calendar.SUNDAY) ? 6 : d - Calendar.MONDAY;
                sums[idx]   += s.reactionSeconds;
                counts[idx] += 1;
            }
            for (int i = 0; i < 7; i++) {
                dailyAverages[i] = (counts[i] == 0) ? null : sums[i] / counts[i];
            }
            chart.setData(dailyAverages);
        }));
    }

    private String formatAvg(double seconds) {
        if (seconds < 60) return Math.round(seconds) + "s";
        return String.format(java.util.Locale.getDefault(), "%.1f dk", seconds / 60.0);
    }

    private void requestPermissions() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.POST_NOTIFICATIONS});
                return;
            }
        }
        checkExactAlarmPermission();
    }

    private void checkExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            AlarmManager am = (AlarmManager) getSystemService(ALARM_SERVICE);
            if (am != null && !am.canScheduleExactAlarms()) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.exact_alarm_permission_needed,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.grant, v -> {
                    Intent intent = new Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                    intent.setData(Uri.parse("package:" + getPackageName()));
                    startActivity(intent);
                }).show();
                return;
            }
        }
        checkBatteryOptimization();
    }

    /**
     * Samsung One UI (ve diğer üreticilerin) agresif pil optimizasyonu,
     * arka planda zamanlanan alarmların gecikmesine veya hiç çalmamasına
     * sebep olabilir. Bu yüzden uygulamayı pil optimizasyonu listesinden
     * hariç tutmak için kullanıcıdan izin istiyoruz.
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager pm = (PowerManager) getSystemService(POWER_SERVICE);
            if (pm != null && !pm.isIgnoringBatteryOptimizations(getPackageName())) {
                Snackbar.make(
                        findViewById(android.R.id.content),
                        R.string.battery_optimization_needed,
                        Snackbar.LENGTH_LONG
                ).setAction(R.string.grant, v -> {
                    try {
                        Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                        intent.setData(Uri.parse("package:" + getPackageName()));
                        startActivity(intent);
                    } catch (Exception e) {
                        Intent intent = new Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS);
                        startActivity(intent);
                    }
                }).show();
            }
        }
    }

    private void openAddAlarm(int alarmId) {
        Intent intent = new Intent(this, AddAlarmActivity.class);
        if (alarmId != -1) {
            intent.putExtra(AddAlarmActivity.EXTRA_ALARM_ID, alarmId);
        }
        startActivity(intent);
    }

    // AlarmActionListener callbacks
    @Override
    public void onAlarmToggle(Alarm alarm, boolean enabled) {
        alarm.isEnabled = enabled;
        viewModel.update(alarm);
        if (enabled) {
            AlarmScheduler.schedule(this, alarm);
        } else {
            AlarmScheduler.cancel(this, alarm);
        }
    }

    @Override
    public void onAlarmEdit(Alarm alarm) {
        openAddAlarm(alarm.id);
    }

    @Override
    public void onAlarmDelete(Alarm alarm) {
        AlarmScheduler.cancel(this, alarm);
        viewModel.delete(alarm);
    }
}
