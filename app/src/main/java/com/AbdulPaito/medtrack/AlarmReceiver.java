package com.AbdulPaito.medtrack;

import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import android.os.PowerManager;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * AlarmReceiver - Receives alarm broadcasts and triggers alarm service
 * Uses WakeLock to ensure alarm fires even when device is in deep sleep
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "⏰ ALARM RECEIVED! Time: " + System.currentTimeMillis());
        
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        PowerManager.WakeLock screenWakeLock = null;
        
        try {
            // Acquire FULL WakeLock to wake device and turn on screen
            wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "MedTrack::AlarmFullWakeLock"
            );
            
            // Acquire wake lock for 10 minutes (longer for better reliability)
            wakeLock.acquire(10 * 60 * 1000L);
            
            // Additional screen wake lock for maximum reliability
            screenWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "MedTrack::ScreenWakeLock"
            );
            screenWakeLock.acquire(10 * 60 * 1000L);
            
            // Get medicine details from intent
            int medicineId = intent.getIntExtra("medicine_id", -1);
            String medicineName = intent.getStringExtra("medicine_name");
            String dosage = intent.getStringExtra("dosage");

            Log.d(TAG, "Medicine: " + medicineName + ", ID: " + medicineId);

            if (medicineId != -1 && medicineName != null) {
                // Check if this is a duplicate alarm (from backup alarms)
                if (isDuplicateAlarm(context, medicineId)) {
                    Log.d(TAG, "Duplicate alarm detected, ignoring");
                    return;
                }
                
                // Mark this alarm as processed
                markAlarmAsProcessed(context, medicineId);
                
                // Show immediate notification first
                showAlarmNotification(context, medicineName, dosage, medicineId);
                
                // Start alarm service for continuous ringing
                Intent serviceIntent = new Intent(context, AlarmSoundService.class);
                serviceIntent.setAction("com.AbdulPaito.medtrack.ALARM_SERVICE");
                serviceIntent.putExtra("medicine_id", medicineId);
                serviceIntent.putExtra("medicine_name", medicineName);
                serviceIntent.putExtra("dosage", dosage);

                try {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        context.startForegroundService(serviceIntent);
                        Log.d(TAG, "✅ Started foreground service");
                    } else {
                        context.startService(serviceIntent);
                        Log.d(TAG, "✅ Started service");
                    }
                } catch (Exception e) {
                    Log.e(TAG, "❌ Failed to start service", e);
                    // Try again after a short delay
                    android.os.Handler handler = new android.os.Handler(context.getMainLooper());
                    handler.postDelayed(() -> {
                        try {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                                context.startForegroundService(serviceIntent);
                            } else {
                                context.startService(serviceIntent);
                            }
                            Log.d(TAG, "✅ Service started on retry");
                        } catch (Exception ex) {
                            Log.e(TAG, "❌ Service start failed on retry", ex);
                        }
                    }, 1000);
                }
                
                // Also try to wake up the device and show notification
                wakeUpDevice(context);
                
            } else {
                Log.e(TAG, "❌ Invalid medicine data: ID=" + medicineId + ", Name=" + medicineName);
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in AlarmReceiver", e);
        } finally {
            // Release wake locks after a delay to ensure service has started
            if (wakeLock != null && wakeLock.isHeld()) {
                final PowerManager.WakeLock finalWakeLock = wakeLock;
                final PowerManager.WakeLock finalScreenWakeLock = screenWakeLock;
                android.os.Handler handler = new android.os.Handler(context.getMainLooper());
                handler.postDelayed(() -> {
                    if (finalWakeLock != null && finalWakeLock.isHeld()) {
                        finalWakeLock.release();
                        Log.d(TAG, "✅ Full WakeLock released");
                    }
                    if (finalScreenWakeLock != null && finalScreenWakeLock.isHeld()) {
                        finalScreenWakeLock.release();
                        Log.d(TAG, "✅ Screen WakeLock released");
                    }
                }, 5000); // Release after 5 seconds
            }
        }
    }
    
    private boolean isDuplicateAlarm(Context context, int medicineId) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("MedTrackPrefs", android.content.Context.MODE_PRIVATE);
        long lastAlarmTime = prefs.getLong("last_alarm_" + medicineId, 0);
        long currentTime = System.currentTimeMillis();
        
        // If alarm was processed within last 30 seconds, consider it duplicate
        // Reduced from 2 minutes to allow proper alarm scheduling
        return (currentTime - lastAlarmTime) < 30000;
    }
    
    private void markAlarmAsProcessed(Context context, int medicineId) {
        android.content.SharedPreferences prefs = context.getSharedPreferences("MedTrackPrefs", android.content.Context.MODE_PRIVATE);
        prefs.edit().putLong("last_alarm_" + medicineId, System.currentTimeMillis()).apply();
    }
    
    private void wakeUpDevice(Context context) {
        try {
            // Try to wake up the device
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            if (powerManager != null && !powerManager.isInteractive()) {
                PowerManager.WakeLock screenWakeLock = powerManager.newWakeLock(
                        PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                        "MedTrack::ScreenWakeLock"
                );
                screenWakeLock.acquire(10000); // Hold for 10 seconds
                screenWakeLock.release();
                Log.d(TAG, "✅ Device woken up");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to wake up device", e);
        }
    }
    
    private void showAlarmNotification(Context context, String medicineName, String dosage, int medicineId) {
        NotificationManager notificationManager = 
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);
        
        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Alarms",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Alarm notifications for medicine reminders");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true); // Bypass Do Not Disturb
            channel.setShowBadge(true);
            channel.setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_ALARM),
                    new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_ALARM)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
            );
            notificationManager.createNotificationChannel(channel);
        }
        
        // Intent to open app when notification is tapped
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                medicineId,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Build notification with enhanced lock screen visibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ MEDICINE TIME!")
                .setContentText(medicineName + " - " + dosage)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Time to take your medicine: " + medicineName + " (" + dosage + ")"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(pendingIntent, true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setLights(0xFFFF0000, 1000, 1000) // Red light
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);
        
        // Show notification
        notificationManager.notify(NOTIFICATION_ID + medicineId, builder.build());
    }
}