package com.AbdulPaito.medtrack;

import java.text.SimpleDateFormat;
import java.util.Date;

public class HistoryItem {
    private String medicineName;
    private String time;
    private String date;
    private String status;

    // Constructor – automatically sets current date
    public HistoryItem(String medicineName, String time, String status) {
        this.medicineName = medicineName;
        this.time = time;
        this.date = getCurrentDate(); // Automatically sets the date
        this.status = status;
    }

    // Optional constructor if you want to manually set the date
    public HistoryItem(String medicineName, String date, String time, String status) {
        this.medicineName = medicineName;
        this.date = date;
        this.time = time;
        this.status = status;
    }

    // Get current date in yyyy-MM-dd format
    private String getCurrentDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(new Date());
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getTime() {
        return time;
    }

    public String getDate() {
        return date;
    }

    public String getStatus() {
        return status;
    }
}
