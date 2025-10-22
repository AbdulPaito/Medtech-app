package com.AbdulPaito.medtrack;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.dialog.MaterialAlertDialogBuilder;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private final Context context;
    private final List<Medicine> medicineList;

    public MedicineAdapter(Context context, List<Medicine> medicineList) {
        this.context = context;
        this.medicineList = medicineList;
    }

    @NonNull
    @Override
    public MedicineViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_medicine, parent, false);
        return new MedicineViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MedicineViewHolder holder, int position) {
        Medicine medicine = medicineList.get(position);

        // 🩷 Display details with icons and bold text
        holder.textMedicineName.setText(medicine.getMedicineName());
        holder.textDosage.setText(medicine.getDosage());
        holder.textTime.setText(formatTime(medicine.getReminderTime()));
        holder.textDate.setText(medicine.getReminderDate());
        holder.textFrequency.setText(medicine.getFrequency());
        holder.textInstructions.setText(medicine.getInstructions());

        // Long click to edit
        holder.itemView.setOnLongClickListener(v -> {
            new MaterialAlertDialogBuilder(context)
                    .setTitle("Edit Medicine")
                    .setMessage("Do you want to edit " + medicine.getMedicineName() + "?")
                    .setPositiveButton("Edit", (dialog, which) -> {
                        android.content.Intent intent = new android.content.Intent(context, EditMedicineActivity.class);
                        intent.putExtra("MEDICINE_ID", medicine.getId());
                        context.startActivity(intent);
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
            return true;
        });

        // ✅ Mark as Taken
        holder.btnMarkTaken.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle("✅ Mark as Taken")
                    .setMessage("Did you take " + medicine.getMedicineName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(context);
                        dbHelper.addHistory(medicine.getMedicineName(),
                                medicine.getReminderTime(),
                                "Taken");

                        dbHelper.deleteMedicine(medicine.getId());

                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            medicineList.remove(pos);
                            notifyItemRemoved(pos);
                        }

                        Toast.makeText(context, "Marked as Taken ✅", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_background))
                    .show();
        });

        // ❌ Mark as Missed
        holder.btnMarkMissed.setOnClickListener(v -> {
            new MaterialAlertDialogBuilder(context, com.google.android.material.R.style.ThemeOverlay_MaterialComponents_MaterialAlertDialog)
                    .setTitle("❌ Mark as Missed")
                    .setMessage("Did you miss " + medicine.getMedicineName() + "?")
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(context);
                        dbHelper.addHistory(medicine.getMedicineName(),
                                medicine.getReminderTime(),
                                "Missed");

                        dbHelper.deleteMedicine(medicine.getId());

                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            medicineList.remove(pos);
                            notifyItemRemoved(pos);
                        }

                        Toast.makeText(context, "Marked as Missed ❌", Toast.LENGTH_SHORT).show();
                    })
                    .setNegativeButton("Cancel", (dialog, which) -> dialog.dismiss())
                    .setBackground(ContextCompat.getDrawable(context, R.drawable.dialog_background))
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    // Update list for search functionality
    public void updateList(List<Medicine> newList) {
        medicineList.clear();
        medicineList.addAll(newList);
        notifyDataSetChanged();
    }

    // 🧩 ViewHolder class
    static class MedicineViewHolder extends RecyclerView.ViewHolder {
        TextView textMedicineName, textDosage, textTime, textDate, textFrequency, textInstructions;
        MaterialButton btnMarkTaken, btnMarkMissed;

        MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            textMedicineName = itemView.findViewById(R.id.text_medicine_name);
            textDosage = itemView.findViewById(R.id.text_dosage);
            textTime = itemView.findViewById(R.id.text_time);
            textDate = itemView.findViewById(R.id.text_date);
            textFrequency = itemView.findViewById(R.id.text_frequency);
            textInstructions = itemView.findViewById(R.id.text_instructions);
            btnMarkTaken = itemView.findViewById(R.id.btn_mark_taken);
            btnMarkMissed = itemView.findViewById(R.id.btn_mark_missed);
        }
    }

    // 🕓 Convert 24-hour time (e.g., "14:30") → 12-hour format ("2:30 PM")
    private String formatTime(String time24) {
        try {
            String[] parts = time24.split(":");
            int hour = Integer.parseInt(parts[0]);
            int minute = Integer.parseInt(parts[1]);
            String amPm = hour >= 12 ? "PM" : "AM";
            int displayHour = hour % 12;
            if (displayHour == 0) displayHour = 12;
            return String.format("%d:%02d %s", displayHour, minute, amPm);
        } catch (Exception e) {
            return time24;
        }
    }
}
