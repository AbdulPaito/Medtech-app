package com.AbdulPaito.medtrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.widget.Toast;
import com.AbdulPaito.medtrack.database.Medicine;
import java.util.Calendar;

/**
 * AlarmScheduler - Schedules reminders for medicines
 */
public class AlarmScheduler {

    private Context context;
    private AlarmManager alarmManager;

    public AlarmScheduler(Context context) {
        this.context = context;
        this.alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
    }

    /**
     * Schedule an alarm for a medicine
     */
    public void scheduleMedicineAlarm(Medicine medicine) {
        // Parse time (format: "HH:mm")
        String[] timeParts = medicine.getReminderTime().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Create calendar for the alarm time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);  // ← ADDED for exact timing

        // If the time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
        }
        
        // Schedule 5-minute reminder notification
        schedule5MinuteReminder(medicine, calendar.getTimeInMillis());

        // Create intent for the alarm
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("medicine_id", medicine.getId());
        intent.putExtra("medicine_name", medicine.getMedicineName());
        intent.putExtra("dosage", medicine.getDosage());

        // Create PendingIntent
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Schedule the alarm with highest precision
        if (alarmManager != null) {
            // Cancel any existing alarm first
            alarmManager.cancel(pendingIntent);  // ← ADDED to prevent duplicates

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                // Use setExactAndAllowWhileIdle for most precise timing
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        calendar.getTimeInMillis(),
                        pendingIntent
                );
            }
        }
    }

    /**
     * Schedule 5-minute reminder notification before main alarm
     */
    private void schedule5MinuteReminder(Medicine medicine, long alarmTimeMillis) {
        // Calculate time 5 minutes before the main alarm
        long reminderTime = alarmTimeMillis - (5 * 60 * 1000); // 5 minutes in milliseconds
        
        // Only schedule if reminder time is in the future
        if (reminderTime > System.currentTimeMillis()) {
            Intent intent = new Intent(context, ReminderNotificationReceiver.class);
            intent.putExtra("medicine_id", medicine.getId());
            intent.putExtra("medicine_name", medicine.getMedicineName());
            intent.putExtra("dosage", medicine.getDosage());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicine.getId() + 10000, // Different request code
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (alarmManager != null) {
                // Cancel any existing 5-min reminder first
                alarmManager.cancel(pendingIntent);
                
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                    );
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                    );
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                    );
                }
            }
        }
    }

    /**
     * Cancel an alarm for a medicine
     */
    public void cancelMedicineAlarm(int medicineId) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicineId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        if (alarmManager != null) {
            alarmManager.cancel(pendingIntent);
            pendingIntent.cancel();
        }
    }

    /**
     * Check if app can schedule exact alarms (Android 12+)
     */
    public boolean canScheduleExactAlarms() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            return alarmManager != null && alarmManager.canScheduleExactAlarms();
        }
        return true;
    }

    /**
     * Request exact alarm permission (Android 12+)
     */
    public void requestExactAlarmPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            if (alarmManager != null && !alarmManager.canScheduleExactAlarms()) {
                Toast.makeText(context,
                        "Please allow exact alarm permission for reminders to work",
                        Toast.LENGTH_LONG).show();

                Intent intent = new Intent(android.provider.Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM);
                intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                context.startActivity(intent);
            }
        }
    }
}