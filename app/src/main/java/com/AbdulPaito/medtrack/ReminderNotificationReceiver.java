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
 * Receiver for 5-minute reminder notifications
 */
public class ReminderNotificationReceiver extends BroadcastReceiver {

    private static final String TAG = "ReminderNotificationReceiver";
    private static final String CHANNEL_ID = "medicine_reminder_channel";
    private static final int NOTIFICATION_ID = 2000;

    @Override
    public void onReceive(Context context, Intent intent) {
        Log.d(TAG, "⏰ 5-min reminder received! Time: " + System.currentTimeMillis());
        
        PowerManager powerManager = (PowerManager) context.getSystemService(Context.POWER_SERVICE);
        PowerManager.WakeLock wakeLock = null;
        PowerManager.WakeLock screenWakeLock = null;
        
        try {
            // Acquire wake locks to ensure device wakes up for reminder
            wakeLock = powerManager.newWakeLock(
                    PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                    "MedTrack::ReminderWakeLock"
            );
            wakeLock.acquire(3 * 60 * 1000L); // Hold for 3 minutes
            
            screenWakeLock = powerManager.newWakeLock(
                    PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                    "MedTrack::ReminderScreenWakeLock"
            );
            screenWakeLock.acquire(3 * 60 * 1000L); // Hold for 3 minutes
            
            String medicineName = intent.getStringExtra("medicine_name");
            String dosage = intent.getStringExtra("dosage");
            int medicineId = intent.getIntExtra("medicine_id", 0);

            Log.d(TAG, "Medicine: " + medicineName + ", ID: " + medicineId);

            // Show notification
            showReminderNotification(context, medicineName, dosage, medicineId);
            
            // Show popup dialog
            showReminderDialog(context, medicineName, dosage);
            
        } catch (Exception e) {
            Log.e(TAG, "❌ Error in ReminderNotificationReceiver", e);
        } finally {
            // Release wake locks after a delay
            if (wakeLock != null && wakeLock.isHeld()) {
                final PowerManager.WakeLock finalWakeLock = wakeLock;
                final PowerManager.WakeLock finalScreenWakeLock = screenWakeLock;
                android.os.Handler handler = new android.os.Handler(context.getMainLooper());
                handler.postDelayed(() -> {
                    if (finalWakeLock != null && finalWakeLock.isHeld()) {
                        finalWakeLock.release();
                        Log.d(TAG, "✅ Reminder WakeLock released");
                    }
                    if (finalScreenWakeLock != null && finalScreenWakeLock.isHeld()) {
                        finalScreenWakeLock.release();
                        Log.d(TAG, "✅ Reminder Screen WakeLock released");
                    }
                }, 2000); // Release after 2 seconds
            }
        }
    }
    
    private void showReminderDialog(Context context, String medicineName, String dosage) {
        // Create intent to show dialog activity
        Intent dialogIntent = new Intent(context, ReminderDialogActivity.class);
        dialogIntent.putExtra("medicine_name", medicineName);
        dialogIntent.putExtra("dosage", dosage);
        dialogIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        try {
            context.startActivity(dialogIntent);
        } catch (Exception e) {
            android.util.Log.e("ReminderNotificationReceiver", "Failed to show reminder dialog", e);
        }
    }

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

        // Intent to open app when notification is tapped
        Intent openAppIntent = new Intent(context, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                medicineId + 5000, // Unique request code
                openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Build notification with enhanced lock screen visibility
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("⏰ Upcoming Medicine Reminder")
                .setContentText(medicineName + " (" + dosage + ") - in 5 minutes!")
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Don't forget! Your medicine " + medicineName + " (" + dosage + ") is due in 5 minutes."))
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_REMINDER)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .setVibrate(new long[]{0, 500, 200, 500})
                .setLights(0xFF00FF00, 1000, 1000) // Green light
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true)
                .setTimeoutAfter(300000); // Auto-dismiss after 5 minutes

        // Show notification
        notificationManager.notify(NOTIFICATION_ID + medicineId, builder.build());
    }
}
