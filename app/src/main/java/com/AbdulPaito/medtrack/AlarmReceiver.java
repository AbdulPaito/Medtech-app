package com.AbdulPaito.medtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

/**
 * AlarmReceiver - Receives alarm broadcasts and shows notifications
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get medicine details from intent
        int medicineId = intent.getIntExtra("medicine_id", -1);
        String medicineName = intent.getStringExtra("medicine_name");
        String dosage = intent.getStringExtra("dosage");

        if (medicineId != -1 && medicineName != null) {
            // Show notification
            NotificationHelper notificationHelper = new NotificationHelper(context);
            notificationHelper.showMedicineReminder(medicineId, medicineName, dosage);
        }
    }
}