package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.repository.ActivityRepository;
import com.example.CRM.client.repository.ClientRepository;
import com.example.CRM.dashboard.model.DashboardPeriod;
import com.example.CRM.dashboard.model.DashboardViewModel;
import com.example.CRM.dashboard.model.DashboardViewModel.LeadStatusMetric;
import com.example.CRM.dashboard.service.DashboardService;
import com.example.CRM.dashboard.model.DashboardReadModels.ActivityAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.ClientAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.LeadAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.MonthlySalesAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.OpportunityAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.PipelineStageAggregate;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.opportunity.repository.OpportunityRepository;
import com.example.CRM.opportunity.repository.PipelineStageRepository;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.domain.Pageable;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

class DashboardServiceTests {
    private final LeadRepository leads = mock(LeadRepository.class);
    private final ClientRepository clients = mock(ClientRepository.class);
    private final OpportunityRepository opportunities = mock(OpportunityRepository.class);
    private final PipelineStageRepository stages = mock(PipelineStageRepository.class);
    private final ActivityRepository activities = mock(ActivityRepository.class);
    private final UserRepository users = mock(UserRepository.class);
    private final Clock clock = Clock.fixed(Instant.parse("2026-10-26T15:00:00Z"), ZoneId.of("America/Lima"));
    private DashboardService service;

    @BeforeEach
    void setUp() {
        service = new DashboardService(leads, clients, opportunities, stages, activities, users, clock);
        when(stages.findDashboardMetrics(any())).thenReturn(List.of());
        when(opportunities.findMonthlyWonSales(any(), any(), any())).thenReturn(List.of());
        when(leads.findRecentVisible(any(), any(Pageable.class))).thenReturn(List.of());
        when(opportunities.findUpcomingToClose(any(), any(Pageable.class))).thenReturn(List.of());
        when(activities.findDueToday(any(), any(), any(), any(Pageable.class))).thenReturn(List.of());
        when(activities.findUpcoming(any(), any(), any(Pageable.class))).thenReturn(List.of());
    }

    @Test
    void adminDashboardAggregatesKpisAndBigDecimals() {
        LeadAggregate lead = mock(LeadAggregate.class);
        when(lead.getNewInPeriod()).thenReturn(4L);
        when(lead.getNewCount()).thenReturn(2L);
        when(lead.getContactedCount()).thenReturn(1L);
        when(lead.getQualifiedCount()).thenReturn(1L);
        when(lead.getDisqualifiedCount()).thenReturn(0L);
        when(lead.getConvertedCount()).thenReturn(1L);
        when(leads.dashboardMetrics(any(), any(), eq(false), eq(null))).thenReturn(lead);

        ClientAggregate client = mock(ClientAggregate.class);
        when(client.getActiveCount()).thenReturn(6L);
        when(client.getNewInPeriod()).thenReturn(2L);
        when(clients.dashboardMetrics(any(), any(), eq(false), eq(null))).thenReturn(client);

        OpportunityAggregate opportunity = mock(OpportunityAggregate.class);
        when(opportunity.getOpenCount()).thenReturn(3L);
        when(opportunity.getOpenAmount()).thenReturn(new BigDecimal("1000.00"));
        when(opportunity.getWeightedAmount()).thenReturn(new BigDecimal("450.00"));
        when(opportunity.getWonCount()).thenReturn(3L);
        when(opportunity.getLostCount()).thenReturn(2L);
        when(opportunity.getWonAmount()).thenReturn(new BigDecimal("750.25"));
        when(opportunities.dashboardMetrics(any(), any(), eq(false), eq(null))).thenReturn(opportunity);

        ActivityAggregate activity = mock(ActivityAggregate.class);
        when(activity.getPendingCount()).thenReturn(5L);
        when(activity.getOverdueCount()).thenReturn(1L);
        when(activity.getDueTodayCount()).thenReturn(2L);
        when(activity.getCompletedInPeriod()).thenReturn(4L);
        when(activities.dashboardMetrics(any(), any(), any(), any(), eq(false), eq(null))).thenReturn(activity);

        DashboardViewModel dashboard = service.getDashboard(authentication("admin", "ADMIN"), DashboardPeriod.MONTH);

        assertThat(dashboard.leads().newInPeriod()).isEqualTo(4);
        assertThat(dashboard.clients().active()).isEqualTo(6);
        assertThat(dashboard.opportunities().openAmount()).isEqualByComparingTo("1000.00");
        assertThat(dashboard.opportunities().weightedAmount()).isEqualByComparingTo("450.00");
        assertThat(dashboard.opportunities().closureRate()).isEqualByComparingTo("60.0");
        assertThat(dashboard.activities().overdue()).isEqualTo(1);
        assertThat(dashboard.leadStatuses()).extracting(LeadStatusMetric::count)
            .containsExactly(2L, 1L, 1L, 0L, 1L);
    }

    @Test
    void salesDashboardPassesOnlyAuthenticatedOwnerToEveryRepository() {
        User seller = mock(User.class);
        when(seller.getId()).thenReturn(42L);
        when(users.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(seller));
        stubEmptyAggregates(42L);

        DashboardViewModel dashboard = service.getDashboard(authentication("seller@example.com", "SALES"), DashboardPeriod.YEAR);

        assertThat(dashboard).isNotNull();
        verify(leads).dashboardMetrics(any(), any(), eq(false), eq(42L));
        verify(clients).dashboardMetrics(any(), any(), eq(false), eq(42L));
        verify(opportunities).dashboardMetrics(any(), any(), eq(false), eq(42L));
        verify(stages).findDashboardMetrics(42L);
        verify(activities).dashboardMetrics(any(), any(), any(), any(), eq(false), eq(42L));
        verify(leads).findRecentVisible(eq(42L), any(Pageable.class));
        verify(opportunities).findUpcomingToClose(eq(42L), any(Pageable.class));
    }

    @Test
    void allPeriodUsesAllFlagAndZeroDataDoesNotProduceNullsOrInfinity() {
        stubEmptyAggregates(null);

        DashboardViewModel dashboard = service.getDashboard(authentication("admin", "ADMIN"), DashboardPeriod.ALL);

        assertThat(dashboard.opportunities().openAmount()).isEqualByComparingTo("0.00");
        assertThat(dashboard.opportunities().closureRate()).isEqualByComparingTo("0.0");
        assertThat(dashboard.activities().pending()).isZero();
        verify(leads).dashboardMetrics(any(), any(), eq(true), eq(null));
        verify(clients).dashboardMetrics(any(), any(), eq(true), eq(null));
        verify(opportunities).dashboardMetrics(any(), any(), eq(true), eq(null));
        verify(activities).dashboardMetrics(any(), any(), any(), any(), eq(true), eq(null));
    }

    @Test
    void invalidPeriodFallsBackToMonth() {
        assertThat(DashboardPeriod.fromParam("unknown")).isEqualTo(DashboardPeriod.MONTH);
        assertThat(DashboardPeriod.fromParam(null)).isEqualTo(DashboardPeriod.MONTH);
        assertThat(DashboardPeriod.fromParam("year")).isEqualTo(DashboardPeriod.YEAR);
        assertThat(DashboardPeriod.YEAR.bounds(clock.instant().atZone(clock.getZone())).from().getMonthValue()).isEqualTo(1);
    }

    private void stubEmptyAggregates(Long ownerId) {
        when(leads.dashboardMetrics(any(), any(), any(Boolean.class), eq(ownerId))).thenReturn(null);
        when(clients.dashboardMetrics(any(), any(), any(Boolean.class), eq(ownerId))).thenReturn(null);
        when(opportunities.dashboardMetrics(any(), any(), any(Boolean.class), eq(ownerId))).thenReturn(null);
        when(activities.dashboardMetrics(any(), any(), any(), any(), any(Boolean.class), eq(ownerId))).thenReturn(null);
    }

    private Authentication authentication(String name, String role) {
        return new TestingAuthenticationToken(name, "", "ROLE_" + role);
    }
}
