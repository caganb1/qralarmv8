package com.qralarm.app;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;
import java.util.List;

/** Minimalist "Saved QRs" manager — iOS Settings style rows with pastel circular icon badges. */
public class SavedQRListActivity extends AppCompatActivity {

    private SavedQRViewModel viewModel;
    private SavedQRAdapter adapter;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_saved_qr_list);

        findViewById(R.id.btn_back_saved_qr).setOnClickListener(v -> finish());

        tvEmpty = findViewById(R.id.tv_saved_qr_empty);
        RecyclerView recycler = findViewById(R.id.recycler_saved_qrs);
        recycler.setLayoutManager(new androidx.recyclerview.widget.LinearLayoutManager(this));

        adapter = new SavedQRAdapter(qr -> {
            viewModel.delete(qr);
        });
        recycler.setAdapter(adapter);

        viewModel = new ViewModelProvider(this).get(SavedQRViewModel.class);
        viewModel.getAllSavedQRs().observe(this, list -> {
            adapter.setItems(list);
            tvEmpty.setVisibility(list.isEmpty() ? View.VISIBLE : View.GONE);
        });
    }

    // ── Adapter ──────────────────────────────────────────────────────────────

    static class SavedQRAdapter extends RecyclerView.Adapter<SavedQRAdapter.VH> {

        interface OnDeleteListener { void onDelete(SavedQR qr); }

        private final List<SavedQR> items = new ArrayList<>();
        private final OnDeleteListener deleteListener;
        // Alternate pastel badge colors for visual rhythm, iOS-Settings style.
        private static final int[] BADGE_BGS = { R.drawable.bg_circle_blue, R.drawable.bg_circle_mint };

        SavedQRAdapter(OnDeleteListener l) { this.deleteListener = l; }

        void setItems(List<SavedQR> newItems) {
            items.clear();
            items.addAll(newItems);
            notifyDataSetChanged();
        }

        @NonNull @Override
        public VH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View v = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_saved_qr, parent, false);
            return new VH(v);
        }

        @Override
        public void onBindViewHolder(@NonNull VH holder, int position) {
            SavedQR qr = items.get(position);
            holder.tvLabel.setText(qr.label != null && !qr.label.isEmpty() ? qr.label : qr.value);
            holder.tvValue.setText(qr.value);
            holder.badgeBg.setBackgroundResource(BADGE_BGS[position % BADGE_BGS.length]);
            holder.btnDelete.setOnClickListener(v -> {
                if (deleteListener != null) deleteListener.onDelete(qr);
            });
            // Hide bottom separator on the last row for a clean iOS-style grouped list.
            holder.separator.setVisibility(position == items.size() - 1 ? View.GONE : View.VISIBLE);
        }

        @Override public int getItemCount() { return items.size(); }

        static class VH extends RecyclerView.ViewHolder {
            TextView  tvLabel, tvValue;
            ImageView badgeBg, btnDelete;
            View      separator;

            VH(View itemView) {
                super(itemView);
                tvLabel   = itemView.findViewById(R.id.tv_saved_qr_label);
                tvValue   = itemView.findViewById(R.id.tv_saved_qr_value);
                badgeBg   = itemView.findViewById(R.id.iv_saved_qr_badge);
                btnDelete = itemView.findViewById(R.id.btn_saved_qr_delete);
                separator = itemView.findViewById(R.id.v_saved_qr_separator);
            }
        }
    }
}
