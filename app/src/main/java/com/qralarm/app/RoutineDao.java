package com.qralarm.app;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface RoutineDao {

    @Insert
    long insert(Routine routine);

    @Update
    void update(Routine routine);

    @Delete
    void delete(Routine routine);

    @Query("SELECT * FROM routines ORDER BY id ASC")
    LiveData<List<Routine>> getAllRoutines();

    @Query("SELECT * FROM routines ORDER BY id ASC")
    List<Routine> getAllRoutinesSync();

    @Query("SELECT * FROM routines WHERE is_selected = 1 LIMIT 1")
    Routine getSelectedRoutine();

    @Query("UPDATE routines SET is_selected = 0")
    void clearSelection();

    @Query("UPDATE routines SET is_selected = 1 WHERE id = :id")
    void selectRoutine(int id);
}
