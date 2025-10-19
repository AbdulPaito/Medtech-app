package com.AbdulPaito.medtrack;

import android.media.AudioAttributes;
import android.media.RingtoneManager;
import android.net.Uri;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.Build;
import androidx.core.app.NotificationCompat;

/**
 * NotificationHelper - Creates and shows notifications
 */
public class NotificationHelper {

    private static final String CHANNEL_ID = "medicine_reminders";
    private static final String CHANNEL_NAME = "Medicine Reminders";
    private Context context;
    private NotificationManager notificationManager;

    public NotificationHelper(Context context) {
        this.context = context;
        this.notificationManager = (NotificationManager)
                context.getSystemService(Context.NOTIFICATION_SERVICE);

        createNotificationChannel();
    }

    /**
     * Create notification channel (required for Android 8.0+)
     */
    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    CHANNEL_NAME,
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Reminders to take your medicine");

            // Enable vibration and lights
            channel.enableVibration(true);
            channel.enableLights(true);
            channel.setVibrationPattern(new long[]{0, 1000, 500, 1000});

            // Set alarm sound
            Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmSound == null) {
                alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
            }
            AudioAttributes audioAttributes = new AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build();
            channel.setSound(alarmSound, audioAttributes);

            if (notificationManager != null) {
                notificationManager.createNotificationChannel(channel);
            }
        }
    }

    /**
     * Show medicine reminder notification
     */
    public void showMedicineReminder(int medicineId, String medicineName, String dosage) {
        // Intent to open app when notification is tapped
        Intent intent = new Intent(context, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);

        PendingIntent pendingIntent = PendingIntent.getActivity(
                context,
                medicineId,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Use alarm sound (fallback to notification sound)
        Uri alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
        if (alarmSound == null) {
            alarmSound = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION);
        }

        // Build notification
        NotificationCompat.Builder builder = new NotificationCompat.Builder(context, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_menu_agenda)
                .setContentTitle("💊 Time to take your medicine!")
                .setContentText(medicineName + " - " + dosage)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(true)
                .setSound(alarmSound)
                .setVibrate(new long[]{0, 1000, 500, 1000})
                .setLights(0xFF00FF00, 500, 500)
                .setContentIntent(pendingIntent)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Don't forget to take " + medicineName + " (" + dosage + ")"))
                .addAction(
                        android.R.drawable.ic_menu_close_clear_cancel,
                        "Mark as Taken",
                        createMarkTakenIntent(medicineId, medicineName)
                )
                .addAction(
                        android.R.drawable.ic_menu_recent_history,
                        "Snooze 10 min",
                        createSnoozeIntent(medicineId, medicineName, dosage)
                );

        // Show notification
        if (notificationManager != null) {
            notificationManager.notify(medicineId, builder.build());
        }
    }

    /**
     * Create intent for "Mark as Taken" action
     */
    private PendingIntent createMarkTakenIntent(int medicineId, String medicineName) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction("ACTION_MARK_TAKEN");
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);

        return PendingIntent.getBroadcast(
                context,
                medicineId * 10 + 1,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Create intent for "Snooze" action
     */
    private PendingIntent createSnoozeIntent(int medicineId, String medicineName, String dosage) {
        Intent intent = new Intent(context, NotificationActionReceiver.class);
        intent.setAction("ACTION_SNOOZE");
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);
        intent.putExtra("dosage", dosage);

        return PendingIntent.getBroadcast(
                context,
                medicineId * 10 + 2,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );
    }

    /**
     * Cancel a notification
     */
    public void cancelNotification(int medicineId) {
        if (notificationManager != null) {
            notificationManager.cancel(medicineId);
        }
    }
}
