package com.example.planlekcji.replacements;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.Config;
import com.example.planlekcji.ckziu_elektryk.client.replacements.Replacement;
import com.example.planlekcji.ckziu_elektryk.client.replacements.ReplacementService;
import com.example.planlekcji.ckziu_elektryk.client.replacements.ReplacementType;
import com.example.planlekcji.ckziu_elektryk.client.timetable.SchoolEntryType;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.listener.ReplacementsDownloadCompleteListener;
import com.example.planlekcji.preview.PreviewDataStore;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

public class ReplacementDataDownloader implements Runnable {
    private final ReplacementsDownloadCompleteListener listener;
    private final CKZiUElektrykClient client;

    public ReplacementDataDownloader(CKZiUElektrykClient client, ReplacementsDownloadCompleteListener listener) {
        this.listener = listener;
        this.client = client;
    }

    @Override
    public void run() {
        SchoolEntryType timetableType = MainActivity.getTimetableType();
        String token = MainActivity.getToken(timetableType);

        if (Config.getOrCreateConfig().isPreviewMode()) {
            listener.onDownloadComplete(PreviewDataStore.getReplacements(timetableType, token));
            return;
        }

        // Cache first
        Context context = MainActivity.getContext();
        String cacheKey = "replacements_" + timetableType.name() + "_" + token;
        Type listType = new TypeToken<List<List<Replacement>>>(){}.getType();

        if (context != null && !token.isEmpty()) {
            try {
                List<List<Replacement>> cached = DatabaseCacheManager.getInstance(context).getObject(cacheKey, listType);
                if (cached != null && !cached.isEmpty()) {
                    listener.onDownloadComplete(cached);
                }
            } catch (Exception e) {
                Log.e("ReplacementsDownloader", "Failed to load cached replacements", e);
            }
        }

        try {
            ReplacementService replacementService = client.getReplacementService();
            Date[] next5Dates = getNext5Dates();
            List<List<Replacement>> latestReplacements = new ArrayList<>();
            boolean hasResponse = false;

            ReplacementType replacementType = (timetableType == SchoolEntryType.CLASSES)
                    ? ReplacementType.CLASSES
                    : ReplacementType.TEACHERS;

            for (Date date : next5Dates) {
                List<Replacement> rawReplacements = replacementService.getReplacements(replacementType, date);
                if (rawReplacements != null) {
                    hasResponse = true;
                    if (timetableType == SchoolEntryType.CLASSES) {
                        List<Replacement> filtered = rawReplacements.stream()
                                .filter(Objects::nonNull)
                                .filter(r -> Objects.equals(r.name(), token))
                                .collect(Collectors.toList());
                        latestReplacements.add(filtered);
                    } else {
                        latestReplacements.add(rawReplacements);
                    }
                }
            }

            if (hasResponse) {
                if (context != null && !token.isEmpty()) {
                    DatabaseCacheManager.getInstance(context).saveObject(cacheKey, latestReplacements);
                }
                listener.onDownloadComplete(latestReplacements);
            } else {
                listener.onDownloadFailed();
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }

    public static Date[] getNext5Dates() {
        Date[] dates = new Date[5];
        Calendar cal = Calendar.getInstance();

        int count = 0;
        while (count < 5) {
            int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);

            if (dayOfWeek != Calendar.SATURDAY && dayOfWeek != Calendar.SUNDAY) {
                dates[count] = cal.getTime();
                count++;
            }

            cal.add(Calendar.DAY_OF_MONTH, 1);
        }

        return dates;
    }
}
