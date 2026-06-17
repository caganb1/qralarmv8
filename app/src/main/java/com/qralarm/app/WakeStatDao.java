package com.qralarm.app;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WakeStatDao {

    @Insert
    void insert(WakeStat stat);

    @Query("SELECT * FROM wake_stats WHERE ring_timestamp >= :sinceMillis ORDER BY ring_timestamp ASC")
    List<WakeStat> getStatsSince(long sinceMillis);

    @Query("SELECT AVG(reaction_seconds) FROM wake_stats WHERE ring_timestamp >= :sinceMillis")
    Double getAverageReactionSeconds(long sinceMillis);
}
