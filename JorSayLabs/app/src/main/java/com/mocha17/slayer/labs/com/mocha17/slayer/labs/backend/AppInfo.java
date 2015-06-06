package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.graphics.drawable.Drawable;

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
        //We want to sort by selected state and then by name
        //in effect, getting two alphabetical lists - one of selected, followed by that of unselected
        if (another == null) {
            return 1;
        }
        if (this.selected == another.selected) {
            return this.name.compareTo(another.name);
        }
        //Selected should be at the top
        if (this.selected == true) {
            return -1;
        } else if (this.selected == false) {
            return 1;
        }
        return 0;
    }
}
