package com.example.CRM.pipeline.web;

import java.math.BigDecimal;
import java.util.List;

public record PipelineView(
    List<PipelineStageView> stages,
    int totalCount,
    BigDecimal totalAmount,
    BigDecimal weightedAmount
) { }
