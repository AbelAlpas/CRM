package com.example.CRM.activity.entity;

public enum ActivityPriority {
    LOW("Baja"),
    MEDIUM("Media"),
    HIGH("Alta");

    private final String label;

    ActivityPriority(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
