package com.AbdulPaito.medtrack;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.widget.ImageView;
import android.widget.RadioButton;
import android.widget.RadioGroup;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private MaterialButton btnChangePicture, btnSaveProfile;
    private MaterialCardView cardMaleAvatar, cardFemaleAvatar, cardCustomAvatar;
    private TextInputEditText editName, editAge;
    private RadioGroup radioGender;
    private RadioButton radioMale, radioFemale;
    
    private SharedPreferences prefs;
    private String selectedAvatarType = "custom"; // male, female, custom, photo
    private String profileImageBase64 = "";
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle("My Profile");
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        prefs = getSharedPreferences("MedTrackPrefs", MODE_PRIVATE);
        
        initViews();
        setupImagePickers();
        loadProfile();
        setupListeners();
    }

    private void initViews() {
        imgProfile = findViewById(R.id.img_profile);
        btnChangePicture = findViewById(R.id.btn_change_picture);
        btnSaveProfile = findViewById(R.id.btn_save_profile);
        cardMaleAvatar = findViewById(R.id.card_male_avatar);
        cardFemaleAvatar = findViewById(R.id.card_female_avatar);
        cardCustomAvatar = findViewById(R.id.card_custom_avatar);
        editName = findViewById(R.id.edit_name);
        editAge = findViewById(R.id.edit_age);
        radioGender = findViewById(R.id.radio_gender);
        radioMale = findViewById(R.id.radio_male);
        radioFemale = findViewById(R.id.radio_female);
    }

    private void setupImagePickers() {
        // Image picker from gallery
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Uri imageUri = result.getData().getData();
                        try {
                            InputStream inputStream = getContentResolver().openInputStream(imageUri);
                            if (inputStream != null) {
                                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                                if (bitmap != null) {
                                    imgProfile.setImageBitmap(bitmap);
                                    profileImageBase64 = bitmapToBase64(bitmap);
                                    selectedAvatarType = "photo";
                                    updateAvatarSelection();
                                    Toast.makeText(this, "Image loaded successfully!", Toast.LENGTH_SHORT).show();
                                } else {
                                    Toast.makeText(this, "Failed to decode image", Toast.LENGTH_SHORT).show();
                                }
                                inputStream.close();
                            } else {
                                Toast.makeText(this, "Failed to open image", Toast.LENGTH_SHORT).show();
                            }
                        } catch (Exception e) {
                            Toast.makeText(this, "Failed to load image: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    }
                });

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                        Bitmap photo = (Bitmap) result.getData().getExtras().get("data");
                        if (photo != null) {
                            imgProfile.setImageBitmap(photo);
                            profileImageBase64 = bitmapToBase64(photo);
                            selectedAvatarType = "photo";
                            updateAvatarSelection();
                            Toast.makeText(this, "Photo captured successfully!", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "Failed to capture photo", Toast.LENGTH_SHORT).show();
                        }
                    }
                });
    }

    private void setupListeners() {
        // Change picture button
        btnChangePicture.setOnClickListener(v -> showImagePickerDialog());

        // Male avatar
        cardMaleAvatar.setOnClickListener(v -> {
            selectedAvatarType = "male";
            imgProfile.setImageResource(R.drawable.ic_male);
            profileImageBase64 = "";
            updateAvatarSelection();
        });

        // Female avatar
        cardFemaleAvatar.setOnClickListener(v -> {
            selectedAvatarType = "female";
            imgProfile.setImageResource(R.drawable.ic_female);
            profileImageBase64 = "";
            updateAvatarSelection();
        });

        // Custom avatar
        cardCustomAvatar.setOnClickListener(v -> {
            selectedAvatarType = "custom";
            imgProfile.setImageResource(R.drawable.ic_person);
            profileImageBase64 = "";
            updateAvatarSelection();
        });

        // Save profile
        btnSaveProfile.setOnClickListener(v -> saveProfile());
    }

    private void showImagePickerDialog() {
        String[] options = {"Take Photo", "Choose from Gallery", "Cancel"};
        
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Select Profile Picture");
        builder.setItems(options, (dialog, which) -> {
            if (which == 0) {
                // Take photo
                if (checkCameraPermission()) {
                    openCamera();
                }
            } else if (which == 1) {
                // Choose from gallery
                if (checkStoragePermission()) {
                    openGallery();
                }
            }
        });
        builder.show();
    }

    private boolean checkCameraPermission() {
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CAMERA)
                != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.CAMERA}, 101);
            return false;
        }
        return true;
    }

    private boolean checkStoragePermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_MEDIA_IMAGES)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_MEDIA_IMAGES}, 102);
                return false;
            }
        } else {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_EXTERNAL_STORAGE)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this,
                        new String[]{Manifest.permission.READ_EXTERNAL_STORAGE}, 102);
                return false;
            }
        }
        return true;
    }

    private void openCamera() {
        Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        cameraLauncher.launch(cameraIntent);
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(galleryIntent);
    }

    private void updateAvatarSelection() {
        // Reset all borders
        cardMaleAvatar.setStrokeColor(getResources().getColor(android.R.color.transparent, null));
        cardFemaleAvatar.setStrokeColor(getResources().getColor(android.R.color.transparent, null));
        cardCustomAvatar.setStrokeColor(getResources().getColor(android.R.color.transparent, null));

        // Highlight selected
        int selectedColor = getResources().getColor(R.color.primary, null);
        switch (selectedAvatarType) {
            case "male":
                cardMaleAvatar.setStrokeColor(selectedColor);
                break;
            case "female":
                cardFemaleAvatar.setStrokeColor(selectedColor);
                break;
            case "custom":
                cardCustomAvatar.setStrokeColor(selectedColor);
                break;
        }
    }

    private void loadProfile() {
        // Load saved profile data
        String name = prefs.getString("user_name", "");
        int age = prefs.getInt("user_age", 0);
        String gender = prefs.getString("user_gender", "");
        selectedAvatarType = prefs.getString("avatar_type", "custom");
        profileImageBase64 = prefs.getString("profile_image", "");

        editName.setText(name);
        if (age > 0) {
            editAge.setText(String.valueOf(age));
        }

        if (gender.equals("Male")) {
            radioMale.setChecked(true);
        } else if (gender.equals("Female")) {
            radioFemale.setChecked(true);
        }

        // Load profile image
        if (!profileImageBase64.isEmpty()) {
            Bitmap bitmap = base64ToBitmap(profileImageBase64);
            imgProfile.setImageBitmap(bitmap);
        } else {
            switch (selectedAvatarType) {
                case "male":
                    imgProfile.setImageResource(R.drawable.ic_male);
                    break;
                case "female":
                    imgProfile.setImageResource(R.drawable.ic_female);
                    break;
                default:
                    imgProfile.setImageResource(R.drawable.ic_person);
                    break;
            }
        }

        updateAvatarSelection();
    }

    private void saveProfile() {
        String name = editName.getText().toString().trim();
        String ageStr = editAge.getText().toString().trim();
        int selectedGenderId = radioGender.getCheckedRadioButtonId();
        String gender = "";

        if (selectedGenderId == R.id.radio_male) {
            gender = "Male";
        } else if (selectedGenderId == R.id.radio_female) {
            gender = "Female";
        }

        if (name.isEmpty()) {
            Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show();
            return;
        }

        int age = 0;
        if (!ageStr.isEmpty()) {
            age = Integer.parseInt(ageStr);
        }

        // Save to SharedPreferences
        SharedPreferences.Editor editor = prefs.edit();
        editor.putString("username", name); // For MainActivity
        editor.putString("user_name", name);
        editor.putInt("user_age", age);
        editor.putString("user_gender", gender);
        editor.putString("avatar_type", selectedAvatarType);
        editor.putString("profile_image", profileImageBase64);
        editor.apply();

        Toast.makeText(this, "Profile saved successfully! ✅", Toast.LENGTH_SHORT).show();
        finish();
    }

    private String bitmapToBase64(Bitmap bitmap) {
        try {
            // Resize bitmap to reduce memory usage
            int maxSize = 512; // Maximum dimension
            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            
            if (width > maxSize || height > maxSize) {
                float ratio = Math.min((float) maxSize / width, (float) maxSize / height);
                int newWidth = Math.round(width * ratio);
                int newHeight = Math.round(height * ratio);
                bitmap = Bitmap.createScaledBitmap(bitmap, newWidth, newHeight, true);
            }
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream); // Use JPEG for smaller size
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            return Base64.encodeToString(byteArray, Base64.DEFAULT);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to process image", Toast.LENGTH_SHORT).show();
            return "";
        }
    }

    private Bitmap base64ToBitmap(String base64Str) {
        byte[] decodedBytes = Base64.decode(base64Str, Base64.DEFAULT);
        return BitmapFactory.decodeByteArray(decodedBytes, 0, decodedBytes.length);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions,
                                           @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        
        if (requestCode == 101) { // Camera permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openCamera();
            } else {
                Toast.makeText(this, "Camera permission is required to take photos", Toast.LENGTH_SHORT).show();
            }
        } else if (requestCode == 102) { // Storage permission
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                Toast.makeText(this, "Storage permission is required to select photos", Toast.LENGTH_SHORT).show();
            }
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
