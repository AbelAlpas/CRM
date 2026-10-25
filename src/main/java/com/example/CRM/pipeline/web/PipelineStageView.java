package com.example.CRM.pipeline.web;

import java.math.BigDecimal;
import java.util.List;

public record PipelineStageView(
    Short id,
    String code,
    String name,
    Short displayOrder,
    List<PipelineOpportunityView> opportunities,
    int count,
    BigDecimal totalAmount
) { }
