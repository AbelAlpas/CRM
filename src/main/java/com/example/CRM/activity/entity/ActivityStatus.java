package com.example.CRM.activity.entity;

public enum ActivityStatus {
    PENDING("Pendiente"),
    IN_PROGRESS("En progreso"),
    COMPLETED("Completada"),
    CANCELLED("Cancelada");

    private final String label;

    ActivityStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
