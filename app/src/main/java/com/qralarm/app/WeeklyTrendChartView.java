package com.qralarm.app;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;

import java.util.Calendar;
import java.util.Locale;

/**
 * Minimalist 7-day bar chart drawn entirely with Canvas (no 3rd-party charting
 * library). Each bar represents the average reaction time (seconds) for that
 * day; colour shifts from mint (fast) → amber (mid) → coral (slow) based on
 * value relative to the week's own range, for a soft, pastel aesthetic.
 */
public class WeeklyTrendChartView extends View {

    private static final String[] DAY_LABELS_TR = {"P", "S", "Ç", "P", "C", "C", "P"}; // Mon..Sun initials

    private final Paint barPaint   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint gridPaint  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint valuePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF barRect    = new RectF();

    /** 7 values, index 0=Monday .. 6=Sunday. Null entry = no data that day. */
    private Float[] dailyAverages = new Float[7];

    public WeeklyTrendChartView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        gridPaint.setColor(getResources().getColor(R.color.chart_grid, null));
        gridPaint.setStrokeWidth(dp(1));

        labelPaint.setColor(getResources().getColor(R.color.text_secondary, null));
        labelPaint.setTextSize(sp(11));
        labelPaint.setTextAlign(Paint.Align.CENTER);

        valuePaint.setColor(getResources().getColor(R.color.text_primary, null));
        valuePaint.setTextSize(sp(11));
        valuePaint.setTextAlign(Paint.Align.CENTER);
        valuePaint.setFakeBoldText(true);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }

    /** Supply the 7 daily averages (seconds), Monday-first. Null = no data. */
    public void setData(Float[] mondayFirstAverages) {
        this.dailyAverages = mondayFirstAverages;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth(), h = getHeight();
        float bottomLabelHeight = sp(16);
        float topValueHeight    = sp(16);
        float chartTop    = topValueHeight;
        float chartBottom = h - bottomLabelHeight;
        float chartHeight = chartBottom - chartTop;

        // Determine max value for scaling (minimum 30s scale to avoid tiny bars).
        float maxVal = 30f;
        for (Float v : dailyAverages) {
            if (v != null && v > maxVal) maxVal = v;
        }

        // Baseline grid line
        canvas.drawLine(0, chartBottom, w, chartBottom, gridPaint);

        int barCount  = 7;
        float slot    = (float) w / barCount;
        float barW    = slot * 0.45f;

        for (int i = 0; i < barCount; i++) {
            float cx = slot * i + slot / 2f;
            Float val = dailyAverages[i];

            float barHeight = (val == null) ? dp(4) // tiny placeholder nub
                    : Math.max(dp(6), chartHeight * (val / maxVal));

            barRect.set(cx - barW / 2f, chartBottom - barHeight, cx + barW / 2f, chartBottom);
            barPaint.setColor(colorFor(val, maxVal));
            float radius = Math.min(barW / 2f, dp(10));
            canvas.drawRoundRect(barRect, radius, radius, barPaint);

            // Day label below
            canvas.drawText(DAY_LABELS_TR[i], cx, h - dp(2), labelPaint);

            // Value above bar (only if has data)
            if (val != null) {
                String txt = formatSeconds(val);
                canvas.drawText(txt, cx, chartBottom - barHeight - dp(6), valuePaint);
            }
        }
    }

    private int colorFor(Float val, float maxVal) {
        if (val == null) return getResources().getColor(R.color.chart_grid, null);
        float ratio = val / maxVal;
        if (ratio < 0.4f)  return getResources().getColor(R.color.chart_bar_fast, null);
        if (ratio < 0.75f) return getResources().getColor(R.color.chart_bar_mid, null);
        return getResources().getColor(R.color.chart_bar_slow, null);
    }

    private String formatSeconds(float seconds) {
        if (seconds < 60) return String.format(Locale.getDefault(), "%.0fs", seconds);
        return String.format(Locale.getDefault(), "%.1fm", seconds / 60f);
    }
}
