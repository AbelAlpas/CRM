package com.example.CRM.client.entity;

import com.example.CRM.lead.entity.Lead;
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
@Table(name = "clients")
public class Client {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY) private Long id;
    @Enumerated(EnumType.STRING) @Column(name = "client_type", nullable = false, length = 20) private ClientType clientType = ClientType.PERSON;
    @Column(name = "first_name", length = 100) private String firstName;
    @Column(name = "last_name", length = 100) private String lastName;
    @Column(name = "company_name", length = 150) private String companyName;
    @Column(name = "document_number", length = 30) private String documentNumber;
    @Column(length = 150) private String email;
    @Column(length = 30) private String phone;
    @Column(length = 255) private String address;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "source_lead_id", unique = true) private Lead sourceLead;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "assigned_to_id") private User assignedTo;
    @ManyToOne(fetch = FetchType.LAZY) @JoinColumn(name = "created_by_id") private User createdBy;
    @Column(nullable = false) private boolean active = true;
    @Column(name = "created_at", nullable = false, updatable = false) private OffsetDateTime createdAt;
    @Column(name = "updated_at", nullable = false) private OffsetDateTime updatedAt;

    public Client() { }
    @PrePersist void onCreate() { var now = OffsetDateTime.now(); createdAt = now; updatedAt = now; }
    @PreUpdate void onUpdate() { updatedAt = OffsetDateTime.now(); }
    public Long getId() { return id; }
    public ClientType getClientType() { return clientType; } public void setClientType(ClientType value) { clientType = value; }
    public String getFirstName() { return firstName; } public void setFirstName(String value) { firstName = value; }
    public String getLastName() { return lastName; } public void setLastName(String value) { lastName = value; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String value) { companyName = value; }
    public String getDocumentNumber() { return documentNumber; } public void setDocumentNumber(String value) { documentNumber = value; }
    public String getEmail() { return email; } public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public String getAddress() { return address; } public void setAddress(String value) { address = value; }
    public Lead getSourceLead() { return sourceLead; } public void setSourceLead(Lead value) { sourceLead = value; }
    public User getAssignedTo() { return assignedTo; } public void setAssignedTo(User value) { assignedTo = value; }
    public User getCreatedBy() { return createdBy; } public void setCreatedBy(User value) { createdBy = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
    public OffsetDateTime getCreatedAt() { return createdAt; } public OffsetDateTime getUpdatedAt() { return updatedAt; }
}
