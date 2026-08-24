package com.example.planlekcji.listener;

import com.example.planlekcji.ckziu_elektryk.client.calendar.Calendar;

public interface CalendarDownloadCompleteListener {
    default void onCacheLoaded(Calendar calendar) {}
    void onDownloadComplete(Calendar calendar);
    void onDownloadFailed();
}
