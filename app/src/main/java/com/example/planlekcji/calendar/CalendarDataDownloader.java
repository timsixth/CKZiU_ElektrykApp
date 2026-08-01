package com.example.planlekcji.calendar;

import com.example.planlekcji.ckziu_elektryk.client.CKZiUElektrykClient;
import com.example.planlekcji.ckziu_elektryk.client.calendar.Calendar;
import com.example.planlekcji.listener.CalendarDownloadCompleteListener;

import java.util.Optional;

public class CalendarDataDownloader implements Runnable {
    private final CKZiUElektrykClient client;
    private final CalendarDownloadCompleteListener listener;

    public CalendarDataDownloader(CKZiUElektrykClient client, CalendarDownloadCompleteListener listener) {
        this.client = client;
        this.listener = listener;
    }

    @Override
    public void run() {
        try {
            Optional<Calendar> calendarOpt = client.getCalenderService().getLatestCalender();
            if (calendarOpt.isPresent()) {
                listener.onDownloadComplete(calendarOpt.get());
            } else {
                listener.onDownloadFailed();
            }
        } catch (Exception e) {
            listener.onDownloadFailed();
        }
    }
}
