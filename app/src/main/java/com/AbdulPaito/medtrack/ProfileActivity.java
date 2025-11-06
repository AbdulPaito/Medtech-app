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
import androidx.core.content.FileProvider;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.card.MaterialCardView;
import com.google.android.material.textfield.TextInputEditText;
import com.yalantis.ucrop.UCrop;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;

public class ProfileActivity extends AppCompatActivity {

    private ImageView imgProfile;
    private MaterialButton btnChangePicture, btnSaveProfile;
    private MaterialCardView cardMaleAvatar, cardFemaleAvatar;
    private TextInputEditText editName, editAge;
    private RadioGroup radioGender;
    private RadioButton radioMale, radioFemale;
    
    private SharedPreferences prefs;
    private String selectedAvatarType = "custom"; // male, female, custom, photo
    private String profileImageBase64 = "";
    
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private Uri tempCameraImageUri;

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
                        if (imageUri != null) {
                            startCropActivity(imageUri);
                        }
                    }
                });

        // Camera launcher
        cameraLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == RESULT_OK) {
                        if (tempCameraImageUri != null) {
                            startCropActivity(tempCameraImageUri);
                        }
                    }
                });
    }
    
    /**
     * Start UCrop activity for circular image cropping
     */
    private void startCropActivity(Uri sourceUri) {
        try {
            String destinationFileName = "cropped_profile_" + System.currentTimeMillis() + ".jpg";
            File destinationFile = new File(getCacheDir(), destinationFileName);
            Uri destinationUri = Uri.fromFile(destinationFile);
            
            UCrop.Options options = new UCrop.Options();
            options.setCircleDimmedLayer(true); // Circular crop overlay
            options.setShowCropGrid(false);
            options.setShowCropFrame(false);
            options.setCompressionQuality(95); // Higher quality
            options.setMaxBitmapSize(2000); // Larger max size for better quality
            options.setToolbarTitle("Crop Profile Picture");
            options.setStatusBarColor(getResources().getColor(R.color.primary, null));
            options.setToolbarColor(getResources().getColor(R.color.primary, null));
            options.setToolbarWidgetColor(getResources().getColor(android.R.color.white, null));
            options.setFreeStyleCropEnabled(false); // Lock to circle
            options.setHideBottomControls(false);
            options.setCropGridStrokeWidth(2);
            
            UCrop.of(sourceUri, destinationUri)
                    .withAspectRatio(1, 1) // Square aspect ratio for perfect circle
                    .withMaxResultSize(1000, 1000) // Good size for display
                    .withOptions(options)
                    .start(this);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to start crop: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
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
        try {
            Intent cameraIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            String fileName = "camera_photo_" + System.currentTimeMillis() + ".jpg";
            File photoFile = new File(getCacheDir(), fileName);
            tempCameraImageUri = FileProvider.getUriForFile(this, 
                    getApplicationContext().getPackageName() + ".fileprovider", photoFile);
            cameraIntent.putExtra(MediaStore.EXTRA_OUTPUT, tempCameraImageUri);
            cameraLauncher.launch(cameraIntent);
        } catch (Exception e) {
            Toast.makeText(this, "Failed to open camera: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        imagePickerLauncher.launch(galleryIntent);
    }

    private void updateAvatarSelection() {
        // Reset all borders
        cardMaleAvatar.setStrokeColor(getResources().getColor(android.R.color.transparent, null));
        cardFemaleAvatar.setStrokeColor(getResources().getColor(android.R.color.transparent, null));

        // Highlight selected
        int selectedColor = getResources().getColor(R.color.primary, null);
        switch (selectedAvatarType) {
            case "male":
                cardMaleAvatar.setStrokeColor(selectedColor);
                break;
            case "female":
                cardFemaleAvatar.setStrokeColor(selectedColor);
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
            // Create circular cropped bitmap
            int size = Math.min(bitmap.getWidth(), bitmap.getHeight());
            Bitmap circularBitmap = Bitmap.createBitmap(size, size, Bitmap.Config.ARGB_8888);
            android.graphics.Canvas canvas = new android.graphics.Canvas(circularBitmap);
            
            android.graphics.Paint paint = new android.graphics.Paint();
            paint.setAntiAlias(true);
            android.graphics.BitmapShader shader = new android.graphics.BitmapShader(bitmap, 
                    android.graphics.Shader.TileMode.CLAMP, android.graphics.Shader.TileMode.CLAMP);
            paint.setShader(shader);
            
            float radius = size / 2f;
            canvas.drawCircle(radius, radius, radius, paint);
            
            // Resize to optimal size
            int maxSize = 600;
            if (size > maxSize) {
                circularBitmap = Bitmap.createScaledBitmap(circularBitmap, maxSize, maxSize, true);
            }
            
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            circularBitmap.compress(Bitmap.CompressFormat.PNG, 100, byteArrayOutputStream); // PNG for transparency
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
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (resultCode == RESULT_OK && requestCode == UCrop.REQUEST_CROP) {
            final Uri resultUri = UCrop.getOutput(data);
            if (resultUri != null) {
                try {
                    InputStream inputStream = getContentResolver().openInputStream(resultUri);
                    if (inputStream != null) {
                        Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                        if (bitmap != null) {
                            imgProfile.setImageBitmap(bitmap);
                            profileImageBase64 = bitmapToBase64(bitmap);
                            selectedAvatarType = "photo";
                            updateAvatarSelection();
                            Toast.makeText(this, "Profile picture updated! 📷", Toast.LENGTH_SHORT).show();
                        }
                        inputStream.close();
                    }
                } catch (Exception e) {
                    Toast.makeText(this, "Failed to load cropped image", Toast.LENGTH_SHORT).show();
                }
            }
        } else if (resultCode == UCrop.RESULT_ERROR) {
            final Throwable cropError = UCrop.getError(data);
            Toast.makeText(this, "Crop error: " + (cropError != null ? cropError.getMessage() : "Unknown"), 
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
