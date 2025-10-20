package com.AbdulPaito.medtrack;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryAdapter extends RecyclerView.Adapter<HistoryAdapter.ViewHolder> {

    private List<HistoryItem> historyList;

    public HistoryAdapter(List<HistoryItem> historyList) {
        this.historyList = historyList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        HistoryItem item = historyList.get(position);

        // Show medicine name with label
        holder.textMedicineName.setText("Medicine Name: " + item.getMedicineName());

        // Convert 24-hour time (if applicable) to 12-hour format with AM/PM
        String formattedTime = formatTo12Hour(item.getTime());
        holder.textMedicineTime.setText("Taken at: " + formattedTime);

        // Show date (e.g., Oct 20, 2025)
        String formattedDate = formatDate(item.getDate());
        holder.textMedicineDate.setText("Date: " + formattedDate);

        // Show status
        holder.textStatus.setText("Status: " + item.getStatus());
    }

    @Override
    public int getItemCount() {
        return historyList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textMedicineName, textMedicineTime, textMedicineDate, textStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textMedicineName = itemView.findViewById(R.id.text_medicine_name);
            textMedicineTime = itemView.findViewById(R.id.text_medicine_time);
            textMedicineDate = itemView.findViewById(R.id.text_medicine_date);
            textStatus = itemView.findViewById(R.id.text_status);
        }
    }

    // Helper method to format 24-hour time (e.g. "18:19") to 12-hour (e.g. "6:19 PM")
    private String formatTo12Hour(String time) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("h:mm a", Locale.getDefault());
            Date date = inputFormat.parse(time);
            return outputFormat.format(date);
        } catch (ParseException e) {
            return time; // fallback if parsing fails
        }
    }

    // Helper method to format date (e.g. "2025-10-20") to readable form (e.g. "Oct 20, 2025")
    private String formatDate(String date) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
            Date parsedDate = inputFormat.parse(date);
            return outputFormat.format(parsedDate);
        } catch (ParseException e) {
            return date; // fallback if parsing fails
        }
    }
}
