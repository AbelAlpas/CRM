package com.example.CRM.opportunity.repository;

import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.OpportunityStatus;
import com.example.CRM.dashboard.model.DashboardReadModels.MonthlySalesAggregate;
import com.example.CRM.dashboard.model.DashboardReadModels.OpportunityAggregate;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    @Query("""
        select coalesce(sum(case when o.status = com.example.CRM.opportunity.entity.OpportunityStatus.OPEN then 1 else 0 end), 0) as openCount,
               coalesce(sum(case when o.status = com.example.CRM.opportunity.entity.OpportunityStatus.OPEN then o.amount else 0 end), 0) as openAmount,
               coalesce(sum(case when o.status = com.example.CRM.opportunity.entity.OpportunityStatus.OPEN then (o.amount * o.probability / 100) else 0 end), 0) as weightedAmount,
               coalesce(sum(case when o.status = com.example.CRM.opportunity.entity.OpportunityStatus.WON and (:allPeriod = true or o.closedAt >= :from) and o.closedAt < :to then 1 else 0 end), 0) as wonCount,
               coalesce(sum(case when o.status = com.example.CRM.opportunity.entity.OpportunityStatus.LOST and (:allPeriod = true or o.closedAt >= :from) and o.closedAt < :to then 1 else 0 end), 0) as lostCount,
               coalesce(sum(case when o.status = com.example.CRM.opportunity.entity.OpportunityStatus.WON and (:allPeriod = true or o.closedAt >= :from) and o.closedAt < :to then o.amount else 0 end), 0) as wonAmount
        from Opportunity o
        where (:ownerId is null or o.owner.id = :ownerId)
        """)
    OpportunityAggregate dashboardMetrics(@Param("from") OffsetDateTime from,
                                          @Param("to") OffsetDateTime to,
                                          @Param("allPeriod") boolean allPeriod,
                                          @Param("ownerId") Long ownerId);

    @Query("""
        select month(o.closedAt) as monthNumber, coalesce(sum(o.amount), 0) as totalAmount
        from Opportunity o
        where o.status = com.example.CRM.opportunity.entity.OpportunityStatus.WON
          and o.closedAt >= :yearStart and o.closedAt < :yearEnd
          and (:ownerId is null or o.owner.id = :ownerId)
        group by month(o.closedAt)
        order by month(o.closedAt)
        """)
    List<MonthlySalesAggregate> findMonthlyWonSales(@Param("yearStart") OffsetDateTime yearStart,
                                                    @Param("yearEnd") OffsetDateTime yearEnd,
                                                    @Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"client", "owner", "stage"})
    @Query("""
        select o from Opportunity o
        where o.status = com.example.CRM.opportunity.entity.OpportunityStatus.OPEN
          and o.expectedCloseDate is not null
          and (:ownerId is null or o.owner.id = :ownerId)
        order by o.expectedCloseDate asc, o.createdAt asc
        """)
    List<Opportunity> findUpcomingToClose(@Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"client", "owner", "createdBy", "stage"})
    Optional<Opportunity> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {"client", "owner", "stage"})
    List<Opportunity> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

    @EntityGraph(attributePaths = {"client", "owner", "stage"})
    @Query("""
        select o from Opportunity o
        where o.status = com.example.CRM.opportunity.entity.OpportunityStatus.OPEN
          and o.stage.active = true
          and o.stage.closed = false
          and (coalesce(:term, '') = '' or lower(o.name) like lower(concat('%', :term, '%'))
            or lower(o.client.firstName) like lower(concat('%', :term, '%'))
            or lower(o.client.lastName) like lower(concat('%', :term, '%'))
            or lower(o.client.companyName) like lower(concat('%', :term, '%'))
            or lower(o.client.documentNumber) like lower(concat('%', :term, '%')))
          and (:ownerId is null or o.owner.id = :ownerId)
          and (:clientId is null or o.client.id = :clientId)
        order by case when o.expectedCloseDate is null then 1 else 0 end,
                 o.expectedCloseDate asc,
                 o.createdAt asc
        """)
    List<Opportunity> findOpenForPipeline(@Param("term") String term,
                                          @Param("ownerId") Long ownerId,
                                          @Param("clientId") Long clientId);

    @EntityGraph(attributePaths = {"client", "owner", "stage"})
    @Query("""
        select o from Opportunity o
        where (coalesce(:term, '') = '' or lower(o.name) like lower(concat('%', :term, '%'))
            or lower(o.client.firstName) like lower(concat('%', :term, '%'))
            or lower(o.client.lastName) like lower(concat('%', :term, '%'))
            or lower(o.client.companyName) like lower(concat('%', :term, '%'))
            or lower(o.client.documentNumber) like lower(concat('%', :term, '%')))
          and (:status is null or o.status = :status)
          and (:stageId is null or o.stage.id = :stageId)
          and (:ownerId is null or o.owner.id = :ownerId)
          and (:clientId is null or o.client.id = :clientId)
          and (:expectedCloseDate is null or o.expectedCloseDate = :expectedCloseDate)
        order by o.createdAt desc
        """)
    Page<Opportunity> search(@Param("term") String term,
                             @Param("status") OpportunityStatus status,
                             @Param("stageId") Short stageId,
                             @Param("ownerId") Long ownerId,
                             @Param("clientId") Long clientId,
                             @Param("expectedCloseDate") LocalDate expectedCloseDate,
                             Pageable pageable);
}
