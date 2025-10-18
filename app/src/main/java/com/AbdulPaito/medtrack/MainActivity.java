package com.AbdulPalto.medtrack;

import android.os.Bundle;
import android.content.Intent;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setupButtons();
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
            Toast.makeText(this, "View Reminders - Coming soon!", Toast.LENGTH_SHORT).show();
        });

        historyCard.setOnClickListener(view -> {
            Toast.makeText(this, "History - Coming soon!", Toast.LENGTH_SHORT).show();
        });

        settingsCard.setOnClickListener(view -> {
            Toast.makeText(this, "Settings - Coming soon!", Toast.LENGTH_SHORT).show();
        });
    }
}