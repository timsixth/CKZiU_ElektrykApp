package com.example.planlekcji.utils;

import android.content.Context;
import android.content.SharedPreferences;

import java.util.Collection;
import java.util.Map;

public class RefreshCooldownManager {
    private static final String PREFS_NAME = "refresh_cooldown_prefs";
    private static final String KEY_PREFIX = "last_refresh_";
    private static RefreshCooldownManager instance;
    private final SharedPreferences prefs;

    private RefreshCooldownManager(Context context) {
        this.prefs = context.getApplicationContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }

    public static synchronized RefreshCooldownManager getInstance(Context context) {
        if (instance == null && context != null) {
            instance = new RefreshCooldownManager(context);
        }
        return instance;
    }

    public boolean shouldRefresh(RefreshDataType type) {
        if (type.getDefaultTtlMillis() <= 0) {
            return true;
        }
        long lastRefresh = getLastRefreshTime(type);
        long now = System.currentTimeMillis();
        return (now - lastRefresh) >= type.getDefaultTtlMillis();
    }

    public boolean isFresh(RefreshDataType type, Object cachedData) {
        if (cachedData == null) {
            return false;
        }
        if (cachedData instanceof Collection<?> && ((Collection<?>) cachedData).isEmpty()) {
            return false;
        }
        if (cachedData instanceof Map<?, ?> && ((Map<?, ?>) cachedData).isEmpty()) {
            return false;
        }
        return !shouldRefresh(type);
    }

    public boolean areAllFresh(RefreshDataType type, Object... cachedItems) {
        if (cachedItems == null || cachedItems.length == 0) {
            return false;
        }
        for (Object item : cachedItems) {
            if (item == null) return false;
            if (item instanceof Collection<?> && ((Collection<?>) item).isEmpty()) return false;
            if (item instanceof Map<?, ?> && ((Map<?, ?>) item).isEmpty()) return false;
        }
        return !shouldRefresh(type);
    }

    public boolean canManualRefresh(RefreshDataType type, long cooldownMillis) {
        long lastRefresh = getLastRefreshTime(type);
        long now = System.currentTimeMillis();
        return (now - lastRefresh) >= cooldownMillis;
    }

    public void recordRefresh(RefreshDataType type) {
        prefs.edit().putLong(KEY_PREFIX + type.name(), System.currentTimeMillis()).apply();
    }

    public long getLastRefreshTime(RefreshDataType type) {
        return prefs.getLong(KEY_PREFIX + type.name(), 0L);
    }

    public void invalidate(RefreshDataType type) {
        prefs.edit().remove(KEY_PREFIX + type.name()).apply();
    }

    public void invalidateAll() {
        prefs.edit().clear().apply();
    }
}
