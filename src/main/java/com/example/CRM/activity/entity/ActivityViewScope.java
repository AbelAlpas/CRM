package com.example.CRM.activity.entity;

public enum ActivityViewScope {
    ALL("Todas"),
    TODAY("Hoy"),
    UPCOMING("Próximas"),
    OVERDUE("Vencidas");

    private final String label;

    ActivityViewScope(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
