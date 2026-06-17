package com.qralarm.app;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "wake_stats")
public class WakeStat {

    @PrimaryKey(autoGenerate = true)
    public int id;

    /** Epoch millis when the alarm started ringing. */
    @ColumnInfo(name = "ring_timestamp")    public long ringTimestamp;

    /** Seconds elapsed between ring start and successful QR dismissal. */
    @ColumnInfo(name = "reaction_seconds")  public int reactionSeconds;

    public WakeStat() {}

    public WakeStat(long ringTimestamp, int reactionSeconds) {
        this.ringTimestamp   = ringTimestamp;
        this.reactionSeconds = reactionSeconds;
    }
}
