package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {



    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButtons();

        // Request notification permission  ← ADD THIS PART HERE
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }
    }

    private void setupButtons() {
        MaterialCardView addMedicineCard = findViewById(R.id.card_add_medicine);
        MaterialCardView viewRemindersCard = findViewById(R.id.card_view_reminders);
        MaterialCardView historyCard = findViewById(R.id.card_history);
        MaterialCardView settingsCard = findViewById(R.id.card_settings);

        addMedicineCard.setOnClickListener(view -> {
            Intent intent = new Intent(this, AddMedicineActivity.class);
            startActivity(intent);
        });

        viewRemindersCard.setOnClickListener(view -> {
            Intent intent = new Intent(this, ReminderListActivity.class);
            startActivity(intent);
        });
        historyCard.setOnClickListener(view -> {
            Toast.makeText(this, "History - Coming soon!", Toast.LENGTH_SHORT).show();
        });

        settingsCard.setOnClickListener(view -> {
            Toast.makeText(this, "Settings - Coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
}