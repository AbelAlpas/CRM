package com.example.CRM.client.web;

import com.example.CRM.client.entity.ClientType;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class ClientForm {
    @NotNull private ClientType clientType = ClientType.PERSON;
    @Size(max = 100) private String firstName;
    @Size(max = 100) private String lastName;
    @Size(max = 150) private String companyName;
    @Size(max = 30) private String documentNumber;
    @Email @Size(max = 150) private String email;
    @Size(max = 30) private String phone;
    @Size(max = 255) private String address;
    private Long assignedToId;
    private boolean active = true;
    public ClientType getClientType() { return clientType; } public void setClientType(ClientType value) { clientType = value; }
    public String getFirstName() { return firstName; } public void setFirstName(String value) { firstName = value; }
    public String getLastName() { return lastName; } public void setLastName(String value) { lastName = value; }
    public String getCompanyName() { return companyName; } public void setCompanyName(String value) { companyName = value; }
    public String getDocumentNumber() { return documentNumber; } public void setDocumentNumber(String value) { documentNumber = value; }
    public String getEmail() { return email; } public void setEmail(String value) { email = value; }
    public String getPhone() { return phone; } public void setPhone(String value) { phone = value; }
    public String getAddress() { return address; } public void setAddress(String value) { address = value; }
    public Long getAssignedToId() { return assignedToId; } public void setAssignedToId(Long value) { assignedToId = value; }
    public boolean isActive() { return active; } public void setActive(boolean value) { active = value; }
}
