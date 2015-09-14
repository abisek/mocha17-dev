package com.mocha17.slayer.notification.db;

import android.app.Notification;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.DatabaseUtils;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.Bundle;
import android.service.notification.StatusBarNotification;
import android.util.Pair;

import com.mocha17.slayer.notification.db.NotificationDBContract.NotificationData;
import com.mocha17.slayer.utils.Logger;

import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executors;

/**
 * For working with the NotificationDB.<br>
 * Created by Chaitanya on 6/21/15.
 */
/* NotificationDBOps is a singleton and NotificationDB is package-private, to be used only by the
DB classes. We are, as a result, going to have only one SQLiteOpenHelper instance at runtime.*/
public class NotificationDBOps {
    private SQLiteDatabase notificationDB;
    private SQLiteOpenHelper notificationDBOpenHelper;

    private static final int NOTIFICATION_UNREAD = 1;
    private static final int NOTIFICATION_READ = 2;

    private static final String [] COLUMN_ROW_ID = {NotificationData._ID};

    private static final String WHERE_ROW_ID = NotificationData._ID + "=?";
    private static final String IS_UNREAD_SELECTION =
            NotificationData.COLUMN_NAME_NOTIFICATION_READ + "=?";
    private static final String ORDER_BY_WHEN = NotificationData.COLUMN_NAME_WHEN + " DESC";
    private static final String LIMIT_FOR_MOST_RECENT = "1";

    private static NotificationDBOps instance;
    public static synchronized NotificationDBOps get(Context context) {
        if (instance == null) {
            instance = new NotificationDBOps(context);
            NotificationDBCleaner.start(context);
        }
        return instance;
    }
    private NotificationDBOps(Context context) {
        notificationDBOpenHelper = new NotificationDB(context);
    }

    /** Stores the notification data in DB, if identical data isn't already present.
     * Identical data means packageName and user-visible data from Notification.
     * <br>This method blocks on getting the database, and should not be called from main thread.
     * @param sbn the notification to be stored
     * @return whether storing the notification was successful
     */
    public boolean storeNotification(final StatusBarNotification sbn) {
        perhapsInitDB(); //do not forget calling perhapsInitDB()!
        if (notificationDB != null && notificationDB.isOpen()) {
            return storeNotificationInternal(sbn);
        }
        return false;
    }

    /** For actually storing the notification in DB. A notification already present isn't stored.
     * @return whether the notification was stored. */
    private boolean storeNotificationInternal(StatusBarNotification sbn) {
        ContentValues cv = toContentValues(sbn, true/*isUnread*/);
        if (cv == null) {
            return false;
        }

        Pair<String, String[]> selectionParams = getIsPresentSelectionParams(cv);
        Cursor cursor = notificationDB.query(NotificationData.TABLE_NAME, COLUMN_ROW_ID,
                selectionParams.first/*selection*/, selectionParams.second/*selectionArgs*/,
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            Logger.d(this, "storeNotificationInternal found notification from " +
                    cv.get(NotificationData.COLUMN_NAME_PACKAGE_NAME) + ", not storing again");
            if (!cursor.isClosed()) {
                cursor.close();
            }
            return false;
        }
        Logger.d(NotificationDBOps.this, "storeNotificationInternal " +
                cv.get(NotificationData.COLUMN_NAME_PACKAGE_NAME) + ", " +
                cv.get(NotificationData.COLUMN_NAME_NOTIFICATION_ID) + " inserting in DB");
        long rowId = notificationDB.insert(NotificationData.TABLE_NAME, null, cv);
        return (rowId != -1);
    }

    /**
     * Queries the database, and returns the most recent unread notification (sorted by 'when').
     * <br>Do not call this method from main thread as it blocks on initializing the
     * SQLiteDatabase object if needed. */
    public Cursor getMostRecentNotification() {
        perhapsInitDB();
        if (notificationDB != null && notificationDB.isOpen()) {
            final String [] selectionArgs = {Integer.toString(NOTIFICATION_UNREAD)};
            /* public Cursor query (String table, String[] columns, String selection,
            String[] selectionArgs, String groupBy, String having, String orderBy, String limit) */
            return notificationDB.query(NotificationData.TABLE_NAME, null, IS_UNREAD_SELECTION,
                    selectionArgs, null, null, ORDER_BY_WHEN,
                    LIMIT_FOR_MOST_RECENT); //get 1 most recent notification
        }
        return null;
    }

    /** Blocks on initializing the database object if needed,
     * and should not be called from main thread. */
    public void markNotificationRead(long rowId) {
        perhapsInitDB();
        if (notificationDB != null && notificationDB.isOpen()) {
            Logger.d(NotificationDBOps.this, "markNotificationRead for rowId: " + rowId);
            final String [] selectionArgs = new String[]{Long.toString(rowId)};
            final ContentValues cv = new ContentValues();
            cv.put(NotificationData.COLUMN_NAME_NOTIFICATION_READ, NOTIFICATION_READ);

            notificationDB.update(NotificationData.TABLE_NAME, cv, WHERE_ROW_ID, selectionArgs);
        }
    }

    public void removeNotification(StatusBarNotification sbn) {
        perhapsInitDB();
        if (notificationDB != null && notificationDB.isOpen()) {
            ContentValues cv = toContentValues(sbn, false/*isUnread*/);
            if (cv == null) {
                return;
            }
            Pair<String, String[]> selectionParams = getSelectionParamsForDelete(cv);
            if (selectionParams == null) {
                return;
            }
            notificationDB.delete(NotificationData.TABLE_NAME,
                   selectionParams.first/*selection*/, selectionParams.second/*selectionArgs*/);
        }
    }

    public void removeReadNotifications() {
        perhapsInitDB();
        if (notificationDB != null && notificationDB.isOpen()) {
            String [] selectionArgs = new String[] {Integer.toString(NOTIFICATION_READ)};
            notificationDB.delete(NotificationData.TABLE_NAME, IS_UNREAD_SELECTION, selectionArgs);
        }
    }

    private ContentValues toContentValues(StatusBarNotification sbn, boolean isUnread) {
        if (sbn == null) {
            return null;
        }

        Notification notification = sbn.getNotification();
        Bundle notificationExtras = notification.extras;

        ContentValues cv = new ContentValues();
        cv.put(NotificationData.COLUMN_NAME_PACKAGE_NAME, sbn.getPackageName());
        cv.put(NotificationData.COLUMN_NAME_NOTIFICATION_ID, sbn.getId());
        cv.put(NotificationData.COLUMN_NAME_NOTIFICATION_TAG, sbn.getTag());
        CharSequence title = notificationExtras.getCharSequence(Notification.EXTRA_TITLE);
        if (title != null) {
            cv.put(NotificationData.COLUMN_NAME_TITLE, title.toString());
        }
        CharSequence text = notificationExtras.getCharSequence(Notification.EXTRA_TEXT);
        if (text != null) {
            cv.put(NotificationData.COLUMN_NAME_TEXT, text.toString());
        }
        CharSequence bigTitle = notificationExtras.getCharSequence(Notification.EXTRA_TITLE_BIG);
        if (bigTitle != null) {
            cv.put(NotificationData.COLUMN_NAME_TITLE_BIG, bigTitle.toString());
        }
        CharSequence bigText = notificationExtras.getCharSequence(Notification.EXTRA_BIG_TEXT);
        if (bigText != null) {
            cv.put(NotificationData.COLUMN_NAME_BIG_TEXT, bigText.toString());
        }
        CharSequence summary = notificationExtras.getCharSequence(Notification.EXTRA_SUMMARY_TEXT);
        if (summary != null) {
            cv.put(NotificationData.COLUMN_NAME_SUMMARY, summary.toString());
        }
        String textLines = charSequenceArrayToString(
                notificationExtras.getCharSequenceArray(Notification.EXTRA_TEXT_LINES));
        if (textLines != null) {
            cv.put(NotificationData.COLUMN_NAME_TEXT_LINES, textLines);
        }
        CharSequence subText = notificationExtras.getCharSequence(Notification.EXTRA_SUB_TEXT);
        if (subText != null) {
            cv.put(NotificationData.COLUMN_NAME_SUBTEXT, subText.toString());
        }
        if (notification.tickerText != null) {
            cv.put(NotificationData.COLUMN_NAME_TICKER_TEXT, notification.tickerText.toString());
        }
        cv.put(NotificationData.COLUMN_NAME_WHEN, Long.toString(notification.when));
        cv.put(NotificationData.COLUMN_NAME_NOTIFICATION_READ,
                isUnread?NOTIFICATION_UNREAD:NOTIFICATION_READ);

        return cv;
    }

    /**@return A Pair of selection clause and selectionArgs for 'is present' query<br>*/
    /*This method generates selection clause and selectionArgs *in tandem*, based on the data in the
    incoming notification.
    Why this particular approach?
    1. A row in the table could have null value for any of the columns. This is something we want to
    express/account for when doing the 'is present' query. Say TITLE is null for the incoming
    notification. We want the query to return a row where title is (strictly) null, not 'null OR
    someText' or "null" the String, or anything else.
    2. The usual approach is to have the selection clause defined as a static final String, and
    selectionArgs could then be created from the ContentValues. This doesn't work -
        2.1 An Exception is generated if a selectionArg is null:
        java.lang.IllegalArgumentException: the bind value at index <index> is null
        2.2 SelectionArg cannot be "null" the String, because that wouldn't match a null in the row.
        2.3 Selection clause cannot be 'is null or =?' because this too results in the Exception.
    3. Android SQLite doesn't seem to do great with nulls - stackoverflow.com/a/15954695/299988
    4. The solution is to generate a selection clause which has 'is null' for a null field in the
    notification, and has "=?" for a field that is present in the notification.
    5. On the surface, simplifying this by replacing the null by a custom value - say "null"; seems
    possible. However, that requires everyone who uses this DB to understand the custom value and
    implement logic to disregard it - it creates this ghastly dependency. Current approach doesn't
    impose such restrictions on DB's users. */
    private Pair<String, String[]> getIsPresentSelectionParams(ContentValues cv) {
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new LinkedList<>();

        selection.append(NotificationData.COLUMN_NAME_PACKAGE_NAME);
        if (cv.getAsString(NotificationData.COLUMN_NAME_PACKAGE_NAME) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_PACKAGE_NAME));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_TITLE);
        if (cv.getAsString(NotificationData.COLUMN_NAME_TITLE) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TITLE));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_TEXT);
        if (cv.getAsString(NotificationData.COLUMN_NAME_TEXT) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TEXT));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_TITLE_BIG);
        if (cv.getAsString(NotificationData.COLUMN_NAME_TITLE_BIG) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TITLE_BIG));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_BIG_TEXT);
        if (cv.getAsString(NotificationData.COLUMN_NAME_BIG_TEXT) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_BIG_TEXT));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_SUMMARY);
        if (cv.getAsString(NotificationData.COLUMN_NAME_SUMMARY) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_SUMMARY));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_TEXT_LINES);
        if (cv.getAsString(NotificationData.COLUMN_NAME_TEXT_LINES) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TEXT_LINES));
        } else {
            selection.append(" is null");
        }
        selection.append(" and ").append(NotificationData.COLUMN_NAME_SUBTEXT);
        if (cv.getAsString(NotificationData.COLUMN_NAME_SUBTEXT) != null) {
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_SUBTEXT));
        } else {
            selection.append(" is null");
        }
        String [] selectionArgsArr = new String[selectionArgs.size()];
        return new Pair<>(selection.toString(), selectionArgs.toArray(selectionArgsArr));
    }

    /* Similar to getIsPresentSelectionParams(). Crucial difference - doesn't add 'is null'.
    Why? Because, the StatusBarNotification we get in onNotificationRemoved() is "light" - it
    doesn't contain some of the heavier fields. Our tests show bigText missing, and documentation
    mentions largeIcon etc. When it comes to not storing duplicates, we want an exact match - if
    a field isn't present in the received StatusBarNotification, it must be null in our records.
    But for deleting, we can only match the fields given to us and cannot have the exact test.*/
    private Pair<String, String[]> getSelectionParamsForDelete(ContentValues cv) {
        StringBuilder selection = new StringBuilder();
        List<String> selectionArgs = new LinkedList<>();

        if (cv.getAsString(NotificationData.COLUMN_NAME_PACKAGE_NAME) == null) {
            //We will bail if there's no packageName.
            /*We are being slightly lazy here. We are constructing the selection String, and it
            would start with 'and' if there's no packageName and wouldn't be valid. We are
            1. choosing to rely on the fact that packageName will always be there and thus
            2. choosing not to have additional logic for adding/not adding each 'and'.*/
            return null;
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_PACKAGE_NAME) != null) {
            selection.append(NotificationData.COLUMN_NAME_PACKAGE_NAME);
            selection.append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_PACKAGE_NAME));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_TITLE) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_TITLE)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TITLE));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_TEXT) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_TEXT)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TEXT));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_TITLE_BIG) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_TITLE_BIG)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TITLE_BIG));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_BIG_TEXT) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_BIG_TEXT)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_BIG_TEXT));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_SUMMARY) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_SUMMARY)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_SUMMARY));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_TEXT_LINES) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_TEXT_LINES)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TEXT_LINES));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_SUBTEXT) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_SUBTEXT)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_SUBTEXT));
        }
        if (cv.getAsString(NotificationData.COLUMN_NAME_TICKER_TEXT) != null) {
            selection.append(" and ")
                    .append(NotificationData.COLUMN_NAME_TICKER_TEXT)
                    .append("=?");
            selectionArgs.add(cv.getAsString(NotificationData.COLUMN_NAME_TICKER_TEXT));
        }
        String [] selectionArgsArr = new String[selectionArgs.size()];
        return new Pair<>(selection.toString(), selectionArgs.toArray(selectionArgsArr));
    }

    /** Initializes the SQLiteDatabase object if needed.<br>Do not call this on main thread as it
     *  blocks on getting the Database if needed.
     */
    private Void perhapsInitDB() {
        if (notificationDB == null || !notificationDB.isOpen()) {
            try {
                Logger.d(this, "perhapsInitDB, DB not open, starting thread");
                return Executors.newSingleThreadExecutor().submit(new Callable<Void>() {
                    /*We are using Callable and Future to return value from the thread. Calling
                    thread blocks in get() till the result is available.*/
                    @Override
                    public Void call() {
                        synchronized (instance) {
                            notificationDB = notificationDBOpenHelper.getWritableDatabase();
                        }
                        return null;
                    }
                }).get();
            } catch (InterruptedException e) {
                e.printStackTrace();
            } catch (ExecutionException e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    /**Utility method to convert an array of Charsequences to a String*/
    private String charSequenceArrayToString(CharSequence[] charSequences) {
        if (charSequences == null || charSequences.length == 0) {
            return null;
        }
        StringBuilder sb = new StringBuilder();
        for (CharSequence charSequence : charSequences) {
            sb.append(charSequence.toString()).append("\n");
        }
        return sb.toString();
    }

    public void shutdown(Context context) {
        Logger.d(this, "shutdown closing DB connection");
        if (notificationDBOpenHelper != null) {
            notificationDBOpenHelper.close();
        }
        if (notificationDB != null && notificationDB.isOpen()) {
            notificationDB.close();
        }
        NotificationDBCleaner.stop(context);
    }

    //For debugging
    private String dumpAllRowsToString() {
        Cursor cursor = notificationDB.query(NotificationData.TABLE_NAME,
                null, null, null, null, null, null);
        String toReturn = DatabaseUtils.dumpCursorToString(cursor);
        if (cursor != null && !cursor.isClosed()) {
            cursor.close();
        }
        return toReturn;
    }
}