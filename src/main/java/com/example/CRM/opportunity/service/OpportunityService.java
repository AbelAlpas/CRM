package com.example.CRM.opportunity.service;

import com.example.CRM.client.entity.Client;
import com.example.CRM.client.repository.ClientRepository;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.OpportunityStageHistory;
import com.example.CRM.opportunity.entity.OpportunityStatus;
import com.example.CRM.opportunity.entity.PipelineStage;
import com.example.CRM.opportunity.repository.OpportunityRepository;
import com.example.CRM.opportunity.repository.OpportunityStageHistoryRepository;
import com.example.CRM.opportunity.repository.PipelineStageRepository;
import com.example.CRM.opportunity.web.OpportunityCreateRequest;
import com.example.CRM.opportunity.web.OpportunityUpdateRequest;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class OpportunityService {
    private static final String PROSPECTING = "PROSPECTING";
    private static final String WON = "WON";
    private static final String LOST = "LOST";
    private static final Set<String> OPEN_STAGE_CODES = Set.of("PROSPECTING", "QUALIFICATION", "PROPOSAL", "NEGOTIATION");

    private final OpportunityRepository opportunities;
    private final PipelineStageRepository stages;
    private final OpportunityStageHistoryRepository history;
    private final ClientRepository clients;
    private final UserRepository users;

    public OpportunityService(OpportunityRepository opportunities, PipelineStageRepository stages,
                              OpportunityStageHistoryRepository history, ClientRepository clients,
                              UserRepository users) {
        this.opportunities = opportunities;
        this.stages = stages;
        this.history = history;
        this.clients = clients;
        this.users = users;
    }

    @Transactional(readOnly = true)
    public Page<Opportunity> search(String term, OpportunityStatus status, Short stageId, Long ownerId,
                                    Long clientId, LocalDate expectedCloseDate, Pageable pageable,
                                    Authentication authentication) {
        User current = currentUser(authentication);
        Long effectiveOwner = isSales(authentication) ? current.getId() : ownerId;
        return opportunities.search(normalize(term), status, stageId, effectiveOwner, clientId, expectedCloseDate, pageable);
    }

    @Transactional(readOnly = true)
    public Opportunity findById(Long id, Authentication authentication) {
        Opportunity opportunity = findExisting(id);
        assertCanAccess(opportunity, authentication);
        return opportunity;
    }

    @Transactional(readOnly = true)
    public List<Opportunity> findByClient(Long clientId, Authentication authentication) {
        Client client = clients.findWithRelationsById(clientId)
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (isSales(authentication) && !isAssignedTo(client.getAssignedTo(), authentication)) {
            throw new AccessDeniedException("No tienes permiso para acceder a las oportunidades de este Cliente");
        }
        return opportunities.findAllByClientIdOrderByCreatedAtDesc(clientId);
    }

    @Transactional(readOnly = true)
    public List<Client> availableClients(Authentication authentication) {
        User current = currentUser(authentication);
        return isSales(authentication)
            ? clients.findAllByActiveTrueAndAssignedToIdOrderByCompanyNameAscFirstNameAsc(current.getId())
            : clients.findAllByActiveTrueOrderByCompanyNameAscFirstNameAsc();
    }

    @Transactional(readOnly = true)
    public List<PipelineStage> allStages() { return stages.findAllByActiveTrueOrderByDisplayOrderAsc(); }

    @Transactional(readOnly = true)
    public List<PipelineStage> openStages() {
        return stages.findAllByActiveTrueAndClosedFalseOrderByDisplayOrderAsc().stream()
            .filter(stage -> OPEN_STAGE_CODES.contains(stage.getCode()))
            .toList();
    }

    @Transactional(readOnly = true)
    public List<OpportunityStageHistory> getStageHistory(Long id, Authentication authentication) {
        findById(id, authentication);
        return history.findAllByOpportunityIdOrderByChangedAtDesc(id);
    }

    @Transactional
    public Opportunity create(OpportunityCreateRequest request, Authentication authentication) {
        User current = currentUser(authentication);
        Client client = clients.findWithRelationsById(request.getClientId())
            .orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        if (!client.isActive()) throw new IllegalArgumentException("No se puede crear una oportunidad para un Cliente inactivo");
        if (isSales(authentication) && !isAssignedTo(client.getAssignedTo(), authentication)) {
            throw new AccessDeniedException("Sólo puedes crear oportunidades para tus Clientes asignados");
        }

        User owner = isSales(authentication) ? current : resolveEnabledOwner(request.getOwnerId());
        PipelineStage initialStage = requireStage(PROSPECTING);
        validateOpenStage(initialStage);

        Opportunity opportunity = new Opportunity();
        opportunity.setClient(client);
        opportunity.setCreatedBy(current);
        opportunity.setOwner(owner);
        opportunity.setStage(initialStage);
        opportunity.setStatus(OpportunityStatus.OPEN);
        opportunity.setClosedAt(null);
        opportunity.setLostReason(null);
        copyEditable(request.getName(), request.getDescription(), request.getAmount(), request.getProbability(), request.getExpectedCloseDate(), opportunity);
        Opportunity saved = opportunities.save(opportunity);
        history.save(new OpportunityStageHistory(saved, null, initialStage, current));
        return saved;
    }

    @Transactional
    public Opportunity update(Long id, OpportunityUpdateRequest request, Authentication authentication) {
        Opportunity opportunity = findForChange(id, authentication);
        copyEditable(request.getName(), request.getDescription(), request.getAmount(), request.getProbability(), request.getExpectedCloseDate(), opportunity);
        if (!isSales(authentication)) opportunity.setOwner(resolveEnabledOwner(request.getOwnerId()));
        return opportunities.save(opportunity);
    }

    @Transactional
    public Opportunity changeStage(Long id, String targetStageCode, Authentication authentication) {
        Opportunity opportunity = findForChange(id, authentication);
        String normalizedCode = normalizeCode(targetStageCode);
        if (!OPEN_STAGE_CODES.contains(normalizedCode)) {
            throw new IllegalArgumentException("La etapa seleccionada no es una etapa comercial abierta");
        }
        PipelineStage target = requireStage(normalizedCode);
        validateOpenStage(target);
        if (opportunity.getStage().getCode().equalsIgnoreCase(target.getCode())) return opportunity;
        return moveAndRecord(opportunity, target, currentUser(authentication));
    }

    @Transactional
    public Opportunity markAsWon(Long id, Authentication authentication) {
        Opportunity opportunity = findForChange(id, authentication);
        PipelineStage wonStage = requireStage(WON);
        if (!wonStage.isActive() || !wonStage.isClosed() || !wonStage.isWon()) {
            throw new IllegalArgumentException("La etapa WON no está configurada correctamente");
        }
        opportunity.setStatus(OpportunityStatus.WON);
        opportunity.setProbability((short) 100);
        opportunity.setClosedAt(OffsetDateTime.now());
        opportunity.setLostReason(null);
        return moveAndRecord(opportunity, wonStage, currentUser(authentication));
    }

    @Transactional
    public Opportunity markAsLost(Long id, String lostReason, Authentication authentication) {
        Opportunity opportunity = findForChange(id, authentication);
        String reason = trimToNull(lostReason);
        if (reason == null) throw new IllegalArgumentException("El motivo de pérdida es obligatorio");
        if (reason.length() > 1000) throw new IllegalArgumentException("El motivo no puede superar 1000 caracteres");
        PipelineStage lostStage = requireStage(LOST);
        if (!lostStage.isActive() || !lostStage.isClosed() || lostStage.isWon()) {
            throw new IllegalArgumentException("La etapa LOST no está configurada correctamente");
        }
        opportunity.setStatus(OpportunityStatus.LOST);
        opportunity.setProbability((short) 0);
        opportunity.setClosedAt(OffsetDateTime.now());
        opportunity.setLostReason(reason);
        return moveAndRecord(opportunity, lostStage, currentUser(authentication));
    }

    private Opportunity moveAndRecord(Opportunity opportunity, PipelineStage target, User actor) {
        PipelineStage previous = opportunity.getStage();
        opportunity.setStage(target);
        Opportunity saved = opportunities.save(opportunity);
        history.save(new OpportunityStageHistory(saved, previous, target, actor));
        return saved;
    }

    private Opportunity findExisting(Long id) {
        return opportunities.findWithRelationsById(id)
            .orElseThrow(() -> new IllegalArgumentException("Oportunidad no encontrada"));
    }

    private Opportunity findForChange(Long id, Authentication authentication) {
        Opportunity opportunity = findExisting(id);
        assertCanAccess(opportunity, authentication);
        if (!opportunity.isOpen()) throw new IllegalArgumentException("La oportunidad ya está cerrada y no puede modificarse");
        return opportunity;
    }

    private PipelineStage requireStage(String code) {
        return stages.findByCode(code).orElseThrow(() -> new IllegalArgumentException("Etapa de Pipeline no encontrada: " + code));
    }

    private void validateOpenStage(PipelineStage stage) {
        if (!stage.isActive()) throw new IllegalArgumentException("La etapa seleccionada está inactiva");
        if (stage.isClosed()) throw new IllegalArgumentException("No se puede usar una etapa cerrada mediante cambio de etapa");
    }

    private User resolveEnabledOwner(Long id) {
        if (id == null) throw new IllegalArgumentException("Selecciona un vendedor responsable");
        User owner = users.findById(id).orElseThrow(() -> new IllegalArgumentException("Vendedor no encontrado"));
        if (!owner.isEnabled()) throw new IllegalArgumentException("El vendedor seleccionado está deshabilitado");
        return owner;
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName())
            .orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    private void assertCanAccess(Opportunity opportunity, Authentication authentication) {
        if (isSales(authentication) && !isAssignedTo(opportunity.getOwner(), authentication)) {
            throw new AccessDeniedException("No tienes permiso para acceder a esta Oportunidad");
        }
    }

    private boolean isAssignedTo(User user, Authentication authentication) {
        return user != null && user.getEmail().equalsIgnoreCase(authentication.getName());
    }

    private boolean isSales(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SALES"));
    }

    private void copyEditable(String name, String description, java.math.BigDecimal amount, Short probability,
                              LocalDate expectedCloseDate, Opportunity opportunity) {
        if (name == null || name.isBlank()) throw new IllegalArgumentException("El nombre es obligatorio");
        if (name.trim().length() > 180) throw new IllegalArgumentException("El nombre no puede superar 180 caracteres");
        if (amount == null || amount.signum() <= 0 || amount.scale() > 2 || amount.precision() - amount.scale() > 12) throw new IllegalArgumentException("El monto debe ser mayor que cero, admitir máximo dos decimales y no superar 12 dígitos enteros");
        if (probability == null || probability < 0 || probability > 100) throw new IllegalArgumentException("La probabilidad debe estar entre 0 y 100");
        opportunity.setName(name.trim());
        opportunity.setDescription(trimToNull(description));
        opportunity.setAmount(amount);
        opportunity.setProbability(probability);
        opportunity.setExpectedCloseDate(expectedCloseDate);
    }

    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String normalizeCode(String value) { return value == null ? "" : value.trim().toUpperCase(java.util.Locale.ROOT); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
