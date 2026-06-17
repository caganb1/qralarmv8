package com.qralarm.app;

import android.app.Dialog;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.NumberPicker;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

/**
 * iOS-style drum-roller time picker presented as a BottomSheetDialog-like
 * full-bleed dialog. Uses two NumberPickers (hours 0–23, minutes 0–59),
 * styled to look like seamless scroll wheels (divider hidden, large text,
 * soft rounded container).
 */
public class WheelTimePicker extends DialogFragment {

    public interface OnTimeSetListener {
        void onTimeSet(int hour, int minute);
    }

    private static final String ARG_HOUR   = "arg_hour";
    private static final String ARG_MINUTE = "arg_minute";

    private OnTimeSetListener listener;
    private int initialHour, initialMinute;

    public static WheelTimePicker newInstance(int hour, int minute) {
        WheelTimePicker f = new WheelTimePicker();
        Bundle b = new Bundle();
        b.putInt(ARG_HOUR, hour);
        b.putInt(ARG_MINUTE, minute);
        f.setArguments(b);
        return f;
    }

    public void setOnTimeSetListener(OnTimeSetListener l) {
        this.listener = l;
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Bundle args = getArguments();
        initialHour   = args != null ? args.getInt(ARG_HOUR, 7)  : 7;
        initialMinute = args != null ? args.getInt(ARG_MINUTE, 0) : 0;

        View view = LayoutInflater.from(getContext())
                .inflate(R.layout.dialog_wheel_time_picker, null);

        NumberPicker hourPicker   = view.findViewById(R.id.picker_hour);
        NumberPicker minutePicker = view.findViewById(R.id.picker_minute);

        configurePicker(hourPicker,   0, 23, initialHour);
        configurePicker(minutePicker, 0, 59, initialMinute);

        TextView btnCancel = view.findViewById(R.id.btn_picker_cancel);
        TextView btnSet     = view.findViewById(R.id.btn_picker_set);

        btnCancel.setOnClickListener(v -> dismiss());
        btnSet.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeSet(hourPicker.getValue(), minutePicker.getValue());
            }
            dismiss();
        });

        Dialog dialog = new Dialog(requireContext(), R.style.Theme_QRAlarm_WheelDialog);
        dialog.setContentView(view);
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
        }
        return dialog;
    }

    private void configurePicker(NumberPicker picker, int min, int max, int value) {
        picker.setMinValue(min);
        picker.setMaxValue(max);
        picker.setValue(value);
        picker.setFormatter(v -> String.format("%02d", v));
        picker.setWrapSelectorWheel(true);
        // Hide the default divider lines for a seamless drum-roller look
        try {
            java.lang.reflect.Field f = NumberPicker.class.getDeclaredField("mSelectionDivider");
            f.setAccessible(true);
            f.set(picker, null);
        } catch (Exception ignored) {
            // Falls back to default dividers on OEMs that obfuscate this field — harmless.
        }
    }
}
