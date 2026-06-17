package com.qralarm.app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface SavedQRDao {

    @Insert
    long insert(SavedQR qr);

    @Delete
    void delete(SavedQR qr);

    @Query("SELECT * FROM saved_qrs ORDER BY created_at DESC")
    LiveData<List<SavedQR>> getAllSavedQRs();

    @Query("SELECT * FROM saved_qrs ORDER BY created_at DESC")
    List<SavedQR> getAllSavedQRsSync();

    @Query("SELECT * FROM saved_qrs WHERE value = :value LIMIT 1")
    SavedQR findByValue(String value);
}
