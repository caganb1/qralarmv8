package com.qralarm.app;

import android.os.Bundle;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SwitchCompat;
import androidx.lifecycle.ViewModelProvider;

import java.util.List;

/** Manage post-alarm "wind-down" routines: create, select, master enable/disable. */
public class RoutineListActivity extends AppCompatActivity {

    private RoutineViewModel viewModel;
    private LinearLayout routineContainer;
    private SwitchCompat masterSwitch;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine_list);

        findViewById(R.id.btn_back_routines).setOnClickListener(v -> finish());

        masterSwitch    = findViewById(R.id.switch_routine_master);
        routineContainer = findViewById(R.id.routine_container);
        tvEmpty         = findViewById(R.id.tv_routine_empty);

        masterSwitch.setChecked(AppPrefs.isRoutineFeatureEnabled(this));
        masterSwitch.setOnCheckedChangeListener((b, checked) ->
                AppPrefs.setRoutineFeatureEnabled(this, checked));

        viewModel = new ViewModelProvider(this).get(RoutineViewModel.class);
        viewModel.getAllRoutines().observe(this, this::renderRoutines);

        findViewById(R.id.btn_add_routine).setOnClickListener(v -> showAddRoutineDialog());
    }

    private void renderRoutines(List<Routine> routines) {
        routineContainer.removeAllViews();
        tvEmpty.setVisibility(routines.isEmpty() ? android.view.View.VISIBLE : android.view.View.GONE);

        for (Routine r : routines) {
            android.view.View row = getLayoutInflater()
                    .inflate(R.layout.item_routine, routineContainer, false);

            TextView tvName     = row.findViewById(R.id.tv_routine_name);
            TextView tvDuration = row.findViewById(R.id.tv_routine_duration);
            RadioButton rb      = row.findViewById(R.id.rb_routine_selected);
            android.view.View btnDelete = row.findViewById(R.id.btn_routine_delete);

            tvName.setText(r.name);
            tvDuration.setText(formatDuration(r.durationSeconds));
            rb.setChecked(r.isSelected);

            rb.setOnClickListener(v -> viewModel.selectRoutine(r.id));
            row.setOnClickListener(v -> viewModel.selectRoutine(r.id));
            btnDelete.setOnClickListener(v -> viewModel.delete(r));

            routineContainer.addView(row);
        }
    }

    private String formatDuration(int seconds) {
        if (seconds < 60) return seconds + " sn";
        int m = seconds / 60, s = seconds % 60;
        return s == 0 ? m + " dk" : m + " dk " + s + " sn";
    }

    private void showAddRoutineDialog() {
        android.view.View dialogView = getLayoutInflater().inflate(R.layout.dialog_add_routine, null);
        EditText etName   = dialogView.findViewById(R.id.et_routine_name);
        SeekBar  seekDur  = dialogView.findViewById(R.id.seek_routine_duration);
        TextView tvDurVal = dialogView.findViewById(R.id.tv_routine_duration_value);

        seekDur.setMax(28); // 0..28 -> 30s..15min in 30s steps (offset below)
        seekDur.setProgress(3); // default ~120s
        updateDurLabel(tvDurVal, seekDur.getProgress());
        seekDur.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override public void onProgressChanged(SeekBar s, int p, boolean u) { updateDurLabel(tvDurVal, p); }
            @Override public void onStartTrackingTouch(SeekBar s) {}
            @Override public void onStopTrackingTouch(SeekBar s) {}
        });

        new AlertDialog.Builder(this)
                .setTitle(R.string.add_routine_title)
                .setView(dialogView)
                .setPositiveButton(R.string.save, (d, w) -> {
                    String name = etName.getText().toString().trim();
                    if (name.isEmpty()) {
                        Toast.makeText(this, R.string.routine_name_required, Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int seconds = progressToSeconds(seekDur.getProgress());
                    viewModel.insert(new Routine(name, seconds));
                })
                .setNegativeButton(R.string.clear, null)
                .show();
    }

    private int progressToSeconds(int progress) {
        return 30 + progress * 30; // 30s .. 870s (~14.5 min)
    }

    private void updateDurLabel(TextView tv, int progress) {
        tv.setText(formatDuration(progressToSeconds(progress)));
    }
}
