package com.example.CRM.opportunity.entity;

import com.example.CRM.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "opportunity_stage_history")
public class OpportunityStageHistory {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "opportunity_id", nullable = false) private Opportunity opportunity;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "from_stage_id") private PipelineStage fromStage;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "to_stage_id", nullable = false) private PipelineStage toStage;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "changed_by_id") private User changedBy;
    @Column(name = "changed_at", nullable = false, updatable = false) private OffsetDateTime changedAt;

    public OpportunityStageHistory() { }

    public OpportunityStageHistory(Opportunity opportunity, PipelineStage fromStage, PipelineStage toStage, User changedBy) {
        this.opportunity = opportunity;
        this.fromStage = fromStage;
        this.toStage = toStage;
        this.changedBy = changedBy;
    }

    @PrePersist void onCreate() { if (changedAt == null) changedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public Opportunity getOpportunity() { return opportunity; }
    public PipelineStage getFromStage() { return fromStage; }
    public PipelineStage getToStage() { return toStage; }
    public User getChangedBy() { return changedBy; }
    public OffsetDateTime getChangedAt() { return changedAt; }
}
