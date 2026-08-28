package com.example.planlekcji.calendar;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.calendar.Calendar;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.listener.CalendarDownloadCompleteListener;
import com.example.planlekcji.utils.RefreshCooldownManager;
import com.example.planlekcji.utils.RefreshDataType;

import java.util.Optional;

public class CalendarDataDownloader implements Runnable {
    private static final String CACHE_KEY = "calendar_latest";
    private final CKZiUElektrykClient client;
    private final CalendarDownloadCompleteListener listener;

    public CalendarDataDownloader(CKZiUElektrykClient client, CalendarDownloadCompleteListener listener) {
        this.client = client;
        this.listener = listener;
    }

    @Override
    public void run() {
        Context context = MainActivity.getContext();
        Calendar cached = null;

        if (context != null) {
            try {
                cached = DatabaseCacheManager.getInstance(context).getObject(CACHE_KEY, Calendar.class);
                if (cached != null) {
                    listener.onCacheLoaded(cached);
                }
            } catch (Exception e) {
                Log.e("CalendarDownloader", "Failed to load cached calendar", e);
            }
        }

        // Complete immediately if cache is fresh within TTL
        RefreshCooldownManager cooldown = (context != null) ? RefreshCooldownManager.getInstance(context) : null;
        if (cooldown != null && cooldown.isFresh(RefreshDataType.CALENDAR, cached)) {
            listener.onDownloadComplete(cached);
            return;
        }

        try {
            Optional<Calendar> calendarOpt = client.getCalenderService().getLatestCalender();
            if (calendarOpt.isPresent()) {
                Calendar cal = calendarOpt.get();
                if (context != null) {
                    DatabaseCacheManager.getInstance(context).saveObject(CACHE_KEY, cal);
                    if (cooldown != null) {
                        cooldown.recordRefresh(RefreshDataType.CALENDAR);
                    }
                }
                listener.onDownloadComplete(cal);
            } else {
                listener.onDownloadFailed();
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }
}
