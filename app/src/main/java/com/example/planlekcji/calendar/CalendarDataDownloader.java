package com.example.planlekcji.calendar;

import android.content.Context;
import android.util.Log;

import com.example.planlekcji.MainActivity;
import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.calendar.Calendar;
import com.example.planlekcji.database.DatabaseCacheManager;
import com.example.planlekcji.listener.CalendarDownloadCompleteListener;

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

        if (context != null) {
            try {
                Calendar cached = DatabaseCacheManager.getInstance(context).getObject(CACHE_KEY, Calendar.class);
                if (cached != null) {
                    listener.onDownloadComplete(cached);
                }
            } catch (Exception e) {
                Log.e("CalendarDownloader", "Failed to load cached calendar", e);
            }
        }

        try {
            Optional<Calendar> calendarOpt = client.getCalenderService().getLatestCalender();
            if (calendarOpt.isPresent()) {
                Calendar cal = calendarOpt.get();
                if (context != null) {
                    DatabaseCacheManager.getInstance(context).saveObject(CACHE_KEY, cal);
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
