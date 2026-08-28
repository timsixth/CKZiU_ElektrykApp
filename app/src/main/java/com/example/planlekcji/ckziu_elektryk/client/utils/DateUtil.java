package com.example.planlekcji.ckziu_elektryk.client.utils;

import android.util.Log;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;

public final class DateUtil {

    private DateUtil() {}

    public static Date parseDate(SimpleDateFormat simpleDateFormat, String text) {
        if (text == null || text.trim().isEmpty()) return null;
        try {
            return simpleDateFormat.parse(text);
        } catch (ParseException e) {
            Log.e("DateUtil", "Failed to parse date: " + text, e);
            return null;
        }
    }

    public static String formatDate(SimpleDateFormat simpleDateFormat, Date date) {
        return simpleDateFormat.format(date);
    }
}
