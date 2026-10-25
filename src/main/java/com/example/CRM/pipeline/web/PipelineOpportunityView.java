package com.example.CRM.pipeline.web;

import java.math.BigDecimal;
import java.time.LocalDate;

public record PipelineOpportunityView(
    Long id,
    String name,
    Long clientId,
    String clientName,
    BigDecimal amount,
    Short probability,
    BigDecimal weightedAmount,
    String ownerName,
    LocalDate expectedCloseDate,
    boolean overdue,
    String stageCode
) { }
