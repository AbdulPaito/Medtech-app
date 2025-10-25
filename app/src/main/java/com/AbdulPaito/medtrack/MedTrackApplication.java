package com.AbdulPaito.medtrack;

import android.app.Application;
import android.content.SharedPreferences;
import androidx.appcompat.app.AppCompatDelegate;

/**
 * Application class to set app-wide configurations
 */
public class MedTrackApplication extends Application {

    @Override
    public void onCreate() {
        super.onCreate();
        
        // Force light mode always - ignore system dark mode
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
    }
}
