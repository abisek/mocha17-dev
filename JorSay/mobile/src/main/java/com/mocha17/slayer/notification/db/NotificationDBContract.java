package com.mocha17.slayer.notification.db;

import android.provider.BaseColumns;

/**
 * Created by Chaitanya on 6/20/15.
 */
public class NotificationDBContract {
    private NotificationDBContract() {
        //private Constructor to prevent instantiation
    }

    //For the Notifications table - currently, the only table we have.
    //BaseColumns_ID is the table key
    public static abstract class NotificationData implements BaseColumns {
        public static final String TABLE_NAME = "notificationsTable";

        //----- Fields from StatusBarNotification -----
        public static final String COLUMN_NAME_PACKAGE_NAME = "packageName";
        public static final String COLUMN_NAME_NOTIFICATION_ID = "notificationId";
        public static final String COLUMN_NAME_NOTIFICATION_TAG = "notificationTag";

        //----- Fields from Notification Extras -----
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_TITLE_BIG = "titleBig";
        public static final String COLUMN_NAME_BIG_TEXT = "bigText";
        public static final String COLUMN_NAME_SUMMARY = "summary";
        public static final String COLUMN_NAME_TEXT_LINES = "textLines";
        public static final String COLUMN_NAME_SUBTEXT= "subtext";

        //----- Fields from Notification -----
        public static final String COLUMN_NAME_TICKER_TEXT = "tickerText";
        public static final String COLUMN_NAME_WHEN = "notificationWhen";

        //----- Fields for bookkeeping -----
        public static final String COLUMN_NAME_NOTIFICATION_READ = "notificationRead";
    }
}