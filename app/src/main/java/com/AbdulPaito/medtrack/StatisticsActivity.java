package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

public class StatisticsActivity extends AppCompatActivity {

    private TextView textAdherenceRate, textStreak;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        // Match only what exists in your XML layout
        textAdherenceRate = findViewById(R.id.textAdherenceRate);
        textStreak = findViewById(R.id.textStreak);

        // Example data (you can later load from database)
        int adherenceRate = 92;
        int streakDays = 5;

        // Set text values with proper Android string formatting
        textAdherenceRate.setText(getString(R.string.adherence_text, adherenceRate));
        textStreak.setText(getString(R.string.streak_text, streakDays));
    }
}
