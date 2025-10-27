package com.AbdulPaito.medtrack;

import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.util.Log;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Shows a popup dialog for 5-minute medicine reminder
 */
public class ReminderDialogActivity extends AppCompatActivity {

    private static final String TAG = "ReminderDialogActivity";
    private PowerManager.WakeLock wakeLock;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        Log.d(TAG, "⏰ ReminderDialogActivity onCreate - Starting");
        
        try {
            // Set window flags BEFORE anything else - critical for lock screen
            Window window = getWindow();
            if (window != null) {
                window.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD
                        | WindowManager.LayoutParams.FLAG_ALLOW_LOCK_WHILE_SCREEN_ON);
                Log.d(TAG, "✅ Window flags set");
            }
            
            // Acquire wake lock to keep screen on
            PowerManager powerManager = (PowerManager) getSystemService(Context.POWER_SERVICE);
            if (powerManager != null) {
                wakeLock = powerManager.newWakeLock(
                        PowerManager.FULL_WAKE_LOCK | PowerManager.ACQUIRE_CAUSES_WAKEUP | PowerManager.ON_AFTER_RELEASE,
                        "MedTrack::ReminderDialogWakeLock"
                );
                wakeLock.acquire(5 * 60 * 1000L); // Hold for 5 minutes
                Log.d(TAG, "✅ Wake lock acquired");
            }
            
            // Additional flags for newer Android versions
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                setShowWhenLocked(true);
                setTurnScreenOn(true);
                KeyguardManager keyguardManager = (KeyguardManager) getSystemService(Context.KEYGUARD_SERVICE);
                if (keyguardManager != null) {
                    keyguardManager.requestDismissKeyguard(this, null);
                }
                Log.d(TAG, "✅ Lock screen flags set for Android O+");
            }

            // Get intent extras with null checks
            String medicineName = getIntent().getStringExtra("medicine_name");
            String dosage = getIntent().getStringExtra("dosage");
            
            // Validate data
            if (medicineName == null || medicineName.isEmpty()) {
                Log.e(TAG, "❌ Medicine name is null or empty!");
                medicineName = "Your Medicine";
            }
            if (dosage == null || dosage.isEmpty()) {
                Log.e(TAG, "❌ Dosage is null or empty!");
                dosage = "prescribed dose";
            }
            
            Log.d(TAG, "📋 Medicine: " + medicineName + ", Dosage: " + dosage);

            // Create message with proper formatting
            final String finalMedicineName = medicineName;
            final String finalDosage = dosage;
            
            // Show alert dialog with try-catch
            AlertDialog.Builder builder = new AlertDialog.Builder(this);
            builder.setTitle("⏰ Medicine Reminder in 5 Minutes!")
                    .setMessage("Your medicine " + finalMedicineName + " (" + finalDosage + ") is due in 5 minutes.\n\nPlease prepare to take it.")
                    .setPositiveButton("OK", (dialogInterface, which) -> {
                        Log.d(TAG, "✅ User tapped OK");
                        finish();
                    })
                    .setCancelable(false)
                    .setOnDismissListener(dialogInterface -> {
                        Log.d(TAG, "✅ Dialog dismissed");
                        finish();
                    });
            
            AlertDialog dialog = builder.create();
            
            // Make dialog show on lock screen with all necessary flags
            Window dialogWindow = dialog.getWindow();
            if (dialogWindow != null) {
                dialogWindow.addFlags(WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                        | WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
                        | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
                Log.d(TAG, "✅ Dialog window flags set");
            }
            
            dialog.show();
            Log.d(TAG, "✅ Dialog shown successfully");
            
        } catch (Exception e) {
            Log.e(TAG, "❌ ERROR in ReminderDialogActivity onCreate", e);
            Toast.makeText(this, "Error showing reminder: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            finish();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "⏰ ReminderDialogActivity onDestroy");
        try {
            // Release wake lock when activity is destroyed
            if (wakeLock != null && wakeLock.isHeld()) {
                wakeLock.release();
                Log.d(TAG, "✅ Wake lock released");
            }
        } catch (Exception e) {
            Log.e(TAG, "❌ Error releasing wake lock", e);
        }
    }
}
