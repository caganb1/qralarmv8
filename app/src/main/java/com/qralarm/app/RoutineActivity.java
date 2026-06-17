package com.qralarm.app;

import android.os.Bundle;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

/**
 * Shown right after a successful QR dismissal, if the routine feature is
 * enabled and a routine has been selected. Minimalist, calming UI: a
 * circular countdown ring plus a Skip/Done button.
 */
public class RoutineActivity extends AppCompatActivity {

    public static final String EXTRA_ROUTINE_NAME     = "extra_routine_name";
    public static final String EXTRA_ROUTINE_SECONDS  = "extra_routine_seconds";

    private CircularCountdownView countdownView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_routine);

        String name     = getIntent().getStringExtra(EXTRA_ROUTINE_NAME);
        int    seconds  = getIntent().getIntExtra(EXTRA_ROUTINE_SECONDS, 120);

        ((TextView) findViewById(R.id.tv_routine_active_name))
                .setText(name != null ? name : getString(R.string.routine_default_name));

        countdownView = findViewById(R.id.countdown_view);
        countdownView.startCountdown(seconds, this::finish);

        findViewById(R.id.btn_routine_done).setOnClickListener(v -> finish());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (countdownView != null) countdownView.cancelCountdown();
    }

    // No back button: gentle wind-down, but allow Done/Skip explicitly.
    @Override public void onBackPressed() {
        finish();
    }
}
