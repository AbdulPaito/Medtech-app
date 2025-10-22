package com.AbdulPaito.medtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.google.android.material.button.MaterialButton;

import java.util.ArrayList;
import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;

    private TextView textTotalTaken;
    private LinearLayout emptyState;
    private MaterialButton btnFilterAll, btnFilterTaken, btnFilterMissed;
    private List<HistoryItem> allHistory;
    private String currentFilter = "All";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Medication History");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textTotalTaken = findViewById(R.id.text_total_taken);
        emptyState = findViewById(R.id.empty_state);
        btnFilterAll = findViewById(R.id.btn_filter_all);
        btnFilterTaken = findViewById(R.id.btn_filter_taken);
        btnFilterMissed = findViewById(R.id.btn_filter_missed);

        dbHelper = new DatabaseHelper(this);

        setupFilterButtons();
        updateHistoryList();
    }

    private void setupFilterButtons() {
        btnFilterAll.setOnClickListener(v -> {
            currentFilter = "All";
            updateFilterButtons();
            filterHistory();
        });

        btnFilterTaken.setOnClickListener(v -> {
            currentFilter = "Taken";
            updateFilterButtons();
            filterHistory();
        });

        btnFilterMissed.setOnClickListener(v -> {
            currentFilter = "Missed";
            updateFilterButtons();
            filterHistory();
        });
    }

    private void updateFilterButtons() {
        // Reset all buttons
        btnFilterAll.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
        btnFilterTaken.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));
        btnFilterMissed.setBackgroundColor(getResources().getColor(android.R.color.transparent, null));

        // Highlight selected button
        int selectedColor = getResources().getColor(R.color.primary, null);
        if (currentFilter.equals("All")) {
            btnFilterAll.setBackgroundColor(selectedColor);
            btnFilterAll.setTextColor(getResources().getColor(android.R.color.white, null));
        } else if (currentFilter.equals("Taken")) {
            btnFilterTaken.setBackgroundColor(getResources().getColor(R.color.success, null));
            btnFilterTaken.setTextColor(getResources().getColor(android.R.color.white, null));
        } else if (currentFilter.equals("Missed")) {
            btnFilterMissed.setBackgroundColor(getResources().getColor(R.color.error, null));
            btnFilterMissed.setTextColor(getResources().getColor(android.R.color.white, null));
        }
    }

    private void updateHistoryList() {
        allHistory = dbHelper.getAllHistory();

        int count = allHistory.size();
        textTotalTaken.setText("Total medicines taken: " + count);

        if (count == 0) {
            emptyState.setVisibility(LinearLayout.VISIBLE);
            recyclerView.setVisibility(RecyclerView.GONE);
        } else {
            emptyState.setVisibility(LinearLayout.GONE);
            recyclerView.setVisibility(RecyclerView.VISIBLE);

            adapter = new HistoryAdapter(allHistory);
            recyclerView.setAdapter(adapter);
        }
        
        updateFilterButtons();
    }

    private void filterHistory() {
        if (allHistory == null) return;

        List<HistoryItem> filteredList = new ArrayList<>();

        if (currentFilter.equals("All")) {
            filteredList = allHistory;
        } else {
            for (HistoryItem item : allHistory) {
                if (item.getStatus().equals(currentFilter)) {
                    filteredList.add(item);
                }
            }
        }

        if (filteredList.isEmpty()) {
            emptyState.setVisibility(LinearLayout.VISIBLE);
            recyclerView.setVisibility(RecyclerView.GONE);
        } else {
            emptyState.setVisibility(LinearLayout.GONE);
            recyclerView.setVisibility(RecyclerView.VISIBLE);

            adapter = new HistoryAdapter(filteredList);
            recyclerView.setAdapter(adapter);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
