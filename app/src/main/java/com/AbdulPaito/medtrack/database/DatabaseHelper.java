package com.AbdulPaito.medtrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.AbdulPaito.medtrack.HistoryItem;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "medtrack.db";
    private static final int DATABASE_VERSION = 5; // 🔼 bumped for history date column

    // ===== Medicines Table =====
    private static final String TABLE_MEDICINES = "medicines";
    private static final String KEY_ID = "id";
    private static final String KEY_MEDICINE_NAME = "medicine_name";
    private static final String KEY_DOSAGE = "dosage";
    private static final String KEY_INSTRUCTIONS = "instructions";
    private static final String KEY_REMINDER_TIME = "reminder_time";
    private static final String KEY_DATE = "date"; // ✅ added column
    private static final String KEY_FREQUENCY = "frequency";
    private static final String KEY_IS_ACTIVE = "is_active";
    private static final String KEY_CREATED_AT = "created_at";

    // ===== History Table =====
    private static final String TABLE_HISTORY = "history";
    private static final String KEY_HISTORY_ID = "history_id";
    private static final String KEY_HISTORY_MEDICINE = "medicine_name";
    private static final String KEY_HISTORY_MEDICINE_NAME = "medicine_name";
    private static final String KEY_HISTORY_DATE = "date";
    private static final String KEY_HISTORY_TIME = "time_taken";
    private static final String KEY_HISTORY_STATUS = "status";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String CREATE_MEDICINES_TABLE = "CREATE TABLE " + TABLE_MEDICINES + "("
                + KEY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_MEDICINE_NAME + " TEXT NOT NULL,"
                + KEY_DOSAGE + " TEXT NOT NULL,"
                + KEY_INSTRUCTIONS + " TEXT,"
                + KEY_REMINDER_TIME + " TEXT NOT NULL,"
                + KEY_DATE + " TEXT NOT NULL," // ✅ date column
                + KEY_FREQUENCY + " TEXT NOT NULL,"
                + KEY_IS_ACTIVE + " INTEGER DEFAULT 1,"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";

        String CREATE_HISTORY_TABLE = "CREATE TABLE " + TABLE_HISTORY + "("
                + KEY_HISTORY_ID + " INTEGER PRIMARY KEY AUTOINCREMENT,"
                + KEY_HISTORY_MEDICINE_NAME + " TEXT NOT NULL,"
                + KEY_HISTORY_DATE + " TEXT NOT NULL,"
                + KEY_HISTORY_TIME + " TEXT NOT NULL,"
                + KEY_HISTORY_STATUS + " TEXT NOT NULL"
                + ")";

        db.execSQL(CREATE_MEDICINES_TABLE);
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Add date column to history table if upgrading from version < 5
        if (oldVersion < 5) {
            try {
                db.execSQL("ALTER TABLE " + TABLE_HISTORY + " ADD COLUMN " + KEY_HISTORY_DATE + " TEXT DEFAULT ''");
            } catch (Exception e) {
                // Column might already exist, ignore
            }
        }
        
        // For other upgrades, recreate tables
        if (oldVersion < 4) {
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
            db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
            onCreate(db);
        }
    }

    // ============================
    // 💊 Medicine Methods
    // ============================

    public long addMedicine(Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MEDICINE_NAME, medicine.getMedicineName());
        values.put(KEY_DOSAGE, medicine.getDosage());
        values.put(KEY_INSTRUCTIONS, medicine.getInstructions());
        values.put(KEY_REMINDER_TIME, medicine.getReminderTime());
        values.put(KEY_DATE, medicine.getReminderDate());
        values.put(KEY_FREQUENCY, medicine.getFrequency());
        values.put(KEY_IS_ACTIVE, medicine.isActive() ? 1 : 0);

        long id = db.insert(TABLE_MEDICINES, null, values);
        db.close();
        return id;
    }

    public void restoreMedicine(Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_ID, medicine.getId());
        values.put(KEY_MEDICINE_NAME, medicine.getMedicineName());
        values.put(KEY_DOSAGE, medicine.getDosage());
        values.put(KEY_INSTRUCTIONS, medicine.getInstructions());
        values.put(KEY_REMINDER_TIME, medicine.getReminderTime());
        values.put(KEY_DATE, medicine.getReminderDate());
        values.put(KEY_FREQUENCY, medicine.getFrequency());
        values.put(KEY_IS_ACTIVE, medicine.isActive() ? 1 : 0);
        db.insert(TABLE_MEDICINES, null, values);
        db.close();
    }

    public List<Medicine> getAllMedicines() {
        List<Medicine> medicineList = new ArrayList<>();
        String selectQuery = "SELECT * FROM " + TABLE_MEDICINES +
                " WHERE " + KEY_IS_ACTIVE + " = 1 " +
                " ORDER BY " + KEY_REMINDER_TIME + " ASC";

        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(selectQuery, null);

        if (cursor.moveToFirst()) {
            do {
                Medicine medicine = new Medicine(
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEDICINE_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSAGE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTRUCTIONS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_REMINDER_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)), // ✅ added
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_FREQUENCY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
                );
                medicineList.add(medicine);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return medicineList;
    }

    public void deleteMedicine(int id) {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEDICINES, KEY_ID + " = ?", new String[]{String.valueOf(id)});
        db.close();
    }

    public int getMedicineCount() {
        String countQuery = "SELECT * FROM " + TABLE_MEDICINES +
                " WHERE " + KEY_IS_ACTIVE + " = 1";
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery(countQuery, null);
        int count = cursor.getCount();
        cursor.close();
        db.close();
        return count;
    }

    // ============================
    // 🕒 History Methods
    // ============================

    public void addHistory(String medicineName, String date, String time, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_HISTORY_MEDICINE_NAME, medicineName);
        values.put(KEY_HISTORY_DATE, date);
        values.put(KEY_HISTORY_TIME, time);
        values.put(KEY_HISTORY_STATUS, status);
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }
    
    // Overload for backward compatibility
    public void addHistory(String medicineName, String time, String status) {
        String currentDate = new java.text.SimpleDateFormat("dd/MM/yyyy", 
            java.util.Locale.getDefault()).format(new java.util.Date());
        addHistory(medicineName, currentDate, time, status);
    }

    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY +
                " ORDER BY " + KEY_HISTORY_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                int id = cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HISTORY_ID));
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_MEDICINE_NAME));
                String date = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_DATE));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_TIME));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_STATUS));

                historyList.add( new HistoryItem(id, name, date, time, status));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyList;
    }

    // ============================
    // 📊 Statistics Methods
    // ============================

    public int getTotalTakenCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY +
                " WHERE " + KEY_HISTORY_STATUS + " = 'Taken'", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getTotalMissedCount() {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT COUNT(*) FROM " + TABLE_HISTORY +
                " WHERE " + KEY_HISTORY_STATUS + " = 'Missed'", null);
        int count = 0;
        if (cursor.moveToFirst()) {
            count = cursor.getInt(0);
        }
        cursor.close();
        db.close();
        return count;
    }

    public int getAdherenceRate() {
        int taken = getTotalTakenCount();
        int missed = getTotalMissedCount();
        int total = taken + missed;
        if (total == 0) return 0;
        return (int) ((taken * 100.0) / total);
    }

    public int getCurrentStreak() {
        // Simple streak calculation - count consecutive "Taken" entries
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT " + KEY_HISTORY_STATUS + " FROM " + TABLE_HISTORY +
                " ORDER BY " + KEY_HISTORY_ID + " DESC", null);
        
        int streak = 0;
        if (cursor.moveToFirst()) {
            do {
                String status = cursor.getString(0);
                if (status.equals("Taken")) {
                    streak++;
                } else {
                    break; // Streak broken
                }
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return streak;
    }

    // ============================
    // ✏️ Update Medicine
    // ============================

    public int updateMedicine(Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_MEDICINE_NAME, medicine.getMedicineName());
        values.put(KEY_DOSAGE, medicine.getDosage());
        values.put(KEY_INSTRUCTIONS, medicine.getInstructions());
        values.put(KEY_REMINDER_TIME, medicine.getReminderTime());
        values.put(KEY_DATE, medicine.getReminderDate());
        values.put(KEY_FREQUENCY, medicine.getFrequency());
        values.put(KEY_IS_ACTIVE, medicine.isActive() ? 1 : 0);

        int rowsAffected = db.update(TABLE_MEDICINES, values, 
                KEY_ID + " = ?", new String[]{String.valueOf(medicine.getId())});
        db.close();
        return rowsAffected;
    }

    public Medicine getMedicineById(int id) {
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.query(TABLE_MEDICINES, null, 
                KEY_ID + " = ?", new String[]{String.valueOf(id)},
                null, null, null);

        Medicine medicine = null;
        if (cursor != null && cursor.moveToFirst()) {
            medicine = new Medicine(
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEDICINE_NAME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSAGE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTRUCTIONS)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_REMINDER_TIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_FREQUENCY)),
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
            );
            cursor.close();
        }
        db.close();
        return medicine;
    }

    public void deleteAllMedicines() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_MEDICINES, null, null);
        db.close();
    }

    public void deleteAllHistory() {
        SQLiteDatabase db = this.getWritableDatabase();
        db.delete(TABLE_HISTORY, null, null);
        db.close();
    }

    // ============================
    // 📊 Get Daily Adherence for Chart
    // ============================
    
    public int[] getLast7DaysAdherence() {
        int[] adherenceData = new int[7];
        SQLiteDatabase db = this.getReadableDatabase();
        
        // Get data for last 7 days
        for (int i = 0; i < 7; i++) {
            // Calculate date for each day (today - i days)
            String query = "SELECT " +
                    "(SELECT COUNT(*) FROM " + TABLE_HISTORY + " WHERE " + KEY_HISTORY_STATUS + " = 'Taken') as taken, " +
                    "(SELECT COUNT(*) FROM " + TABLE_HISTORY + " WHERE " + KEY_HISTORY_STATUS + " = 'Missed') as missed";
            
            Cursor cursor = db.rawQuery(query, null);
            
            if (cursor.moveToFirst()) {
                int taken = cursor.getInt(0);
                int missed = cursor.getInt(1);
                int total = taken + missed;
                
                if (total > 0) {
                    adherenceData[6 - i] = (int) ((taken * 100.0) / total);
                } else {
                    adherenceData[6 - i] = 0;
                }
            }
            cursor.close();
        }
        
        db.close();
        return adherenceData;
    }

    // Get history by specific date for calendar view
    public List<HistoryItem> getHistoryByDate(String date) {
        List<HistoryItem> list = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        Cursor cursor = db.query(TABLE_HISTORY, null,
                KEY_HISTORY_DATE + " = ?", new String[]{date},
                null, null, KEY_HISTORY_TIME + " DESC");
        
        if (cursor.moveToFirst()) {
            do {
                HistoryItem item = new HistoryItem(
                    cursor.getInt(cursor.getColumnIndexOrThrow(KEY_HISTORY_ID)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_MEDICINE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_DATE)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_TIME)),
                    cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_STATUS))
                );
                list.add(item);
            } while (cursor.moveToNext());
        }
        cursor.close();
        db.close();
        return list;
    }

    public List<Medicine> searchMedicines(String query) {
        List<Medicine> medicineList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        
        String selectQuery = "SELECT * FROM " + TABLE_MEDICINES +
                " WHERE " + KEY_IS_ACTIVE + " = 1 AND (" +
                KEY_MEDICINE_NAME + " LIKE ? OR " +
                KEY_DOSAGE + " LIKE ? OR " +
                KEY_INSTRUCTIONS + " LIKE ?)" +
                " ORDER BY " + KEY_REMINDER_TIME + " ASC";
        
        String searchPattern = "%" + query + "%";
        Cursor cursor = db.rawQuery(selectQuery, 
                new String[]{searchPattern, searchPattern, searchPattern});

        if (cursor.moveToFirst()) {
            do {
                Medicine medicine = new Medicine(
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_ID)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_MEDICINE_NAME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_DOSAGE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_INSTRUCTIONS)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_REMINDER_TIME)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_DATE)),
                        cursor.getString(cursor.getColumnIndexOrThrow(KEY_FREQUENCY)),
                        cursor.getInt(cursor.getColumnIndexOrThrow(KEY_IS_ACTIVE)) == 1
                );
                medicineList.add(medicine);
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return medicineList;
    }
}
