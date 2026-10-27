package com.example.CRM.activity.repository;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityStatus;
import com.example.CRM.activity.entity.ActivityType;
import com.example.CRM.dashboard.model.DashboardReadModels.ActivityAggregate;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ActivityRepository extends JpaRepository<Activity, Long> {
    @Query("""
        select coalesce(sum(case when a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                                                    com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS) then 1 else 0 end), 0) as pendingCount,
               coalesce(sum(case when a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                                                    com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
                                      and a.dueAt < :now then 1 else 0 end), 0) as overdueCount,
               coalesce(sum(case when a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                                                    com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
                                      and ((a.dueAt >= :dayStart and a.dueAt < :dayEnd)
                                        or (a.scheduledAt >= :dayStart and a.scheduledAt < :dayEnd)) then 1 else 0 end), 0) as dueTodayCount,
               coalesce(sum(case when a.status = com.example.CRM.activity.entity.ActivityStatus.COMPLETED
                                      and (:allPeriod = true or a.completedAt >= :periodStart)
                                      and a.completedAt < :now then 1 else 0 end), 0) as completedInPeriod
        from Activity a
        where (:ownerId is null or a.owner.id = :ownerId)
        """)
    ActivityAggregate dashboardMetrics(@Param("now") Instant now,
                                       @Param("dayStart") Instant dayStart,
                                       @Param("dayEnd") Instant dayEnd,
                                       @Param("periodStart") Instant periodStart,
                                       @Param("allPeriod") boolean allPeriod,
                                       @Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"owner", "lead", "client", "opportunity"})
    @Query("""
        select a from Activity a
        where a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                           com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
          and (:ownerId is null or a.owner.id = :ownerId)
          and ((a.dueAt >= :dayStart and a.dueAt < :dayEnd)
              or (a.scheduledAt >= :dayStart and a.scheduledAt < :dayEnd))
        order by coalesce(a.dueAt, a.scheduledAt) asc, a.createdAt asc
        """)
    List<Activity> findDueToday(@Param("dayStart") Instant dayStart,
                                @Param("dayEnd") Instant dayEnd,
                                @Param("ownerId") Long ownerId,
                                Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "createdBy", "lead", "client", "opportunity"})
    Optional<Activity> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {"owner", "createdBy", "lead", "client", "opportunity"})
    @Query("""
        select a from Activity a
        left join a.lead l
        left join a.client c
        left join a.opportunity o
        where (coalesce(:term, '') = ''
            or lower(a.subject) like lower(concat('%', :term, '%'))
            or lower(coalesce(a.description, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(l.firstName, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(l.lastName, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(l.companyName, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(c.firstName, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(c.lastName, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(c.companyName, '')) like lower(concat('%', :term, '%'))
            or lower(coalesce(o.name, '')) like lower(concat('%', :term, '%')))
          and (:type is null or a.activityType = :type)
          and (:status is null or a.status = :status)
          and (:priority is null or a.priority = :priority)
          and (:ownerId is null or a.owner.id = :ownerId)
          and (:leadId is null or a.lead.id = :leadId)
          and (:clientId is null or a.client.id = :clientId)
          and (:opportunityId is null or a.opportunity.id = :opportunityId)
          and (
            :allScope = true
            or (:todayScope = true
                and ((a.dueAt >= :dayStart and a.dueAt < :dayEnd)
                    or (a.scheduledAt >= :dayStart and a.scheduledAt < :dayEnd)))
            or (:upcomingScope = true
                and a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                                 com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
                and ((a.dueAt is not null and a.dueAt >= :now)
                    or (a.dueAt is null and a.scheduledAt >= :now)))
            or (:overdueScope = true
                and a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                                 com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
                and a.dueAt < :now)
          )
        order by case when a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                                        com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
                           and a.dueAt < :now then 0 else 1 end,
                 coalesce(a.dueAt, a.scheduledAt, a.createdAt) asc,
                 a.createdAt desc
        """)
    Page<Activity> search(@Param("term") String term,
                          @Param("type") ActivityType type,
                          @Param("status") ActivityStatus status,
                          @Param("priority") ActivityPriority priority,
                          @Param("ownerId") Long ownerId,
                          @Param("leadId") Long leadId,
                          @Param("clientId") Long clientId,
                          @Param("opportunityId") Long opportunityId,
                          @Param("allScope") boolean allScope,
                          @Param("todayScope") boolean todayScope,
                          @Param("upcomingScope") boolean upcomingScope,
                          @Param("overdueScope") boolean overdueScope,
                          @Param("now") Instant now,
                          @Param("dayStart") Instant dayStart,
                          @Param("dayEnd") Instant dayEnd,
                          Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "createdBy", "lead"})
    @Query("select a from Activity a where a.lead.id = :leadId and (:ownerId is null or a.owner.id = :ownerId) order by coalesce(a.dueAt, a.scheduledAt, a.createdAt) asc")
    List<Activity> findForLead(@Param("leadId") Long leadId, @Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "createdBy", "client"})
    @Query("select a from Activity a where a.client.id = :clientId and (:ownerId is null or a.owner.id = :ownerId) order by coalesce(a.dueAt, a.scheduledAt, a.createdAt) asc")
    List<Activity> findForClient(@Param("clientId") Long clientId, @Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "createdBy", "opportunity"})
    @Query("select a from Activity a where a.opportunity.id = :opportunityId and (:ownerId is null or a.owner.id = :ownerId) order by coalesce(a.dueAt, a.scheduledAt, a.createdAt) asc")
    List<Activity> findForOpportunity(@Param("opportunityId") Long opportunityId, @Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "lead", "client", "opportunity"})
    @Query("""
        select a from Activity a
        where a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                           com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
          and (:ownerId is null or a.owner.id = :ownerId)
          and ((a.dueAt is not null and a.dueAt >= :now)
              or (a.dueAt is null and a.scheduledAt >= :now))
        order by coalesce(a.dueAt, a.scheduledAt) asc
        """)
    List<Activity> findUpcoming(@Param("now") Instant now, @Param("ownerId") Long ownerId, Pageable pageable);

    @EntityGraph(attributePaths = {"owner", "lead", "client", "opportunity"})
    @Query("""
        select a from Activity a
        where a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                           com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
          and a.dueAt < :now
          and (:ownerId is null or a.owner.id = :ownerId)
        order by a.dueAt asc
        """)
    List<Activity> findOverdue(@Param("now") Instant now, @Param("ownerId") Long ownerId, Pageable pageable);

    @Query("select count(a) from Activity a where a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING, com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS) and (:ownerId is null or a.owner.id = :ownerId)")
    long countPending(@Param("ownerId") Long ownerId);

    @Query("""
        select count(a) from Activity a
        where a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                           com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
          and a.dueAt < :now
          and (:ownerId is null or a.owner.id = :ownerId)
        """)
    long countOverdue(@Param("now") Instant now, @Param("ownerId") Long ownerId);

    @Query("""
        select count(a) from Activity a
        where a.status in (com.example.CRM.activity.entity.ActivityStatus.PENDING,
                           com.example.CRM.activity.entity.ActivityStatus.IN_PROGRESS)
          and (:ownerId is null or a.owner.id = :ownerId)
          and ((a.dueAt >= :dayStart and a.dueAt < :dayEnd)
              or (a.scheduledAt >= :dayStart and a.scheduledAt < :dayEnd))
        """)
    long countDueToday(@Param("dayStart") Instant dayStart, @Param("dayEnd") Instant dayEnd,
                       @Param("ownerId") Long ownerId);

    @EntityGraph(attributePaths = {"owner", "lead", "client", "opportunity"})
    @Query("""
        select a from Activity a
        where a.status = com.example.CRM.activity.entity.ActivityStatus.COMPLETED
          and a.completedAt >= :from and a.completedAt < :to
          and (:ownerId is null or a.owner.id = :ownerId)
        order by a.completedAt desc
        """)
    List<Activity> findCompletedBetween(@Param("from") Instant from, @Param("to") Instant to,
                                        @Param("ownerId") Long ownerId, Pageable pageable);
}
