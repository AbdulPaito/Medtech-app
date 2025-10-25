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
 * AlarmReceiver - Completely rewritten for reliable alarm handling
 */
public class AlarmReceiver extends BroadcastReceiver {

    private static final String TAG = "AlarmReceiver";
    private static final String CHANNEL_ID = "alarm_channel";
    private static final int NOTIFICATION_ID = 1000;

    @Override
    public void onReceive(Context context, Intent intent) {
        long currentTime = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault());
        Log.d(TAG, "🚨 ALARM RECEIVED! Time: " + sdf.format(new java.util.Date(currentTime)));
        
        // Get medicine details
        int medicineId = intent.getIntExtra("medicine_id", -1);
        String medicineName = intent.getStringExtra("medicine_name");
        String dosage = intent.getStringExtra("dosage");

        Log.d(TAG, "💊 Medicine: " + medicineName + ", ID: " + medicineId + ", Dosage: " + dosage);

        if (medicineId == -1 || medicineName == null) {
            Log.e(TAG, "❌ Invalid medicine data received");
            return;
        }

        // Acquire wake locks to ensure device wakes up
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        
        try {
            // Acquire wake lock to wake device
            wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "MedTrack::AlarmWakeLock"
            );
            wakeLock.acquire(10 * 60 * 1000L); // Hold for 10 minutes
            
            Log.d(TAG, "✅ WakeLock acquired");
            
            // Show alarm notification
            showAlarmNotification(context, medicineName, dosage, medicineId);
            
            // Start alarm service for continuous ringing
            startAlarmService(context, medicineId, medicineName, dosage);
            
            Log.d(TAG, "✅ Alarm processed successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error processing alarm", e);
        } finally {
            // Release wake lock after delay
            if (wakeLock != null && wakeLock.isHeld()) {
                final PowerManager.WakeLock finalWakeLock = wakeLock;
                android.os.Handler handler = new android.os.Handler(context.getMainLooper());
                handler.postDelayed(() -> {
                    if (finalWakeLock != null && finalWakeLock.isHeld()) {
                        finalWakeLock.release();
                        Log.d(TAG, "✅ WakeLock released");
                    }
                }, 5000); // Release after 5 seconds
            }
        }
    }
    
    /**
     * Start alarm service for continuous ringing
     */
    private void startAlarmService(Context context, int medicineId, String medicineName, String dosage) {
        Intent serviceIntent = new Intent(context, AlarmSoundService.class);
        serviceIntent.setAction("com.AbdulPaito.medtrack.ALARM_SERVICE");
        serviceIntent.putExtra("medicine_id", medicineId);
        serviceIntent.putExtra("medicine_name", medicineName);
        serviceIntent.putExtra("dosage", dosage);

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(serviceIntent);
                Log.d(TAG, "✅ Started foreground alarm service");
            } else {
                context.startService(serviceIntent);
                Log.d(TAG, "✅ Started alarm service");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to start alarm service", e);
        }
    }
    
    /**
     * Show alarm notification
     */
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
        
        // Build notification
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
        Log.d(TAG, "✅ Alarm notification shown");
    }
}