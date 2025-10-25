package com.AbdulPaito.medtrack;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;
import com.AbdulPaito.medtrack.database.Medicine;
import java.util.Calendar;

/**
 * AlarmScheduler - Schedules reminders for medicines
 */
public class AlarmScheduler {

    private static final String TAG = "AlarmScheduler";
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

        // Create intent for the alarm with explicit action
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.AbdulPaito.medtrack.ALARM_TRIGGER");
        intent.putExtra("medicine_id", medicine.getId());
        intent.putExtra("medicine_name", medicine.getMedicineName());
        intent.putExtra("dosage", medicine.getDosage());

        // Create PendingIntent with more flags for better reliability
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

            Log.d(TAG, "📅 Scheduling alarm for: " + medicine.getMedicineName());
            Log.d(TAG, "⏰ Time: " + medicine.getReminderTime() + " (" + calendar.getTimeInMillis() + ")");
            Log.d(TAG, "🆔 Medicine ID: " + medicine.getId());

            // Schedule multiple alarms for better reliability
            scheduleMultipleAlarms(medicine, calendar.getTimeInMillis());
            
        } else {
            Log.e(TAG, "❌ AlarmManager is null!");
        }
    }
    
    private void scheduleMultipleAlarms(Medicine medicine, long alarmTime) {
        try {
            Intent intent = new Intent(context, AlarmReceiver.class);
            intent.setAction("com.AbdulPaito.medtrack.ALARM_TRIGGER");
            intent.putExtra("medicine_id", medicine.getId());
            intent.putExtra("medicine_name", medicine.getMedicineName());
            intent.putExtra("dosage", medicine.getDosage());

            // Main alarm - schedule ONLY at exact time
            PendingIntent mainIntent = PendingIntent.getBroadcast(
                    context,
                    medicine.getId(),
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );

            // Schedule main alarm at EXACT time (no backup alarms)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        mainIntent
                );
                Log.d(TAG, "✅ Alarm scheduled at EXACT time using setExactAndAllowWhileIdle");
            } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                alarmManager.setExact(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        mainIntent
                );
                Log.d(TAG, "✅ Alarm scheduled at EXACT time using setExact");
            } else {
                alarmManager.set(
                        AlarmManager.RTC_WAKEUP,
                        alarmTime,
                        mainIntent
                );
                Log.d(TAG, "✅ Alarm scheduled at EXACT time using set");
            }
            
            // Log exact alarm time for debugging
            java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss", java.util.Locale.getDefault());
            Log.d(TAG, "🕐 Alarm will ring at: " + sdf.format(new java.util.Date(alarmTime)));
        } catch (Exception e) {
            Log.e(TAG, "❌ Error scheduling alarm", e);
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
            intent.setAction("com.AbdulPaito.medtrack.REMINDER_TRIGGER");
            intent.putExtra("medicine_id", medicine.getId());
            intent.putExtra("medicine_name", medicine.getMedicineName());
            intent.putExtra("dosage", medicine.getDosage());
            
            PendingIntent pendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicine.getId() + 30000, // Different request code (changed from 10000 to avoid conflict)
                    intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            
            if (alarmManager != null) {
                // Cancel any existing 5-min reminder first
                alarmManager.cancel(pendingIntent);
                
                // Use setExactAndAllowWhileIdle for reliable wake-up even in Doze mode
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                    alarmManager.setExactAndAllowWhileIdle(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                    );
                    Log.d(TAG, "✅ 5-min reminder scheduled using setExactAndAllowWhileIdle (wakes from Doze)");
                } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
                    alarmManager.setExact(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                    );
                    Log.d(TAG, "✅ 5-min reminder scheduled using setExact");
                } else {
                    alarmManager.set(
                            AlarmManager.RTC_WAKEUP,
                            reminderTime,
                            pendingIntent
                    );
                    Log.d(TAG, "✅ 5-min reminder scheduled using set");
                }
            }
        }
    }

    /**
     * Cancel an alarm for a medicine
     */
    public void cancelMedicineAlarm(int medicineId) {
        if (alarmManager != null) {
            // Cancel main alarm
            Intent mainIntent = new Intent(context, AlarmReceiver.class);
            mainIntent.setAction("com.AbdulPaito.medtrack.ALARM_TRIGGER");
            PendingIntent mainPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicineId,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(mainPendingIntent);
            mainPendingIntent.cancel();
            
            // Cancel old backup alarm (if it exists from previous version)
            PendingIntent backupPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicineId + 10000,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(backupPendingIntent);
            backupPendingIntent.cancel();
            
            // Cancel 5-minute reminder
            Intent reminderIntent = new Intent(context, ReminderNotificationReceiver.class);
            reminderIntent.setAction("com.AbdulPaito.medtrack.REMINDER_TRIGGER");
            PendingIntent reminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicineId + 30000,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(reminderPendingIntent);
            reminderPendingIntent.cancel();
            
            // Cancel auto-repeat alarm (from AlarmSoundService)
            PendingIntent repeatPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicineId + 20000,
                    mainIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(repeatPendingIntent);
            repeatPendingIntent.cancel();
            
            Log.d(TAG, "✅ Cancelled all alarms for medicine ID: " + medicineId);
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