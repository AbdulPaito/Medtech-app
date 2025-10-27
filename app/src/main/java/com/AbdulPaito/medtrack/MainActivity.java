package com.AbdulPaito.medtrack;

import android.Manifest;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.PowerManager;
import android.provider.Settings;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

public class MainActivity extends AppCompatActivity {

    private static final int NOTIFICATION_PERMISSION_CODE = 100;
    private TextView textMedicineCount;
    private TextView textNextReminder;
    private TextView textWelcome;
    private ImageView imgUserProfile;
    private MaterialCardView cardProfileImage;
    private MaterialCardView addMedicineCard;
    private MaterialCardView viewRemindersCard;
    private BottomNavigationView bottomNav;
    private DatabaseHelper databaseHelper;
    private SharedPreferences prefs;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            setContentView(R.layout.activity_main);

            databaseHelper = new DatabaseHelper(this);
            prefs = getSharedPreferences("MedTrackPrefs", MODE_PRIVATE);
            
            initViews();
            setupButtons();
            setupBottomNavigation();
            setupWelcomeName();
            updateStats();
            
            // Check if opened by alarm
            handleAlarmIntent();
            
            // Start background service to keep app alive (with error handling)
            // Temporarily disabled to fix crash
            /*
            try {
                startBackgroundService();
            } catch (Exception e) {
                android.util.Log.e("MainActivity", "Failed to start background service", e);
                // Continue without background service if it fails
            }
            */
            
            // Always check and request permissions if not granted
            checkAndRequestPermissionsIfNeeded();
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in onCreate", e);
            Toast.makeText(this, "App initialization error. Please restart.", Toast.LENGTH_LONG).show();
        }
    }
    
    private void startBackgroundService() {
        try {
            Intent serviceIntent = new Intent(this, BackgroundKeepAliveService.class);
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(serviceIntent);
            } else {
                startService(serviceIntent);
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Failed to start background service", e);
        }
    }
    
    private void handleAlarmIntent() {
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("alarm_active", false)) {
            // App opened by alarm - keep it open and show alarm status
            String medicineName = intent.getStringExtra("medicine_name");
            String dosage = intent.getStringExtra("dosage");
            
            if (medicineName != null && dosage != null) {
                showAlarmStatus(medicineName, dosage);
            }
        }
    }
    
    private void showAlarmStatus(String medicineName, String dosage) {
        // Show a persistent alarm status in the UI
        textNextReminder.setText("🔔 ALARM ACTIVE: " + medicineName + " (" + dosage + ")");
        textNextReminder.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
        
        // You can add more visual indicators here if needed
        Toast.makeText(this, "⏰ " + medicineName + " alarm is active!", Toast.LENGTH_LONG).show();
    }

    private void initViews() {
        try {
            textMedicineCount = findViewById(R.id.text_medicine_count);
            textNextReminder = findViewById(R.id.text_next_reminder);
            textWelcome = findViewById(R.id.text_welcome);
            imgUserProfile = findViewById(R.id.img_user_profile);
            cardProfileImage = findViewById(R.id.card_profile_image);
            
            // Load and display user profile image
            loadProfileImage();
            
            // Profile image click listener
            if (cardProfileImage != null) {
                cardProfileImage.setOnClickListener(v -> {
                    try {
                        Intent intent = new Intent(this, ProfileActivity.class);
                        startActivity(intent);
                    } catch (Exception e) {
                        android.util.Log.e("MainActivity", "Error opening ProfileActivity", e);
                    }
                });
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error in initViews", e);
        }
    }
    
    private void loadProfileImage() {
        try {
            String avatarType = prefs.getString("avatar_type", "custom");
            String profileImageBase64 = prefs.getString("profile_image", "");
            
            if (imgUserProfile == null || cardProfileImage == null) {
                return; // Views not initialized yet
            }
            
            if (!profileImageBase64.isEmpty()) {
                // Load custom photo
                Bitmap bitmap = base64ToBitmap(profileImageBase64);
                if (bitmap != null) {
                    imgUserProfile.setImageBitmap(bitmap);
                    imgUserProfile.setScaleType(android.widget.ImageView.ScaleType.FIT_XY);
                    // Set card background to solid color for circular image
                    cardProfileImage.setCardBackgroundColor(0xFF4F46E5); // Indigo background
                }
            } else {
                // Load avatar based on type
                switch (avatarType) {
                    case "male":
                        imgUserProfile.setImageResource(R.drawable.ic_male);
                        cardProfileImage.setCardBackgroundColor(0xFF2196F3); // Blue
                        break;
                    case "female":
                        imgUserProfile.setImageResource(R.drawable.ic_female);
                        cardProfileImage.setCardBackgroundColor(0xFFE91E63); // Pink
                        break;
                    default:
                        imgUserProfile.setImageResource(R.drawable.ic_person);
                        cardProfileImage.setCardBackgroundColor(0xFF9C27B0); // Purple
                        break;
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error loading profile image", e);
        }
    }
    
    private Bitmap base64ToBitmap(String base64Str) {
        try {
            if (base64Str == null || base64Str.isEmpty()) {
                return null;
            }
            byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error converting base64 to bitmap", e);
            return null;
        }
    }

    // Editable "Hello, User!"
    private void setupWelcomeName() {
        SharedPreferences prefs = getSharedPreferences("MedTrackPrefs", MODE_PRIVATE);
        String username = prefs.getString("username", "User");
        textWelcome.setText("Hello, " + username + "!");

        textWelcome.setOnClickListener(v -> {
            final EditText input = new EditText(this);
            input.setHint("Enter your name");

            new AlertDialog.Builder(this)
                    .setTitle("Change Name")
                    .setView(input)
                    .setPositiveButton("Save", (dialog, which) -> {
                        String newName = input.getText().toString().trim();
                        if (!newName.isEmpty()) {
                            textWelcome.setText("Hello, " + newName + "!");
                            prefs.edit().putString("username", newName).apply();
                        }
                    })
                    .setNegativeButton("Cancel", null)
                    .show();
        });
    }

    private void setupButtons() {
        MaterialCardView addMedicineCard = findViewById(R.id.card_add_medicine);
        MaterialCardView viewRemindersCard = findViewById(R.id.card_view_reminders);

        addMedicineCard.setOnClickListener(view -> {
            startActivity(new Intent(this, AddMedicineActivity.class));
        });

        viewRemindersCard.setOnClickListener(view -> {
            startActivity(new Intent(this, ReminderListActivity.class));
        });
    }

    private void setupBottomNavigation() {
        BottomNavigationView bottomNav = findViewById(R.id.bottom_navigation);
        bottomNav.setSelectedItemId(R.id.navigation_home);

        bottomNav.setOnItemSelectedListener(item -> {
            int itemId = item.getItemId();

            if (itemId == R.id.navigation_home) {
                return true;
            } else if (itemId == R.id.navigation_medicines) {
                startActivity(new Intent(this, ReminderListActivity.class));
                return true;
            } else if (itemId == R.id.navigation_history) {
                startActivity(new Intent(this, HistoryActivity.class));
                return true;
            } else if (itemId == R.id.navigation_statistics) {  // ✅ Added Stats tab
                startActivity(new Intent(this, StatisticsActivity.class));
                overridePendingTransition(0, 0);
                return true;
            } else if (itemId == R.id.navigation_settings) {
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            }

            return false;
        });
    }


    private void updateStats() {
        try {
            if (databaseHelper == null) {
                return;
            }
            
            int count = databaseHelper.getMedicineCount();
            int adherenceRate = databaseHelper.getAdherenceRate();
            int streak = databaseHelper.getCurrentStreak();
            
            if (textMedicineCount == null || textNextReminder == null) {
                return; // Views not initialized yet
            }
            
            if (count == 0) {
                textMedicineCount.setText("No medicines added yet");
                textNextReminder.setText("Add your first medicine to get started!");
            } else {
                // Show medicine count
                String medicineText = count == 1 ? "1 medicine scheduled" : count + " medicines scheduled";
                textMedicineCount.setText(medicineText);
                
                // Show adherence and streak
                if (adherenceRate > 0 || streak > 0) {
                    String statsText = "";
                    if (adherenceRate > 0) {
                        statsText += "📊 " + adherenceRate + "% adherence";
                    }
                    if (streak > 0) {
                        if (!statsText.isEmpty()) statsText += " • ";
                        statsText += "🔥 " + streak + " day streak";
                    }
                    textNextReminder.setText(statsText);
                } else {
                    textNextReminder.setText("Stay consistent! 💪");
                }
            }
        } catch (Exception e) {
            android.util.Log.e("MainActivity", "Error updating stats", e);
        }
    }

    private void checkAndRequestPermissionsIfNeeded() {
        boolean needsNotificationPermission = false;
        boolean needsAlarmPermission = false;
        boolean needsFullScreenPermission = false;
        
        // Check notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                needsNotificationPermission = true;
            }
        }
        
        // Check full screen intent permission (Android 14+ / API 34+)
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && !notificationManager.canUseFullScreenIntent()) {
                needsFullScreenPermission = true;
            }
        }
        
        // Check alarm permission (Android 12+)
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (!scheduler.canScheduleExactAlarms()) {
            needsAlarmPermission = true;
        }
        
        // Only show dialog if permissions are needed and not already shown this session
        if ((needsNotificationPermission || needsAlarmPermission || needsFullScreenPermission) && 
            !prefs.getBoolean("permission_dialog_shown_this_session", false)) {
            prefs.edit().putBoolean("permission_dialog_shown_this_session", true).apply();
            requestNecessaryPermissions();
        }
    }
    
    private void requestNecessaryPermissions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("⏰ Allow Alarms & Notifications");
        builder.setMessage("MedTrack needs permission to:\n\n" +
                "📢 Send notifications for your medicine reminders\n" +
                "⏰ Set alarms that work even when app is closed\n" +
                "🔓 Wake phone screen when alarms ring (even on lock screen)\n\n" +
                "These permissions are required for the app to work properly.");
        builder.setPositiveButton("Allow", (dialog, which) -> {
            requestAllPermissions();
        });
        builder.setNegativeButton("Cancel", (dialog, which) -> {
            Toast.makeText(this, "⚠️ Permissions required for alarms to work on lock screen", Toast.LENGTH_LONG).show();
        });
        builder.setCancelable(false);
        builder.show();
    }
    
    private void requestAllPermissions() {
        // Build list of permissions to request based on Android version
        java.util.ArrayList<String> permissionsToRequest = new java.util.ArrayList<>();
        
        // Notification permission (Android 13+)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS);
            }
        }
        
        // Request runtime permissions first
        if (!permissionsToRequest.isEmpty()) {
            ActivityCompat.requestPermissions(this,
                    permissionsToRequest.toArray(new String[0]),
                    NOTIFICATION_PERMISSION_CODE);
        } else {
            // If notification permission already granted or not needed, check alarm permission
            checkAndRequestAlarmPermission();
        }
    }
    
    private void checkAndRequestAlarmPermission() {
        // Request exact alarm permission (Android 12+) - requires special handling
        AlarmScheduler scheduler = new AlarmScheduler(this);
        if (!scheduler.canScheduleExactAlarms()) {
            new AlertDialog.Builder(this)
                    .setTitle("⏰ Allow Exact Alarms")
                    .setMessage("Please enable 'Alarms & reminders' permission so your medicine alarms work even when the app is closed or your phone is locked.")
                    .setPositiveButton("Open Settings", (d, w) -> {
                        scheduler.requestExactAlarmPermission();
                        // After alarm permission, check full screen intent
                        checkAndRequestFullScreenIntent();
                    })
                    .setNegativeButton("Skip", (d, w) -> {
                        Toast.makeText(this, "⚠️ Alarms may not work reliably without this permission", Toast.LENGTH_LONG).show();
                        // Still check full screen intent even if skipped
                        checkAndRequestFullScreenIntent();
                    })
                    .setCancelable(false)
                    .show();
        } else {
            // Check full screen intent permission
            checkAndRequestFullScreenIntent();
        }
    }
    
    private void checkAndRequestFullScreenIntent() {
        // Check full screen intent permission (Android 14+)
        if (Build.VERSION.SDK_INT >= 34) {
            NotificationManager notificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);
            if (notificationManager != null && !notificationManager.canUseFullScreenIntent()) {
                new AlertDialog.Builder(this)
                        .setTitle("🔓 Allow Lock Screen Alarms")
                        .setMessage("To wake your phone and show alarms on the lock screen, please enable 'Display over other apps' permission.\n\nThis ensures you never miss a medicine reminder!")
                        .setPositiveButton("Open Settings", (d, w) -> {
                            try {
                                Intent intent = new Intent(android.provider.Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT);
                                intent.setData(Uri.parse("package:" + getPackageName()));
                                startActivity(intent);
                                Toast.makeText(this, "✅ Enable 'Use full screen intent' to wake phone for alarms", Toast.LENGTH_LONG).show();
                            } catch (Exception e) {
                                Toast.makeText(this, "Please enable full screen intent in app settings", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton("Skip", (d, w) -> {
                            Toast.makeText(this, "⚠️ Alarms may not wake phone on lock screen", Toast.LENGTH_LONG).show();
                        })
                        .setCancelable(false)
                        .show();
            } else {
                Toast.makeText(this, "✅ All permissions granted! Alarms will work perfectly on lock screen.", Toast.LENGTH_LONG).show();
            }
        } else {
            Toast.makeText(this, "✅ All permissions granted! Alarms will work perfectly.", Toast.LENGTH_LONG).show();
        }
    }
    
    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == NOTIFICATION_PERMISSION_CODE) {
            boolean allGranted = true;
            for (int result : grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED) {
                    allGranted = false;
                    break;
                }
            }
            
            if (allGranted) {
                Toast.makeText(this, "✅ Notification permission granted!", Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, "⚠️ Notification permission denied. Alarms may not show.", Toast.LENGTH_LONG).show();
            }
            
            // After notification permission, check alarm permission
            checkAndRequestAlarmPermission();
        }
    }

    /**
     * Check if battery optimization is disabled for reliable alarms
     */
    private void checkBatteryOptimization() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            PowerManager powerManager = (PowerManager) getSystemService(POWER_SERVICE);
            String packageName = getPackageName();
            
            if (powerManager != null && !powerManager.isIgnoringBatteryOptimizations(packageName)) {
                // Only show once per session
                if (!prefs.getBoolean("battery_optimization_shown", false)) {
                    new AlertDialog.Builder(this)
                            .setTitle("Battery Optimization")
                            .setMessage("To ensure alarms work reliably even when your phone is locked or in Doze mode, please disable battery optimization for MedTrack.\n\nThis allows alarms to ring at the exact scheduled time.")
                            .setPositiveButton("Disable Optimization", (dialog, which) -> {
                                try {
                                    Intent intent = new Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS);
                                    intent.setData(Uri.parse("package:" + packageName));
                                    startActivity(intent);
                                } catch (Exception e) {
                                    Toast.makeText(this, "Unable to open battery settings", Toast.LENGTH_SHORT).show();
                                }
                                prefs.edit().putBoolean("battery_optimization_shown", true).apply();
                            })
                            .setNegativeButton("Later", (dialog, which) -> {
                                prefs.edit().putBoolean("battery_optimization_shown", true).apply();
                            })
                            .show();
                }
            }
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        loadProfileImage(); // Reload profile image when returning from ProfileActivity
        
        // Update welcome name
        String username = prefs.getString("username", "User");
        textWelcome.setText("Hello, " + username + "!");
        
        // Reset session flag so permissions are checked again if user returns from settings
        prefs.edit().putBoolean("permission_dialog_shown_this_session", false).apply();
        
        // Check permissions again in case user granted them in settings
        checkAndRequestPermissionsIfNeeded();
    }
    
    @Override
    public void onBackPressed() {
        // Check if alarm is active
        Intent intent = getIntent();
        if (intent != null && intent.getBooleanExtra("alarm_active", false)) {
            // Don't allow back button during alarm - show message instead
            Toast.makeText(this, "⏰ Alarm is active! Use notification buttons to stop.", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Normal back button behavior when no alarm
        super.onBackPressed();
    }
}
