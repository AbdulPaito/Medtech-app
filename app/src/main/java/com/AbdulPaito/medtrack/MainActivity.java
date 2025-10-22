package com.AbdulPaito.medtrack;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Build;
import android.os.Bundle;
import android.util.Base64;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

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
        setContentView(R.layout.activity_main);

        databaseHelper = new DatabaseHelper(this);
        prefs = getSharedPreferences("MedTrackPrefs", MODE_PRIVATE);
        
        initViews();
        setupButtons();
        setupBottomNavigation();
        setupWelcomeName();
        updateStats();
        
        // Request permissions on first launch
        if (!prefs.getBoolean("permissions_requested", false)) {
            requestNecessaryPermissions();
        }
    }

    private void initViews() {
        textMedicineCount = findViewById(R.id.text_medicine_count);
        textNextReminder = findViewById(R.id.text_next_reminder);
        textWelcome = findViewById(R.id.text_welcome);
        imgUserProfile = findViewById(R.id.img_user_profile);
        cardProfileImage = findViewById(R.id.card_profile_image);
        
        // Load and display user profile image
        loadProfileImage();
        
        // Profile image click listener
        cardProfileImage.setOnClickListener(v -> {
            Intent intent = new Intent(this, ProfileActivity.class);
            startActivity(intent);
        });
    }
    
    private void loadProfileImage() {
        String avatarType = prefs.getString("avatar_type", "custom");
        String profileImageBase64 = prefs.getString("profile_image", "");
        
        if (!profileImageBase64.isEmpty()) {
            // Load custom photo
            Bitmap bitmap = base64ToBitmap(profileImageBase64);
            imgUserProfile.setImageBitmap(bitmap);
            cardProfileImage.setCardBackgroundColor(getResources().getColor(R.color.primary));
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
    }
    
    private Bitmap base64ToBitmap(String base64Str) {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
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
        int count = databaseHelper.getMedicineCount();
        int adherenceRate = databaseHelper.getAdherenceRate();
        int streak = databaseHelper.getCurrentStreak();
        
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
    }

    private void requestNecessaryPermissions() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Welcome to MedTrack! 💊");
        builder.setMessage("To ensure your medicine reminders work perfectly, we need to:\n\n" +
                "✅ Send notifications\n" +
                "✅ Schedule exact alarms\n" +
                "✅ Work even when app is closed\n\n" +
                "Please allow these permissions in the next screens.");
        builder.setPositiveButton("Continue", (dialog, which) -> {
            // Request notification permission for Android 13+
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(this,
                            new String[]{Manifest.permission.POST_NOTIFICATIONS},
                            NOTIFICATION_PERMISSION_CODE);
                }
            }
            
            // Request exact alarm permission
            AlarmScheduler scheduler = new AlarmScheduler(this);
            if (!scheduler.canScheduleExactAlarms()) {
                scheduler.requestExactAlarmPermission();
            }
            
            // Mark as requested
            prefs.edit().putBoolean("permissions_requested", true).apply();
        });
        builder.setCancelable(false);
        builder.show();
    }

    @Override
    protected void onResume() {
        super.onResume();
        updateStats();
        loadProfileImage(); // Reload profile image when returning from ProfileActivity
        
        // Update welcome name
        String username = prefs.getString("username", "User");
        textWelcome.setText("Hello, " + username + "!");
    }
}
