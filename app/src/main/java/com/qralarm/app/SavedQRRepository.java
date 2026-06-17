package com.qralarm.app;

import android.content.Context;

import androidx.lifecycle.LiveData;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class SavedQRRepository {

    private final SavedQRDao dao;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();

    public SavedQRRepository(Context context) {
        dao = AlarmDatabase.getInstance(context).savedQRDao();
    }

    public LiveData<List<SavedQR>> getAllSavedQRs() { return dao.getAllSavedQRs(); }

    public void insert(SavedQR qr) { executor.execute(() -> dao.insert(qr)); }
    public void delete(SavedQR qr) { executor.execute(() -> dao.delete(qr)); }

    /** Saves the code only if a code with the same value isn't already stored. */
    public void insertIfNew(String label, String value) {
        executor.execute(() -> {
            if (dao.findByValue(value) == null) {
                dao.insert(new SavedQR(label, value));
            }
        });
    }

    public interface OnListLoaded { void onLoaded(List<SavedQR> list); }

    public void getAllSync(OnListLoaded cb) {
        executor.execute(() -> {
            List<SavedQR> list = dao.getAllSavedQRsSync();
            if (cb != null) cb.onLoaded(list);
        });
    }
}
