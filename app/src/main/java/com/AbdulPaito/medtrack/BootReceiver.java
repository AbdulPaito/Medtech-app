package com.AbdulPaito.medtrack;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import com.AbdulPaito.medtrack.database.DatabaseHelper;
import com.AbdulPaito.medtrack.database.Medicine;
import java.util.List;

/**
 * BootReceiver - Reschedules all alarms after device reboot
 */
public class BootReceiver extends BroadcastReceiver {

    private static final String TAG = "BootReceiver";

    @Override
    public void onReceive(Context context, Intent intent) {
        if (Intent.ACTION_BOOT_COMPLETED.equals(intent.getAction())) {
            Log.d(TAG, "Device boot completed, rescheduling alarms...");
            
            // Wait a bit for system to be ready
            android.os.Handler handler = new android.os.Handler();
            handler.postDelayed(() -> {
                try {
                    // Reschedule all medicine alarms
                    DatabaseHelper dbHelper = new DatabaseHelper(context);
                    List<Medicine> medicines = dbHelper.getAllMedicines();
                    
                    AlarmScheduler scheduler = new AlarmScheduler(context);
                    int rescheduledCount = 0;
                    
                    for (Medicine medicine : medicines) {
                        scheduler.scheduleMedicineAlarm(medicine);
                        rescheduledCount++;
                    }
                    
                    Log.d(TAG, "Rescheduled " + rescheduledCount + " medicine alarms");
                } catch (Exception e) {
                    Log.e(TAG, "Error rescheduling alarms", e);
                }
            }, 5000); // Wait 5 seconds after boot
        }
    }
}
