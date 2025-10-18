package com.AbdulPaito.medtrack;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.List;

public class ReminderListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<Medicine> medicineList;
    private View emptyView;  // ← CHANGED from TextView to View
    private FloatingActionButton fabAdd;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reminder_list);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Reminders");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        initViews();
        loadMedicines();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_medicines);
        emptyView = findViewById(R.id.text_empty_state);
        fabAdd = findViewById(R.id.fab_add);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            startActivity(intent);
        });
    }

    private void loadMedicines() {
        medicineList = databaseHelper.getAllMedicines();

        if (medicineList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);

            adapter = new MedicineAdapter(medicineList, this::onMedicineClick, this::onDeleteClick);
            recyclerView.setAdapter(adapter);
        }
    }

    private void onMedicineClick(Medicine medicine) {
        new AlertDialog.Builder(this)
                .setTitle("Mark as Taken")
                .setMessage("Did you take " + medicine.getMedicineName() + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Toast.makeText(this, "Marked as taken!", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void onDeleteClick(Medicine medicine, int position) {
        new AlertDialog.Builder(this)
                .setTitle("Delete Medicine")
                .setMessage("Are you sure you want to delete " + medicine.getMedicineName() + "?")
                .setPositiveButton("Delete", (dialog, which) -> {
                    databaseHelper.deleteMedicine(medicine.getId());
                    medicineList.remove(position);
                    adapter.notifyItemRemoved(position);

                    if (medicineList.isEmpty()) {
                        recyclerView.setVisibility(View.GONE);
                        emptyView.setVisibility(View.VISIBLE);
                    }

                    Toast.makeText(this, "Medicine deleted", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadMedicines();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}