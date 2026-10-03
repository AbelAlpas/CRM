package com.example.CRM.lead.entity;

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
import java.time.OffsetDateTime;

@Entity
@Table(name = "leads")
public class Lead {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "first_name", nullable = false, length = 100) private String firstName;
    @Column(name = "last_name", length = 100) private String lastName;
    @Column(name = "company_name", length = 150) private String companyName;
    @Column(length = 150) private String email;
    @Column(length = 30) private String phone;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private LeadSource source = LeadSource.OTHER;
    @Enumerated(EnumType.STRING) @Column(nullable = false, length = 30) private LeadStatus status = LeadStatus.NEW;
    @Column(columnDefinition = "text") private String notes;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_id") private User assignedTo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id") private User createdBy;
    @Column(name = "converted_at") private OffsetDateTime convertedAt;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public Lead() { }
    @PrePersist void onCreate() { var now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }

    public Long getId() { return id; }
    public String getFirstName() { return firstName; } public void setFirstName(String value) { firstName = value; }
    public String getLastName() { return lastName; } public void setLastName(String value) { lastName = value; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String value) { companyName = value; }
    public String getEmail() { return email; } public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public LeadSource getSource() { return source; } public void setSource(LeadSource value) { source = value; }
    public LeadStatus getStatus() { return status; } public void setStatus(LeadStatus value) { status = value; }
    public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }
    public User getAssignedTo() { return assignedTo; } public void setAssignedTo(User value) { assignedTo = value; }
    public User getCreatedBy() { return createdBy; } public void setCreatedBy(User value) { createdBy = value; }
    public OffsetDateTime getConvertedAt() { return convertedAt; }
    public void setConvertedAt(OffsetDateTime value) { convertedAt = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; }
    public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
