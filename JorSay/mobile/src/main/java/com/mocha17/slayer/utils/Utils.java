package com.mocha17.slayer.utils;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.text.TextUtils;

import com.mocha17.slayer.SlayerApp;

/**
 * Created by mocha on 7/2/15.
 */
public class Utils {
    /**
     * @param packageName - an application package name
     * @return user-readable application name if found, incoming package name otherwise */
    public static String getAppName(String packageName) {
        if (TextUtils.isEmpty(packageName)) {
            return packageName;
        }
        PackageManager packageManager = SlayerApp.getInstance().getPackageManager();
        try {
            ApplicationInfo applicationInfo = packageManager.getApplicationInfo(
                    packageName, PackageManager.COMPONENT_ENABLED_STATE_DEFAULT);
            return applicationInfo.loadLabel(packageManager).toString();
        } catch (PackageManager.NameNotFoundException e) {
            Logger.e(Utils.class, "getAppName NameNotFound for " + packageName + ", returning it");
            return packageName;
        }
    }

    /** Returns a String with emoji characters removed from the input String */
    public static String withoutEmoji(String s) {
        if (TextUtils.isEmpty(s)) {
            return s;
        }
        //From http://stackoverflow.com/a/12014635/299988
        //We could also go for [\\p{InMiscellaneousSymbolsAndPictographs}|\\p{InEmoticons}]+ but
        //it does not match the '❤' symbol and might not match others
        return s.replaceAll("\\p{So}+", "");
    }
}
