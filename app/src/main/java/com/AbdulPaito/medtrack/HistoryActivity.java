package com.AbdulPaito.medtrack;

import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.AbdulPaito.medtrack.database.DatabaseHelper;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private HistoryAdapter adapter;
    private DatabaseHelper dbHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_history);

        recyclerView = findViewById(R.id.recycler_history);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        dbHelper = new DatabaseHelper(this);
        List<HistoryItem> historyList = dbHelper.getAllHistory(); // make sure you have this method in DatabaseHelper

        adapter = new HistoryAdapter(historyList);
        recyclerView.setAdapter(adapter);
    }
}
