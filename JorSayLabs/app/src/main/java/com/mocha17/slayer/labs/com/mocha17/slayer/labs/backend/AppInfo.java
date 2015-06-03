package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.graphics.drawable.Drawable;
import android.text.TextUtils;

/**
 * For holding relevant information from android.content.pm.ApplicationInfo.
 * Created by Chaitanya on 6/2/15.
 */
public class AppInfo implements Comparable<AppInfo> {
    public String packageName;
    public String name;
    public Drawable icon;
    public boolean selected;

    @Override
    public String toString() {
        return "AppInfo{" +
                "name='" + name + '\'' +
                ", selected=" + selected +
                '}';
    }

    public AppInfo(String packageName, String name, Drawable icon, boolean selected) {
        this.packageName = packageName;
        this.name = name;
        this.icon = icon;
        this.selected = selected;


    }

    @Override
    public int compareTo(AppInfo another) {
        if (another == null || TextUtils.isEmpty(another.name)) {
            return 1;
        }
        return this.name.compareTo(another.name);
    }
}
