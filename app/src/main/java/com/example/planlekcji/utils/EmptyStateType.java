package com.example.planlekcji.utils;

import com.example.planlekcji.R;

public enum EmptyStateType {
    TIMETABLE(
            R.drawable.timetable_icon,
            R.string.empty_state_timetable_title,
            R.string.empty_state_timetable_description
    ),
    REPLACEMENTS(
            R.drawable.replacement_icon,
            R.string.no_replacements,
            R.string.no_replacements_description
    ),
    ARTICLES(
            R.drawable.articles_icon,
            R.string.empty_state_articles_title,
            R.string.empty_state_articles_description
    ),
    CALENDAR(
            R.drawable.calendar_icon,
            R.string.empty_state_calendar_title,
            R.string.empty_state_calendar_description
    );

    private final int iconResId;
    private final int titleResId;
    private final int descriptionResId;

    EmptyStateType(int iconResId, int titleResId, int descriptionResId) {
        this.iconResId = iconResId;
        this.titleResId = titleResId;
        this.descriptionResId = descriptionResId;
    }

    public int getIconResId() {
        return iconResId;
    }

    public int getTitleResId() {
        return titleResId;
    }

    public int getDescriptionResId() {
        return descriptionResId;
    }
}
