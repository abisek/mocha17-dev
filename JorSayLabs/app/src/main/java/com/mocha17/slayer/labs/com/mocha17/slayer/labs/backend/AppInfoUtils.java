package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import java.util.LinkedList;
import java.util.List;

/**
 * Convenience methods for working with AppInfo
 * Created by Chaitanya on 6/2/15.
 */
public class AppInfoUtils {
    /**
     *
     * @param appInfos List of AppInfo objects
     * @return a list of selected packages, empty if none selected or appInfos is null/empty.
     */
    public static List<String> getSelectedPackages(List<AppInfo> appInfos) {
        List<String> selectedPackages = new LinkedList<String>();
        if (appInfos == null || appInfos.isEmpty()) {
            return selectedPackages;
        }
        for (AppInfo appInfo : appInfos) {
            if (appInfo.selected) {
                selectedPackages.add(appInfo.packageName);
            }
        }
        return selectedPackages;
    }
}