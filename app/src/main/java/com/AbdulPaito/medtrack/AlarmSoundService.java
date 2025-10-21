package com.AbdulPaito.medtrack;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.media.AudioAttributes;
import android.media.AudioManager;
import android.media.Ringtone;
import android.media.RingtoneManager;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.IBinder;
import android.os.VibrationEffect;
import android.os.Vibrator;
import androidx.core.app.NotificationCompat;

/**
 * AlarmSoundService - Plays alarm sound for 1 minute with vibration
 */
public class AlarmSoundService extends Service {

    private static final String CHANNEL_ID = "alarm_service_channel";
    private Ringtone ringtone;
    private Vibrator vibrator;
    private Handler handler;
    private int medicineId;
    private String medicineName;
    private String dosage;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            medicineId = intent.getIntExtra("medicine_id", -1);
            medicineName = intent.getStringExtra("medicine_name");
            dosage = intent.getStringExtra("dosage");

            // Start foreground service with notification
            startForeground(medicineId, createForegroundNotification());

            // Start alarm sound and vibration
            startAlarmSound();
            startVibration();

            // Auto-stop after 1 minute and schedule auto-repeat
            handler.postDelayed(() -> {
                stopAlarmSound();
                scheduleAutoRepeat();
                stopSelf();
            }, 60000); // 60 seconds = 1 minute
        }

        return START_NOT_STICKY;
    }

    private void startAlarmSound() {
        try {
            Uri alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
            if (alarmUri == null) {
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
            }

            ringtone = RingtoneManager.getRingtone(getApplicationContext(), alarmUri);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                ringtone.setLooping(true);
            }

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                AudioAttributes audioAttributes = new AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build();
                ringtone.setAudioAttributes(audioAttributes);
            } else {
                ringtone.setStreamType(AudioManager.STREAM_ALARM);
            }

            ringtone.play();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void startVibration() {
        if (vibrator != null && vibrator.hasVibrator()) {
            long[] pattern = {0, 1000, 500, 1000, 500}; // vibrate pattern

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(VibrationEffect.createWaveform(pattern, 0));
            } else {
                vibrator.vibrate(pattern, 0);
            }
        }
    }

    private void stopAlarmSound() {
        if (ringtone != null && ringtone.isPlaying()) {
            ringtone.stop();
        }

        if (vibrator != null) {
            vibrator.cancel();
        }
    }

    private void scheduleAutoRepeat() {
        // Schedule alarm to repeat in 5 minutes if user didn't respond
        Intent intent = new Intent(this, AlarmReceiver.class);
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);
        intent.putExtra("dosage", dosage);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                medicineId + 10000, // Different request code for repeat
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        android.app.AlarmManager alarmManager =
                (android.app.AlarmManager) getSystemService(Context.ALARM_SERVICE);

        if (alarmManager != null) {
            long triggerTime = System.currentTimeMillis() + (5 * 60 * 1000); // 5 minutes

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            } else {
                alarmManager.setExact(
                        android.app.AlarmManager.RTC_WAKEUP,
                        triggerTime,
                        pendingIntent
                );
            }
        }
    }

    private Notification createForegroundNotification() {
        // Intent for "Take Now" action
        Intent takeNowIntent = new Intent(this, NotificationActionReceiver.class);
        takeNowIntent.setAction("ACTION_MARK_TAKEN");
        takeNowIntent.putExtra("medicine_id", medicineId);
        takeNowIntent.putExtra("medicine_name", medicineName);
        PendingIntent takeNowPendingIntent = PendingIntent.getBroadcast(
                this, medicineId * 10 + 1, takeNowIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent for "Snooze 5 min" action
        Intent snoozeIntent = new Intent(this, NotificationActionReceiver.class);
        snoozeIntent.setAction("ACTION_SNOOZE");
        snoozeIntent.putExtra("medicine_id", medicineId);
        snoozeIntent.putExtra("medicine_name", medicineName);
        snoozeIntent.putExtra("dosage", dosage);
        PendingIntent snoozePendingIntent = PendingIntent.getBroadcast(
                this, medicineId * 10 + 2, snoozeIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ MEDICINE REMINDER!")
                .setContentText(medicineName + " - " + dosage)
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setFullScreenIntent(takeNowPendingIntent, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Take Now", takeNowPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 5min", snoozePendingIntent);

        return builder.build();
    }

    private void createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            NotificationChannel channel = new NotificationChannel(
                    CHANNEL_ID,
                    "Alarm Service",
                    NotificationManager.IMPORTANCE_HIGH
            );
            channel.setDescription("Keeps alarm running");

            NotificationManager manager = getSystemService(NotificationManager.class);
            if (manager != null) {
                manager.createNotificationChannel(channel);
            }
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        stopAlarmSound();
        if (handler != null) {
            handler.removeCallbacksAndMessages(null);
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}