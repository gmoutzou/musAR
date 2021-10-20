package com.gmoutzou.musar.utils;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import prof.onto.Profile;

public class DatabaseHelper extends SQLiteOpenHelper {

    // Database Version
    private static final int DATABASE_VERSION = 1;

    // Database Name
    private static final String DATABASE_NAME = "profiledb";

    private static final String TABLE_NAME = "profiles";
    private static final String COLUMN_PROFILE_ACCOUNT = "pr_account";
    private static final String COLUMN_PROFILE_NAME = "pr_name";
    private static final String CREATE_TABLE =
            "CREATE TABLE " + TABLE_NAME
                    + "("
                    + COLUMN_PROFILE_ACCOUNT + " TEXT PRIMARY KEY, "
                    + COLUMN_PROFILE_NAME + " TEXT"
                    + ")";


    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    // Creating Tables
    @Override
    public void onCreate(SQLiteDatabase db) {
        // create profiles table
        db.execSQL(CREATE_TABLE);
    }

    // Upgrading database
    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Drop older table if existed
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);

        // Create tables again
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public boolean insertProfileToDB(SQLiteDatabase db, Profile profile) {
        long id = 0;

        ContentValues values = new ContentValues();
        // `id` and `timestamp` will be inserted automatically.
        // no need to add them
        values.put(COLUMN_PROFILE_ACCOUNT, profile.getAccount());
        values.put(COLUMN_PROFILE_NAME, profile.getName());

        // insert row
        id = db.insert(TABLE_NAME, null, values);

        // return newly inserted row id
        return id > 0;
    }

    public Profile getProfileFromDB(SQLiteDatabase db, String account) {
        Profile profile = null;

        Cursor cursor = db.query(TABLE_NAME,
                new String[]{COLUMN_PROFILE_ACCOUNT, COLUMN_PROFILE_NAME},
                COLUMN_PROFILE_ACCOUNT + " = ?",
                new String[]{account}, null, null, null, null);

        if (cursor != null) {
            if (cursor.moveToFirst()) {
                profile = new Profile();
                profile.setAccount(cursor.getString(cursor.getColumnIndex(COLUMN_PROFILE_ACCOUNT)));
                profile.setName(cursor.getString(cursor.getColumnIndex(COLUMN_PROFILE_NAME)));
            }
            // close cursor
            cursor.close();
        }

        return profile;
    }

    public boolean updateProfileInDB(SQLiteDatabase db, Profile profile) {
        long count = 0;

        ContentValues values = new ContentValues();
        values.put(COLUMN_PROFILE_ACCOUNT, profile.getAccount());
        values.put(COLUMN_PROFILE_NAME, profile.getName());
        count = db.update(TABLE_NAME, values, COLUMN_PROFILE_ACCOUNT + " = ?",
                new String[]{profile.getAccount()});

        return count > 0;
    }

    public boolean deleteProfileFromDB(SQLiteDatabase db, Profile profile) {
        long count = 0;

        count = db.delete(TABLE_NAME, COLUMN_PROFILE_ACCOUNT + " = ?",
                new String[]{profile.getAccount()});
        
        return count > 0;
    }
}