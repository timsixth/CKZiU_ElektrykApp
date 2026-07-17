package com.example.planlekcji.ckziu_elektryk.client.timetable;

import androidx.annotation.NonNull;

public class SchoolEntry {

    private final String shortcut;
    private String name;

    public SchoolEntry(String shortcut) {
        this.shortcut = shortcut;
        this.name = "";
    }

    public String shortcut() {
        return shortcut;
    }

    public String name() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    @NonNull
    @Override
    public String toString() {
        return "SchoolEntry{" +
                "shortcut='" + shortcut + '\'' +
                ", name='" + name + '\'' +
                '}';
    }
}
