package com.example.planlekcji.ckziu_elektryk.client.timetable.lesson;

public record Subject(String name, String shortcut) {
    public Subject {
        if (name != null && !name.isEmpty()) {
            name = Character.toUpperCase(name.charAt(0)) + name.substring(1);
        }
    }
}
