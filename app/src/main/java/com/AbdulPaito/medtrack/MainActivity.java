package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.content.Intent;
import android.widget.TextView;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import android.app.AlertDialog;
import android.widget.EditText;
import android.content.SharedPreferences;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.card.MaterialCardView;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.AbdulPaito.medtrack.database.DatabaseHelper;

public class MainActivity extends AppCompatActivity {

    private TextView textMedicineCount;
    private TextView textNextReminder;
    private TextView textWelcome; // 👈 Added
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);

        initViews();
        setupButtons();
        setupBottomNavigation();
        setupWelcomeName(); // 👈 Added here
        updateStats();

        // Request notification permission
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void initViews() {
        textMedicineCount = findViewById(R.id.text_medicine_count);
        textNextReminder = findViewById(R.id.text_next_reminder);
        textWelcome = findViewById(R.id.text_welcome); // 👈 make sure this ID exists in XML
    }

    // 👇 This handles editable "Hello, User!"
    private void setupWelcomeName() {
        SharedPreferences prefs = getSharedPreferences("MedTrackPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        textWelcome.setText("Hello, " + username + "!");

        textWelcome.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setHint("Enter your name");

            new AlertDialog.Builder(this)
                    .setTitle("Change Name")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            textWelcome.setText("Hello, " + newName + "!");
                            prefs.edit().putString("username", newName).apply();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupButtons() {
        MaterialCardView addMedicineCard = findViewById(R.id.card_add_medicine);
        MaterialCardView viewRemindersCard = findViewById(R.id.card_view_reminders);

        addMedicineCard.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            startActivity(intent);
        });

        viewRemindersCard.setOnClickListener(view -> {
            Intent intent = new Intent(this, ReminderListActivity.class);
            startActivity(intent);
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_medicines) {
                startActivity(new Intent(this, ReminderListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (itemId == R.id.navigation_settings) {
                // TODO: Open Settings Activity
                return true;
            }
            return false;
        });
    }

    private void updateStats() {
        int count = databaseHelper.getMedicineCount();
        if (count == 0) {
            textMedicineCount.setText("No medicines added yet");
            textNextReminder.setText("Add your first medicine to get started!");
        } else if (count == 1) {
            textMedicineCount.setText("1 medicine scheduled");
            textNextReminder.setText("Stay healthy! 💪");
        } else {
            textMedicineCount.setText(count + " medicines scheduled");
            textNextReminder.setText("Taking care of your health! 🌟");
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
    }
}
