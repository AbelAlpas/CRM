package com.example.CRM.opportunity.repository;

import com.example.CRM.opportunity.entity.OpportunityStageHistory;
import java.util.List;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface OpportunityStageHistoryRepository extends JpaRepository<OpportunityStageHistory, Long> {
    @EntityGraph(attributePaths = {"fromStage", "toStage", "changedBy"})
    List<OpportunityStageHistory> findAllByOpportunityIdOrderByChangedAtDesc(Long opportunityId);
}
