package com.example.CRM.opportunity.repository;

import com.example.CRM.opportunity.entity.PipelineStage;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PipelineStageRepository extends JpaRepository<PipelineStage, Short> {
    Optional<PipelineStage> findByCode(String code);
    List<PipelineStage> findAllByActiveTrueOrderByDisplayOrderAsc();
    List<PipelineStage> findAllByActiveTrueAndClosedFalseOrderByDisplayOrderAsc();
}
