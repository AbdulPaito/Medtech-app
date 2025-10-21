package com.AbdulPaito.medtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;

/**
 * AlarmReceiver - Receives alarm broadcasts and triggers alarm service
 */
public class AlarmReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        // Get medicine details from intent
        int medicineId = intent.getIntExtra("medicine_id", -1);
        String medicineName = intent.getStringExtra("medicine_name");
        String dosage = intent.getStringExtra("dosage");

        if (medicineId != -1 && medicineName != null) {
            // Start alarm service for continuous ringing
            Intent serviceIntent = new Intent(context, AlarmSoundService.class);
            serviceIntent.putExtra("medicine_id", medicineId);
            serviceIntent.putExtra("medicine_name", medicineName);
            serviceIntent.putExtra("dosage", dosage);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
            } else {
                context.startService(serviceIntent);
            }
        }
    }
}