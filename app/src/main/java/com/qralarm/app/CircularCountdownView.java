package com.qralarm.app;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.RectF;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

/**
 * Minimalist circular countdown ring. Pastel track + animated progress arc,
 * with the remaining time drawn in the center. No external libs.
 */
public class CircularCountdownView extends View {

    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint progressPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint textPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final RectF arcRect = new RectF();

    private float progressFraction = 1f; // 1 = full time remaining, 0 = done
    private String centerText = "";

    private ValueAnimator animator;

    public CircularCountdownView(Context ctx, AttributeSet attrs) {
        super(ctx, attrs);
        init();
    }

    private void init() {
        trackPaint.setStyle(Paint.Style.STROKE);
        trackPaint.setStrokeWidth(dp(14));
        trackPaint.setColor(getResources().getColor(R.color.surface_light, null));
        trackPaint.setStrokeCap(Paint.Cap.ROUND);

        progressPaint.setStyle(Paint.Style.STROKE);
        progressPaint.setStrokeWidth(dp(14));
        progressPaint.setColor(getResources().getColor(R.color.accent_mint, null));
        progressPaint.setStrokeCap(Paint.Cap.ROUND);

        textPaint.setColor(getResources().getColor(R.color.text_primary, null));
        textPaint.setTextSize(sp(42));
        textPaint.setTextAlign(Paint.Align.CENTER);
        textPaint.setFakeBoldText(true);
    }

    private float dp(float v) { return v * getResources().getDisplayMetrics().density; }
    private float sp(float v) { return v * getResources().getDisplayMetrics().scaledDensity; }

    @Override
    protected void onSizeChanged(int w, int h, int oldw, int oldh) {
        super.onSizeChanged(w, h, oldw, oldh);
        float inset = trackPaint.getStrokeWidth();
        arcRect.set(inset, inset, w - inset, h - inset);
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        canvas.drawArc(arcRect, 0, 360, false, trackPaint);
        canvas.drawArc(arcRect, -90, 360f * progressFraction, false, progressPaint);

        float cx = getWidth() / 2f;
        float cy = getHeight() / 2f - ((textPaint.descent() + textPaint.ascent()) / 2f);
        canvas.drawText(centerText, cx, cy, textPaint);
    }

    /** Starts an animated countdown from totalSeconds to 0. */
    public void startCountdown(int totalSeconds, Runnable onFinish) {
        if (animator != null) animator.cancel();
        progressFraction = 1f;
        centerText = formatTime(totalSeconds);

        animator = ValueAnimator.ofInt(totalSeconds * 10, 0); // tenths of a second for smooth arc
        animator.setDuration(totalSeconds * 1000L);
        animator.setInterpolator(new LinearInterpolator());
        animator.addUpdateListener(a -> {
            int tenths = (int) a.getAnimatedValue();
            progressFraction = tenths / (float) (totalSeconds * 10);
            int wholeSeconds = (tenths + 9) / 10; // ceil for friendlier countdown display
            centerText = formatTime(wholeSeconds);
            invalidate();
        });
        animator.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(android.animation.Animator a) {
                centerText = formatTime(0);
                invalidate();
                if (onFinish != null) onFinish.run();
            }
        });
        animator.start();
    }

    public void cancelCountdown() {
        if (animator != null) animator.cancel();
    }

    private String formatTime(int totalSeconds) {
        int m = totalSeconds / 60, s = totalSeconds % 60;
        return String.format("%d:%02d", m, s);
    }
}
