package com.example.planlekcji.utils;

public enum RefreshDataType {
    ARTICLES(24 * 60 * 60 * 1000L),
    CALENDAR(24 * 60 * 60 * 1000L),
    SCHOOL_ENTRIES(24 * 60 * 60 * 1000L),
    TIMETABLE(0L),
    REPLACEMENTS(0L);

    private final long defaultTtlMillis;

    RefreshDataType(long defaultTtlMillis) {
        this.defaultTtlMillis = defaultTtlMillis;
    }

    public long getDefaultTtlMillis() {
        return defaultTtlMillis;
    }
}
