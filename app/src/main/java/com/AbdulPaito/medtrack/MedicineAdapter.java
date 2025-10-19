package com.AbdulPaito.medtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;

import java.util.List;

public class MedicineAdapter extends RecyclerView.Adapter<MedicineAdapter.MedicineViewHolder> {

    private List<Medicine> medicineList;
    private OnMedicineClickListener clickListener;
    private OnDeleteClickListener deleteListener;

    // Interfaces for click events (optional)
    public interface OnMedicineClickListener {
        void onMedicineClick(Medicine medicine);
    }

    public interface OnDeleteClickListener {
        void onDeleteClick(Medicine medicine, int position);
    }

    public MedicineAdapter(List<Medicine> medicineList,
                           OnMedicineClickListener clickListener,
                           OnDeleteClickListener deleteListener) {
        this.medicineList = medicineList;
        this.clickListener = clickListener;
        this.deleteListener = deleteListener;
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

        holder.textMedicineName.setText(medicine.getMedicineName());
        holder.textDosage.setText(medicine.getDosage());
        holder.textTime.setText("⏰ " + formatTime(medicine.getReminderTime()));

        // Show instructions if available
        if (medicine.getInstructions() != null && !medicine.getInstructions().isEmpty()) {
            holder.textInstructions.setVisibility(View.VISIBLE);
            holder.textInstructions.setText(medicine.getInstructions());
        } else {
            holder.textInstructions.setVisibility(View.GONE);
        }

        // ✅ Mark as Taken Button
        holder.btnMarkTaken.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Mark as Taken")
                    .setMessage("Did you take " + medicine.getMedicineName() + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Yes", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(v.getContext());

                        // 1) Add to history
                        dbHelper.addHistory(
                                medicine.getMedicineName(),
                                medicine.getReminderTime(),
                                "Taken"
                        );

                        // 2) Delete from medicines table
                        dbHelper.deleteMedicine(medicine.getId());

                        // 3) Remove from list (smooth)
                        v.postDelayed(() -> {
                            int pos = holder.getAdapterPosition();
                            if (pos != RecyclerView.NO_POSITION) {
                                medicineList.remove(pos);
                                notifyItemRemoved(pos);
                            }
                        }, 500);
                    })
                    .show();
        });

        // 🗑️ Delete button (confirmation dialog)
        holder.btnDelete.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(v.getContext())
                    .setTitle("Delete Medicine")
                    .setMessage("Are you sure you want to delete " + medicine.getMedicineName() + "?")
                    .setNegativeButton("Cancel", null)
                    .setPositiveButton("Delete", (dialog, which) -> {
                        DatabaseHelper dbHelper = new DatabaseHelper(v.getContext());
                        dbHelper.deleteMedicine(medicine.getId());

                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            medicineList.remove(pos);
                            notifyItemRemoved(pos);
                        }

                        android.widget.Toast.makeText(v.getContext(),
                                "Medicine deleted", android.widget.Toast.LENGTH_SHORT).show();

                        if (deleteListener != null) {
                            deleteListener.onDeleteClick(medicine, pos);
                        }
                    })
                    .show();
        });
    }

    @Override
    public int getItemCount() {
        return medicineList.size();
    }

    /**
     * ✅ Helper method: remove an item when swiped (for swipe-to-delete)
     */
    public Medicine removeItem(int position) {
        Medicine removed = medicineList.get(position);
        medicineList.remove(position);
        notifyItemRemoved(position);
        return removed;
    }

    /**
     * ✅ Helper method: restore an item if user taps "Undo"
     */
    public void restoreItem(Medicine medicine, int position) {
        medicineList.add(position, medicine);
        notifyItemInserted(position);
    }

    /**
     * Format time to 12-hour with AM/PM
     */
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

    static class MedicineViewHolder extends RecyclerView.ViewHolder {
        TextView textMedicineName;
        TextView textDosage;
        TextView textTime;
        TextView textInstructions;
        Button btnMarkTaken;
        ImageButton btnDelete;

        MedicineViewHolder(@NonNull View itemView) {
            super(itemView);
            textMedicineName = itemView.findViewById(R.id.text_medicine_name);
            textDosage = itemView.findViewById(R.id.text_dosage);
            textTime = itemView.findViewById(R.id.text_time);
            textInstructions = itemView.findViewById(R.id.text_instructions);
            btnMarkTaken = itemView.findViewById(R.id.btn_mark_taken);
            btnDelete = itemView.findViewById(R.id.btn_delete);
        }
    }
}
