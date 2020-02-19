package com.welcome.gpsmocktest.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import androidx.annotation.Nullable;

public class SearchDBHelper extends SQLiteOpenHelper {

    private static final int DB_VERSION = 1;
    private static final String DB_NAME = "SearcHistory.db";
    public static final String TABLE_NAME = "HistorySearch";
    private static final String CREATE_TABLE = "create table if not exists " + TABLE_NAME +
            " (ID INTEGER PRIMARY KEY AUTOINCREMENT, SearchKey TEXT NOT NULL, Description TEXT, TimeStamp BIGINT NOT NULL, IsLocate INTEGER NOT NULL, WGS84Longitude TEXT, WGS84Latitude TEXT, BD09Longitude TEXT, BD09Latitude TEXT)";

    public SearchDBHelper(@Nullable Context context) {
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
