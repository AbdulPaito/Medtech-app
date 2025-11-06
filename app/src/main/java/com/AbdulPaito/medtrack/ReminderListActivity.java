package com.AbdulPaito.medtrack;

import androidx.recyclerview.widget.ItemTouchHelper;
import com.google.android.material.snackbar.Snackbar;
import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.ArrayList;
import java.util.List;

public class ReminderListActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private MedicineAdapter adapter;
    private DatabaseHelper databaseHelper;
    private List<Medicine> medicineList;
    private List<Medicine> filteredList;
    private View emptyView;  // Empty state view
    private FloatingActionButton fabAdd;
    private EditText searchBar;
    private TextView textPendingHeader;

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
        setupSearch();
        loadMedicines();
    }

    private void initViews() {
        recyclerView = findViewById(R.id.recycler_view_medicines);
        emptyView = findViewById(R.id.text_empty_state);
        fabAdd = findViewById(R.id.fab_add);
        searchBar = findViewById(R.id.search_bar);
        textPendingHeader = findViewById(R.id.text_pending_header);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        fabAdd.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            startActivity(intent);
        });
    }

    private void setupSearch() {
        searchBar.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterMedicines(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });
    }

    private void filterMedicines(String query) {
        if (query.isEmpty()) {
            filteredList = new ArrayList<>(medicineList);
        } else {
            filteredList = databaseHelper.searchMedicines(query);
        }

        if (adapter != null) {
            adapter.updateList(filteredList);
            
            if (filteredList.isEmpty()) {
                recyclerView.setVisibility(View.GONE);
                emptyView.setVisibility(View.VISIBLE);
            } else {
                recyclerView.setVisibility(View.VISIBLE);
                emptyView.setVisibility(View.GONE);
            }
        }
    }

    private void loadMedicines() {
        medicineList = databaseHelper.getAllMedicines();

        if (medicineList.isEmpty()) {
            recyclerView.setVisibility(View.GONE);
            emptyView.setVisibility(View.VISIBLE);
            textPendingHeader.setVisibility(View.GONE);
        } else {
            recyclerView.setVisibility(View.VISIBLE);
            emptyView.setVisibility(View.GONE);
            textPendingHeader.setVisibility(View.VISIBLE);

            adapter = new MedicineAdapter(this, medicineList);
            recyclerView.setAdapter(adapter);

            // 👇 Swipe-to-delete with undo confirmation
            ItemTouchHelper itemTouchHelper = new ItemTouchHelper(
                    new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {

                        @Override
                        public boolean onMove(RecyclerView recyclerView,
                                              RecyclerView.ViewHolder viewHolder,
                                              RecyclerView.ViewHolder target) {
                            return false; // no drag/drop
                        }

                        @Override
                        public void onSwiped(RecyclerView.ViewHolder viewHolder, int direction) {
                            int position = viewHolder.getAdapterPosition();
                            Medicine deletedMedicine = medicineList.get(position);

                            new AlertDialog.Builder(ReminderListActivity.this)
                                    .setTitle("Delete Medicine")
                                    .setMessage("Are you sure you want to delete " + deletedMedicine.getMedicineName() + "?")
                                    .setPositiveButton("Delete", (dialog, which) -> {
                                        // CRITICAL FIX: Cancel alarms before deleting medicine
                                        AlarmScheduler alarmScheduler = new AlarmScheduler(ReminderListActivity.this);
                                        alarmScheduler.cancelMedicineAlarm(deletedMedicine.getId());

                                        // Delete from DB and remove from list
                                        databaseHelper.deleteMedicine(deletedMedicine.getId());
                                        medicineList.remove(position);
                                        adapter.notifyItemRemoved(position);

                                        // Show Snackbar with Undo option
                                        Snackbar.make(recyclerView, deletedMedicine.getMedicineName() + " deleted", Snackbar.LENGTH_LONG)
                                                .setAction("UNDO", v -> {
                                                    // Reinsert medicine into DB and list
                                                    databaseHelper.addMedicine(deletedMedicine);
                                                    medicineList.add(position, deletedMedicine);
                                                    adapter.notifyItemInserted(position);
                                                    
                                                    // Reschedule alarms for restored medicine
                                                    AlarmScheduler restoreScheduler = new AlarmScheduler(ReminderListActivity.this);
                                                    restoreScheduler.scheduleMedicineAlarm(deletedMedicine);
                                                })
                                                .show();

                                        // Show empty view if list becomes empty
                                        if (medicineList.isEmpty()) {
                                            recyclerView.setVisibility(View.GONE);
                                            emptyView.setVisibility(View.VISIBLE);
                                        }
                                    })
                                    .setNegativeButton("Cancel", (dialog, which) -> {
                                        adapter.notifyItemChanged(position);
                                        dialog.dismiss();
                                    })
                                    .show();
                        }
                    });

            itemTouchHelper.attachToRecyclerView(recyclerView);
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
                    // CRITICAL FIX: Cancel alarms before deleting medicine
                    AlarmScheduler alarmScheduler = new AlarmScheduler(this);
                    alarmScheduler.cancelMedicineAlarm(medicine.getId());

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
