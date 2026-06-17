package com.qralarm.app;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "routines")
public class Routine {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "name")            public String name;
    @ColumnInfo(name = "duration_seconds")public int    durationSeconds;
    @ColumnInfo(name = "is_selected")     public boolean isSelected; // which routine fires after QR dismiss

    public Routine() {}

    public Routine(String name, int durationSeconds) {
        this.name = name;
        this.durationSeconds = durationSeconds;
        this.isSelected = false;
    }
}
