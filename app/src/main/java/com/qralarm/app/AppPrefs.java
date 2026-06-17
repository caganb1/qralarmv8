package com.qralarm.app;

import android.content.Context;
import android.content.SharedPreferences;

/** Tiny SharedPreferences wrapper for app-wide feature toggles. */
public class AppPrefs {

    private static final String PREFS_NAME        = "qralarm_prefs";
    private static final String KEY_ROUTINE_ENABLED = "routine_feature_enabled";

    public static boolean isRoutineFeatureEnabled(Context ctx) {
        return prefs(ctx).getBoolean(KEY_ROUTINE_ENABLED, false);
    }

    public static void setRoutineFeatureEnabled(Context ctx, boolean enabled) {
        prefs(ctx).edit().putBoolean(KEY_ROUTINE_ENABLED, enabled).apply();
    }

    private static SharedPreferences prefs(Context ctx) {
        return ctx.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }
}
