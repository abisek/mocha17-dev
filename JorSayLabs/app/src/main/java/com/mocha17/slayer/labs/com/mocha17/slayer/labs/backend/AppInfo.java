package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.graphics.drawable.Drawable;

/**
 * For holding relevant information from android.content.pm.ApplicationInfo.
 * Created by Chaitanya on 6/2/15.
 */
public class AppInfo {
    public String packageName;
    public String name;
    public Drawable icon;
    public boolean selected;

    @Override
    public String toString() {
        return "AppInfo{" +
                "packageName='" + packageName + '\'' +
                ", name='" + name + '\'' +
                ", icon=" + icon +
                ", selected=" + selected +
                '}';
    }

    public AppInfo(String packageName, String name, Drawable icon, boolean selected) {
        this.packageName = packageName;
        this.name = name;
        this.icon = icon;
        this.selected = selected;


    }
}
