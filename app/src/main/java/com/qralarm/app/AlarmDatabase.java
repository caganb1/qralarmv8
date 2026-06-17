package com.qralarm.app;

import android.content.Context;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {Alarm.class, Routine.class, WakeStat.class, SavedQR.class},
        version = 4,
        exportSchema = false
)
public abstract class AlarmDatabase extends RoomDatabase {

    private static volatile AlarmDatabase INSTANCE;

    public abstract AlarmDao    alarmDao();
    public abstract RoutineDao  routineDao();
    public abstract WakeStatDao wakeStatDao();
    public abstract SavedQRDao  savedQRDao();

    // ── Migration 1 → 2 ──────────────────────────────────────────────────────
    static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `alarms_new` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`hour` INTEGER NOT NULL," +
                "`minute` INTEGER NOT NULL," +
                "`label` TEXT," +
                "`is_enabled` INTEGER NOT NULL," +
                "`repeat_days` TEXT," +
                "`qr_code_value` TEXT," +
                "`qr_code_label` TEXT," +
                "`uri_calm` TEXT," +
                "`uri_medium` TEXT," +
                "`uri_loud` TEXT)"
            );
            db.execSQL(
                "INSERT INTO alarms_new " +
                "(id, hour, minute, label, is_enabled, repeat_days, " +
                " qr_code_value, qr_code_label, uri_calm, uri_medium, uri_loud) " +
                "SELECT id, hour, minute, label, is_enabled, repeat_days, " +
                "       qr_code_value, qr_code_label, sound_uri, NULL, NULL " +
                "FROM alarms"
            );
            db.execSQL("DROP TABLE alarms");
            db.execSQL("ALTER TABLE alarms_new RENAME TO alarms");
        }
    };

    // ── Migration 2 → 3 ──────────────────────────────────────────────────────
    // Adds `routines` and `wake_stats` tables. Alarm table is untouched.
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `routines` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`name` TEXT," +
                "`duration_seconds` INTEGER NOT NULL," +
                "`is_selected` INTEGER NOT NULL DEFAULT 0)"
            );
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `wake_stats` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`ring_timestamp` INTEGER NOT NULL," +
                "`reaction_seconds` INTEGER NOT NULL)"
            );
        }
    };

    // ── Migration 3 → 4 ──────────────────────────────────────────────────────
    // Adds `saved_qrs` table. No existing tables touched.
    static final Migration MIGRATION_3_4 = new Migration(3, 4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                "CREATE TABLE IF NOT EXISTS `saved_qrs` (" +
                "`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                "`label` TEXT," +
                "`value` TEXT," +
                "`created_at` INTEGER NOT NULL)"
            );
        }
    };

    // ── Singleton ─────────────────────────────────────────────────────────────

    public static AlarmDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AlarmDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                            context.getApplicationContext(),
                            AlarmDatabase.class,
                            "alarm_database"
                    )
                    .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
                    .build();
                }
            }
        }
        return INSTANCE;
    }
}
