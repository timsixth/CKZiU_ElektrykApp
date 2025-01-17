package com.example.planlekcji.ckziu_elektryk.client.timetable.info;

import com.example.planlekcji.ckziu_elektryk.client.utils.DateUtil;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public record TimetableInfo(String applyAt, Date generatedAt) {

    public static final SimpleDateFormat GENERATED_AT_FORMATTER = new SimpleDateFormat("yyyy-MM-dd", Locale.ROOT);

    public static Date parseDate(String text) {
        return DateUtil.parsedDate(GENERATED_AT_FORMATTER, text);
    }
}
