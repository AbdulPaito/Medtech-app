package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.view.View;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView textSelectedDate, textDayInfo, textEmptyState;
    private RecyclerView recyclerViewHistory;
    private HistoryCalendarAdapter adapter;
    private DatabaseHelper dbHelper;
    private List<HistoryItem> historyList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_calendar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Calendar View");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        calendarView = findViewById(R.id.calendarView);
        textSelectedDate = findViewById(R.id.textSelectedDate);
        textDayInfo = findViewById(R.id.textDayInfo);
        textEmptyState = findViewById(R.id.textEmptyState);
        recyclerViewHistory = findViewById(R.id.recyclerViewHistory);
        dbHelper = new DatabaseHelper(this);

        // Setup RecyclerView
        historyList = new ArrayList<>();
        adapter = new HistoryCalendarAdapter(this, historyList);
        recyclerViewHistory.setLayoutManager(new LinearLayoutManager(this));
        recyclerViewHistory.setAdapter(adapter);

        // Show today's data initially
        showTodayData();

        // Handle date selection
        calendarView.setOnDateChangeListener((view, year, month, dayOfMonth) -> {
            String date = formatDate(dayOfMonth, month + 1, year);
            showDayHistory(date, dayOfMonth, month, year);
        });
    }

    private void showTodayData() {
        Calendar calendar = Calendar.getInstance();
        int day = calendar.get(Calendar.DAY_OF_MONTH);
        int month = calendar.get(Calendar.MONTH);
        int year = calendar.get(Calendar.YEAR);
        
        String date = formatDate(day, month + 1, year);
        showDayHistory(date, day, month, year);
    }

    private void showDayHistory(String date, int day, int month, int year) {
        // Format display date
        Calendar cal = Calendar.getInstance();
        cal.set(year, month, day);
        SimpleDateFormat displayFormat = new SimpleDateFormat("EEEE, MMMM dd, yyyy", Locale.getDefault());
        textSelectedDate.setText(displayFormat.format(cal.getTime()));

        // Get history for this date
        List<HistoryItem> dayHistory = dbHelper.getHistoryByDate(date);
        
        if (dayHistory.isEmpty()) {
            // Show empty state
            textDayInfo.setText("📭 No medicines recorded");
            recyclerViewHistory.setVisibility(View.GONE);
            textEmptyState.setVisibility(View.VISIBLE);
        } else {
            // Show summary
            int taken = 0, missed = 0;
            for (HistoryItem item : dayHistory) {
                if (item.getStatus().equals("Taken")) {
                    taken++;
                } else {
                    missed++;
                }
            }
            
            String summary = "📊 Total: " + dayHistory.size() + " medicine(s) • " +
                           "✅ Taken: " + taken + " • ❌ Missed: " + missed;
            textDayInfo.setText(summary);
            
            // Update RecyclerView
            historyList.clear();
            historyList.addAll(dayHistory);
            adapter.updateList(historyList);
            
            recyclerViewHistory.setVisibility(View.VISIBLE);
            textEmptyState.setVisibility(View.GONE);
        }
    }

    private String formatDate(int day, int month, int year) {
        // Format with leading zeros to match database format (dd/MM/yyyy)
        return String.format(Locale.getDefault(), "%02d/%02d/%d", day, month, year);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
