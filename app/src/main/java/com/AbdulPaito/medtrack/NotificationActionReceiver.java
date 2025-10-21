package com.AbdulPaito.medtrack;

import android.app.AlarmManager;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import com.AbdulPaito.medtrack.database.DatabaseHelper;

/**
 * NotificationActionReceiver - Handles notification button actions
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int medicineId = intent.getIntExtra("medicine_id", -1);
        String medicineName = intent.getStringExtra("medicine_name");
        String dosage = intent.getStringExtra("dosage");

        if (action == null || medicineId == -1) return;

        // Stop alarm sound service
        Intent stopServiceIntent = new Intent(context, AlarmSoundService.class);
        context.stopService(stopServiceIntent);

        // Cancel all scheduled repeats for this medicine
        cancelAutoRepeat(context, medicineId);

        switch (action) {
            case "ACTION_MARK_TAKEN":
                markAsTaken(context, medicineId, medicineName);
                break;

            case "ACTION_SNOOZE":
                snoozeAlarm(context, medicineId, medicineName, dosage);
                break;
        }

        // Dismiss notification
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        if (notificationManager != null) {
            notificationManager.cancel(medicineId);
        }
    }

    private void markAsTaken(Context context, int medicineId, String medicineName) {
        DatabaseHelper dbHelper = new DatabaseHelper(context);

        // Get current time
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        String currentTime = sdf.format(new java.util.Date());

        // Add to history as "Taken"
        dbHelper.addHistory(medicineName, currentTime, "Taken");

        // Delete from medicines table
        dbHelper.deleteMedicine(medicineId);

        Toast.makeText(context, "✅ " + medicineName + " marked as taken!", Toast.LENGTH_SHORT).show();
    }

    private void snoozeAlarm(Context context, int medicineId, String medicineName, String dosage) {
        // Schedule alarm for 5 minutes later
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);
        intent.putExtra("dosage", dosage);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            long triggerTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
        }

        Toast.makeText(context, "⏰ Snoozed for 5 minutes", Toast.LENGTH_SHORT).show();
    }

    private void cancelAutoRepeat(Context context, int medicineId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId + 10000, // Same request code used for auto-repeat
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
        }
    }
}