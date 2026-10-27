package com.example.CRM.activity.entity;

public enum ActivityType {
    TASK("Tarea"),
    CALL("Llamada"),
    MEETING("Reunión"),
    FOLLOW_UP("Seguimiento");

    private final String label;

    ActivityType(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
