package com.qralarm.app;

import android.content.Context;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class WakeStatRepository {

    private final WakeStatDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public WakeStatRepository(Context context) {
        dao = AlarmDatabase.getInstance(context).wakeStatDao();
    }

    public void insert(WakeStat stat) {
        executor.execute(() -> dao.insert(stat));
    }

    public interface OnAverageLoaded { void onLoaded(Double avgSeconds); }

    public void getAverageSince(long sinceMillis, OnAverageLoaded cb) {
        executor.execute(() -> {
            Double avg = dao.getAverageReactionSeconds(sinceMillis);
            if (cb != null) cb.onLoaded(avg);
        });
    }

    public interface OnStatsLoaded { void onLoaded(List<WakeStat> stats); }

    public void getStatsSince(long sinceMillis, OnStatsLoaded cb) {
        executor.execute(() -> {
            List<WakeStat> stats = dao.getStatsSince(sinceMillis);
            if (cb != null) cb.onLoaded(stats);
        });
    }
}
