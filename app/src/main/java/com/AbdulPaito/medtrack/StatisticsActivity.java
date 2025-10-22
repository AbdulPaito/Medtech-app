package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class StatisticsActivity extends AppCompatActivity {

    private TextView textAdherenceRate, textStreak, textLastUpdated;
    private TextView textTotalTaken, textTotalMissed;
    private ProgressBar progressAdherence;
    private Button btnExportReport;
    private android.widget.LinearLayout chartContainer;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_statistics);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Statistics & Analytics");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        initViews();
        loadStatistics();
        createChart();
        setupExportButton();
    }

    private void initViews() {
        textAdherenceRate = findViewById(R.id.textAdherenceRate);
        textStreak = findViewById(R.id.textStreak);
        textLastUpdated = findViewById(R.id.textLastUpdated);
        progressAdherence = findViewById(R.id.progressAdherence);
        textTotalTaken = findViewById(R.id.textTotalTaken);
        textTotalMissed = findViewById(R.id.textTotalMissed);
        chartContainer = findViewById(R.id.chartContainer);
        btnExportReport = findViewById(R.id.btn_export_report);
    }

    private void loadStatistics() {
        // Get real data from database
        int adherenceRate = databaseHelper.getAdherenceRate();
        int streakDays = databaseHelper.getCurrentStreak();
        int takenCount = databaseHelper.getTotalTakenCount();
        int missedCount = databaseHelper.getTotalMissedCount();

        // Update adherence rate
        progressAdherence.setProgress(adherenceRate);
        textAdherenceRate.setText(getString(R.string.adherence_text, adherenceRate));

        // Update streak
        textStreak.setText(getString(R.string.streak_text, streakDays));

        // Update total doses
        textTotalTaken.setText(String.valueOf(takenCount));
        textTotalMissed.setText(String.valueOf(missedCount));

        // Update last updated time
        String currentTime = new SimpleDateFormat("MMM dd, yyyy HH:mm", Locale.getDefault())
                .format(new Date());
        textLastUpdated.setText("Updated: " + currentTime);
    }

    private void createChart() {
        // Simple bar chart for last 7 days
        chartContainer.removeAllViews();
        
        // Get REAL adherence data from database
        int[] adherenceData = databaseHelper.getLast7DaysAdherence();
        
        // Create 7 bars representing last 7 days with REAL data
        String[] days = {"Mon", "Tue", "Wed", "Thu", "Fri", "Sat", "Sun"};
        int[] values = adherenceData; // REAL DATA from database!
        
        for (int i = 0; i < 7; i++) {
            android.widget.LinearLayout barLayout = new android.widget.LinearLayout(this);
            barLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
            barLayout.setGravity(android.view.Gravity.BOTTOM | android.view.Gravity.CENTER_HORIZONTAL);
            android.widget.LinearLayout.LayoutParams params = new android.widget.LinearLayout.LayoutParams(
                    0, android.widget.LinearLayout.LayoutParams.MATCH_PARENT, 1);
            params.setMargins(4, 0, 4, 0);
            barLayout.setLayoutParams(params);
            
            // Bar
            android.view.View bar = new android.view.View(this);
            int barHeight = (int) (values[i] * 1.2); // Scale to fit
            android.widget.LinearLayout.LayoutParams barParams = new android.widget.LinearLayout.LayoutParams(
                    android.widget.LinearLayout.LayoutParams.MATCH_PARENT, barHeight);
            bar.setLayoutParams(barParams);
            bar.setBackgroundColor(getResources().getColor(R.color.primary, null));
            
            // Day label
            TextView dayLabel = new TextView(this);
            dayLabel.setText(days[i]);
            dayLabel.setTextSize(10);
            dayLabel.setTextColor(getResources().getColor(R.color.text_secondary, null));
            dayLabel.setGravity(android.view.Gravity.CENTER);
            dayLabel.setPadding(0, 8, 0, 0);
            
            barLayout.addView(bar);
            barLayout.addView(dayLabel);
            chartContainer.addView(barLayout);
        }
    }

    private void setupExportButton() {
        btnExportReport.setOnClickListener(v -> {
            exportReport();
        });
    }

    private void exportReport() {
        // Get statistics
        int adherenceRate = databaseHelper.getAdherenceRate();
        int streakDays = databaseHelper.getCurrentStreak();
        int takenCount = databaseHelper.getTotalTakenCount();
        int missedCount = databaseHelper.getTotalMissedCount();
        int totalMedicines = databaseHelper.getMedicineCount();

        // Create report text
        StringBuilder report = new StringBuilder();
        report.append("📊 MedTrack Health Report\n\n");
        report.append("Generated: ").append(new SimpleDateFormat("MMM dd, yyyy HH:mm", 
                Locale.getDefault()).format(new Date())).append("\n\n");
        report.append("📈 Statistics:\n");
        report.append("• Adherence Rate: ").append(adherenceRate).append("%\n");
        report.append("• Current Streak: ").append(streakDays).append(" days\n");
        report.append("• Total Taken: ").append(takenCount).append("\n");
        report.append("• Total Missed: ").append(missedCount).append("\n");
        report.append("• Active Medicines: ").append(totalMedicines).append("\n\n");
        report.append("💊 Keep up the great work!\n");

        // Share report
        android.content.Intent shareIntent = new android.content.Intent(android.content.Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, "MedTrack Health Report");
        shareIntent.putExtra(android.content.Intent.EXTRA_TEXT, report.toString());
        startActivity(android.content.Intent.createChooser(shareIntent, "Share Report"));
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadStatistics(); // Refresh when returning to activity
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
