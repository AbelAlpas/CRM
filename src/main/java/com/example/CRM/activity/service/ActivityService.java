package com.example.CRM.activity.service;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityStatus;
import com.example.CRM.activity.entity.ActivityType;
import com.example.CRM.activity.entity.ActivityViewScope;
import com.example.CRM.activity.repository.ActivityRepository;
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
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ActivityService {
    private final ActivityRepository activities;
    private final UserRepository users;
    private final LeadService leads;
    private final ClientService clients;
    private final OpportunityService opportunities;
    private final ZoneId zoneId;

    public ActivityService(ActivityRepository activities, UserRepository users, LeadService leads,
                           ClientService clients, OpportunityService opportunities) {
        this.activities = activities;
        this.users = users;
        this.leads = leads;
        this.clients = clients;
        this.opportunities = opportunities;
        this.zoneId = ZoneId.systemDefault();
    }

    @Transactional(readOnly = true)
    public Page<Activity> search(String term, ActivityType type, ActivityStatus status,
                                 ActivityPriority priority, Long ownerId, Long leadId, Long clientId,
                                 Long opportunityId, ActivityViewScope scope, Pageable pageable,
                                 Authentication authentication) {
        Instant now = Instant.now();
        LocalDate today = LocalDate.now(zoneId);
        Long effectiveOwner = ownerIdForVisibility(ownerId, authentication);
        ActivityViewScope effectiveScope = scope == null ? ActivityViewScope.ALL : scope;
        return activities.search(normalize(term), type, status, priority, effectiveOwner, leadId, clientId,
            opportunityId, effectiveScope == ActivityViewScope.ALL, effectiveScope == ActivityViewScope.TODAY,
            effectiveScope == ActivityViewScope.UPCOMING, effectiveScope == ActivityViewScope.OVERDUE, now,
            today.atStartOfDay(zoneId).toInstant(), today.plusDays(1).atStartOfDay(zoneId).toInstant(), pageable);
    }

    @Transactional(readOnly = true)
    public Activity findById(Long id, Authentication authentication) {
        Activity activity = findExisting(id);
        assertCanAccess(activity, authentication);
        return activity;
    }

    @Transactional(readOnly = true)
    public ActivityCreateRequest prepareCreateRequest(Long leadId, Long clientId, Long opportunityId,
                                                      Authentication authentication) {
        validateSingleRelation(leadId, clientId, opportunityId);
        ActivityCreateRequest request = new ActivityCreateRequest();
        if (leadId != null) {
            leads.findById(leadId, authentication);
            request.setLeadId(leadId);
        }
        if (clientId != null) {
            clients.findById(clientId, authentication);
            request.setClientId(clientId);
        }
        if (opportunityId != null) {
            opportunities.findById(opportunityId, authentication);
            request.setOpportunityId(opportunityId);
        }
        return request;
    }

    @Transactional
    public Activity create(ActivityCreateRequest request, Authentication authentication) {
        validateSingleRelation(request.getLeadId(), request.getClientId(), request.getOpportunityId());
        User current = currentUser(authentication);
        Activity activity = new Activity();
        copyEditable(request.getActivityType(), request.getSubject(), request.getDescription(),
            request.getPriority(), request.getScheduledAt(), request.getDueAt(), activity);
        activity.setOwner(isSales(authentication) ? current : resolveEnabledOwner(request.getOwnerId()));
        activity.setCreatedBy(current);
        activity.setStatus(ActivityStatus.PENDING);
        activity.setCompletedAt(null);
        resolveRelation(request.getLeadId(), request.getClientId(), request.getOpportunityId(),
            activity, authentication);
        return activities.save(activity);
    }

    @Transactional
    public Activity update(Long id, ActivityUpdateRequest request, Authentication authentication) {
        Activity activity = findForChange(id, authentication);
        copyEditable(request.getActivityType(), request.getSubject(), request.getDescription(),
            request.getPriority(), request.getScheduledAt(), request.getDueAt(), activity);
        if (!isSales(authentication)) activity.setOwner(resolveEnabledOwner(request.getOwnerId()));
        return activities.save(activity);
    }

    @Transactional
    public Activity markInProgress(Long id, Authentication authentication) {
        Activity activity = findForChange(id, authentication);
        if (activity.getStatus() != ActivityStatus.PENDING) {
            throw new IllegalArgumentException("Solo una actividad pendiente puede marcarse en progreso");
        }
        activity.setStatus(ActivityStatus.IN_PROGRESS);
        return activities.save(activity);
    }

    @Transactional
    public Activity complete(Long id, Authentication authentication) {
        Activity activity = findForChange(id, authentication);
        activity.setStatus(ActivityStatus.COMPLETED);
        activity.setCompletedAt(Instant.now());
        return activities.save(activity);
    }

    @Transactional
    public Activity cancel(Long id, Authentication authentication) {
        Activity activity = findForChange(id, authentication);
        activity.setStatus(ActivityStatus.CANCELLED);
        activity.setCompletedAt(null);
        return activities.save(activity);
    }

    @Transactional(readOnly = true)
    public List<Activity> findByLead(Long leadId, Authentication authentication, int limit) {
        leads.findById(leadId, authentication);
        return activities.findForLead(leadId, ownerIdForVisibility(null, authentication), page(limit));
    }

    @Transactional(readOnly = true)
    public List<Activity> findByClient(Long clientId, Authentication authentication, int limit) {
        clients.findById(clientId, authentication);
        return activities.findForClient(clientId, ownerIdForVisibility(null, authentication), page(limit));
    }

    @Transactional(readOnly = true)
    public List<Activity> findByOpportunity(Long opportunityId, Authentication authentication, int limit) {
        opportunities.findById(opportunityId, authentication);
        return activities.findForOpportunity(opportunityId, ownerIdForVisibility(null, authentication), page(limit));
    }

    @Transactional(readOnly = true)
    public List<Activity> getUpcomingActivities(int limit, Authentication authentication) {
        return activities.findUpcoming(Instant.now(), ownerIdForVisibility(null, authentication), page(limit));
    }

    @Transactional(readOnly = true)
    public List<Activity> getOverdueActivities(int limit, Authentication authentication) {
        return activities.findOverdue(Instant.now(), ownerIdForVisibility(null, authentication), page(limit));
    }

    @Transactional(readOnly = true)
    public long countPending(Authentication authentication) {
        return activities.countPending(ownerIdForVisibility(null, authentication));
    }

    @Transactional(readOnly = true)
    public long countOverdue(Authentication authentication) {
        return activities.countOverdue(Instant.now(), ownerIdForVisibility(null, authentication));
    }

    @Transactional(readOnly = true)
    public long countDueToday(Authentication authentication) {
        LocalDate today = LocalDate.now(zoneId);
        return activities.countDueToday(today.atStartOfDay(zoneId).toInstant(),
            today.plusDays(1).atStartOfDay(zoneId).toInstant(), ownerIdForVisibility(null, authentication));
    }

    @Transactional(readOnly = true)
    public List<Activity> getRecentCompletedActivities(Instant from, Instant to, int limit,
                                                       Authentication authentication) {
        return activities.findCompletedBetween(from, to, ownerIdForVisibility(null, authentication), page(limit));
    }

    @Transactional(readOnly = true)
    public List<Lead> availableLeads(Authentication authentication) {
        return leads.search(null, null, null, null, Pageable.unpaged(), authentication).getContent();
    }

    @Transactional(readOnly = true)
    public List<Client> availableClients(Authentication authentication) {
        return clients.search(null, null, null, null, Pageable.unpaged(), authentication).getContent();
    }

    @Transactional(readOnly = true)
    public List<Opportunity> availableOpportunities(Authentication authentication) {
        return opportunities.search(null, null, null, null, null, null, Pageable.unpaged(), authentication).getContent();
    }

    public ActivityUpdateRequest toUpdateRequest(Activity activity) {
        ActivityUpdateRequest request = new ActivityUpdateRequest();
        request.setActivityType(activity.getActivityType());
        request.setSubject(activity.getSubject());
        request.setDescription(activity.getDescription());
        request.setPriority(activity.getPriority());
        request.setScheduledAt(toLocalDateTime(activity.getScheduledAt()));
        request.setDueAt(toLocalDateTime(activity.getDueAt()));
        request.setOwnerId(activity.getOwner() == null ? null : activity.getOwner().getId());
        return request;
    }

    private Activity findExisting(Long id) {
        return activities.findWithRelationsById(id)
            .orElseThrow(() -> new IllegalArgumentException("Actividad no encontrada"));
    }

    private Activity findForChange(Long id, Authentication authentication) {
        Activity activity = findExisting(id);
        assertCanAccess(activity, authentication);
        if (!activity.isOpen()) {
            throw new IllegalArgumentException("La actividad completada o cancelada no puede modificarse");
        }
        return activity;
    }

    private void resolveRelation(Long leadId, Long clientId, Long opportunityId, Activity activity,
                                 Authentication authentication) {
        if (leadId != null) activity.setLead(leads.findById(leadId, authentication));
        if (clientId != null) activity.setClient(clients.findById(clientId, authentication));
        if (opportunityId != null) activity.setOpportunity(opportunities.findById(opportunityId, authentication));
    }

    private void validateSingleRelation(Long leadId, Long clientId, Long opportunityId) {
        long relationCount = Stream.of(leadId, clientId, opportunityId).filter(Objects::nonNull).count();
        if (relationCount > 1) {
            throw new IllegalArgumentException("Una actividad solo puede relacionarse con un Lead, Cliente u Oportunidad");
        }
    }

    private void copyEditable(ActivityType type, String subject, String description, ActivityPriority priority,
                              LocalDateTime scheduledAt, LocalDateTime dueAt, Activity activity) {
        if (type == null) throw new IllegalArgumentException("El tipo de actividad es obligatorio");
        if (subject == null || subject.isBlank()) throw new IllegalArgumentException("El asunto es obligatorio");
        if (subject.trim().length() > 180) throw new IllegalArgumentException("El asunto no puede superar 180 caracteres");
        activity.setActivityType(type);
        activity.setSubject(subject.trim());
        activity.setDescription(trimToNull(description));
        activity.setPriority(priority == null ? ActivityPriority.MEDIUM : priority);
        activity.setScheduledAt(toInstant(scheduledAt));
        activity.setDueAt(toInstant(dueAt));
    }

    private User resolveEnabledOwner(Long id) {
        if (id == null) throw new IllegalArgumentException("Selecciona un responsable");
        User owner = users.findById(id)
            .orElseThrow(() -> new IllegalArgumentException("Responsable no encontrado"));
        if (!owner.isEnabled()) throw new IllegalArgumentException("El responsable seleccionado esta deshabilitado");
        return owner;
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    private void assertCanAccess(Activity activity, Authentication authentication) {
        if (isSales(authentication)
                && (activity.getOwner() == null
                    || !activity.getOwner().getEmail().equalsIgnoreCase(authentication.getName()))) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta Actividad");
        }
    }

    private Long ownerIdForVisibility(Long requestedOwnerId, Authentication authentication) {
        return isSales(authentication) ? currentUser(authentication).getId() : requestedOwnerId;
    }

    private boolean isSales(Authentication authentication) {
        return authentication.getAuthorities().stream()
            .anyMatch(authority -> authority.getAuthority().equals("ROLE_SALES"));
    }

    private Pageable page(int limit) {
        return org.springframework.data.domain.PageRequest.of(0, Math.max(1, limit));
    }

    private Instant toInstant(LocalDateTime value) {
        return value == null ? null : value.atZone(zoneId).toInstant();
    }

    private LocalDateTime toLocalDateTime(Instant value) {
        return value == null ? null : LocalDateTime.ofInstant(value, zoneId);
    }

    private String normalize(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }

    private String trimToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
