package com.AbdulPaito.medtrack;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.view.Window;
import android.view.WindowManager;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows a popup dialog for 5-minute medicine reminder
 */
public class ReminderDialogActivity extends AppCompatActivity {

    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // Acquire wake lock to keep screen on
        PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
        wakeLock = powerManager.newWakeLock(
                PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                "MedTrack::ReminderDialogWakeLock"
        );
        wakeLock.acquire(5 * 60 * 1000L); // Hold for 5 minutes
        
        // Show dialog on lock screen and wake up device
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            setShowWhenLocked(true);
            setTurnScreenOn(true);
            KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
            keyguardManager.requestDismissKeyguard(this, null);
        } else {
            Window window = getWindow();
            window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                    | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                    | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                    | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        }

        String medicineName = getIntent().getStringExtra("medicine_name");
        String dosage = getIntent().getStringExtra("dosage");

        // Show alert dialog
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("⏰ Medicine Reminder in 5 Minutes!")
                .setMessage("Your medicine " + medicineName + " (" + dosage + ") is due in 5 minutes.\n\nPlease prepare to take it.")
                .setPositiveButton("OK", (dialogInterface, which) -> {
                    finish();
                })
                .setCancelable(false)
                .setOnDismissListener(dialogInterface -> finish())
                .create();
        
        // Make dialog show on lock screen
        dialog.getWindow().addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON);
        
        dialog.show();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Release wake lock when activity is destroyed
        if (wakeLock != null && wakeLock.isHeld()) {
            wakeLock.release();
        }
    }
}
