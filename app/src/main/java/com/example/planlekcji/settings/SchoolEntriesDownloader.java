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
import com.example.planlekcji.utils.RefreshCooldownManager;
import com.example.planlekcji.utils.RefreshDataType;
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

        // Cache-first: load from local DB immediately
        if (context != null) {
            List<SchoolEntry> cachedClasses = DatabaseCacheManager.getInstance(context).getObject(CACHE_CLASSES, entryListType);
            List<SchoolEntry> cachedTeachers = DatabaseCacheManager.getInstance(context).getObject(CACHE_TEACHERS, entryListType);
            List<SchoolEntry> cachedClassrooms = DatabaseCacheManager.getInstance(context).getObject(CACHE_CLASSROOMS, entryListType);

            if (cachedClasses != null) classesSchoolEntries = cachedClasses;
            if (cachedTeachers != null) teachersSchoolEntries = cachedTeachers;
            if (cachedClassrooms != null) classroomsSchoolEntries = cachedClassrooms;
        }

        // If all three are cached and TTL is not expired -> skip network
        RefreshCooldownManager cooldown = (context != null) ? RefreshCooldownManager.getInstance(context) : null;
        if (cooldown != null && cooldown.areAllFresh(RefreshDataType.SCHOOL_ENTRIES, classesSchoolEntries, teachersSchoolEntries, classroomsSchoolEntries)) {
            sortEntries();
            return;
        }

        // Fetch from network (cache was empty or TTL expired)
        try {
            TimetableService timetableService = client.getTimetableService(SchoolEntryType.CLASSES);
            classesSchoolEntries = timetableService.getList();

            timetableService = client.getTimetableService(SchoolEntryType.TEACHERS);
            teachersSchoolEntries = timetableService.getList();

            timetableService = client.getTimetableService(SchoolEntryType.CLASSROOMS);
            classroomsSchoolEntries = timetableService.getList();

            if (context != null) {
                if (classesSchoolEntries != null && !classesSchoolEntries.isEmpty()) {
                    DatabaseCacheManager.getInstance(context).saveObject(CACHE_CLASSES, classesSchoolEntries);
                }
                if (teachersSchoolEntries != null && !teachersSchoolEntries.isEmpty()) {
                    DatabaseCacheManager.getInstance(context).saveObject(CACHE_TEACHERS, teachersSchoolEntries);
                }
                if (classroomsSchoolEntries != null && !classroomsSchoolEntries.isEmpty()) {
                    DatabaseCacheManager.getInstance(context).saveObject(CACHE_CLASSROOMS, classroomsSchoolEntries);
                }
                if (cooldown != null) {
                    cooldown.recordRefresh(RefreshDataType.SCHOOL_ENTRIES);
                }
            }
        } catch (Exception e) {
            Log.e("SchoolEntriesDownloader", "Failed to download school entries from network", e);
        }

        if (classesSchoolEntries == null) classesSchoolEntries = new ArrayList<>();
        if (teachersSchoolEntries == null) teachersSchoolEntries = new ArrayList<>();
        if (classroomsSchoolEntries == null) classroomsSchoolEntries = new ArrayList<>();

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
