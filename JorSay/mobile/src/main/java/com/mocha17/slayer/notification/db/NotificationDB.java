package com.mocha17.slayer.notification.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.mocha17.slayer.notification.db.NotificationDBContract.NotificationData;
import com.mocha17.slayer.utils.Logger;

/**
 * Created by Chaitanya on 6/20/15.
 */
/*package-private*/ class NotificationDB extends SQLiteOpenHelper {
    public static final String DATABASE_NAME = "NotificationDB.db";
    private static int DATABASE_VERSION = 1; //to be updated on schema change

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA = ",";

    private static final String SQL_CREATE_ENTRIES_NOTIFICATION_DATA =
            "CREATE TABLE " + NotificationData.TABLE_NAME + " (" +
                    NotificationData._ID + INTEGER_TYPE + " PRIMARY KEY," +
                    NotificationData.COLUMN_NAME_NOTIFICATION_ID + INTEGER_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_PACKAGE_NAME + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_TITLE + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_TEXT + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_TITLE_BIG + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_BIG_TEXT + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_SUMMARY + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_TEXT_LINES + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_SUBTEXT + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_TICKER_TEXT + TEXT_TYPE + COMMA +
                    NotificationData.COLUMN_NAME_WHEN + TEXT_TYPE + " )";

    private static final String SQL_DELETE_ENTRIES_NOTIFICATION_DATA =
            "DROP TABLE IF EXISTS " + NotificationData.TABLE_NAME;

    public NotificationDB(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_ENTRIES_NOTIFICATION_DATA);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        Logger.d(this, "onUpgrade from " + oldVersion + " to " + newVersion);
        db.execSQL(SQL_DELETE_ENTRIES_NOTIFICATION_DATA);
        DATABASE_VERSION = newVersion;
        onCreate(db);
    }
}