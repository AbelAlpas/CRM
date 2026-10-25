package com.example.CRM.opportunity.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.OffsetDateTime;

@Entity
@Table(name = "pipeline_stages")
public class PipelineStage {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Short id;
    @Column(nullable = false, unique = true, length = 40) private String code;
    @Column(nullable = false, length = 80) private String name;
    @Column(name = "display_order", nullable = false) private Short displayOrder;
    @Column(name = "is_closed", nullable = false) private boolean closed;
    @Column(name = "is_won", nullable = false) private boolean won;
    @Column(nullable = false) private boolean active;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    protected PipelineStage() { }

    public Short getId() { return id; }
    public String getCode() { return code; }
    public void setCode(String value) { code = value; }
    public String getName() { return name; }
    public void setName(String value) { name = value; }
    public Short getDisplayOrder() { return displayOrder; }
    public void setDisplayOrder(Short value) { displayOrder = value; }
    public boolean isClosed() { return closed; }
    public void setClosed(boolean value) { closed = value; }
    public boolean isWon() { return won; }
    public void setWon(boolean value) { won = value; }
    public boolean isActive() { return active; }
    public void setActive(boolean value) { active = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
