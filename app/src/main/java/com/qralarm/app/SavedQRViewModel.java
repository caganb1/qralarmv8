package com.qralarm.app;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

public class SavedQRViewModel extends AndroidViewModel {

    private final SavedQRRepository repo;
    private final LiveData<List<SavedQR>> allSavedQRs;

    public SavedQRViewModel(@NonNull Application application) {
        super(application);
        repo = new SavedQRRepository(application);
        allSavedQRs = repo.getAllSavedQRs();
    }

    public LiveData<List<SavedQR>> getAllSavedQRs() { return allSavedQRs; }

    public void delete(SavedQR qr) { repo.delete(qr); }
}
