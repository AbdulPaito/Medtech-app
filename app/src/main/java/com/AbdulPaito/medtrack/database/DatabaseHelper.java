package com.AbdulPaito.medtrack.database;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import java.util.ArrayList;
import java.util.List;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "medtrack.db";
    private static final int DATABASE_VERSION = 1;
    private static final String TABLE_MEDICINES = "medicines";

    private static final String KEY_ID = "id";
    private static final String KEY_MEDICINE_NAME = "medicine_name";
    private static final String KEY_DOSAGE = "dosage";
    private static final String KEY_INSTRUCTIONS = "instructions";
    private static final String KEY_REMINDER_TIME = "reminder_time";
    private static final String KEY_FREQUENCY = "frequency";
    private static final String KEY_IS_ACTIVE = "is_active";
    private static final String KEY_CREATED_AT = "created_at";

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
                + KEY_FREQUENCY + " TEXT NOT NULL,"
                + KEY_IS_ACTIVE + " INTEGER DEFAULT 1,"
                + KEY_CREATED_AT + " DATETIME DEFAULT CURRENT_TIMESTAMP"
                + ")";

        db.execSQL(CREATE_MEDICINES_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_MEDICINES);
        onCreate(db);
    }

    public long addMedicine(Medicine medicine) {
        SQLiteDatabase db = this.getWritableDatabase();

        ContentValues values = new ContentValues();
        values.put(KEY_MEDICINE_NAME, medicine.getMedicineName());
        values.put(KEY_DOSAGE, medicine.getDosage());
        values.put(KEY_INSTRUCTIONS, medicine.getInstructions());
        values.put(KEY_REMINDER_TIME, medicine.getReminderTime());
        values.put(KEY_FREQUENCY, medicine.getFrequency());
        values.put(KEY_IS_ACTIVE, medicine.isActive() ? 1 : 0);

        long id = db.insert(TABLE_MEDICINES, null, values);
        db.close();
        return id;
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
                        cursor.getInt(0),
                        cursor.getString(1),
                        cursor.getString(2),
                        cursor.getString(3),
                        cursor.getString(4),
                        cursor.getString(5),
                        cursor.getInt(6) == 1
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
        db.delete(TABLE_MEDICINES, KEY_ID + " = ?",
                new String[]{String.valueOf(id)});
        db.close();
    }
}