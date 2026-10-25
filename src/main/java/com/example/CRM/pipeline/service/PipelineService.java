package com.example.CRM.pipeline.service;

import com.example.CRM.client.entity.Client;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.PipelineStage;
import com.example.CRM.opportunity.service.OpportunityService;
import com.example.CRM.pipeline.web.PipelineOpportunityView;
import com.example.CRM.pipeline.web.PipelineStageView;
import com.example.CRM.pipeline.web.PipelineView;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PipelineService {
    private final OpportunityService opportunities;

    public PipelineService(OpportunityService opportunities) {
        this.opportunities = opportunities;
    }

    @Transactional(readOnly = true)
    public PipelineView getPipeline(String search, Long ownerId, Long clientId, Authentication authentication) {
        List<PipelineStage> stages = opportunities.openStages();
        List<Opportunity> visible = opportunities.findOpenForPipeline(search, ownerId, clientId, authentication);
        Map<String, List<PipelineOpportunityView>> grouped = new LinkedHashMap<>();
        for (PipelineStage stage : stages) grouped.put(stage.getCode(), new ArrayList<>());

        BigDecimal totalAmount = BigDecimal.ZERO;
        BigDecimal weightedAmount = BigDecimal.ZERO;
        LocalDate today = LocalDate.now();
        for (Opportunity opportunity : visible) {
            List<PipelineOpportunityView> stageCards = grouped.get(opportunity.getStage().getCode());
            if (stageCards == null) continue;
            PipelineOpportunityView card = toView(opportunity, today);
            stageCards.add(card);
            totalAmount = totalAmount.add(opportunity.getAmount());
            weightedAmount = weightedAmount.add(card.weightedAmount());
        }

        List<PipelineStageView> stageViews = stages.stream().map(stage -> {
            List<PipelineOpportunityView> cards = List.copyOf(grouped.get(stage.getCode()));
            BigDecimal stageTotal = cards.stream()
                .map(PipelineOpportunityView::amount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
            return new PipelineStageView(stage.getId(), stage.getCode(), stage.getName(), stage.getDisplayOrder(),
                cards, cards.size(), stageTotal);
        }).toList();

        int totalCount = grouped.values().stream()
            .mapToInt(List::size)
            .sum();
        return new PipelineView(stageViews, totalCount, totalAmount, weightedAmount.setScale(2, RoundingMode.HALF_UP));
    }

    @Transactional(readOnly = true)
    public List<Client> availableClients(Authentication authentication) {
        return opportunities.availableClients(authentication);
    }

    @Transactional
    public void moveOpportunity(Long opportunityId, String stageCode, Authentication authentication) {
        opportunities.changeStage(opportunityId, stageCode, authentication);
    }

    private PipelineOpportunityView toView(Opportunity opportunity, LocalDate today) {
        Client client = opportunity.getClient();
        String clientName = client.getClientType().name().equals("COMPANY")
            ? client.getCompanyName()
            : client.getFirstName() + (client.getLastName() == null ? "" : " " + client.getLastName());
        String ownerName = opportunity.getOwner() == null
            ? "Sin asignar"
            : opportunity.getOwner().getFirstName() + " " + opportunity.getOwner().getLastName();
        boolean overdue = opportunity.getExpectedCloseDate() != null && opportunity.getExpectedCloseDate().isBefore(today);
        return new PipelineOpportunityView(opportunity.getId(), opportunity.getName(), client.getId(), clientName,
            opportunity.getAmount(), opportunity.getProbability(), opportunity.getWeightedValue(), ownerName,
            opportunity.getExpectedCloseDate(), overdue, opportunity.getStage().getCode());
    }
}
