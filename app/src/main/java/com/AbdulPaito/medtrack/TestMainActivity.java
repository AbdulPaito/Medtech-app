package com.AbdulPaito.medtrack;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Test MainActivity - Minimal version to test if app can start
 */
public class TestMainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        try {
            // Create a simple layout programmatically
            LinearLayout layout = new LinearLayout(this);
            layout.setOrientation(LinearLayout.VERTICAL);
            layout.setPadding(50, 50, 50, 50);
            
            TextView textView = new TextView(this);
            textView.setText("MedTrack Test - App is working!");
            textView.setTextSize(18);
            textView.setPadding(0, 0, 0, 30);
            
            Button button = new Button(this);
            button.setText("Go to Main App");
            button.setOnClickListener(v -> {
                try {
                    Intent intent = new Intent(this, MainActivity.class);
                    startActivity(intent);
                } catch (Exception e) {
                    Toast.makeText(this, "Error opening MainActivity: " + e.getMessage(), Toast.LENGTH_LONG).show();
                }
            });
            
            layout.addView(textView);
            layout.addView(button);
            setContentView(layout);
            
            Toast.makeText(this, "App started successfully!", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error: " + e.getMessage(), Toast.LENGTH_LONG).show();
        }
    }
}
