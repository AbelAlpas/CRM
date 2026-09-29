package com.example.CRM.lead.entity;

public enum LeadStatus {
    NEW("Nuevo"), CONTACTED("Contactado"), QUALIFIED("Calificado"), DISQUALIFIED("Descalificado"), CONVERTED("Convertido");
    private final String label;
    LeadStatus(String label) { this.label = label; }
    public String getLabel() { return label; }
}
