package com.qralarm.app;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "saved_qrs")
public class SavedQR {

    @PrimaryKey(autoGenerate = true)
    public int id;

    @ColumnInfo(name = "label") public String label;
    @ColumnInfo(name = "value") public String value;
    @ColumnInfo(name = "created_at") public long createdAt;

    public SavedQR() {}

    public SavedQR(String label, String value) {
        this.label     = label;
        this.value     = value;
        this.createdAt = System.currentTimeMillis();
    }
}
