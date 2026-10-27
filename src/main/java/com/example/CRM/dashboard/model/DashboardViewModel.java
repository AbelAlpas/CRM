package com.example.CRM.dashboard.model;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;

public record DashboardViewModel(
    DashboardPeriod period,
    LeadMetrics leads,
    ClientMetrics clients,
    OpportunityMetrics opportunities,
    ActivityMetrics activities,
    List<LeadStatusMetric> leadStatuses,
    List<PipelineStageMetric> pipelineStages,
    List<MonthlySalesMetric> monthlySales,
    List<RecentLead> recentLeads,
    List<UpcomingOpportunity> upcomingOpportunities,
    List<ActivityItem> todayActivities,
    List<ActivityItem> upcomingActivities
) {
    public record LeadMetrics(long newInPeriod) { }

    public record ClientMetrics(long active, long newInPeriod) { }

    public record OpportunityMetrics(
        long open,
        BigDecimal openAmount,
        BigDecimal wonAmount,
        BigDecimal weightedAmount,
        long won,
        long lost,
        BigDecimal closureRate
    ) { }

    public record ActivityMetrics(long pending, long overdue, long dueToday, long completedInPeriod) { }

    public record LeadStatusMetric(String code, String label, long count) { }

    public record PipelineStageMetric(
        Short id,
        String code,
        String name,
        long opportunityCount,
        BigDecimal totalAmount,
        int barWidth
    ) { }

    public record MonthlySalesMetric(int month, String label, BigDecimal totalAmount, int barWidth) { }

    public record RecentLead(
        Long id,
        String name,
        String company,
        String status,
        String seller,
        OffsetDateTime createdAt
    ) { }

    public record UpcomingOpportunity(
        Long id,
        String name,
        String client,
        BigDecimal amount,
        String stage,
        LocalDate expectedCloseDate,
        boolean overdue
    ) { }

    public record ActivityItem(
        Long id,
        String subject,
        String type,
        String status,
        Instant eventAt,
        String relatedName,
        String relatedUrl,
        boolean overdue
    ) { }
}
