package com.mocha17.slayer.notification.db;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.text.TextUtils;

import com.mocha17.slayer.notification.db.NotificationDBContract.NotificationData;
import com.mocha17.slayer.utils.Logger;

/**
 * For working with the NotificationDB.<br>
 * Created by Chaitanya on 6/21/15.
 */
/* NotificationDBOps is a singleton and NotificationDB is package-private, to be used only by the
DB classes. We are, as a result, going to have only one SQLiteOpenHelper instance at runtime.*/
public class NotificationDBOps {
    private SQLiteDatabase notificationDB;
    private SQLiteOpenHelper notificationDBOpenHelper;

    private static final String [] COLUMN_ROW_ID = {NotificationData._ID};
    //(package+notificationID)
    private static final String IS_PRESENT_SELECTION = NotificationData.COLUMN_NAME_PACKAGE_NAME
            + "=?" + " and " + NotificationData.COLUMN_NAME_NOTIFICATION_ID + "=?";
    private static final String WHERE_ROW_ID = NotificationData._ID + "=?";
    private static final String ORDER_BY_WHEN = NotificationData.COLUMN_NAME_WHEN + " DESC";
    private static final String LIMIT_FOR_MOST_RECENT = "1";

    private static NotificationDBOps instance;
    public static synchronized NotificationDBOps get(Context context) {
        if (instance == null) {
            instance = new NotificationDBOps(context);
        }
        return instance;
    }
    private NotificationDBOps(Context context) {
        notificationDBOpenHelper = new NotificationDB(context);
    }

    /** Stores the notification data in DB. If (package+id) combination is already present, existing
     * record will be updated. Can be called from main thread.
     * @param sbn the notification to be stored
     */
    public void storeNotification(StatusBarNotification sbn) {
        if (sbn == null) {
            return;
        }

        Notification notification = sbn.getNotification();
        Bundle notificationExtras = notification.extras;

        ContentValues cv = new ContentValues();
        cv.put(NotificationData.COLUMN_NAME_NOTIFICATION_ID, sbn.getId());
        cv.put(NotificationData.COLUMN_NAME_PACKAGE_NAME, sbn.getPackageName());
        cv.put(NotificationData.COLUMN_NAME_TITLE,
                notificationExtras.getString(Notification.EXTRA_TITLE));
        CharSequence text = notificationExtras.getCharSequence(Notification.EXTRA_TEXT);
        if (!TextUtils.isEmpty(text)) {
            cv.put(NotificationData.COLUMN_NAME_TEXT, text.toString());
        }
        cv.put(NotificationData.COLUMN_NAME_TITLE_BIG,
                notificationExtras.getString(Notification.EXTRA_TITLE_BIG));
        cv.put(NotificationData.COLUMN_NAME_BIG_TEXT,
                notificationExtras.getString(Notification.EXTRA_BIG_TEXT));
        cv.put(NotificationData.COLUMN_NAME_SUMMARY,
                notificationExtras.getString(Notification.EXTRA_SUMMARY_TEXT));
        cv.put(NotificationData.COLUMN_NAME_TEXT_LINES,
                notificationExtras.getString(Notification.EXTRA_TEXT_LINES));
        cv.put(NotificationData.COLUMN_NAME_SUBTEXT,
                notificationExtras.getString(Notification.EXTRA_SUB_TEXT));
        if (!TextUtils.isEmpty(notification.tickerText)) {
            cv.put(NotificationData.COLUMN_NAME_TICKER_TEXT, notification.tickerText.toString());
        }
        cv.put(NotificationData.COLUMN_NAME_WHEN, Long.toString(notification.when));

        insertOrUpdate(cv);
    }

    /**For doing getWritableDatabase() in a separate thread as needed.*/
    private void insertOrUpdate(final ContentValues cv) {
        if (notificationDB != null && notificationDB.isOpen()) {
            insertOrUpdateInternal(cv);
        } else {
            new Thread() {
                @Override
                public void run() {
                    Logger.d(NotificationDBOps.this, "insertOrUpdate - getting WritableDatabase");
                    synchronized (instance) {
                        notificationDB = notificationDBOpenHelper.getWritableDatabase();
                    }
                    insertOrUpdateInternal(cv);
                }
            }.start();
        }
    }

    /** For actually performing the insertOrUpdate operation */
    private void insertOrUpdateInternal(ContentValues cv) {
        //Check if this (package+id) is already present
        String [] selectionArgs = new String[] {
            cv.getAsString(NotificationData.COLUMN_NAME_PACKAGE_NAME),
            cv.getAsString(NotificationData.COLUMN_NAME_NOTIFICATION_ID)
        };
        /* public Cursor query (String table, String[] columns, String selection, String[]
        selectionArgs, String groupBy, String having, String orderBy) */
        Cursor cursor = notificationDB.query(NotificationData.TABLE_NAME, COLUMN_ROW_ID,
                IS_PRESENT_SELECTION, selectionArgs, null, null, ORDER_BY_WHEN);
        if (cursor != null && cursor.getCount() > 0) {
            if (cursor.getCount() > 1) {
                //Will happen only when tag is playing a role.
                //developer.android.com/reference/android/app/NotificationManager.html#
                // notify(java.lang.String, int, android.app.Notification)
                //TODO - Add Tag to the uniqueness tes
                Logger.w(this, "insertOrUpdateInternal found more than one notifications for " +
                        cv.get(NotificationData.COLUMN_NAME_PACKAGE_NAME) + ", " +
                        cv.get(NotificationData.COLUMN_NAME_NOTIFICATION_ID) +
                        ", updating most recent one");
            }
            cursor.moveToFirst();
            int rowId = cursor.getInt(cursor.getColumnIndex(NotificationData._ID));

            Logger.d(NotificationDBOps.this, "insertOrUpdateInternal " +
                    cv.get(NotificationData.COLUMN_NAME_PACKAGE_NAME) + ", " +
                    cv.get(NotificationData.COLUMN_NAME_NOTIFICATION_ID) +
                    " already present, updating");

            notificationDB.update(NotificationData.TABLE_NAME, cv, WHERE_ROW_ID,
                    new String[]{Integer.toString(rowId)});
        } else {
            Logger.d(NotificationDBOps.this, "insertOrUpdateInternal " +
                    cv.get(NotificationData.COLUMN_NAME_PACKAGE_NAME) + ", " +
                    cv.get(NotificationData.COLUMN_NAME_NOTIFICATION_ID) +
                    " inserting in DB");
            notificationDB.insert(NotificationData.TABLE_NAME, null, cv);
        }
    }

    /**
     * Queries the database, returns the most recent notification (sorted by 'when'), and removes it
     * from DB.<br>Do not call this method from main thread as it blocks on initializing the
     * SQLiteDatabase object if needed. */

    /*storeNotification() above can be safely called from main thread as it does the
    getWritableDatabase() call from a separate thread if needed. (The call itself is synchronized on
    the singleton NotificationDBOps instance object.) We could have done the same for query, but it
    presents a complication - returning a value from the thread, or blocking the calling thread till
    either SQLiteDatabase object is initialized or the query result is available. We could go for
    ExecutorService+Callable+Future, but then the code gets complex and with complexity, the
    potential for errors and problems increases.
    So, what's the solution? We are taking the benefit of the fact that none of our queries are
    coming from main thread - they are coming from JorSayReader, which is an IntentService. The
    getWritableDatabase() call in this method can safely block or take longer - it is the caller's
    responsibility to make sure that this method is not called on main thread.

    This, in combination with having only one SQLiteDatabase and only one SQLiteOpenHelper, is our
    strategy for dealing with long-running DB related operations and DB being accessed from multiple
    threads. We will tweak/change this based on how it works out for the app.*/

    public Cursor getMostRecentNotification() {
        synchronized (instance) {
            //the null check is also synchronized, preventing multiple initializations.
            //In all likelihood, the storeNotification() method would have initialized this as
            //queries happen from the reader, which is triggered from the notification listener.
            if (notificationDB == null || !notificationDB.isOpen()) {
                Logger.d(NotificationDBOps.this, "getMostRecentNotification - " +
                        "getting WritableDatabase");
                notificationDB = notificationDBOpenHelper.getWritableDatabase();
            }
        }

        /* public Cursor query (String table, String[] columns, String selection, String[]
        selectionArgs, String groupBy, String having, String orderBy, String limit) */
        Cursor cursor =
                notificationDB.query(NotificationData.TABLE_NAME, null, null, null, null, null,
                        ORDER_BY_WHEN, LIMIT_FOR_MOST_RECENT); //get 1 most recent notification
        if (cursor != null && cursor.moveToFirst()) {
            int rowId =cursor.getInt(
                    cursor.getColumnIndex(NotificationDBContract.NotificationData._ID));

            //Delete the most recent notification entry
            notificationDB.delete(NotificationData.TABLE_NAME, WHERE_ROW_ID,
                    new String[]{Integer.toString(rowId)});
        }
        //return retrieved data
        return cursor;
    }

    public void removeNotification(String packageName, int notificationId) {
        if(TextUtils.isEmpty(packageName)) {
            return;
        }
        String [] selectionArgs = new String[] {packageName, Integer.toString(notificationId)};
        notificationDB.delete(NotificationData.TABLE_NAME, IS_PRESENT_SELECTION, selectionArgs);
    }

    public void shutdown() {
        Logger.d(this, "shutdown closing DB connection");
        notificationDB.close();
        notificationDBOpenHelper.close();
    }
}