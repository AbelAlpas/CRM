package com.example.CRM.lead.repository;

import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadSource;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.dashboard.model.DashboardReadModels.LeadAggregate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    @Query("""
        select coalesce(sum(case when (:allPeriod = true or l.createdAt >= :from) and l.createdAt < :to then 1 else 0 end), 0) as newInPeriod,
               coalesce(sum(case when l.status = com.example.CRM.lead.entity.LeadStatus.NEW then 1 else 0 end), 0) as newCount,
               coalesce(sum(case when l.status = com.example.CRM.lead.entity.LeadStatus.CONTACTED then 1 else 0 end), 0) as contactedCount,
               coalesce(sum(case when l.status = com.example.CRM.lead.entity.LeadStatus.QUALIFIED then 1 else 0 end), 0) as qualifiedCount,
               coalesce(sum(case when l.status = com.example.CRM.lead.entity.LeadStatus.DISQUALIFIED then 1 else 0 end), 0) as disqualifiedCount,
               coalesce(sum(case when l.status = com.example.CRM.lead.entity.LeadStatus.CONVERTED then 1 else 0 end), 0) as convertedCount
        from Lead l
        where (:assignedToId is null or l.assignedTo.id = :assignedToId)
        """)
    LeadAggregate dashboardMetrics(@Param("from") OffsetDateTime from,
                                   @Param("to") OffsetDateTime to,
                                   @Param("allPeriod") boolean allPeriod,
                                   @Param("assignedToId") Long assignedToId);

    @EntityGraph(attributePaths = {"assignedTo"})
    @Query("""
        select l from Lead l
        where (:assignedToId is null or l.assignedTo.id = :assignedToId)
        order by l.createdAt desc
        """)
    List<Lead> findRecentVisible(@Param("assignedToId") Long assignedToId, Pageable pageable);

    @EntityGraph(attributePaths = {"assignedTo", "createdBy"})
    @Query("""
        select l from Lead l
        where (coalesce(:term, '') = '' or lower(l.firstName) like lower(concat('%', :term, '%'))
            or lower(l.lastName) like lower(concat('%', :term, '%'))
            or lower(l.companyName) like lower(concat('%', :term, '%'))
            or lower(l.email) like lower(concat('%', :term, '%'))
            or lower(l.phone) like lower(concat('%', :term, '%')))
          and (:status is null or l.status = :status)
          and (:source is null or l.source = :source)
          and (:assignedToId is null or l.assignedTo.id = :assignedToId)
        order by l.createdAt desc
        """)
    Page<Lead> search(@Param("term") String term, @Param("status") LeadStatus status,
                      @Param("source") LeadSource source, @Param("assignedToId") Long assignedToId,
                      Pageable pageable);

    @EntityGraph(attributePaths = {"assignedTo", "createdBy"})
    Optional<Lead> findWithUsersById(Long id);

    @EntityGraph(attributePaths = {"assignedTo", "createdBy"})
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<Lead> findWithUsersForUpdateById(Long id);
}
