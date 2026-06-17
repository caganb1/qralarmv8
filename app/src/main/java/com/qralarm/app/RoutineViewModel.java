package com.qralarm.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class RoutineViewModel extends AndroidViewModel {

    private final RoutineRepository repo;
    private final LiveData<List<Routine>> allRoutines;

    public RoutineViewModel(@NonNull Application application) {
        super(application);
        repo = new RoutineRepository(application);
        allRoutines = repo.getAllRoutines();
    }

    public LiveData<List<Routine>> getAllRoutines() { return allRoutines; }

    public void insert(Routine r) { repo.insert(r); }
    public void delete(Routine r) { repo.delete(r); }
    public void selectRoutine(int id) { repo.selectRoutine(id); }
}
