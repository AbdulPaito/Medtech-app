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
 * AlarmScheduler - Completely rewritten for reliable alarm scheduling
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
     * Schedule an alarm for a medicine - NEW IMPLEMENTATION
     */
    public void scheduleMedicineAlarm(Medicine medicine) {
        Log.d(TAG, "🚀 Starting NEW alarm scheduling for: " + medicine.getMedicineName());
        
        // Parse time (format: "HH:mm")
        String[] timeParts = medicine.getReminderTime().split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Create calendar for the alarm time
        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, hour);
        calendar.set(Calendar.MINUTE, minute);
        calendar.set(Calendar.SECOND, 0);
        calendar.set(Calendar.MILLISECOND, 0);

        // If the time has passed today, schedule for tomorrow
        if (calendar.getTimeInMillis() <= System.currentTimeMillis()) {
            calendar.add(Calendar.DAY_OF_MONTH, 1);
            Log.d(TAG, "⏰ Time passed today, scheduling for tomorrow");
        }
        
        long alarmTime = calendar.getTimeInMillis();
        long currentTime = System.currentTimeMillis();
        long timeDifference = alarmTime - currentTime;
        long minutesDifference = timeDifference / (60 * 1000);
        
        // Detailed time logging for debugging
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault());
        Log.d(TAG, "⏰ CURRENT PHONE TIME: " + sdf.format(new java.util.Date(currentTime)));
        Log.d(TAG, "⏰ ALARM SCHEDULED FOR: " + medicine.getReminderTime());
        Log.d(TAG, "⏰ ALARM TIME (EXACT): " + sdf.format(new java.util.Date(alarmTime)));
        Log.d(TAG, "⏰ TIME DIFFERENCE: " + minutesDifference + " minutes (" + timeDifference + " milliseconds)");
        Log.d(TAG, "⏰ PHONE TIME SOURCE: System.currentTimeMillis()");

        if (alarmManager != null) {
            // Cancel any existing alarms first
            cancelAllAlarmsForMedicine(medicine.getId());
            
            // Schedule 5-minute reminder if there's enough time
            if (minutesDifference >= 6) { // At least 6 minutes to allow 5-minute reminder
                schedule5MinuteReminder(medicine, alarmTime);
                Log.d(TAG, "✅ 5-minute reminder scheduled");
            } else {
                Log.d(TAG, "⚠️ Not enough time for 5-minute reminder (only " + minutesDifference + " minutes)");
            }
            
            // Schedule main alarm
            scheduleMainAlarm(medicine, alarmTime);
            
            Log.d(TAG, "✅ All alarms scheduled successfully for: " + medicine.getMedicineName());
        } else {
            Log.e(TAG, "❌ AlarmManager is null!");
        }
    }
    
    /**
     * Schedule 5-minute reminder - NEW IMPLEMENTATION
     */
    private void schedule5MinuteReminder(Medicine medicine, long alarmTime) {
        // Calculate exactly 5 minutes before
        long reminderTime = alarmTime - (5 * 60 * 1000);
        
        // Detailed logging for 5-minute reminder
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault());
        Log.d(TAG, "⏰ MAIN ALARM TIME: " + sdf.format(new java.util.Date(alarmTime)));
        Log.d(TAG, "⏰ 5-MIN REMINDER TIME: " + sdf.format(new java.util.Date(reminderTime)));
        Log.d(TAG, "⏰ EXACTLY 5 MINUTES BEFORE: " + ((alarmTime - reminderTime) / (60 * 1000)) + " minutes");
        
        Intent intent = new Intent(context, ReminderNotificationReceiver.class);
        intent.setAction("com.AbdulPaito.medtrack.REMINDER_TRIGGER");
        intent.putExtra("medicine_id", medicine.getId());
        intent.putExtra("medicine_name", medicine.getMedicineName());
        intent.putExtra("dosage", medicine.getDosage());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.getId() + 50000, // Unique request code for reminders
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule with highest precision
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
            );
            Log.d(TAG, "✅ 5-minute reminder scheduled with setExactAndAllowWhileIdle");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
            );
            Log.d(TAG, "✅ 5-minute reminder scheduled with setExact");
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    reminderTime,
                    pendingIntent
            );
            Log.d(TAG, "✅ 5-minute reminder scheduled with set");
        }
    }
    
    /**
     * Schedule main alarm - NEW IMPLEMENTATION
     */
    private void scheduleMainAlarm(Medicine medicine, long alarmTime) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.setAction("com.AbdulPaito.medtrack.ALARM_TRIGGER");
        intent.putExtra("medicine_id", medicine.getId());
        intent.putExtra("medicine_name", medicine.getMedicineName());
        intent.putExtra("dosage", medicine.getDosage());
        
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                medicine.getId(), // Main alarm uses medicine ID
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Schedule with highest precision
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
            );
            Log.d(TAG, "✅ Main alarm scheduled with setExactAndAllowWhileIdle");
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            alarmManager.setExact(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
            );
            Log.d(TAG, "✅ Main alarm scheduled with setExact");
        } else {
            alarmManager.set(
                    AlarmManager.RTC_WAKEUP,
                    alarmTime,
                    pendingIntent
            );
            Log.d(TAG, "✅ Main alarm scheduled with set");
        }
    }
    
    /**
     * Cancel all alarms for a medicine - NEW IMPLEMENTATION
     */
    public void cancelMedicineAlarm(int medicineId) {
        Log.d(TAG, "🗑️ Cancelling all alarms for medicine ID: " + medicineId);
        cancelAllAlarmsForMedicine(medicineId);
        Log.d(TAG, "✅ All alarms cancelled for medicine ID: " + medicineId);
    }
    
    /**
     * Cancel all alarms for a specific medicine
     */
    private void cancelAllAlarmsForMedicine(int medicineId) {
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
            
            // Cancel 5-minute reminder
            Intent reminderIntent = new Intent(context, ReminderNotificationReceiver.class);
            reminderIntent.setAction("com.AbdulPaito.medtrack.REMINDER_TRIGGER");
            PendingIntent reminderPendingIntent = PendingIntent.getBroadcast(
                    context,
                    medicineId + 50000,
                    reminderIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
            );
            alarmManager.cancel(reminderPendingIntent);
            reminderPendingIntent.cancel();
            
            Log.d(TAG, "✅ Cancelled main alarm and 5-minute reminder for medicine ID: " + medicineId);
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