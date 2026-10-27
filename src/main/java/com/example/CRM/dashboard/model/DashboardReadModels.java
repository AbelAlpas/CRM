package com.example.CRM.dashboard.model;

import java.math.BigDecimal;

public final class DashboardReadModels {
    private DashboardReadModels() { }

    public interface LeadAggregate {
        Long getNewInPeriod();
        Long getNewCount();
        Long getContactedCount();
        Long getQualifiedCount();
        Long getDisqualifiedCount();
        Long getConvertedCount();
    }

    public interface ClientAggregate {
        Long getActiveCount();
        Long getNewInPeriod();
    }

    public interface OpportunityAggregate {
        Long getOpenCount();
        BigDecimal getOpenAmount();
        BigDecimal getWeightedAmount();
        Long getWonCount();
        Long getLostCount();
        BigDecimal getWonAmount();
    }

    public interface PipelineStageAggregate {
        Short getStageId();
        String getStageCode();
        String getStageName();
        Short getDisplayOrder();
        Long getOpportunityCount();
        BigDecimal getTotalAmount();
    }

    public interface MonthlySalesAggregate {
        Integer getMonthNumber();
        BigDecimal getTotalAmount();
    }

    public interface ActivityAggregate {
        Long getPendingCount();
        Long getOverdueCount();
        Long getDueTodayCount();
        Long getCompletedInPeriod();
    }
}
