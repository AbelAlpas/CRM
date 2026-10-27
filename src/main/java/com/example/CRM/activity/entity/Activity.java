package com.example.CRM.activity.entity;

import com.example.CRM.client.entity.Client;
import com.example.CRM.lead.entity.Lead;
import com.example.CRM.opportunity.entity.Opportunity;
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
import java.time.Instant;

@Entity
@Table(name = "activities")
public class Activity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "activity_type", nullable = false, length = 30)
    private ActivityType activityType;

    @Column(nullable = false, length = 180)
    private String subject;

    @Column(columnDefinition = "text")
    private String description;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private ActivityStatus status = ActivityStatus.PENDING;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private ActivityPriority priority = ActivityPriority.MEDIUM;

    @Column(name = "scheduled_at")
    private Instant scheduledAt;

    @Column(name = "due_at")
    private Instant dueAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "owner_id")
    private User owner;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_id")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "lead_id")
    private Lead lead;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "client_id")
    private Client client;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "opportunity_id")
    private Opportunity opportunity;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public Activity() { }

    @PrePersist
    void onCreate() {
        Instant now = Instant.now();
        createdAt = now;
        updatedAt = now;
        if (status == null) status = ActivityStatus.PENDING;
        if (priority == null) priority = ActivityPriority.MEDIUM;
    }

    @PreUpdate
    void onUpdate() {
        updatedAt = Instant.now();
    }

    public boolean isOverdue() {
        return dueAt != null
            && dueAt.isBefore(Instant.now())
            && (status == ActivityStatus.PENDING || status == ActivityStatus.IN_PROGRESS);
    }

    public boolean isOpen() {
        return status == ActivityStatus.PENDING || status == ActivityStatus.IN_PROGRESS;
    }

    public Long getId() { return id; }
    public ActivityType getActivityType() { return activityType; }
    public void setActivityType(ActivityType value) { activityType = value; }
    public String getSubject() { return subject; }
    public void setSubject(String value) { subject = value; }
    public String getDescription() { return description; }
    public void setDescription(String value) { description = value; }
    public ActivityStatus getStatus() { return status; }
    public void setStatus(ActivityStatus value) { status = value; }
    public ActivityPriority getPriority() { return priority; }
    public void setPriority(ActivityPriority value) { priority = value; }
    public Instant getScheduledAt() { return scheduledAt; }
    public void setScheduledAt(Instant value) { scheduledAt = value; }
    public Instant getDueAt() { return dueAt; }
    public void setDueAt(Instant value) { dueAt = value; }
    public Instant getCompletedAt() { return completedAt; }
    public void setCompletedAt(Instant value) { completedAt = value; }
    public User getOwner() { return owner; }
    public void setOwner(User value) { owner = value; }
    public User getCreatedBy() { return createdBy; }
    public void setCreatedBy(User value) { createdBy = value; }
    public Lead getLead() { return lead; }
    public void setLead(Lead value) { lead = value; }
    public Client getClient() { return client; }
    public void setClient(Client value) { client = value; }
    public Opportunity getOpportunity() { return opportunity; }
    public void setOpportunity(Opportunity value) { opportunity = value; }
    public Instant getCreatedAt() { return createdAt; }
    public Instant getUpdatedAt() { return updatedAt; }
}
