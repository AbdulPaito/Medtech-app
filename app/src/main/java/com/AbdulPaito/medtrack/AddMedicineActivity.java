package com.AbdulPaito.medtrack;

import android.app.DatePickerDialog;
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
import java.util.Calendar;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText editMedicineName;
    private EditText editDosage;
    private EditText editInstructions;
    private EditText editDate;  // ✅ added for date
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
        editDate = findViewById(R.id.edit_date); // ✅ initialize date input
        timePicker = findViewById(R.id.time_picker);
        radioGroupFrequency = findViewById(R.id.radio_group_frequency);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        timePicker.setIs24HourView(false); // 12-hour format with AM/PM

        // ✅ Open date picker when clicking on the date field
        editDate.setOnClickListener(v -> showDatePicker());
    }

    private void setupButtons() {
        btnSave.setOnClickListener(view -> saveMedicine());
        btnCancel.setOnClickListener(view -> finish());
    }

    private void showDatePicker() {
        // ✅ Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // ✅ Show DatePickerDialog
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, selectedYear, selectedMonth, selectedDay) -> {
                    String formattedDate = String.format("%02d/%02d/%d",
                            selectedMonth + 1, selectedDay, selectedYear);
                    editDate.setText(formattedDate);
                },
                year, month, day
        );

        datePickerDialog.show();
    }

    // ✅ FIXED VERSION (saves medicine with date)
    private void saveMedicine() {
        String medicineName = editMedicineName.getText().toString().trim();
        String dosage = editDosage.getText().toString().trim();
        String instructions = editInstructions.getText().toString().trim();
        String selectedDate = editDate.getText().toString().trim();

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

        if (selectedDate.isEmpty()) {
            editDate.setError("Please select a date");
            editDate.requestFocus();
            return;
        }

        int hour = timePicker.getHour();
        int minute = timePicker.getMinute();
        String reminderTime = String.format("%02d:%02d", hour, minute);

        int selectedFrequencyId = radioGroupFrequency.getCheckedRadioButtonId();
        RadioButton selectedRadioButton = findViewById(selectedFrequencyId);
        String frequency = selectedRadioButton.getText().toString().toLowerCase();

        // ✅ Correct order
        Medicine medicine = new Medicine(
                medicineName,
                dosage,
                instructions,
                reminderTime,
                selectedDate,
                frequency
        );

        long id = databaseHelper.addMedicine(medicine);

        if (id > 0) {
            medicine.setId((int) id);

            AlarmScheduler alarmScheduler = new AlarmScheduler(this);
            alarmScheduler.scheduleMedicineAlarm(medicine);

            String displayTime = formatTime(hour, minute);
            Toast.makeText(this,
                    "Medicine added for " + selectedDate + " at " + displayTime,
                    Toast.LENGTH_LONG).show();
            finish();
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
