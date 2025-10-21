package com.AbdulPaito.medtrack;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;
import com.google.android.material.textfield.TextInputLayout;
import com.google.android.material.textfield.TextInputEditText;
import java.util.Calendar;

public class AddMedicineActivity extends AppCompatActivity {

    private EditText editMedicineName;
    private EditText editDosage;
    private EditText editInstructions;
    private EditText editDate;
    private TimePicker timePicker;
    private RadioGroup radioGroupFrequency;
    private RadioButton radioDaily;
    private RadioButton radio12Hours;
    private RadioButton radioCustom;
    private TextInputLayout layoutCustomFrequency;
    private TextInputEditText editCustomFrequency;
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
        setupFrequencyListener();
    }

    private void initViews() {
        editMedicineName = findViewById(R.id.edit_medicine_name);
        editDosage = findViewById(R.id.edit_dosage);
        editInstructions = findViewById(R.id.edit_instructions);
        editDate = findViewById(R.id.edit_date);
        timePicker = findViewById(R.id.time_picker);
        radioGroupFrequency = findViewById(R.id.radio_group_frequency);
        radioDaily = findViewById(R.id.radio_daily);
        radio12Hours = findViewById(R.id.radio_12hours);
        radioCustom = findViewById(R.id.radio_custom);
        layoutCustomFrequency = findViewById(R.id.layout_custom_frequency);
        editCustomFrequency = findViewById(R.id.edit_custom_frequency);
        btnSave = findViewById(R.id.btn_save);
        btnCancel = findViewById(R.id.btn_cancel);

        timePicker.setIs24HourView(false); // 12-hour format with AM/PM

        // Open date picker when clicking on the date field
        editDate.setOnClickListener(v -> showDatePicker());
    }

    private void setupButtons() {
        btnSave.setOnClickListener(view -> saveMedicine());
        btnCancel.setOnClickListener(view -> finish());
    }

    // ✅ NEW: Show/hide custom frequency input
    private void setupFrequencyListener() {
        radioGroupFrequency.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_custom) {
                // Show custom input when Custom is selected
                layoutCustomFrequency.setVisibility(View.VISIBLE);
                editCustomFrequency.requestFocus();
            } else {
                // Hide custom input for Daily or Every 12 hours
                layoutCustomFrequency.setVisibility(View.GONE);
            }
        });
    }

    private void showDatePicker() {
        // Get current date
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

        // Show DatePickerDialog
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

    // ✅ UPDATED: Handle custom frequency
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

        // ✅ Get frequency based on selection
        String frequency;
        if (radioDaily.isChecked()) {
            frequency = "Daily";
        } else if (radio12Hours.isChecked()) {
            frequency = "Every 12 hours";
        } else if (radioCustom.isChecked()) {
            // Get custom frequency from input
            frequency = editCustomFrequency.getText().toString().trim();

            if (frequency.isEmpty()) {
                editCustomFrequency.setError("Please enter custom frequency");
                editCustomFrequency.requestFocus();
                return;
            }
        } else {
            frequency = "Daily"; // default
        }

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
                    "Medicine added for " + selectedDate + " at " + displayTime + " (" + frequency + ")",
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