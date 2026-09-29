package com.example.CRM.lead.repository;

import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadSource;
import com.example.CRM.lead.entity.LeadStatus;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface LeadRepository extends JpaRepository<Lead, Long> {
    @EntityGraph(attributePaths = {"assignedTo", "createdBy"})
    @Query("""
        select l from Lead l
        where (:term is null or lower(l.firstName) like lower(concat('%', :term, '%'))
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
}
