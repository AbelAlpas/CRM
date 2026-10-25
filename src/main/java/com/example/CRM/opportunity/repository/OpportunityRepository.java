package com.example.CRM.opportunity.repository;

import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.OpportunityStatus;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface OpportunityRepository extends JpaRepository<Opportunity, Long> {
    @EntityGraph(attributePaths = {"client", "owner", "createdBy", "stage"})
    Optional<Opportunity> findWithRelationsById(Long id);

    @EntityGraph(attributePaths = {"client", "owner", "stage"})
    List<Opportunity> findAllByClientIdOrderByCreatedAtDesc(Long clientId);

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
