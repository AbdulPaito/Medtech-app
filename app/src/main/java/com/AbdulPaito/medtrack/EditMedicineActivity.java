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

public class EditMedicineActivity extends AppCompatActivity {

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
    private Medicine medicine;
    private int medicineId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_medicine);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("Edit Medicine");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        databaseHelper = new DatabaseHelper(this);
        medicineId = getIntent().getIntExtra("MEDICINE_ID", -1);
        
        initViews();
        loadMedicineData();
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

        timePicker.setIs24HourView(false);
        editDate.setOnClickListener(v -> showDatePicker());
        
        // Change button text for editing
        btnSave.setText("Update Medicine");
    }

    private void loadMedicineData() {
        medicine = databaseHelper.getMedicineById(medicineId);
        if (medicine != null) {
            editMedicineName.setText(medicine.getMedicineName());
            editDosage.setText(medicine.getDosage());
            editInstructions.setText(medicine.getInstructions());
            editDate.setText(medicine.getReminderDate());

            // Set time
            String[] timeParts = medicine.getReminderTime().split(":");
            if (timeParts.length == 2) {
                timePicker.setHour(Integer.parseInt(timeParts[0]));
                timePicker.setMinute(Integer.parseInt(timeParts[1]));
            }

            // Set frequency
            String frequency = medicine.getFrequency();
            if (frequency.equals("Daily")) {
                radioDaily.setChecked(true);
            } else if (frequency.equals("Every 12 hours")) {
                radio12Hours.setChecked(true);
            } else {
                radioCustom.setChecked(true);
                layoutCustomFrequency.setVisibility(View.VISIBLE);
                editCustomFrequency.setText(frequency);
            }
        }
    }

    private void setupButtons() {
        btnSave.setOnClickListener(view -> updateMedicine());
        btnCancel.setOnClickListener(view -> finish());
    }

    private void setupFrequencyListener() {
        radioGroupFrequency.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.radio_custom) {
                layoutCustomFrequency.setVisibility(View.VISIBLE);
                editCustomFrequency.requestFocus();
            } else {
                layoutCustomFrequency.setVisibility(View.GONE);
            }
        });
    }

    private void showDatePicker() {
        final Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int day = calendar.get(Calendar.DAY_OF_MONTH);

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

    private void updateMedicine() {
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

        String frequency;
        if (radioDaily.isChecked()) {
            frequency = "Daily";
        } else if (radio12Hours.isChecked()) {
            frequency = "Every 12 hours";
        } else if (radioCustom.isChecked()) {
            frequency = editCustomFrequency.getText().toString().trim();
            if (frequency.isEmpty()) {
                editCustomFrequency.setError("Please enter custom frequency");
                editCustomFrequency.requestFocus();
                return;
            }
        } else {
            frequency = "Daily";
        }

        medicine.setMedicineName(medicineName);
        medicine.setDosage(dosage);
        medicine.setInstructions(instructions);
        medicine.setReminderTime(reminderTime);
        medicine.setReminderDate(selectedDate);
        medicine.setFrequency(frequency);

        int rowsAffected = databaseHelper.updateMedicine(medicine);

        if (rowsAffected > 0) {
            AlarmScheduler alarmScheduler = new AlarmScheduler(this);
            alarmScheduler.scheduleMedicineAlarm(medicine);

            Toast.makeText(this, "Medicine updated successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error updating medicine", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
