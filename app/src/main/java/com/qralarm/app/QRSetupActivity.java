package com.qralarm.app;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.google.zxing.integration.android.IntentIntegrator;
import com.google.zxing.integration.android.IntentResult;

import java.util.List;

public class QRSetupActivity extends AppCompatActivity {

    public static final int REQUEST_CODE = 7001;
    public static final String EXTRA_CURRENT_VALUE = "extra_current_value";
    public static final String EXTRA_CURRENT_LABEL = "extra_current_label";
    public static final String RESULT_QR_VALUE = "result_qr_value";
    public static final String RESULT_QR_LABEL = "result_qr_label";

    private EditText etCode, etLabel;
    private TextView tvCurrent;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_qr_setup);

        findViewById(R.id.btn_back_qr).setOnClickListener(v -> finish());

        etCode    = findViewById(R.id.et_qr_code);
        etLabel   = findViewById(R.id.et_qr_label);
        tvCurrent = findViewById(R.id.tv_current_qr);

        String currentValue = getIntent().getStringExtra(EXTRA_CURRENT_VALUE);
        String currentLabel = getIntent().getStringExtra(EXTRA_CURRENT_LABEL);
        if (currentValue != null && !currentValue.isEmpty()) {
            etCode.setText(currentValue);
            etLabel.setText(currentLabel != null ? currentLabel : "");
            tvCurrent.setText(getString(R.string.current_qr_value, currentValue));
        }

        findViewById(R.id.btn_scan_to_set).setOnClickListener(v -> startScan());
        findViewById(R.id.btn_pick_saved_qr).setOnClickListener(v -> showSavedQrPicker());
        findViewById(R.id.btn_save_qr).setOnClickListener(v -> saveAndReturn());
        findViewById(R.id.btn_clear_qr).setOnClickListener(v -> {
            etCode.setText("");
            etLabel.setText("");
            tvCurrent.setText(R.string.no_qr_configured_setup);
        });
    }

    private void startScan() {
        IntentIntegrator integrator = new IntentIntegrator(this);
        integrator.setDesiredBarcodeFormats(IntentIntegrator.ALL_CODE_TYPES);
        integrator.setPrompt(getString(R.string.scan_prompt_setup));
        integrator.setBeepEnabled(true);
        integrator.setOrientationLocked(true);
        integrator.initiateScan();
    }

    /** Lets the user choose a previously scanned/saved code instead of scanning again. */
    private void showSavedQrPicker() {
        new SavedQRRepository(this).getAllSync(savedList -> runOnUiThread(() -> {
            if (savedList.isEmpty()) {
                Toast.makeText(this, R.string.no_saved_qrs_yet, Toast.LENGTH_SHORT).show();
                return;
            }
            String[] labels = new String[savedList.size()];
            for (int i = 0; i < savedList.size(); i++) {
                SavedQR qr = savedList.get(i);
                labels[i] = (qr.label != null && !qr.label.isEmpty()) ? qr.label : qr.value;
            }
            new AlertDialog.Builder(this)
                    .setTitle(R.string.saved_qrs_title)
                    .setItems(labels, (d, which) -> {
                        SavedQR chosen = savedList.get(which);
                        etCode.setText(chosen.value);
                        etLabel.setText(chosen.label != null ? chosen.label : "");
                        tvCurrent.setText(getString(R.string.current_qr_value, chosen.value));
                    })
                    .show();
        }));
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        IntentResult result = IntentIntegrator.parseActivityResult(requestCode, resultCode, data);
        if (result != null) {
            if (result.getContents() != null) {
                String scanned = result.getContents();
                etCode.setText(scanned);
                tvCurrent.setText(getString(R.string.scanned_value, scanned));
                Toast.makeText(this, R.string.scan_success, Toast.LENGTH_SHORT).show();

                // Auto-save newly scanned codes to the Saved QR library (skips duplicates).
                String label = etLabel.getText().toString().trim();
                new SavedQRRepository(this).insertIfNew(label, scanned);
            }
        } else {
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private void saveAndReturn() {
        String code  = etCode.getText().toString().trim();
        String label = etLabel.getText().toString().trim();

        if (TextUtils.isEmpty(code)) {
            Toast.makeText(this, R.string.qr_code_empty_warning, Toast.LENGTH_SHORT).show();
            // Allow saving empty to clear the QR requirement.
        } else {
            // Persist to Saved QR library too, in case it was typed manually.
            new SavedQRRepository(this).insertIfNew(label, code);
        }

        Intent result = new Intent();
        result.putExtra(RESULT_QR_VALUE, code);
        result.putExtra(RESULT_QR_LABEL, label);
        setResult(RESULT_OK, result);
        finish();
    }
}
