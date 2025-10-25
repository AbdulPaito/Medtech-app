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
import android.os.PowerManager;
import android.os.VibrationEffect;
import android.os.Vibrator;
import android.util.Log;
import androidx.core.app.NotificationCompat;

/**
 * AlarmSoundService - Plays alarm sound for 1 minute with vibration
 * Uses WakeLock to keep device awake while alarm is playing
 */
public class AlarmSoundService extends Service {

    private static final String CHANNEL_ID = "alarm_service_channel";
    private Ringtone ringtone;
    private Vibrator vibrator;
    private Handler handler;
    private PowerManager.WakeLock wakeLock;
    private int medicineId;
    private String medicineName;
    private String dosage;

    private PowerManager.WakeLock screenWakeLock;

    @Override
    public void onCreate() {
        super.onCreate();
        createNotificationChannel();
        handler = new Handler();
        vibrator = (Vibrator) getSystemService(Context.VIBRATOR_SERVICE);
        
        // Acquire FULL WakeLock to keep device awake and screen on during alarm
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "MedTrack::AlarmServiceWakeLock"
        );
        
        // Also acquire screen wake lock for maximum visibility
        screenWakeLock = powerManager.newWakeLock(
                PowerManager.SCREEN_BRIGHT_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP,
                "MedTrack::AlarmServiceScreenWakeLock"
        );
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent != null) {
            medicineId = intent.getIntExtra("medicine_id", -1);
            medicineName = intent.getStringExtra("medicine_name");
            dosage = intent.getStringExtra("dosage");

            // Acquire WakeLocks to keep device awake and screen on
            if (wakeLock != null && !wakeLock.isHeld()) {
                wakeLock.acquire(2 * 60 * 1000L); // Hold for 2 minutes max
            }
            if (screenWakeLock != null && !screenWakeLock.isHeld()) {
                screenWakeLock.acquire(2 * 60 * 1000L); // Hold for 2 minutes max
            }

            // Start foreground service with notification
            try {
                startForeground(medicineId, createForegroundNotification());
            } catch (Exception e) {
                android.util.Log.e("AlarmSoundService", "Failed to start foreground", e);
            }

            // Start alarm sound and vibration
            startAlarmSound();
            startVibration();

            // Open app and keep it open when alarm goes off
            openAppAndKeepOpen();

            // Auto-stop after 1 minute and schedule auto-repeat
            handler.postDelayed(() -> {
                stopAlarmSound();
                scheduleAutoRepeat();
                stopSelf();
            }, 60000); // 60 seconds = 1 minute
        }

        // Return START_REDELIVER_INTENT so service restarts if killed
        return START_REDELIVER_INTENT;
    }
    
    private void openAppAndKeepOpen() {
        try {
            Intent appIntent = new Intent(this, MainActivity.class);
            appIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            appIntent.putExtra("alarm_active", true);
            appIntent.putExtra("medicine_name", medicineName);
            appIntent.putExtra("dosage", dosage);
            startActivity(appIntent);
            Log.d("AlarmSoundService", "✅ App opened and kept open");
        } catch (Exception e) {
            Log.e("AlarmSoundService", "❌ Failed to open app", e);
        }
    }

    private void startAlarmSound() {
        try {
            // Check if silent mode is enabled
            android.content.SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
            boolean silentMode = prefs.getBoolean("silent_mode", false);
            
            if (silentMode) {
                // Silent mode - only vibration, no sound
                return;
            }
            
            // Get custom alarm sound URI from settings
            String customSoundUri = prefs.getString("alarmSoundUri", null);
            Uri alarmUri;
            
            if (customSoundUri != null && !customSoundUri.equals("default")) {
                // Use custom alarm sound
                alarmUri = Uri.parse(customSoundUri);
            } else {
                // Use default alarm sound
                alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM);
                if (alarmUri == null) {
                    alarmUri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE);
                }
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
        // Check if vibration is enabled in settings
        android.content.SharedPreferences prefs = getSharedPreferences("AppSettings", MODE_PRIVATE);
        boolean vibrationEnabled = prefs.getBoolean("vibration_enabled", true);
        
        if (!vibrationEnabled) {
            return;
        }
        
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
        intent.setAction("com.AbdulPaito.medtrack.ALARM_TRIGGER");
        intent.putExtra("medicine_id", medicineId);
        intent.putExtra("medicine_name", medicineName);
        intent.putExtra("dosage", dosage);

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                this,
                medicineId + 20000, // Different request code for auto-repeat (not 10000 which is used by 5-min reminder)
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
        // Intent for "Stop" action - only stops sound, keeps medicine pending
        Intent stopIntent = new Intent(this, NotificationActionReceiver.class);
        stopIntent.setAction("ACTION_STOP");
        stopIntent.putExtra("medicine_id", medicineId);
        stopIntent.putExtra("medicine_name", medicineName);
        PendingIntent stopPendingIntent = PendingIntent.getBroadcast(
                this, medicineId * 10 + 1, stopIntent,
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

        // Intent for "I Took It" action
        Intent takenIntent = new Intent(this, NotificationActionReceiver.class);
        takenIntent.setAction("ACTION_TAKEN");
        takenIntent.putExtra("medicine_id", medicineId);
        takenIntent.putExtra("medicine_name", medicineName);
        takenIntent.putExtra("dosage", dosage);
        PendingIntent takenPendingIntent = PendingIntent.getBroadcast(
                this, medicineId * 10 + 3, takenIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        // Intent to open app when notification is tapped
        Intent openAppIntent = new Intent(this, MainActivity.class);
        openAppIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        PendingIntent openAppPendingIntent = PendingIntent.getActivity(
                this, medicineId * 10 + 4, openAppIntent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(android.R.drawable.ic_lock_idle_alarm)
                .setContentTitle("⏰ MEDICINE REMINDER!")
                .setContentText(medicineName + " - " + dosage)
                .setStyle(new NotificationCompat.BigTextStyle()
                        .bigText("Time to take your medicine: " + medicineName + " (" + dosage + ")"))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setAutoCancel(false)
                .setOngoing(true)
                .setContentIntent(openAppPendingIntent)
                .setFullScreenIntent(openAppPendingIntent, true)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
                .addAction(android.R.drawable.ic_menu_recent_history, "Snooze 5min", snoozePendingIntent)
                .addAction(android.R.drawable.ic_menu_send, "I Took It", takenPendingIntent)
                .setVibrate(new long[]{0, 1000, 500, 1000, 500, 1000})
                .setLights(0xFFFF0000, 1000, 1000) // Red light
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                .setDefaults(NotificationCompat.DEFAULT_ALL)
                .setWhen(System.currentTimeMillis())
                .setShowWhen(true);

        return builder.build();
    }

    private void createNotificationChannel() {
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
        
        // Release WakeLocks when service is destroyed
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
        if (screenWakeLock != null && screenWakeLock.isHeld()) {
            screenWakeLock.release();
        }
    }

    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }
}