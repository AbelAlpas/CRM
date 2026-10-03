package com.example.CRM.client.entity;

public enum ClientType {
    PERSON("Persona"), COMPANY("Empresa");
    private final String label;
    ClientType(String label) { this.label = label; }
    public String getLabel() { return label; }
}
