package com.example.planlekcji.settings;

import android.content.SharedPreferences;

/**
 * Utility for managing group-related user preferences in SharedPreferences.
 */
public final class GroupPreferenceManager {

    public static final String KEY_HIDE_UNSELECTED = "group_pref_hide_unselected";

    private GroupPreferenceManager() {}

    public static boolean isHideUnselected(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_HIDE_UNSELECTED, false);
    }

    public static void setHideUnselected(SharedPreferences prefs, boolean hide) {
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_HIDE_UNSELECTED, hide).apply();
        }
    }
}

