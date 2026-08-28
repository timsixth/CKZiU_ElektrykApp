package com.example.planlekcji.ckziu_elektryk.client.calendar;

import androidx.annotation.NonNull;

import com.google.gson.annotations.SerializedName;

public class CalendarEvent {

    @SerializedName("date_raw")
    private final String dateRaw;
    private final String description;

    @SerializedName("extra_info")
    private String extraInfo;

    public CalendarEvent(String dateRaw, String description, String extraInfo) {
        this.dateRaw = dateRaw;
        this.description = description;
        this.extraInfo = extraInfo;
    }


    public String dateRaw() {
        return dateRaw;
    }

    public String description() {
        return description;
    }

    public String extraInfo() {
        return extraInfo;
    }

    @NonNull
    @Override
    public String toString() {
        return "CalendarEvent{" +
                "dateRaw='" + dateRaw + '\'' +
                ", description='" + description + '\'' +
                ", extraInfo='" + extraInfo + '\'' +
                '}';
    }
}
