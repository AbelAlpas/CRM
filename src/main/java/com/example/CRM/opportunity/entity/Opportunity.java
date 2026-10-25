package com.example.CRM.opportunity.entity;

import com.example.CRM.client.entity.Client;
import com.example.CRM.user.entity.User;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.OffsetDateTime;

@Entity
@Table(name = "opportunities")
public class Opportunity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Column(nullable = false, length = 180) private String name;
    @Column(columnDefinition = "text") private String description;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "client_id", nullable = false) private Client client;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "owner_id") private User owner;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id") private User createdBy;
    @ManyToOne(fetch = FetchType.LAZY, optional = false) @JoinColumn(name = "stage_id", nullable = false) private PipelineStage stage;
    @Column(nullable = false, precision = 14, scale = 2) private BigDecimal amount;
    @Column(nullable = false) private Short probability;
    @Column(name = "expected_close_date") private LocalDate expectedCloseDate;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 20) private OpportunityStatus status = OpportunityStatus.OPEN;
    @Column(name = "lost_reason", columnDefinition = "text") private String lostReason;
    @Column(name = "closed_at") private OffsetDateTime closedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public Opportunity() { }

    @PrePersist void onCreate() { var now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public Client getClient() { return client; }
    public void setClient(Client value) { client = value; }
    public User getOwner() { return owner; }
    public void setOwner(User value) { owner = value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { createdBy = value; }
    public PipelineStage getStage() { return stage; }
    public void setStage(PipelineStage value) { stage = value; }
    public BigDecimal getAmount() { return amount; }
    public void setAmount(BigDecimal value) { amount = value; }
    public Short getProbability() { return probability; }
    public void setProbability(Short value) { probability = value; }
    public LocalDate getExpectedCloseDate() { return expectedCloseDate; }
    public void setExpectedCloseDate(LocalDate value) { expectedCloseDate = value; }
    public OpportunityStatus getStatus() { return status; }
    public void setStatus(OpportunityStatus value) { status = value; }
    public String getLostReason() { return lostReason; }
    public void setLostReason(String value) { lostReason = value; }
    public OffsetDateTime getClosedAt() { return closedAt; }
    public void setClosedAt(OffsetDateTime value) { closedAt = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
    public BigDecimal getWeightedValue() {
        if (amount == null || probability == null) return BigDecimal.ZERO;
        return amount.multiply(BigDecimal.valueOf(probability)).divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
    }
    public boolean isOpen() { return status == OpportunityStatus.OPEN; }
}
