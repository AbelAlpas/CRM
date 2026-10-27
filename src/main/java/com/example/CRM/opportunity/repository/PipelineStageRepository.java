package com.example.CRM.opportunity.repository;

import com.example.CRM.opportunity.entity.PipelineStage;
import com.example.CRM.dashboard.model.DashboardReadModels.PipelineStageAggregate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Short> {
    @Query("""
        select s.id as stageId, s.code as stageCode, s.name as stageName,
               s.displayOrder as displayOrder, count(o.id) as opportunityCount,
               coalesce(sum(o.amount), 0) as totalAmount
        from PipelineStage s
        left join Opportunity o on o.stage = s
            and o.status = com.example.CRM.opportunity.entity.OpportunityStatus.OPEN
            and (:ownerId is null or o.owner.id = :ownerId)
        where s.active = true and s.closed = false
        group by s.id, s.code, s.name, s.displayOrder
        order by s.displayOrder asc
        """)
    List<PipelineStageAggregate> findDashboardMetrics(@Param("ownerId") Long ownerId);

    Optional<PipelineStage> findByCode(String code);
    List<PipelineStage> findAllByActiveTrueOrderByDisplayOrderAsc();
    List<PipelineStage> findAllByActiveTrueAndClosedFalseOrderByDisplayOrderAsc();
}
