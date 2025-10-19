package com.AbdulPaito.medtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText editMedicineName;
    private EditText editDosage;
    private EditText editInstructions;
    private TimePicker timePicker;
    private RadioGroup radioGroupFrequency;
    private Button btnSave;
    private Button btnCancel;
    private DatabaseHelper databaseHelper;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Add Medicine");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        initViews();
        setupButtons();
    }

    private void initViews() {
        editMedicineName = findViewById(R.id.edit_medicine_name);
        editDosage = findViewById(R.id.edit_dosage);
        editInstructions = findViewById(R.id.edit_instructions);
        timePicker = findViewById(R.id.time_picker);
        radioGroupFrequency = findViewById(R.id.radio_group_frequency);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        timePicker.setIs24HourView(false);  // 12-hour format with AM/PM
    }

    private void setupButtons() {
        btnSave.setOnClickListener(view -> saveMedicine());
        btnCancel.setOnClickListener(view -> finish());
    }

    private void saveMedicine() {
        String medicineName = editMedicineName.getText().toString().trim();
        String dosage = editDosage.getText().toString().trim();
        String instructions = editInstructions.getText().toString().trim();

        if (medicineName.isEmpty()) {
            editMedicineName.setError("Please enter medicine name");
            editMedicineName.requestFocus();
            return;
        }

        if (dosage.isEmpty()) {
            editDosage.setError("Please enter dosage");
            editDosage.requestFocus();
            return;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String reminderTime = String.format("%02d:%02d", hour, minute);

        int selectedFrequencyId = radioGroupFrequency.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedFrequencyId);
        String frequency = selectedRadioButton.getText().toString().toLowerCase();

        Medicine medicine = new Medicine(medicineName, dosage, instructions, reminderTime, frequency);

        long id = databaseHelper.addMedicine(medicine);

        if (id > 0) {
            medicine.setId((int) id);

            AlarmScheduler alarmScheduler = new AlarmScheduler(this);
            alarmScheduler.scheduleMedicineAlarm(medicine);

            // Format time with AM/PM for display
            String displayTime = formatTime(hour, minute);
            Toast.makeText(this, "Medicine added! Reminder set for " + displayTime,
                    Toast.LENGTH_LONG).show();
            finish();  // Close activity and return to home
        } else {
            Toast.makeText(this, "Error adding medicine", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Format time to 12-hour with AM/PM
     */
    private String formatTime(int hour, int minute) {
        String amPm = hour >= 12 ? "PM" : "AM";
        int displayHour = hour % 12;
        if (displayHour == 0) displayHour = 12;
        return String.format("%d:%02d %s", displayHour, minute, amPm);
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}