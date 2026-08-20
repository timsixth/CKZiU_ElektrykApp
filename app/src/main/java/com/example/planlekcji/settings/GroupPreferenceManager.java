package com.example.planlekcji.settings;

import android.content.SharedPreferences;

import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.GroupLesson;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.Lesson;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.LessonDetails;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.SingleLesson;
import com.example.planlekcji.timetable.model.DayOfWeek;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Utility for managing user group preferences in SharedPreferences
 * and extracting available group options from timetable data.
 */
public final class GroupPreferenceManager {

    private static final String PREF_PREFIX = "group_pref_";

    private GroupPreferenceManager() {}

    public static String buildKey(String classToken, String subjectName) {
        return PREF_PREFIX + classToken + "_" + subjectName;
    }

    public static void saveChoice(
            SharedPreferences prefs,
            String classToken,
            String subjectName,
            String groupLabel
    ) {
        String key = buildKey(classToken, subjectName);
        SharedPreferences.Editor editor = prefs.edit();
        if (groupLabel != null && !groupLabel.trim().isEmpty()) {
            editor.putString(key, groupLabel.trim());
        } else {
            editor.remove(key);
        }
        editor.apply();
    }

    public static String getChoice(
            SharedPreferences prefs,
            String classToken,
            String subjectName
    ) {
        return prefs.getString(buildKey(classToken, subjectName), null);
    }

    public static void resetClassChoices(
            SharedPreferences prefs,
            String classToken
    ) {
        String prefix = PREF_PREFIX + classToken + "_";
        SharedPreferences.Editor editor = prefs.edit();
        for (String key : prefs.getAll().keySet()) {
            if (key != null && key.startsWith(prefix)) {
                editor.remove(key);
            }
        }
        editor.apply();
    }

    public static String formatGroupLabel(String classroom, String teacher) {
        String c = classroom != null ? classroom.trim() : "";
        String t = teacher != null ? teacher.trim() : "";
        if (!c.isEmpty() && !t.isEmpty()) return c + " · " + t;
        if (!c.isEmpty()) return c;
        return t;
    }

    public static Map<String, List<String>> extractGroupOptions(
            Map<DayOfWeek, List<Lesson>> timetableMap
    ) {
        if (timetableMap == null) return Collections.emptyMap();

        Map<String, Set<String>> subjectGroupsMap = new LinkedHashMap<>();

        for (List<Lesson> lessons : timetableMap.values()) {
            if (lessons == null) continue;
            for (Lesson lesson : lessons) {
                List<LessonDetails> detailsList = Collections.emptyList();
                if (lesson instanceof SingleLesson) {
                    detailsList = Collections.singletonList(((SingleLesson) lesson).getDetails());
                } else if (lesson instanceof GroupLesson) {
                    detailsList = ((GroupLesson) lesson).getLessonsDetails();
                }

                for (LessonDetails details : detailsList) {
                    if (details == null || details.getSubject() == null) continue;
                    String subjectName = details.getSubject().name();
                    if (subjectName == null || subjectName.trim().isEmpty()) continue;

                    String label = formatGroupLabel(details.getClassroom(), details.getTeacher());
                    if (label.isEmpty()) continue;

                    subjectGroupsMap.computeIfAbsent(subjectName.trim(), k -> new LinkedHashSet<>()).add(label);
                }
            }
        }

        Map<String, List<String>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Set<String>> entry : subjectGroupsMap.entrySet()) {
            if (entry.getValue().size() >= 2) {
                result.put(entry.getKey(), new ArrayList<>(entry.getValue()));
            }
        }

        return result;
    }
}
