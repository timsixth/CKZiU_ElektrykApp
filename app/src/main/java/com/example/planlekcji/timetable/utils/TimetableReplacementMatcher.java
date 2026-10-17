package com.example.planlekcji.timetable.utils;

import com.example.planlekcji.ckziu_elektryk.client.replacements.Replacement;
import com.example.planlekcji.ckziu_elektryk.client.replacements.ReplacementChange;
import com.example.planlekcji.replacements.DayReplacements;
import com.example.planlekcji.utils.DayOfWeek;

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public final class TimetableReplacementMatcher {

    private TimetableReplacementMatcher() {}

    /**
     * Calculates the target date for a given DayOfWeek based on active week rules:
     * - Monday through Friday: active week is the current week.
     * - Saturday and Sunday: active week is the next week.
     */
    public static Date getTargetDateForDayOfWeek(DayOfWeek dayOfWeek, Calendar referenceCalendar) {
        if (dayOfWeek == null) return null;

        Calendar cal = (referenceCalendar != null) ? (Calendar) referenceCalendar.clone() : Calendar.getInstance();

        int currentDayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        if (currentDayOfWeek == Calendar.SATURDAY || currentDayOfWeek == Calendar.SUNDAY) {
            cal.add(Calendar.WEEK_OF_YEAR, 1);
        }

        int targetCalendarDay = switch (dayOfWeek) {
            case MONDAY -> Calendar.MONDAY;
            case TUESDAY -> Calendar.TUESDAY;
            case WEDNESDAY -> Calendar.WEDNESDAY;
            case THURSDAY -> Calendar.THURSDAY;
            case FRIDAY -> Calendar.FRIDAY;
        };

        cal.set(Calendar.DAY_OF_WEEK, targetCalendarDay);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);

        return cal.getTime();
    }

    /**
     * Checks whether there is an active replacement for the specified class, day of week, and lesson number.
     */
    public static boolean hasReplacementForLesson(List<DayReplacements> replacements, DayOfWeek dayOfWeek, int lessonNumber, String classToken, Calendar referenceCalendar) {
        if (replacements == null || replacements.isEmpty() || dayOfWeek == null || classToken == null || classToken.trim().isEmpty()) {
            return false;
        }

        Date targetDate = getTargetDateForDayOfWeek(dayOfWeek, referenceCalendar);
        if (targetDate == null) return false;

        DayReplacements targetDayReplacements = null;
        for (DayReplacements dr : replacements) {
            if (dr != null && isSameDay(dr.date(), targetDate)) {
                targetDayReplacements = dr;
                break;
            }
        }

        if (targetDayReplacements == null || targetDayReplacements.replacements() == null) {
            return false;
        }

        String token = classToken.trim();

        for (Replacement replacement : targetDayReplacements.replacements()) {
            if (replacement == null || replacement.name() == null) continue;
            if (!replacement.name().trim().equalsIgnoreCase(token)) continue;

            if (replacement.changes() != null) {
                for (ReplacementChange change : replacement.changes()) {
                    if (change != null && isPeriodMatchingLesson(change.period(), lessonNumber)) {
                        return true;
                    }
                }
            }
        }

        return false;
    }

    public static boolean isSameDay(Date d1, Date d2) {
        if (d1 == null || d2 == null) return false;
        Calendar c1 = Calendar.getInstance();
        c1.setTime(d1);
        Calendar c2 = Calendar.getInstance();
        c2.setTime(d2);
        return c1.get(Calendar.YEAR) == c2.get(Calendar.YEAR)
                && c1.get(Calendar.DAY_OF_YEAR) == c2.get(Calendar.DAY_OF_YEAR);
    }

    public static boolean isPeriodMatchingLesson(String period, int lessonNumber) {
        if (period == null || period.trim().isEmpty()) return false;
        String p = period.trim().toLowerCase(Locale.ROOT);
        if (p.contains("cały dzień") || p.contains("caly dzien")) {
            return true;
        }
        if (p.contains("-")) {
            String[] parts = p.split("-");
            try {
                int start = Integer.parseInt(parts[0].trim().replaceAll("[^0-9]", ""));
                int end = Integer.parseInt(parts[1].trim().replaceAll("[^0-9]", ""));
                return lessonNumber >= start && lessonNumber <= end;
            } catch (Exception ignored) {
                return false;
            }
        }
        try {
            int num = Integer.parseInt(p.replaceAll("[^0-9]", ""));
            return num == lessonNumber;
        } catch (Exception ignored) {
            return false;
        }
    }
}
