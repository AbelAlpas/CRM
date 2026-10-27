package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityStatus;
import com.example.CRM.activity.entity.ActivityType;
import com.example.CRM.activity.entity.ActivityViewScope;
import com.example.CRM.activity.repository.ActivityRepository;
import com.example.CRM.activity.service.ActivityService;
import com.example.CRM.activity.web.ActivityCreateRequest;
import com.example.CRM.activity.web.ActivityUpdateRequest;
import com.example.CRM.client.entity.Client;
import com.example.CRM.client.service.ClientService;
import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.service.LeadService;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.service.OpportunityService;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.time.Instant;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ActivityServiceTests {
    @Mock ActivityRepository activities;
    @Mock UserRepository users;
    @Mock LeadService leads;
    @Mock ClientService clients;
    @Mock OpportunityService opportunities;

    private ActivityService service;
    private User admin;
    private User seller;
    private Authentication adminAuth;
    private Authentication salesAuth;

    @BeforeEach
    void setUp() {
        service = new ActivityService(activities, users, leads, clients, opportunities);
        admin = user("Admin", "CRM", "admin@example.com");
        seller = user("Ana", "Ventas", "seller@example.com");
        adminAuth = new TestingAuthenticationToken(admin.getEmail(), "", "ROLE_ADMIN");
        salesAuth = new TestingAuthenticationToken(seller.getEmail(), "", "ROLE_SALES");
        lenient().when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        lenient().when(users.findByEmailIgnoreCase(seller.getEmail())).thenReturn(Optional.of(seller));
        lenient().when(activities.save(any(Activity.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void adminCreatesPendingActivityWithSelectedOwnerAndDefaultPriority() {
        User owner = user("Luis", "Asesor", "luis@example.com");
        when(users.findById(20L)).thenReturn(Optional.of(owner));
        ActivityCreateRequest request = validCreate();
        request.setOwnerId(20L);
        request.setPriority(null);

        Activity created = service.create(request, adminAuth);

        assertThat(created.getOwner()).isSameAs(owner);
        assertThat(created.getCreatedBy()).isSameAs(admin);
        assertThat(created.getStatus()).isEqualTo(ActivityStatus.PENDING);
        assertThat(created.getPriority()).isEqualTo(ActivityPriority.MEDIUM);
        assertThat(created.getCompletedAt()).isNull();
    }

    @Test
    void managerCanCreateActivity() {
        User owner = user("Luis", "Asesor", "luis@example.com");
        when(users.findById(20L)).thenReturn(Optional.of(owner));
        ActivityCreateRequest request = validCreate();
        request.setOwnerId(20L);
        Authentication manager = new TestingAuthenticationToken(admin.getEmail(), "", "ROLE_MANAGER");

        Activity created = service.create(request, manager);

        assertThat(created.getOwner()).isSameAs(owner);
    }

    @Test
    void salesActivityIsAlwaysAssignedToAuthenticatedUser() {
        ActivityCreateRequest request = validCreate();
        request.setOwnerId(999L);

        Activity created = service.create(request, salesAuth);

        assertThat(created.getOwner()).isSameAs(seller);
        verify(users, never()).findById(999L);
    }

    @Test
    void createsActivitiesForEachSupportedRelationAndWithoutRelation() {
        Lead lead = new Lead();
        Client client = new Client();
        Opportunity opportunity = new Opportunity();
        when(leads.findById(1L, salesAuth)).thenReturn(lead);
        when(clients.findById(2L, salesAuth)).thenReturn(client);
        when(opportunities.findById(3L, salesAuth)).thenReturn(opportunity);

        ActivityCreateRequest leadRequest = validCreate();
        leadRequest.setLeadId(1L);
        ActivityCreateRequest clientRequest = validCreate();
        clientRequest.setClientId(2L);
        ActivityCreateRequest opportunityRequest = validCreate();
        opportunityRequest.setOpportunityId(3L);

        assertThat(service.create(leadRequest, salesAuth).getLead()).isSameAs(lead);
        assertThat(service.create(clientRequest, salesAuth).getClient()).isSameAs(client);
        assertThat(service.create(opportunityRequest, salesAuth).getOpportunity()).isSameAs(opportunity);
        Activity general = service.create(validCreate(), salesAuth);
        assertThat(general.getLead()).isNull();
        assertThat(general.getClient()).isNull();
        assertThat(general.getOpportunity()).isNull();
    }

    @Test
    void rejectsMoreThanOneCommercialRelation() {
        ActivityCreateRequest request = validCreate();
        request.setLeadId(1L);
        request.setClientId(2L);

        assertThatThrownBy(() -> service.create(request, salesAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("solo puede relacionarse");
        verify(activities, never()).save(any());
    }

    @Test
    void rejectsMissingOrForbiddenRelatedEntity() {
        ActivityCreateRequest missing = validCreate();
        missing.setLeadId(8L);
        when(leads.findById(8L, salesAuth)).thenThrow(new IllegalArgumentException("Lead no encontrado"));

        assertThatThrownBy(() -> service.create(missing, salesAuth))
            .isInstanceOf(IllegalArgumentException.class);

        ActivityCreateRequest forbidden = validCreate();
        forbidden.setClientId(9L);
        when(clients.findById(9L, salesAuth)).thenThrow(new AccessDeniedException("denegado"));
        assertThatThrownBy(() -> service.create(forbidden, salesAuth))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void pendingActivityCanStart() {
        Activity activity = openActivity(seller, ActivityStatus.PENDING);
        when(activities.findWithRelationsById(5L)).thenReturn(Optional.of(activity));

        service.markInProgress(5L, salesAuth);

        assertThat(activity.getStatus()).isEqualTo(ActivityStatus.IN_PROGRESS);
    }

    @Test
    void pendingAndInProgressActivitiesCanComplete() {
        Activity pending = openActivity(seller, ActivityStatus.PENDING);
        Activity inProgress = openActivity(seller, ActivityStatus.IN_PROGRESS);
        when(activities.findWithRelationsById(5L)).thenReturn(Optional.of(pending));
        when(activities.findWithRelationsById(6L)).thenReturn(Optional.of(inProgress));

        service.complete(5L, salesAuth);
        service.complete(6L, salesAuth);

        assertThat(pending.getStatus()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(pending.getCompletedAt()).isNotNull();
        assertThat(inProgress.getStatus()).isEqualTo(ActivityStatus.COMPLETED);
        assertThat(inProgress.getCompletedAt()).isNotNull();
    }

    @Test
    void pendingAndInProgressActivitiesCanCancelWithoutCompletionDate() {
        Activity pending = openActivity(seller, ActivityStatus.PENDING);
        Activity inProgress = openActivity(seller, ActivityStatus.IN_PROGRESS);
        when(activities.findWithRelationsById(5L)).thenReturn(Optional.of(pending));
        when(activities.findWithRelationsById(6L)).thenReturn(Optional.of(inProgress));

        service.cancel(5L, salesAuth);
        service.cancel(6L, salesAuth);

        assertThat(pending.getStatus()).isEqualTo(ActivityStatus.CANCELLED);
        assertThat(pending.getCompletedAt()).isNull();
        assertThat(inProgress.getStatus()).isEqualTo(ActivityStatus.CANCELLED);
        assertThat(inProgress.getCompletedAt()).isNull();
    }

    @Test
    void terminalActivitiesCannotBeChangedOrEdited() {
        Activity completed = openActivity(seller, ActivityStatus.COMPLETED);
        Activity cancelled = openActivity(seller, ActivityStatus.CANCELLED);
        when(activities.findWithRelationsById(5L)).thenReturn(Optional.of(completed));
        when(activities.findWithRelationsById(6L)).thenReturn(Optional.of(cancelled));

        assertThatThrownBy(() -> service.complete(5L, salesAuth)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.cancel(5L, salesAuth)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.complete(6L, salesAuth)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(5L, validUpdate(), salesAuth)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.update(6L, validUpdate(), salesAuth)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void salesCannotAccessAnotherOwnersActivity() {
        Activity activity = openActivity(user("Otro", "Vendedor", "other@example.com"), ActivityStatus.PENDING);
        when(activities.findWithRelationsById(5L)).thenReturn(Optional.of(activity));

        assertThatThrownBy(() -> service.findById(5L, salesAuth))
            .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    void salesSearchIsAlwaysRestrictedToAuthenticatedOwner() {
        User authenticatedSeller = mock(User.class);
        when(authenticatedSeller.getId()).thenReturn(77L);
        when(users.findByEmailIgnoreCase("seller@example.com")).thenReturn(Optional.of(authenticatedSeller));
        Authentication authentication = new TestingAuthenticationToken("seller@example.com", "", "ROLE_SALES");
        service.search(null, null, null, null, 999L, null, null, null,
            ActivityViewScope.ALL, Pageable.unpaged(), authentication);

        verify(activities).search(any(), any(), any(), any(), org.mockito.ArgumentMatchers.eq(77L),
            any(), any(), any(), org.mockito.ArgumentMatchers.eq(true),
            org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.eq(false),
            org.mockito.ArgumentMatchers.eq(false), any(), any(), any(), any(Pageable.class));
    }

    @Test
    void overdueStateIsCalculatedOnlyForOpenActivities() {
        Activity activity = openActivity(seller, ActivityStatus.PENDING);
        activity.setDueAt(Instant.now().minusSeconds(60));
        assertThat(activity.isOverdue()).isTrue();

        activity.setStatus(ActivityStatus.COMPLETED);
        assertThat(activity.isOverdue()).isFalse();
    }

    private ActivityCreateRequest validCreate() {
        ActivityCreateRequest request = new ActivityCreateRequest();
        request.setActivityType(ActivityType.CALL);
        request.setSubject("Llamar al prospecto");
        request.setPriority(ActivityPriority.HIGH);
        return request;
    }

    private ActivityUpdateRequest validUpdate() {
        ActivityUpdateRequest request = new ActivityUpdateRequest();
        request.setActivityType(ActivityType.MEETING);
        request.setSubject("Reunion comercial");
        request.setPriority(ActivityPriority.MEDIUM);
        return request;
    }

    private Activity openActivity(User owner, ActivityStatus status) {
        Activity activity = new Activity();
        activity.setActivityType(ActivityType.TASK);
        activity.setSubject("Actividad de prueba");
        activity.setPriority(ActivityPriority.MEDIUM);
        activity.setOwner(owner);
        activity.setStatus(status);
        return activity;
    }

    private User user(String firstName, String lastName, String email) {
        return new User(firstName, lastName, email, "encoded");
    }
}
