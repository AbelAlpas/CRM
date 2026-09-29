package com.example.CRM.user.web;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.HashSet;
import java.util.Set;

public class UserForm {
    @NotBlank @Size(max = 80) private String firstName;
    @NotBlank @Size(max = 80) private String lastName;
    @NotBlank @Email @Size(max = 150) private String email;
    @Size(min = 8, max = 72) private String password;
    private boolean enabled = true;
    private Set<String> roles = new HashSet<>();
    public String getFirstName() { return firstName; } public void setFirstName(String v) { firstName = v; }
    public String getLastName() { return lastName; } public void setLastName(String v) { lastName = v; }
    public String getEmail() { return email; } public void setEmail(String v) { email = v; }
    public String getPassword() { return password; } public void setPassword(String v) { password = v; }
    public boolean isEnabled() { return enabled; } public void setEnabled(boolean v) { enabled = v; }
    public Set<String> getRoles() { return roles; } public void setRoles(Set<String> v) { roles = v; }
}
