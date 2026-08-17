package com.example.planlekcji.timetable;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.Config;
import com.example.planlekcji.ckziu_elektryk.client.timetable.AbstractTimetableService;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.ckziu_elektryk.client.timetable.TimetableService;
import com.example.planlekcji.ckziu_elektryk.client.timetable.info.TimetableInfo;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.Lesson;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.listener.TimetableDownloadCompleteListener;
import com.example.planlekcji.preview.PreviewDataStore;
import com.example.planlekcji.timetable.model.DayOfWeek;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import java.util.List;
import java.util.Map;
import java.util.Optional;

public class TimetableDataDownloader implements Runnable {
    private final TimetableDownloadCompleteListener listener;
    private final CKZiUElektrykClient client;

    public TimetableDataDownloader(CKZiUElektrykClient client, TimetableDownloadCompleteListener listener) {
        this.listener = listener;
        this.client = client;
    }

    @Override
    public void run() {
        SchoolEntryType schoolEntryType = MainActivity.getTimetableType();
        String token = MainActivity.getToken(schoolEntryType).replaceAll(" ", "");

        if (Config.getOrCreateConfig().isPreviewMode()) {
            listener.onDownloadComplete(PreviewDataStore.getTimetable(schoolEntryType, token));
            return;
        }

        // Cache first
        Context context = MainActivity.getContext();
        String cacheKey = "timetable_" + schoolEntryType.name() + "_" + token;
        if (context != null && !token.isEmpty()) {
            try {
                String cachedJson = DatabaseCacheManager.getInstance(context).getRawJson(cacheKey);
                if (cachedJson != null) {
                    JsonObject jsonObject = JsonParser.parseString(cachedJson).getAsJsonObject();
                    Map<DayOfWeek, List<Lesson>> cachedMap = AbstractTimetableService.parseTimetable(jsonObject);
                    if (cachedMap != null && !cachedMap.isEmpty()) {
                        listener.onDownloadComplete(cachedMap);
                    }
                }
            } catch (Exception e) {
                Log.e("TimetableDownloader", "Failed to load cached timetable", e);
            }
        }

        try {
            Optional<TimetableInfo> timetableInfoOptional = client.getTimetableInfo();

            if (timetableInfoOptional.isEmpty()) {
                listener.onDownloadFailed();
                return;
            }

            if (token.isEmpty()) {
                Log.e("Error", "Token is empty");
                listener.onDownloadFailed();
                return;
            }

            TimetableService timetableService = client.getTimetableService(schoolEntryType);
            JsonObject jsonObject = timetableService.getTimetableJsonObject(token);

            if (jsonObject != null && !jsonObject.isEmpty()) {
                if (context != null) {
                    DatabaseCacheManager.getInstance(context).saveRawJson(cacheKey, jsonObject.toString());
                }
                Map<DayOfWeek, List<Lesson>> map = AbstractTimetableService.parseTimetable(jsonObject);
                if (map != null && !map.isEmpty()) {
                    listener.onDownloadComplete(map);
                } else {
                    listener.onDownloadFailed();
                }
            } else {
                listener.onDownloadFailed();
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }
}
