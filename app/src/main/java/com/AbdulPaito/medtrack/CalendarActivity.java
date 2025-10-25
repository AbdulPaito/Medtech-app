package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.widget.CalendarView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class CalendarActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private TextView textSelectedDate, textDayInfo;
    private DatabaseHelper dbHelper;

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
        dbHelper = new DatabaseHelper(this);

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
            textDayInfo.setText("📭 No medicines taken on this day");
        } else {
            StringBuilder info = new StringBuilder();
            info.append("📊 Total: ").append(dayHistory.size()).append(" medicine(s)\n\n");
            
            int taken = 0, missed = 0;
            for (HistoryItem item : dayHistory) {
                if (item.getStatus().equals("Taken")) {
                    taken++;
                    info.append("✅ ");
                } else {
                    missed++;
                    info.append("❌ ");
                }
                info.append(item.getMedicineName())
                    .append(" - ").append(item.getTime())
                    .append("\n");
            }
            
            info.append("\n📈 Summary:\n");
            info.append("✅ Taken: ").append(taken).append("\n");
            info.append("❌ Missed: ").append(missed);
            
            textDayInfo.setText(info.toString());
        }
    }

    private String formatDate(int day, int month, int year) {
        return day + "/" + month + "/" + year;
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
