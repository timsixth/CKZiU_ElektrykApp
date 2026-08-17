package com.example.planlekcji.settings;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.Config;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntry;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.ckziu_elektryk.client.timetable.TimetableService;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.preview.PreviewDataStore;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class SchoolEntriesDownloader implements Runnable {
    private static final String CACHE_CLASSES = "school_entries_classes";
    private static final String CACHE_TEACHERS = "school_entries_teachers";
    private static final String CACHE_CLASSROOMS = "school_entries_classrooms";

    private List<SchoolEntry> classesSchoolEntries = new ArrayList<>();
    private List<SchoolEntry> teachersSchoolEntries = new ArrayList<>();
    private List<SchoolEntry> classroomsSchoolEntries = new ArrayList<>();
    private final CKZiUElektrykClient client;

    public SchoolEntriesDownloader(CKZiUElektrykClient client) {
        this.client = client;
    }

    @Override
    public void run() {
        if (Config.getOrCreateConfig().isPreviewMode()) {
            classesSchoolEntries = PreviewDataStore.getSchoolEntries(SchoolEntryType.CLASSES);
            teachersSchoolEntries = PreviewDataStore.getSchoolEntries(SchoolEntryType.TEACHERS);
            classroomsSchoolEntries = PreviewDataStore.getSchoolEntries(SchoolEntryType.CLASSROOMS);

            sortEntries();
            return;
        }

        Context context = MainActivity.getContext();
        Type entryListType = new TypeToken<List<SchoolEntry>>(){}.getType();

        try {
            TimetableService timetableService = client.getTimetableService(SchoolEntryType.CLASSES);
            classesSchoolEntries = timetableService.getList();

            timetableService = client.getTimetableService(SchoolEntryType.TEACHERS);
            teachersSchoolEntries = timetableService.getList();

            timetableService = client.getTimetableService(SchoolEntryType.CLASSROOMS);
            classroomsSchoolEntries = timetableService.getList();

            if (classesSchoolEntries != null && !classesSchoolEntries.isEmpty() && context != null) {
                DatabaseCacheManager.getInstance(context).saveObject(CACHE_CLASSES, classesSchoolEntries);
            }
            if (teachersSchoolEntries != null && !teachersSchoolEntries.isEmpty() && context != null) {
                DatabaseCacheManager.getInstance(context).saveObject(CACHE_TEACHERS, teachersSchoolEntries);
            }
            if (classroomsSchoolEntries != null && !classroomsSchoolEntries.isEmpty() && context != null) {
                DatabaseCacheManager.getInstance(context).saveObject(CACHE_CLASSROOMS, classroomsSchoolEntries);
            }
        } catch (Exception e) {
            Log.e("SchoolEntriesDownloader", "Failed to download school entries from network, loading cache", e);
        }

        // Fallback to cache if empty
        if (context != null) {
            if (classesSchoolEntries == null || classesSchoolEntries.isEmpty()) {
                List<SchoolEntry> cached = DatabaseCacheManager.getInstance(context).getObject(CACHE_CLASSES, entryListType);
                if (cached != null) classesSchoolEntries = cached;
            }
            if (teachersSchoolEntries == null || teachersSchoolEntries.isEmpty()) {
                List<SchoolEntry> cached = DatabaseCacheManager.getInstance(context).getObject(CACHE_TEACHERS, entryListType);
                if (cached != null) teachersSchoolEntries = cached;
            }
            if (classroomsSchoolEntries == null || classroomsSchoolEntries.isEmpty()) {
                List<SchoolEntry> cached = DatabaseCacheManager.getInstance(context).getObject(CACHE_CLASSROOMS, entryListType);
                if (cached != null) classroomsSchoolEntries = cached;
            }
        }

        if (classesSchoolEntries == null) {
            classesSchoolEntries = new ArrayList<>();
        }
        if (teachersSchoolEntries == null) {
            teachersSchoolEntries = new ArrayList<>();
        }
        if (classroomsSchoolEntries == null) {
            classroomsSchoolEntries = new ArrayList<>();
        }

        sortEntries();
    }

    private void sortEntries() {
        classesSchoolEntries.sort(Comparator.comparing(SchoolEntry::shortcut));
        teachersSchoolEntries.sort(Comparator.comparing(SchoolEntry::shortcut, Collator.getInstance(new Locale("pl"))));
        classroomsSchoolEntries.sort(Comparator.comparing(SchoolEntry::shortcut));
    }

    public List<SchoolEntry> getClassesSchoolEntries() {
        return classesSchoolEntries;
    }

    public List<SchoolEntry> getTeachersSchoolEntries() {
        return teachersSchoolEntries;
    }

    public List<SchoolEntry> getClassroomsSchoolEntries() {
        return classroomsSchoolEntries;
    }
}
