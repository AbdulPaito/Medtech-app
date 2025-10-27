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
 * ReminderNotificationReceiver - Completely rewritten for exact 5-minute reminders
 */
public class ReminderNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderNotificationReceiver";
    private static final String CHANNEL_ID = "medicine_reminder_channel";
    private static final int NOTIFICATION_ID = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        if (context == null || intent == null) {
            Log.e(TAG, "❌ Context or Intent is null!");
            return;
        }
        
        long currentTime = System.currentTimeMillis();
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", java.util.Locale.getDefault());
        Log.d(TAG, "⏰ 5-MINUTE REMINDER RECEIVED! Time: " + sdf.format(new java.util.Date(currentTime)));
        
        try {
            // Get medicine details with null checks
            String medicineName = intent.getStringExtra("medicine_name");
            String dosage = intent.getStringExtra("dosage");
            int medicineId = intent.getIntExtra("medicine_id", 0);

            Log.d(TAG, "📊 Reminder for: " + medicineName + ", ID: " + medicineId + ", Dosage: " + dosage);

            // Validate data
            if (medicineName == null || medicineName.isEmpty()) {
                Log.e(TAG, "❌ Medicine name is null or empty");
                medicineName = "Your Medicine";
            }
            if (dosage == null || dosage.isEmpty()) {
                Log.e(TAG, "❌ Dosage is null or empty");
                dosage = "1 dose";
            }
            if (medicineId == 0) {
                Log.e(TAG, "❌ Invalid medicine ID: 0");
                medicineId = 1;
            }

            // Acquire wake locks to ensure device wakes up
            PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
            PowerManager.WakeLock wakeLock = null;
            
            if (powerManager == null) {
                Log.e(TAG, "❌ PowerManager is null!");
                return;
            }
            
            try {
                // Acquire wake lock to wake device
                wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "MedTrack::ReminderWakeLock"
                );
                wakeLock.acquire(5 * 60 * 1000L); // Hold for 5 minutes
                
                Log.d(TAG, "✅ Reminder WakeLock acquired");
                
                // Show reminder notification
                showReminderNotification(context, medicineName, dosage, medicineId);
                
                // Show popup dialog
                showReminderDialog(context, medicineName, dosage);
                
                Log.d(TAG, "✅ 5-minute reminder processed successfully");
                
            } catch (Exception e) {
                Log.e(TAG, "❌ Error processing reminder", e);
                e.printStackTrace();
            } finally {
                // Release wake lock after delay
                if (wakeLock != null && wakeLock.isHeld()) {
                    final PowerManager.WakeLock finalWakeLock = wakeLock;
                    android.os.Handler handler = new android.os.Handler(context.getMainLooper());
                    handler.postDelayed(() -> {
                        try {
                            if (finalWakeLock != null && finalWakeLock.isHeld()) {
                                finalWakeLock.release();
                                Log.d(TAG, "✅ Reminder WakeLock released");
                            }
                        } catch (Exception ex) {
                            Log.e(TAG, "❌ Error releasing wake lock", ex);
                        }
                    }, 3000); // Release after 3 seconds
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ CRITICAL ERROR in ReminderNotificationReceiver", e);
            e.printStackTrace();
        }
    }
    
    /**
     * Show reminder dialog
     */
    private void showReminderDialog(Context context, String medicineName, String dosage) {
        try {
            Intent dialogIntent = new Intent(context, ReminderDialogActivity.class);
            dialogIntent.putExtra("medicine_name", medicineName);
            dialogIntent.putExtra("dosage", dosage);
            // Critical flags for lock screen - add FLAG_ACTIVITY_NO_USER_ACTION
            dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK 
                    | Intent.FLAG_ACTIVITY_NO_USER_ACTION
                    | Intent.FLAG_ACTIVITY_EXCLUDE_FROM_RECENTS);
            context.startActivity(dialogIntent);
            Log.d(TAG, "✅ Reminder dialog started");
        } catch (Exception e) {
            Log.e(TAG, "❌ Failed to show reminder dialog", e);
            e.printStackTrace();
        }
    }

    /**
     * Show reminder notification
     */
    private void showReminderNotification(Context context, String medicineName, String dosage, int medicineId) {
        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

        // Create notification channel for Android 8.0+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Medicine Reminders",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Notifications for upcoming medicine doses");
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setLockscreenVisibility(NotificationCompat.VISIBILITY_PUBLIC);
            channel.setBypassDnd(true); // Bypass Do Not Disturb
            channel.setShowBadge(true);
            channel.setSound(
                    android.media.RingtoneManager.getDefaultUri(android.media.RingtoneManager.TYPE_NOTIFICATION),
                    new android.media.AudioAttributes.Builder()
                            .setUsage(android.media.AudioAttributes.USAGE_NOTIFICATION)
                            .setContentType(android.media.AudioAttributes.CONTENT_TYPE_SONIFICATION)
                            .setFlags(android.media.AudioAttributes.FLAG_AUDIBILITY_ENFORCED)
                            .build()
            );
            notificationManager.createNotificationChannel(channel);
        }

        // Intent to open ReminderDialogActivity when notification is tapped
        Intent dialogIntent = new Intent(context, ReminderDialogActivity.class);
        dialogIntent.putExtra("medicine_name", medicineName);
        dialogIntent.putExtra("dosage", dosage);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_NO_USER_ACTION);
        
        PendingIntent fullScreenIntent = PendingIntent.getActivity(
                context,
                medicineId + 10000,
                dialogIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
        
        // Intent for tap action
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        PendingIntent contentIntent = PendingIntent.getActivity(
                context,
                medicineId + 10001,
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification with FULL SCREEN INTENT for lock screen wake
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⏰ Upcoming Medicine Reminder")
                .setContentText(medicineName + " (" + dosage + ") - in 5 minutes!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Don't forget! Your medicine " + medicineName + " (" + dosage + ") is due in exactly 5 minutes."))
                .setPriority(NotificationCompat.PRIORITY_MAX) // Changed to MAX
                .setCategory(NotificationCompat.CATEGORY_ALARM) // Changed to ALARM
                .setAutoCancel(true)
                .setContentIntent(contentIntent)
                .setFullScreenIntent(fullScreenIntent, true) // CRITICAL for lock screen
                .setVibrate(new long[]{0, 500, 200, 500})
                .setLights(0xFF00FF00, 1000, 1000) // Green light
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setOngoing(false)
                .setTimeoutAfter(300000); // Auto-dismiss after 5 minutes

        // Show notification
        notificationManager.notify(NOTIFICATION_ID + medicineId, builder.build());
        Log.d(TAG, "✅ Reminder notification shown");
    }
}