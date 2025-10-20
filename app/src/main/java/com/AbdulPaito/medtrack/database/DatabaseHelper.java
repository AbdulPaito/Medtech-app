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
    private static final int DATABASE_VERSION = 4; // 🔼 bumped again (important for schema update)

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
    private static final String KEY_HISTORY_MEDICINE_NAME = "medicine_name";
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
                + KEY_HISTORY_TIME + " TEXT NOT NULL,"
                + KEY_HISTORY_STATUS + " TEXT NOT NULL"
                + ")";

        db.execSQL(CREATE_MEDICINES_TABLE);
        db.execSQL(CREATE_HISTORY_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_HISTORY);
        onCreate(db);
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

    public void addHistory(String medicineName, String time, String status) {
        SQLiteDatabase db = this.getWritableDatabase();
        ContentValues values = new ContentValues();
        values.put(KEY_HISTORY_MEDICINE_NAME, medicineName);
        values.put(KEY_HISTORY_TIME, time);
        values.put(KEY_HISTORY_STATUS, status);
        db.insert(TABLE_HISTORY, null, values);
        db.close();
    }

    public List<HistoryItem> getAllHistory() {
        List<HistoryItem> historyList = new ArrayList<>();
        SQLiteDatabase db = this.getReadableDatabase();
        Cursor cursor = db.rawQuery("SELECT * FROM " + TABLE_HISTORY +
                " ORDER BY " + KEY_HISTORY_ID + " DESC", null);

        if (cursor.moveToFirst()) {
            do {
                String name = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_MEDICINE_NAME));
                String time = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_TIME));
                String status = cursor.getString(cursor.getColumnIndexOrThrow(KEY_HISTORY_STATUS));

                historyList.add(new HistoryItem(name, time, status));
            } while (cursor.moveToNext());
        }

        cursor.close();
        db.close();
        return historyList;
    }
}
