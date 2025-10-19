package com.AbdulPaito.medtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;
import java.util.Calendar;

/**
 * NotificationActionReceiver - Handles notification button actions
 */
public class NotificationActionReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        int medicineId = intent.getIntExtra("medicine_id", -1);
        String medicineName = intent.getStringExtra("medicine_name");

        if (action == null || medicineId == -1) {
            return;
        }

        NotificationHelper notificationHelper = new NotificationHelper(context);

        if ("ACTION_MARK_TAKEN".equals(action)) {
            // Mark as taken
            notificationHelper.cancelNotification(medicineId);

            Toast.makeText(context,
                    medicineName + " marked as taken!",
                    Toast.LENGTH_SHORT).show();

        } else if ("ACTION_SNOOZE".equals(action)) {
            // Snooze for 10 minutes
            notificationHelper.cancelNotification(medicineId);

            String dosage = intent.getStringExtra("dosage");
            scheduleSnoozeNotification(context, medicineId, medicineName, dosage);

            Toast.makeText(context,
                    "Reminder snoozed for 10 minutes",
                    Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Schedule a notification after 10 minutes
     */
    private void scheduleSnoozeNotification(Context context, int medicineId,
                                            String medicineName, String dosage) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);
        intent.putExtra("dosage", dosage);

        android.app.PendingIntent pendingIntent = android.app.PendingIntent.getBroadcast(
                context,
                medicineId * 100,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT |
                        android.app.PendingIntent.FLAG_IMMUTABLE
        );

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) context.getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            Calendar calendar = Calendar.getInstance();
            calendar.add(Calendar.MINUTE, 10);

            alarmManager.setExact(
                    android.app.AlarmManager.RTC_WAKEUP,
                    calendar.getTimeInMillis(),
                    pendingIntent
            );
        }
    }
}