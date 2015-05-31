package com.mocha17.slayer.labs.com.mocha17.slayer.labs.backend;

import android.content.Context;

/**
 * Created by Chaitanya on 5/31/15.
 * This is a thin wrapper around the Android SwitchPreference, and provides
 * us with some convenience Constructor and members. Particularly, we have Constructors
 * that avoid calling multiple methods like setOrder(), setKey() etc. from Fragment code.
 * Also, the 'added' and 'order' information is stored in this class, and client code can either
 * directly access it ('added') or doesn't need to worry about it ('order').
 */
public class SwitchPreference extends android.preference.SwitchPreference {
    private boolean added;

    public SwitchPreference(Context context, int order) {
        super(context);
        super.setOrder(order);
    }

    public SwitchPreference(Context context, int order, String key, String title) {
        super(context);
        super.setOrder(order);
        super.setKey(key);
        super.setTitle(title);
    }

    public SwitchPreference(Context context, int order, String key, String title, String summary) {
        super(context);
        super.setOrder(order);
        super.setKey(key);
        super.setTitle(title);
        super.setSummary(summary);
    }

    public boolean isAdded() {
        return added;
    }

    public void added(boolean added) {
        this.added = added;
    }

}