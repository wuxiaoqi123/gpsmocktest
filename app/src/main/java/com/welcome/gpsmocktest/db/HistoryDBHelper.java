package com.welcome.gpsmocktest.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class HistoryDBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "GpsMock.db";
    public static final String TABLE_NAME = "HistoryLocation";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (ID INTEGER PRIMARY KEY AUTOINCREMENT, Location TEXT, WGS84Longitude TEXT NOT NULL, WGS84Latitude TEXT NOT NULL, TimeStamp BIGINT NOT NULL, BD09Longitude TEXT NOT NULL, BD09Latitude TEXT NOT NULL)";

    public HistoryDBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(CREATE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        String sql = "DROP TABLE IF EXISTS " + TABLE_NAME;
        db.execSQL(sql);
        onCreate(db);
    }
}
