package com.example.CRM.lead.entity;

public enum LeadSource {
    WEBSITE("Sitio web"), FACEBOOK("Facebook"), INSTAGRAM("Instagram"), WHATSAPP("WhatsApp"), REFERRAL("Referido"), PHONE("Telefono"), EMAIL("Correo electronico"), OTHER("Otro");
    private final String label;
    LeadSource(String label) { this.label = label; }
    public String getLabel() { return label; }
}
