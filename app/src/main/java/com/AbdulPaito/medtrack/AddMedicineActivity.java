package com.AbdulPalto.medtrack;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.TimePicker;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.AbdulPalto.medtrack.database.DatabaseHelper;
import com.AbdulPalto.medtrack.database.Medicine;

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

        timePicker.setIs24HourView(true);
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
            Toast.makeText(this, "Medicine added successfully!", Toast.LENGTH_SHORT).show();
            finish();
        } else {
            Toast.makeText(this, "Error adding medicine", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}