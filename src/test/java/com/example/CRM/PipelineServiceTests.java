package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.CRM.client.entity.Client;
import com.example.CRM.client.entity.ClientType;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.PipelineStage;
import com.example.CRM.opportunity.service.OpportunityService;
import com.example.CRM.pipeline.service.PipelineService;
import com.example.CRM.pipeline.web.PipelineView;
import com.example.CRM.user.entity.User;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class PipelineServiceTests {
    @Mock OpportunityService opportunities;

    private PipelineService service;
    private Authentication authentication;

    @BeforeEach
    void setUp() {
        service = new PipelineService(opportunities);
        authentication = new TestingAuthenticationToken("admin@example.com", "", "ROLE_ADMIN");
    }

    @Test
    void groupsOpportunitiesAndCalculatesPipelineMetrics() {
        PipelineStage prospecting = stage((short) 1, "PROSPECTING", "Prospeccion", (short) 1);
        PipelineStage proposal = stage((short) 3, "PROPOSAL", "Propuesta", (short) 3);
        when(opportunities.openStages()).thenReturn(List.of(prospecting, proposal));
        when(opportunities.findOpenForPipeline("fierro", null, null, authentication)).thenReturn(List.of(
            opportunity("Venta de fierro", prospecting, new BigDecimal("1000.00"), (short) 50, LocalDate.now().minusDays(1)),
            opportunity("Pedido de cemento", proposal, new BigDecimal("2000.00"), (short) 40, LocalDate.now().plusDays(5))
        ));

        PipelineView result = service.getPipeline("fierro", null, null, authentication);

        assertThat(result.stages()).extracting(stage -> stage.code())
            .containsExactly("PROSPECTING", "PROPOSAL");
        assertThat(result.stages()).extracting(stage -> stage.count()).containsExactly(1, 1);
        assertThat(result.totalCount()).isEqualTo(2);
        assertThat(result.totalAmount()).isEqualByComparingTo("3000.00");
        assertThat(result.weightedAmount()).isEqualByComparingTo("1300.00");
        assertThat(result.stages().getFirst().opportunities().getFirst().overdue()).isTrue();
    }

    @Test
    void keepsStagesVisibleWhenTheyHaveNoOpportunities() {
        PipelineStage qualification = stage((short) 2, "QUALIFICATION", "Calificacion", (short) 2);
        when(opportunities.openStages()).thenReturn(List.of(qualification));
        when(opportunities.findOpenForPipeline(null, null, null, authentication)).thenReturn(List.of());

        PipelineView result = service.getPipeline(null, null, null, authentication);

        assertThat(result.stages()).hasSize(1);
        assertThat(result.stages().getFirst().opportunities()).isEmpty();
        assertThat(result.stages().getFirst().totalAmount()).isEqualByComparingTo(BigDecimal.ZERO);
    }

    @Test
    void delegatesStageMovementToOpportunityModule() {
        service.moveOpportunity(25L, "NEGOTIATION", authentication);

        verify(opportunities).changeStage(25L, "NEGOTIATION", authentication);
    }

    @Test
    void delegatesVisibleClientOptionsToOpportunityModule() {
        Client client = new Client();
        when(opportunities.availableClients(authentication)).thenReturn(List.of(client));

        assertThat(service.availableClients(authentication)).containsExactly(client);
    }

    private PipelineStage stage(short id, String code, String name, short order) {
        PipelineStage stage = mock(PipelineStage.class);
        when(stage.getId()).thenReturn(id);
        when(stage.getCode()).thenReturn(code);
        when(stage.getName()).thenReturn(name);
        when(stage.getDisplayOrder()).thenReturn(order);
        return stage;
    }

    private Opportunity opportunity(String name, PipelineStage stage, BigDecimal amount, short probability,
                                    LocalDate expectedCloseDate) {
        Client client = new Client();
        client.setClientType(ClientType.COMPANY);
        client.setCompanyName("Ferreteria Central");
        User owner = new User("Ana", "Ventas", "ana@example.com", "encoded");
        Opportunity opportunity = new Opportunity();
        opportunity.setName(name);
        opportunity.setClient(client);
        opportunity.setOwner(owner);
        opportunity.setStage(stage);
        opportunity.setAmount(amount);
        opportunity.setProbability(probability);
        opportunity.setExpectedCloseDate(expectedCloseDate);
        return opportunity;
    }
}
