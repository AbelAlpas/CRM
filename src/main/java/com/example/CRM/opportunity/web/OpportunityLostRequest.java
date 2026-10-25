package com.example.CRM.opportunity.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class OpportunityLostRequest {
    @NotBlank(message = "El motivo de pérdida es obligatorio")
    @Size(max = 1000, message = "El motivo no puede superar 1000 caracteres")
    private String lostReason;

    public String getLostReason() { return lostReason; }
    public void setLostReason(String value) { lostReason = value; }
}
