package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.AbdulPaito.medtrack.database.DatabaseHelper;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;

    private TextView textTotalTaken;
    private LinearLayout emptyState;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        textTotalTaken = findViewById(R.id.text_total_taken);
        emptyState = findViewById(R.id.empty_state);

        dbHelper = new DatabaseHelper(this);

        updateHistoryList();
    }

    private void updateHistoryList() {
        List<HistoryItem> historyList = dbHelper.getAllHistory();

        int count = historyList.size();
        textTotalTaken.setText("Total medicines taken: " + count);

        if (count == 0) {
            emptyState.setVisibility(LinearLayout.VISIBLE);
            recyclerView.setVisibility(RecyclerView.GONE);
        } else {
            emptyState.setVisibility(LinearLayout.GONE);
            recyclerView.setVisibility(RecyclerView.VISIBLE);

            adapter = new HistoryAdapter(historyList);
            recyclerView.setAdapter(adapter);
        }
    }
}
