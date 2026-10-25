package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CRM.client.entity.Client;
import com.example.CRM.client.entity.ClientType;
import com.example.CRM.client.repository.ClientRepository;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.OpportunityStageHistory;
import com.example.CRM.opportunity.entity.OpportunityStatus;
import com.example.CRM.opportunity.entity.PipelineStage;
import com.example.CRM.opportunity.repository.OpportunityRepository;
import com.example.CRM.opportunity.repository.OpportunityStageHistoryRepository;
import com.example.CRM.opportunity.repository.PipelineStageRepository;
import com.example.CRM.opportunity.service.OpportunityService;
import com.example.CRM.opportunity.web.OpportunityCreateRequest;
import com.example.CRM.opportunity.web.OpportunityUpdateRequest;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.math.BigDecimal;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class OpportunityServiceTests {
    @Mock OpportunityRepository opportunities;
    @Mock PipelineStageRepository stages;
    @Mock OpportunityStageHistoryRepository history;
    @Mock ClientRepository clients;
    @Mock UserRepository users;

    private OpportunityService service;
    private User admin;
    private User seller;
    private Authentication adminAuth;
    private Authentication salesAuth;

    @BeforeEach
    void setUp() {
        service = new OpportunityService(opportunities, stages, history, clients, users);
        admin = user("Admin", "CRM", "admin@example.com");
        seller = user("Ana", "Ventas", "seller@example.com");
        adminAuth = new TestingAuthenticationToken(admin.getEmail(), "", "ROLE_ADMIN");
        salesAuth = new TestingAuthenticationToken(seller.getEmail(), "", "ROLE_SALES");
        lenient().when(users.findByEmailIgnoreCase(admin.getEmail())).thenReturn(Optional.of(admin));
        lenient().when(users.findByEmailIgnoreCase(seller.getEmail())).thenReturn(Optional.of(seller));
        lenient().when(opportunities.save(any(Opportunity.class))).thenAnswer(invocation -> invocation.getArgument(0));
        lenient().when(history.save(any(OpportunityStageHistory.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test
    void adminCreatesOpenOpportunityWithInitialHistory() {
        Client client = activeClient(seller);
        PipelineStage prospecting = stage("PROSPECTING", true, false, false);
        when(clients.findWithRelationsById(10L)).thenReturn(Optional.of(client));
        when(users.findById(20L)).thenReturn(Optional.of(seller));
        when(stages.findByCode("PROSPECTING")).thenReturn(Optional.of(prospecting));

        Opportunity created = service.create(createRequest(10L, 20L), adminAuth);

        assertThat(created.getClient()).isSameAs(client);
        assertThat(created.getOwner()).isSameAs(seller);
        assertThat(created.getCreatedBy()).isSameAs(admin);
        assertThat(created.getStatus()).isEqualTo(OpportunityStatus.OPEN);
        assertThat(created.getStage()).isSameAs(prospecting);
        assertThat(created.getAmount()).isEqualByComparingTo("12500.50");
        assertThat(created.getClosedAt()).isNull();
        ArgumentCaptor<OpportunityStageHistory> event = ArgumentCaptor.forClass(OpportunityStageHistory.class);
        verify(history).save(event.capture());
        assertThat(event.getValue().getFromStage()).isNull();
        assertThat(event.getValue().getToStage()).isSameAs(prospecting);
        assertThat(event.getValue().getChangedBy()).isSameAs(admin);
    }

    @Test
    void salesIsAutomaticallyOwnerForOwnClient() {
        Client client = activeClient(seller);
        PipelineStage prospecting = stage("PROSPECTING", true, false, false);
        when(clients.findWithRelationsById(10L)).thenReturn(Optional.of(client));
        when(stages.findByCode("PROSPECTING")).thenReturn(Optional.of(prospecting));
        OpportunityCreateRequest request = createRequest(10L, 999L);

        Opportunity created = service.create(request, salesAuth);

        assertThat(created.getOwner()).isSameAs(seller);
        verify(users, never()).findById(999L);
    }

    @Test
    void salesCannotCreateForAnotherSellersClient() {
        Client client = activeClient(user("Otro", "Vendedor", "other@example.com"));
        when(clients.findWithRelationsById(10L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.create(createRequest(10L, null), salesAuth))
            .isInstanceOf(AccessDeniedException.class);
        verify(opportunities, never()).save(any());
    }

    @Test
    void inactiveClientIsRejected() {
        Client client = activeClient(seller);
        client.setActive(false);
        when(clients.findWithRelationsById(10L)).thenReturn(Optional.of(client));

        assertThatThrownBy(() -> service.create(createRequest(10L, 20L), adminAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("inactivo");
    }

    @Test
    void changeStageRecordsRealTransition() {
        PipelineStage prospecting = stage("PROSPECTING", true, false, false);
        PipelineStage qualification = stage("QUALIFICATION", true, false, false);
        Opportunity opportunity = openOpportunity(seller, prospecting);
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));
        when(stages.findByCode("QUALIFICATION")).thenReturn(Optional.of(qualification));

        service.changeStage(5L, "qualification", salesAuth);

        assertThat(opportunity.getStage()).isSameAs(qualification);
        ArgumentCaptor<OpportunityStageHistory> event = ArgumentCaptor.forClass(OpportunityStageHistory.class);
        verify(history).save(event.capture());
        assertThat(event.getValue().getFromStage()).isSameAs(prospecting);
        assertThat(event.getValue().getToStage()).isSameAs(qualification);
        assertThat(event.getValue().getChangedBy()).isSameAs(seller);
    }

    @Test
    void genericStageChangeRejectsWonAndLost() {
        Opportunity opportunity = openOpportunity(seller, stage("NEGOTIATION", true, false, false));
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> service.changeStage(5L, "WON", salesAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("abierta");
        assertThatThrownBy(() -> service.changeStage(5L, "LOST", salesAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("abierta");
    }

    @Test
    void marksOpenOpportunityAsWonConsistently() {
        PipelineStage previous = stage("NEGOTIATION", true, false, false);
        PipelineStage won = stage("WON", true, true, true);
        Opportunity opportunity = openOpportunity(seller, previous);
        opportunity.setLostReason("dato anterior");
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));
        when(stages.findByCode("WON")).thenReturn(Optional.of(won));

        service.markAsWon(5L, salesAuth);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.WON);
        assertThat(opportunity.getStage()).isSameAs(won);
        assertThat(opportunity.getProbability()).isEqualTo((short) 100);
        assertThat(opportunity.getClosedAt()).isNotNull();
        assertThat(opportunity.getLostReason()).isNull();
        verify(history).save(any(OpportunityStageHistory.class));
    }

    @Test
    void marksOpenOpportunityAsLostWithReason() {
        PipelineStage lost = stage("LOST", true, true, false);
        Opportunity opportunity = openOpportunity(seller, stage("PROPOSAL", true, false, false));
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));
        when(stages.findByCode("LOST")).thenReturn(Optional.of(lost));

        service.markAsLost(5L, "  Sin presupuesto aprobado  ", salesAuth);

        assertThat(opportunity.getStatus()).isEqualTo(OpportunityStatus.LOST);
        assertThat(opportunity.getStage()).isSameAs(lost);
        assertThat(opportunity.getProbability()).isZero();
        assertThat(opportunity.getClosedAt()).isNotNull();
        assertThat(opportunity.getLostReason()).isEqualTo("Sin presupuesto aprobado");
    }

    @Test
    void losingWithoutReasonIsRejected() {
        Opportunity opportunity = openOpportunity(seller, stage("PROPOSAL", true, false, false));
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> service.markAsLost(5L, "   ", salesAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("obligatorio");
    }

    @Test
    void closedOpportunityCannotBeEdited() {
        Opportunity opportunity = openOpportunity(seller, stage("WON", true, true, true));
        opportunity.setStatus(OpportunityStatus.WON);
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> service.update(5L, updateRequest(), salesAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("cerrada");
    }

    @Test
    void salesCannotAccessAnotherOwnersOpportunity() {
        Opportunity opportunity = openOpportunity(user("Otro", "Vendedor", "other@example.com"), stage("PROSPECTING", true, false, false));
        when(opportunities.findWithRelationsById(5L)).thenReturn(Optional.of(opportunity));

        assertThatThrownBy(() -> service.findById(5L, salesAuth))
            .isInstanceOf(AccessDeniedException.class);
    }

    private OpportunityCreateRequest createRequest(Long clientId, Long ownerId) {
        OpportunityCreateRequest request = new OpportunityCreateRequest();
        request.setClientId(clientId);
        request.setOwnerId(ownerId);
        request.setName("Abastecimiento de obra");
        request.setAmount(new BigDecimal("12500.50"));
        request.setProbability((short) 40);
        return request;
    }

    private OpportunityUpdateRequest updateRequest() {
        OpportunityUpdateRequest request = new OpportunityUpdateRequest();
        request.setName("Actualizada");
        request.setAmount(new BigDecimal("5000.00"));
        request.setProbability((short) 50);
        return request;
    }

    private Opportunity openOpportunity(User owner, PipelineStage stage) {
        Opportunity opportunity = new Opportunity();
        opportunity.setName("Venta mayorista");
        opportunity.setClient(activeClient(owner));
        opportunity.setOwner(owner);
        opportunity.setStage(stage);
        opportunity.setStatus(OpportunityStatus.OPEN);
        opportunity.setAmount(new BigDecimal("8000.00"));
        opportunity.setProbability((short) 60);
        return opportunity;
    }

    private Client activeClient(User assignedTo) {
        Client client = new Client();
        client.setClientType(ClientType.COMPANY);
        client.setCompanyName("Ferretería Demo");
        client.setAssignedTo(assignedTo);
        client.setActive(true);
        return client;
    }

    private PipelineStage stage(String code, boolean active, boolean closed, boolean won) {
        PipelineStage stage = mock(PipelineStage.class);
        lenient().when(stage.getCode()).thenReturn(code);
        lenient().when(stage.getName()).thenReturn(code);
        lenient().when(stage.isActive()).thenReturn(active);
        lenient().when(stage.isClosed()).thenReturn(closed);
        lenient().when(stage.isWon()).thenReturn(won);
        return stage;
    }

    private User user(String firstName, String lastName, String email) {
        return new User(firstName, lastName, email, "encoded");
    }
}
