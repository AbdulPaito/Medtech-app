package com.AbdulPaito.medtrack;

import android.content.SharedPreferences;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Bundle;
import android.widget.Button;
import android.widget.Toast;
import android.content.Intent;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import com.google.android.material.materialswitch.MaterialSwitch;
import com.google.android.material.button.MaterialButton;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SettingsActivity extends AppCompatActivity {

    private static final int RINGTONE_REQUEST_CODE = 100;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Settings");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);

        // Initialize views
        MaterialSwitch switchNotifications = findViewById(R.id.switchNotifications);
        MaterialSwitch switchVibration = findViewById(R.id.switchVibration);
        MaterialSwitch switchSilentMode = findViewById(R.id.switchSilentMode);
        Button selectSoundBtn = findViewById(R.id.selectSoundBtn);
        MaterialButton btnBackupData = findViewById(R.id.btnBackupData);
        MaterialButton btnRestoreData = findViewById(R.id.btnRestoreData);
        MaterialButton btnDeleteHistory = findViewById(R.id.btnDeleteHistory);
        MaterialButton btnCalendarView = findViewById(R.id.btnCalendarView);
        Button btnAppGuide = findViewById(R.id.btnAppGuide);

        // Load preferences
        switchNotifications.setChecked(prefs.getBoolean("notifications_enabled", true));
        switchVibration.setChecked(prefs.getBoolean("vibration_enabled", true));
        switchSilentMode.setChecked(prefs.getBoolean("silent_mode", false));

        // Notifications toggle
        switchNotifications.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("notifications_enabled", checked).apply();
            Toast.makeText(this, checked ? "Notifications enabled" : "Notifications disabled", 
                Toast.LENGTH_SHORT).show();
        });

        // Vibration toggle
        switchVibration.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("vibration_enabled", checked).apply();
            Toast.makeText(this, checked ? "Vibration enabled" : "Vibration disabled", 
                Toast.LENGTH_SHORT).show();
        });

        // Silent mode toggle
        switchSilentMode.setOnCheckedChangeListener((v, checked) -> {
            prefs.edit().putBoolean("silent_mode", checked).apply();
            if (checked) {
                switchVibration.setChecked(true);
                Toast.makeText(this, "Silent mode enabled (vibration only)", Toast.LENGTH_SHORT).show();
            }
        });

        // Select sound
        selectSoundBtn.setOnClickListener(v -> {
            Intent intent = new Intent(RingtoneManager.ACTION_RINGTONE_PICKER);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "Select Alarm Sound");
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true);
            intent.putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false);

            String existingUri = prefs.getString("alarmSoundUri", null);
            if (existingUri != null) {
                intent.putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, Uri.parse(existingUri));
            }

            startActivityForResult(intent, RINGTONE_REQUEST_CODE);
        });

        // Backup data
        btnBackupData.setOnClickListener(v -> backupData());

        // Restore data
        btnRestoreData.setOnClickListener(v -> restoreData());

        // Delete history
        btnDeleteHistory.setOnClickListener(v -> deleteAllHistory());

        // Calendar view
        btnCalendarView.setOnClickListener(v -> {
            Intent intent = new Intent(this, CalendarActivity.class);
            startActivity(intent);
        });

        // App guide
        btnAppGuide.setOnClickListener(v -> showAppGuide());
    }

    private void backupData() {
        Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        intent.putExtra(Intent.EXTRA_TITLE, "medtrack_backup_" + 
            new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(new Date()) + ".json");
        startActivityForResult(intent, 1001);
    }

    private void restoreData() {
        Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType("application/json");
        startActivityForResult(intent, 1002);
    }

    private void deleteAllHistory() {
        new AlertDialog.Builder(this)
            .setTitle("Delete All History?")
            .setMessage("This will permanently delete all medication history. This cannot be undone!")
            .setIcon(android.R.drawable.ic_dialog_alert)
            .setPositiveButton("Delete", (dialog, which) -> {
                DatabaseHelper db = new DatabaseHelper(this);
                db.deleteAllHistory();
                Toast.makeText(this, "✅ All history deleted", Toast.LENGTH_SHORT).show();
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    private void showAppGuide() {
        String guideText = "📱 MedTrack App Guide\n\n" +
                "🏠 HOME:\n" +
                "• View your medicine schedule\n" +
                "• See adherence rate and streak\n" +
                "• Quick access to add medicines\n\n" +
                "💊 ADD MEDICINE:\n" +
                "• Enter medicine name and dosage\n" +
                "• Set reminder time and date\n" +
                "• Choose frequency (Daily, 12 hours, Custom)\n\n" +
                "📋 MY MEDICINES:\n" +
                "• View all scheduled medicines\n" +
                "• Search medicines using search bar\n" +
                "• Long-press to edit a medicine\n" +
                "• Swipe to delete with undo option\n" +
                "• Mark as Taken or Missed\n\n" +
                "📊 STATISTICS:\n" +
                "• View adherence rate and progress\n" +
                "• Track your streak\n" +
                "• See compliance chart\n" +
                "• Export report for doctor\n\n" +
                "📜 HISTORY:\n" +
                "• View all taken/missed medicines\n" +
                "• Filter by status\n" +
                "• Track your medication history\n\n" +
                "⚙️ SETTINGS:\n" +
                "• Toggle dark mode\n" +
                "• Select alarm sound\n" +
                "• View app information\n\n" +
                "🔔 NOTIFICATIONS:\n" +
                "• Alarms ring at exact scheduled time\n" +
                "• Works even when app is closed\n" +
                "• Quick actions: Taken/Missed\n\n" +
                "💡 TIPS:\n" +
                "• Enable notifications for reminders\n" +
                "• Build your streak for motivation\n" +
                "• Export reports before doctor visits\n" +
                "• Use search to find medicines quickly\n\n" +
                "Made with ❤️ by Abdul David Paito";

        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("📖 App Guide")
                .setMessage(guideText)
                .setPositiveButton("Got it!", null)
                .show();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RINGTONE_REQUEST_CODE && resultCode == RESULT_OK && data != null) {
            Uri ringtoneUri = data.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI);

            if (ringtoneUri != null) {
                prefs.edit().putString("alarmSoundUri", ringtoneUri.toString()).apply();
                Toast.makeText(this, "Alarm sound selected!", Toast.LENGTH_SHORT).show();
            } else {
                prefs.edit().putString("alarmSoundUri", "default").apply();
                Toast.makeText(this, "Default alarm sound selected!", Toast.LENGTH_SHORT).show();
            }
        } else if (resultCode == RESULT_OK && data != null) {
            Uri uri = data.getData();
            
            if (requestCode == 1001) {
                // Backup
                performBackup(uri);
            } else if (requestCode == 1002) {
                // Restore
                performRestore(uri);
            }
        }
    }

    private void performBackup(Uri uri) {
        try {
            DatabaseHelper db = new DatabaseHelper(this);
            JSONObject backup = new JSONObject();
            
            // Backup medicines
            JSONArray medicinesArray = new JSONArray();
            List<Medicine> medicines = db.getAllMedicines();
            for (Medicine med : medicines) {
                JSONObject medObj = new JSONObject();
                medObj.put("name", med.getMedicineName());
                medObj.put("dosage", med.getDosage());
                medObj.put("instructions", med.getInstructions());
                medObj.put("time", med.getReminderTime());
                medObj.put("date", med.getReminderDate());
                medObj.put("frequency", med.getFrequency());
                medicinesArray.put(medObj);
            }
            backup.put("medicines", medicinesArray);
            
            // Backup history
            JSONArray historyArray = new JSONArray();
            List<HistoryItem> history = db.getAllHistory();
            for (HistoryItem item : history) {
                JSONObject histObj = new JSONObject();
                histObj.put("medicine", item.getMedicineName());
                histObj.put("date", item.getDate());
                histObj.put("time", item.getTime());
                histObj.put("status", item.getStatus());
                historyArray.put(histObj);
            }
            backup.put("history", historyArray);
            
            // Write to file
            OutputStream outputStream = getContentResolver().openOutputStream(uri);
            outputStream.write(backup.toString(4).getBytes());
            outputStream.close();
            
            Toast.makeText(this, " Backup successful!", Toast.LENGTH_SHORT).show();
            
        } catch (Exception e) {
            Toast.makeText(this, " Backup failed: " + e.getMessage(), 
                Toast.LENGTH_LONG).show();
        }
    }

    private void performRestore(Uri uri) {
        new AlertDialog.Builder(this)
            .setTitle("Restore Data?")
            .setMessage("This will replace all current data. Continue?")
            .setPositiveButton("Restore", (dialog, which) -> {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(uri);
                    BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream));
                    StringBuilder jsonString = new StringBuilder();
                    String line;
                    while ((line = reader.readLine()) != null) {
                        jsonString.append(line);
                    }
                    reader.close();
                    
                    JSONObject backup = new JSONObject(jsonString.toString());
                    DatabaseHelper db = new DatabaseHelper(this);
                    
                    // CRITICAL FIX: Cancel all alarms before clearing data
                    AlarmScheduler alarmScheduler = new AlarmScheduler(this);
                    alarmScheduler.cancelAllAlarms(this);
                    
                    // Clear existing data
                    db.deleteAllMedicines();
                    db.deleteAllHistory();
                    
                    // Restore medicines
                    JSONArray medicinesArray = backup.getJSONArray("medicines");
                    for (int i = 0; i < medicinesArray.length(); i++) {
                        JSONObject medObj = medicinesArray.getJSONObject(i);
                        Medicine med = new Medicine(
                            0,
                            medObj.getString("name"),
                            medObj.getString("dosage"),
                            medObj.getString("instructions"),
                            medObj.getString("time"),
                            medObj.getString("date"),
                            medObj.getString("frequency"),
                            true
                        );
                        long id = db.addMedicine(med);
                        med.setId((int) id);
                        
                        // Reschedule alarms for restored medicine
                        alarmScheduler.scheduleMedicineAlarm(med);
                    }
                    
                    // Restore history
                    JSONArray historyArray = backup.getJSONArray("history");
                    for (int i = 0; i < historyArray.length(); i++) {
                        JSONObject histObj = historyArray.getJSONObject(i);
                        db.addHistory(
                            histObj.getString("medicine"),
                            histObj.getString("date"),
                            histObj.getString("time"),
                            histObj.getString("status")
                        );
                    }
                    
                    Toast.makeText(this, " Restore successful!", Toast.LENGTH_SHORT).show();
                    
                } catch (Exception e) {
                    Toast.makeText(this, " Restore failed: " + e.getMessage(), 
                        Toast.LENGTH_LONG).show();
                }
            })
            .setNegativeButton("Cancel", null)
            .show();
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
