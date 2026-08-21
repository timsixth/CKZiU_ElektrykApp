package com.example.planlekcji.settings;

import android.content.SharedPreferences;

/**
 * Utility for managing group-related user preferences in SharedPreferences.
 */
public final class GroupPreferenceManager {

    public static final String KEY_HIDE_UNSELECTED = "group_pref_hide_unselected";
    public static final String KEY_LOCK_SELECTION = "group_pref_lock_selection";
    private static final String PREF_SLOT_PREFIX = "group_slot_";

    private GroupPreferenceManager() {}

    public static boolean isHideUnselected(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_HIDE_UNSELECTED, false);
    }

    public static void setHideUnselected(SharedPreferences prefs, boolean hide) {
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_HIDE_UNSELECTED, hide).apply();
        }
    }

    public static boolean isLockSelection(SharedPreferences prefs) {
        return prefs != null && prefs.getBoolean(KEY_LOCK_SELECTION, false);
    }

    public static void setLockSelection(SharedPreferences prefs, boolean lock) {
        if (prefs != null) {
            prefs.edit().putBoolean(KEY_LOCK_SELECTION, lock).apply();
        }
    }

    public static void resetClassChoices(SharedPreferences prefs, String classToken) {
        if (prefs == null || classToken == null || classToken.isEmpty()) return;
        String prefix = PREF_SLOT_PREFIX + classToken + "_";
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key != null && key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }
}

