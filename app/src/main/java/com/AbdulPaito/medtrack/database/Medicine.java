package com.AbdulPalto.medtrack.database;

/**
 * Medicine - Represents a single medicine entry
 * This class holds all information about a medicine
 */
public class Medicine {
    private int id;
    private String medicineName;
    private String dosage;
    private String instructions;
    private String reminderTime;  // Format: HH:mm (e.g., "09:30")
    private String frequency;     // daily, 12hours, custom
    private boolean isActive;

    // Constructor for creating new medicine (without ID)
    public Medicine(String medicineName, String dosage, String instructions,
                    String reminderTime, String frequency) {
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.instructions = instructions;
        this.reminderTime = reminderTime;
        this.frequency = frequency;
        this.isActive = true;
    }

    // Constructor for loading from database (with ID)
    public Medicine(int id, String medicineName, String dosage, String instructions,
                    String reminderTime, String frequency, boolean isActive) {
        this.id = id;
        this.medicineName = medicineName;
        this.dosage = dosage;
        this.instructions = instructions;
        this.reminderTime = reminderTime;
        this.frequency = frequency;
        this.isActive = isActive;
    }

    // Getters and Setters
    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getMedicineName() {
        return medicineName;
    }

    public void setMedicineName(String medicineName) {
        this.medicineName = medicineName;
    }

    public String getDosage() {
        return dosage;
    }

    public void setDosage(String dosage) {
        this.dosage = dosage;
    }

    public String getInstructions() {
        return instructions;
    }

    public void setInstructions(String instructions) {
        this.instructions = instructions;
    }

    public String getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(String reminderTime) {
        this.reminderTime = reminderTime;
    }

    public String getFrequency() {
        return frequency;
    }

    public void setFrequency(String frequency) {
        this.frequency = frequency;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }
}