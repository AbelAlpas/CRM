package com.example.CRM.opportunity.entity;

public enum OpportunityStatus {
    OPEN("Abierta"),
    WON("Ganada"),
    LOST("Perdida");

    private final String label;

    OpportunityStatus(String label) { this.label = label; }

    public String getLabel() { return label; }
}
