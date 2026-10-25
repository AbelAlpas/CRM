package com.example.CRM.opportunity.web;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;

public class OpportunityUpdateRequest {
    @NotBlank(message = "El nombre es obligatorio") @Size(max = 180) private String name;
    private String description;
    @NotNull(message = "El monto es obligatorio") @DecimalMin(value = "0.01", message = "El monto debe ser mayor que cero") @Digits(integer = 12, fraction = 2, message = "El monto admite máximo dos decimales") private BigDecimal amount;
    @NotNull(message = "La probabilidad es obligatoria") @Min(0) @Max(100) private Short probability;
    private LocalDate expectedCloseDate;
    private Long ownerId;

    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public Short getProbability() { return probability; }
    public void setProbability(Short value) { probability = value; }
    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDate value) { expectedCloseDate = value; }
    public Long getOwnerId() { return ownerId; }
    public void setOwnerId(Long value) { ownerId = value; }
}
