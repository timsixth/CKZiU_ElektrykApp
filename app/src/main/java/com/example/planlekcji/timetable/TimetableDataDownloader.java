package com.example.planlekcji.timetable;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.Config;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.ckziu_elektryk.client.timetable.TimetableService;
import com.example.planlekcji.ckziu_elektryk.client.timetable.info.TimetableInfo;
import com.example.planlekcji.ckziu_elektryk.client.timetable.lesson.Lesson;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.listener.TimetableDownloadCompleteListener;
import com.example.planlekcji.preview.PreviewDataStore;
import com.example.planlekcji.timetable.model.DayOfWeek;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
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
                Type type = new TypeToken<Map<DayOfWeek, List<Lesson>>>(){}.getType();
                Map<DayOfWeek, List<Lesson>> cachedMap = DatabaseCacheManager.getInstance(context).getObject(cacheKey, type);
                if (cachedMap != null && !cachedMap.isEmpty()) {
                    listener.onDownloadComplete(cachedMap);
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
            Map<DayOfWeek, List<Lesson>> map = timetableService.getTimetable(token);

            if (map != null && !map.isEmpty()) {
                if (context != null) {
                    DatabaseCacheManager.getInstance(context).saveObject(cacheKey, map);
                }
                listener.onDownloadComplete(map);
            } else {
                listener.onDownloadFailed();
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }
}
