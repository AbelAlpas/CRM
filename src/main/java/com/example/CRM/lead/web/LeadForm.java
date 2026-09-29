package com.example.CRM.lead.web;

import com.example.CRM.lead.entity.LeadSource;
import com.example.CRM.lead.entity.LeadStatus;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class LeadForm {
    @NotBlank @Size(max = 100) private String firstName;
    @Size(max = 100) private String lastName;
    @Size(max = 150) private String companyName;
    @Email @Size(max = 150) private String email;
    @Size(max = 30) private String phone;
    @NotNull private LeadSource source = LeadSource.OTHER;
    private LeadStatus status = LeadStatus.NEW;
    private String notes;
    private Long assignedToId;
    public String getFirstName() { return firstName; } public void setFirstName(String value) { firstName = value; }
    public String getLastName() { return lastName; } public void setLastName(String value) { lastName = value; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String value) { companyName = value; }
    public String getEmail() { return email; } public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public LeadSource getSource() { return source; } public void setSource(LeadSource value) { source = value; }
    public LeadStatus getStatus() { return status; } public void setStatus(LeadStatus value) { status = value; }
    public String getNotes() { return notes; } public void setNotes(String value) { notes = value; }
    public Long getAssignedToId() { return assignedToId; } public void setAssignedToId(Long value) { assignedToId = value; }
}
