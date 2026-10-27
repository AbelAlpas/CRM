package com.example.CRM.dashboard.service;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.repository.ActivityRepository;
import com.example.CRM.client.entity.Client;
import com.example.CRM.client.repository.ClientRepository;
import com.example.CRM.dashboard.model.DashboardPeriod;
import com.example.CRM.dashboard.model.DashboardPeriod.PeriodBounds;
import com.example.CRM.dashboard.model.DashboardReadModels.ActivityAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.ClientAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.LeadAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.MonthlySalesAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.OpportunityAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.PipelineStageAggregate;
import com.example.CRM.dashboard.model.DashboardViewModel;
import com.example.CRM.dashboard.model.DashboardViewModel.ActivityItem;
import com.example.CRM.dashboard.model.DashboardViewModel.ActivityMetrics;
import com.example.CRM.dashboard.model.DashboardViewModel.ClientMetrics;
import com.example.CRM.dashboard.model.DashboardViewModel.LeadMetrics;
import com.example.CRM.dashboard.model.DashboardViewModel.LeadStatusMetric;
import com.example.CRM.dashboard.model.DashboardViewModel.MonthlySalesMetric;
import com.example.CRM.dashboard.model.DashboardViewModel.OpportunityMetrics;
import com.example.CRM.dashboard.model.DashboardViewModel.PipelineStageMetric;
import com.example.CRM.dashboard.model.DashboardViewModel.RecentLead;
import com.example.CRM.dashboard.model.DashboardViewModel.UpcomingOpportunity;
import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.repository.OpportunityRepository;
import com.example.CRM.opportunity.repository.PipelineStageRepository;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.Month;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class DashboardService {
    private static final int SHORT_LIST_SIZE = 5;
    private static final Locale SPANISH = Locale.forLanguageTag("es-PE");

    private final LeadRepository leads;
    private final ClientRepository clients;
    private final OpportunityRepository opportunities;
    private final PipelineStageRepository stages;
    private final ActivityRepository activities;
    private final UserRepository users;
    private final Clock clock;

    public DashboardService(LeadRepository leads, ClientRepository clients,
                            OpportunityRepository opportunities, PipelineStageRepository stages,
                            ActivityRepository activities, UserRepository users, Clock clock) {
        this.leads = leads;
        this.clients = clients;
        this.opportunities = opportunities;
        this.stages = stages;
        this.activities = activities;
        this.users = users;
        this.clock = clock;
    }

    @Transactional(readOnly = true)
    public DashboardViewModel getDashboard(Authentication authentication, DashboardPeriod requestedPeriod) {
        DashboardPeriod period = requestedPeriod == null ? DashboardPeriod.MONTH : requestedPeriod;
        ZonedDateTime now = ZonedDateTime.now(clock);
        PeriodBounds bounds = period.bounds(now);
        Long ownerId = visibleOwnerId(authentication);

        LeadAggregate leadAggregate = leads.dashboardMetrics(bounds.from(), bounds.to(), bounds.all(), ownerId);
        ClientAggregate clientAggregate = clients.dashboardMetrics(bounds.from(), bounds.to(), bounds.all(), ownerId);
        OpportunityAggregate opportunityAggregate = opportunities.dashboardMetrics(
            bounds.from(), bounds.to(), bounds.all(), ownerId);

        LocalDate today = now.toLocalDate();
        Instant dayStart = today.atStartOfDay(now.getZone()).toInstant();
        Instant dayEnd = today.plusDays(1).atStartOfDay(now.getZone()).toInstant();
        ActivityAggregate activityAggregate = activities.dashboardMetrics(
            now.toInstant(), dayStart, dayEnd, bounds.from().toInstant(), bounds.all(), ownerId);

        List<PipelineStageMetric> pipeline = pipelineMetrics(stages.findDashboardMetrics(ownerId));
        OffsetDateTime yearStart = today.withDayOfYear(1).atStartOfDay(now.getZone()).toOffsetDateTime();
        OffsetDateTime yearEnd = today.plusYears(1).withDayOfYear(1).atStartOfDay(now.getZone()).toOffsetDateTime();
        List<MonthlySalesMetric> monthlySales = monthlySales(
            opportunities.findMonthlyWonSales(yearStart, yearEnd, ownerId));

        List<RecentLead> recentLeads = leads.findRecentVisible(ownerId, page(SHORT_LIST_SIZE)).stream()
            .map(this::toRecentLead)
            .toList();
        List<UpcomingOpportunity> upcomingOpportunities = opportunities
            .findUpcomingToClose(ownerId, page(SHORT_LIST_SIZE)).stream()
            .map(opportunity -> toUpcomingOpportunity(opportunity, today))
            .toList();
        List<ActivityItem> todayActivities = activities
            .findDueToday(dayStart, dayEnd, ownerId, page(6)).stream()
            .map(activity -> toActivityItem(activity, now.toInstant()))
            .toList();
        List<ActivityItem> upcomingActivities = activities
            .findUpcoming(now.toInstant(), ownerId, page(SHORT_LIST_SIZE)).stream()
            .map(activity -> toActivityItem(activity, now.toInstant()))
            .toList();

        long won = number(opportunityAggregate == null ? null : opportunityAggregate.getWonCount());
        long lost = number(opportunityAggregate == null ? null : opportunityAggregate.getLostCount());
        return new DashboardViewModel(
            period,
            new LeadMetrics(number(leadAggregate == null ? null : leadAggregate.getNewInPeriod())),
            new ClientMetrics(
                number(clientAggregate == null ? null : clientAggregate.getActiveCount()),
                number(clientAggregate == null ? null : clientAggregate.getNewInPeriod())),
            new OpportunityMetrics(
                number(opportunityAggregate == null ? null : opportunityAggregate.getOpenCount()),
                money(opportunityAggregate == null ? null : opportunityAggregate.getOpenAmount()),
                money(opportunityAggregate == null ? null : opportunityAggregate.getWonAmount()),
                money(opportunityAggregate == null ? null : opportunityAggregate.getWeightedAmount()),
                won, lost, closureRate(won, lost)),
            new ActivityMetrics(
                number(activityAggregate == null ? null : activityAggregate.getPendingCount()),
                number(activityAggregate == null ? null : activityAggregate.getOverdueCount()),
                number(activityAggregate == null ? null : activityAggregate.getDueTodayCount()),
                number(activityAggregate == null ? null : activityAggregate.getCompletedInPeriod())),
            leadStatuses(leadAggregate),
            pipeline,
            monthlySales,
            recentLeads,
            upcomingOpportunities,
            todayActivities,
            upcomingActivities
        );
    }

    private List<LeadStatusMetric> leadStatuses(LeadAggregate aggregate) {
        return List.of(
            leadStatus(LeadStatus.NEW, aggregate == null ? null : aggregate.getNewCount()),
            leadStatus(LeadStatus.CONTACTED, aggregate == null ? null : aggregate.getContactedCount()),
            leadStatus(LeadStatus.QUALIFIED, aggregate == null ? null : aggregate.getQualifiedCount()),
            leadStatus(LeadStatus.DISQUALIFIED, aggregate == null ? null : aggregate.getDisqualifiedCount()),
            leadStatus(LeadStatus.CONVERTED, aggregate == null ? null : aggregate.getConvertedCount())
        );
    }

    private LeadStatusMetric leadStatus(LeadStatus status, Long count) {
        return new LeadStatusMetric(status.name(), status.getLabel(), number(count));
    }

    private List<PipelineStageMetric> pipelineMetrics(List<PipelineStageAggregate> aggregates) {
        long maximum = aggregates.stream()
            .map(PipelineStageAggregate::getOpportunityCount)
            .mapToLong(this::number)
            .max()
            .orElse(0);
        return aggregates.stream()
            .map(item -> new PipelineStageMetric(item.getStageId(), item.getStageCode(), item.getStageName(),
                number(item.getOpportunityCount()), money(item.getTotalAmount()),
                width(number(item.getOpportunityCount()), maximum)))
            .toList();
    }

    private List<MonthlySalesMetric> monthlySales(List<MonthlySalesAggregate> aggregates) {
        Map<Integer, MonthlySalesAggregate> byMonth = aggregates.stream()
            .collect(Collectors.toMap(MonthlySalesAggregate::getMonthNumber, Function.identity()));
        BigDecimal maximum = aggregates.stream()
            .map(MonthlySalesAggregate::getTotalAmount)
            .filter(value -> value != null)
            .max(BigDecimal::compareTo)
            .orElse(BigDecimal.ZERO);
        return IntStream.rangeClosed(1, 12)
            .mapToObj(month -> {
                MonthlySalesAggregate aggregate = byMonth.get(month);
                BigDecimal amount = money(aggregate == null ? null : aggregate.getTotalAmount());
                String label = Month.of(month).getDisplayName(TextStyle.SHORT, SPANISH);
                label = Character.toUpperCase(label.charAt(0)) + label.substring(1);
                return new MonthlySalesMetric(month, label, amount, width(amount, maximum));
            })
            .toList();
    }

    private RecentLead toRecentLead(Lead lead) {
        return new RecentLead(lead.getId(), personName(lead.getFirstName(), lead.getLastName()),
            valueOrDefault(lead.getCompanyName(), "Sin empresa"), lead.getStatus().getLabel(),
            userName(lead.getAssignedTo()), lead.getCreatedAt());
    }

    private UpcomingOpportunity toUpcomingOpportunity(Opportunity opportunity, LocalDate today) {
        return new UpcomingOpportunity(opportunity.getId(), opportunity.getName(), clientName(opportunity.getClient()),
            money(opportunity.getAmount()), opportunity.getStage().getName(), opportunity.getExpectedCloseDate(),
            opportunity.getExpectedCloseDate().isBefore(today));
    }

    private ActivityItem toActivityItem(Activity activity, Instant now) {
        String relatedName = null;
        String relatedUrl = null;
        if (activity.getLead() != null) {
            relatedName = personName(activity.getLead().getFirstName(), activity.getLead().getLastName());
            relatedUrl = "/leads/" + activity.getLead().getId();
        } else if (activity.getClient() != null) {
            relatedName = clientName(activity.getClient());
            relatedUrl = "/clients/" + activity.getClient().getId();
        } else if (activity.getOpportunity() != null) {
            relatedName = activity.getOpportunity().getName();
            relatedUrl = "/opportunities/" + activity.getOpportunity().getId();
        }
        Instant eventAt = activity.getDueAt() != null ? activity.getDueAt() : activity.getScheduledAt();
        boolean overdue = activity.getDueAt() != null && activity.getDueAt().isBefore(now) && activity.isOpen();
        return new ActivityItem(activity.getId(), activity.getSubject(), activity.getActivityType().getLabel(),
            activity.getStatus().getLabel(), eventAt, relatedName, relatedUrl, overdue);
    }

    private String clientName(Client client) {
        if (client == null) return "Sin cliente";
        return client.getClientType().name().equals("COMPANY")
            ? valueOrDefault(client.getCompanyName(), "Empresa sin nombre")
            : personName(client.getFirstName(), client.getLastName());
    }

    private String userName(User user) {
        return user == null ? "Sin asignar" : personName(user.getFirstName(), user.getLastName());
    }

    private String personName(String firstName, String lastName) {
        String first = valueOrDefault(firstName, "Sin nombre");
        return lastName == null || lastName.isBlank() ? first : first + " " + lastName;
    }

    private String valueOrDefault(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private Long visibleOwnerId(Authentication authentication) {
        if (!isSales(authentication)) return null;
        return users.findByEmailIgnoreCase(authentication.getName())
            .map(User::getId)
            .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    private boolean isSales(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SALES"));
    }

    private BigDecimal closureRate(long won, long lost) {
        long closed = won + lost;
        if (closed == 0) return BigDecimal.ZERO.setScale(1);
        return BigDecimal.valueOf(won)
            .multiply(BigDecimal.valueOf(100))
            .divide(BigDecimal.valueOf(closed), 1, RoundingMode.HALF_UP);
    }

    private BigDecimal money(BigDecimal value) {
        return (value == null ? BigDecimal.ZERO : value).setScale(2, RoundingMode.HALF_UP);
    }

    private long number(Long value) {
        return value == null ? 0 : value;
    }

    private int width(long value, long maximum) {
        if (value <= 0 || maximum <= 0) return 0;
        double ratio = (double) value / maximum;
        if (ratio <= 0.25) return 25;
        if (ratio <= 0.5) return 50;
        if (ratio <= 0.75) return 75;
        return 100;
    }

    private int width(BigDecimal value, BigDecimal maximum) {
        if (value.signum() <= 0 || maximum.signum() <= 0) return 0;
        BigDecimal ratio = value.divide(maximum, 4, RoundingMode.HALF_UP);
        if (ratio.compareTo(new BigDecimal("0.25")) <= 0) return 25;
        if (ratio.compareTo(new BigDecimal("0.50")) <= 0) return 50;
        if (ratio.compareTo(new BigDecimal("0.75")) <= 0) return 75;
        return 100;
    }

    private PageRequest page(int size) {
        return PageRequest.of(0, size);
    }
}
