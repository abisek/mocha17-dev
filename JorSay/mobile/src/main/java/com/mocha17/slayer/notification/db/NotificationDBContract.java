package com.mocha17.slayer.notification.db;

import android.provider.BaseColumns;

/**
 * Created by Chaitanya on 6/20/15.
 */
public class NotificationDBContract {
    private NotificationDBContract() {
        //private Constructor to prevent instantiation
    }

    //For the Notifications table - currently, the only table we have
    public static abstract class NotificationData implements BaseColumns {
        public static final String TABLE_NAME = "notificationsTable";

        //----- Fields from StatusBarNotification -----
        /*NOTIFICATION_ID and PACKAGE_NAME are our uniqueness test. We are using
        BaseColumns._ID for table key.*/
        public static final String COLUMN_NAME_NOTIFICATION_ID = "notificationId";
        public static final String COLUMN_NAME_PACKAGE_NAME = "packageName";

        //----- Fields from Notification Extras -----
        public static final String COLUMN_NAME_TITLE = "title";
        public static final String COLUMN_NAME_TEXT = "text";
        public static final String COLUMN_NAME_TITLE_BIG = "titleBig";
        public static final String COLUMN_NAME_BIG_TEXT = "bigText";
        public static final String COLUMN_NAME_SUMMARY = "summary";
        public static final String COLUMN_NAME_TEXT_LINES = "textLines";
        public static final String COLUMN_NAME_SUBTEXT= "subtext";

        //----- Fields from Notification -----
        public static final String COLUMN_NAME_TICKER_TEXT = "ticketText";
        public static final String COLUMN_NAME_WHEN = "notificationWhen";

        //----- Fields for bookkeeping -----
        public static final String COLUMN_NAME_NOTIFICATION_READ = "notificationRead";
    }
}