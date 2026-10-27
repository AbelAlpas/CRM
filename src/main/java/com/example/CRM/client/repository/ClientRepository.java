package com.example.CRM.client.repository;

import com.example.CRM.client.entity.Client;
import com.example.CRM.client.entity.ClientType;
import com.example.CRM.dashboard.model.DashboardReadModels.ClientAggregate;
import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ClientRepository extends JpaRepository<Client, Long> {
    @Query("""
        select coalesce(sum(case when c.active = true then 1 else 0 end), 0) as activeCount,
               coalesce(sum(case when (:allPeriod = true or c.createdAt >= :from) and c.createdAt < :to then 1 else 0 end), 0) as newInPeriod
        from Client c
        where (:assignedToId is null or c.assignedTo.id = :assignedToId)
        """)
    ClientAggregate dashboardMetrics(@Param("from") OffsetDateTime from,
                                     @Param("to") OffsetDateTime to,
                                     @Param("allPeriod") boolean allPeriod,
                                     @Param("assignedToId") Long assignedToId);

    boolean existsByDocumentNumberIgnoreCase(String documentNumber);
    boolean existsByDocumentNumberIgnoreCaseAndIdNot(String documentNumber, Long id);
    boolean existsBySourceLeadId(Long sourceLeadId);
    @EntityGraph(attributePaths = {"assignedTo", "createdBy", "sourceLead"})
    Optional<Client> findWithRelationsById(Long id);
    @EntityGraph(attributePaths = {"assignedTo", "createdBy", "sourceLead"})
    Optional<Client> findBySourceLeadId(Long sourceLeadId);
    @EntityGraph(attributePaths = {"assignedTo"})
    List<Client> findAllByActiveTrueOrderByCompanyNameAscFirstNameAsc();
    @EntityGraph(attributePaths = {"assignedTo"})
    List<Client> findAllByActiveTrueAndAssignedToIdOrderByCompanyNameAscFirstNameAsc(Long assignedToId);
    @EntityGraph(attributePaths = {"assignedTo", "createdBy", "sourceLead"})
    @Query("""
        select c from Client c
        where (coalesce(:term, '') = '' or lower(c.firstName) like lower(concat('%', :term, '%'))
            or lower(c.lastName) like lower(concat('%', :term, '%'))
            or lower(c.companyName) like lower(concat('%', :term, '%'))
            or lower(c.documentNumber) like lower(concat('%', :term, '%'))
            or lower(c.email) like lower(concat('%', :term, '%'))
            or lower(c.phone) like lower(concat('%', :term, '%')))
          and (:type is null or c.clientType = :type)
          and (:active is null or c.active = :active)
          and (:assignedToId is null or c.assignedTo.id = :assignedToId)
        order by c.createdAt desc
        """)
    Page<Client> search(@Param("term") String term, @Param("type") ClientType type,
                        @Param("active") Boolean active, @Param("assignedToId") Long assignedToId,
                        Pageable pageable);
}
