package com.AbdulPaito.medtrack;

public class HistoryItem {
    private String medicineName;
    private String time;
    private String status;

    public HistoryItem(String medicineName, String time, String status) {
        this.medicineName = medicineName;
        this.time = time;
        this.status = status;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public String getTime() {
        return time;
    }

    public String getStatus() {
        return status;
    }
}
