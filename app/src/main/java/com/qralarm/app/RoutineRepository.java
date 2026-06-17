package com.qralarm.app;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class RoutineRepository {

    private final RoutineDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public RoutineRepository(Context context) {
        dao = AlarmDatabase.getInstance(context).routineDao();
    }

    public LiveData<List<Routine>> getAllRoutines() { return dao.getAllRoutines(); }

    public void insert(Routine r) { executor.execute(() -> dao.insert(r)); }
    public void update(Routine r) { executor.execute(() -> dao.update(r)); }
    public void delete(Routine r) { executor.execute(() -> dao.delete(r)); }

    public void selectRoutine(int id) {
        executor.execute(() -> {
            dao.clearSelection();
            dao.selectRoutine(id);
        });
    }

    public interface OnRoutineLoaded { void onLoaded(Routine routine); }

    public void getSelectedRoutine(OnRoutineLoaded cb) {
        executor.execute(() -> {
            Routine r = dao.getSelectedRoutine();
            if (cb != null) cb.onLoaded(r);
        });
    }
}
