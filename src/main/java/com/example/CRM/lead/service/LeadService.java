package com.example.CRM.lead.service;

import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadSource;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.lead.web.LeadForm;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeadService {
    private static final Set<LeadStatus> MANUAL_STATUSES = Set.of(LeadStatus.NEW, LeadStatus.CONTACTED, LeadStatus.QUALIFIED, LeadStatus.DISQUALIFIED);
    private final LeadRepository leads;
    private final UserRepository users;

    public LeadService(LeadRepository leads, UserRepository users) { this.leads = leads; this.users = users; }

    @Transactional(readOnly = true)
    public Page<Lead> search(String term, LeadStatus status, LeadSource source, Long assignedToId, Pageable pageable, Authentication authentication) {
        User current = currentUser(authentication);
        Long effectiveAssignee = isSales(authentication) ? current.getId() : assignedToId;
        return leads.search(normalize(term), status, source, effectiveAssignee, pageable);
    }

    @Transactional(readOnly = true)
    public Lead findById(Long id, Authentication authentication) {
        Lead lead = leads.findWithUsersById(id).orElseThrow(() -> new IllegalArgumentException("Lead no encontrado"));
        assertCanAccess(lead, authentication);
        return lead;
    }

    @Transactional
    public Lead create(LeadForm form, Authentication authentication) {
        User current = currentUser(authentication);
        Lead lead = new Lead();
        copy(form, lead, authentication);
        lead.setCreatedBy(current);
        lead.setStatus(LeadStatus.NEW);
        return leads.save(lead);
    }

    @Transactional
    public Lead update(Long id, LeadForm form, Authentication authentication) {
        Lead lead = findById(id, authentication);
        copy(form, lead, authentication);
        if (form.getStatus() == LeadStatus.CONVERTED) throw new IllegalArgumentException("CONVERTED se reserva para la conversion a Cliente");
        lead.setStatus(form.getStatus() == null ? lead.getStatus() : form.getStatus());
        return leads.save(lead);
    }

    @Transactional
    public Lead changeStatus(Long id, LeadStatus status, Authentication authentication) {
        if (!MANUAL_STATUSES.contains(status)) throw new IllegalArgumentException("Estado no permitido en M2");
        Lead lead = findById(id, authentication);
        lead.setStatus(status);
        return leads.save(lead);
    }

    @Transactional
    public Lead assignToUser(Long id, Long assignedToId, Authentication authentication) {
        if (!isPrivileged(authentication)) throw new AccessDeniedException("No tienes permiso para asignar Leads");
        Lead lead = findById(id, authentication);
        lead.setAssignedTo(resolveUser(assignedToId));
        return leads.save(lead);
    }

    private void copy(LeadForm form, Lead lead, Authentication authentication) {
        lead.setFirstName(form.getFirstName().trim()); lead.setLastName(trimToNull(form.getLastName()));
        lead.setCompanyName(trimToNull(form.getCompanyName())); lead.setEmail(trimToNull(form.getEmail()));
        lead.setPhone(trimToNull(form.getPhone())); lead.setSource(form.getSource()); lead.setNotes(trimToNull(form.getNotes()));
        if (isSales(authentication)) {
            lead.setAssignedTo(currentUser(authentication));
        } else {
            lead.setAssignedTo(resolveUser(form.getAssignedToId()));
        }
    }

    private User resolveUser(Long id) {
        return id == null ? null : users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario asignado no encontrado"));
    }

    private User currentUser(Authentication authentication) {
        return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado"));
    }

    private void assertCanAccess(Lead lead, Authentication authentication) {
        if (isSales(authentication) && (lead.getAssignedTo() == null || !lead.getAssignedTo().getEmail().equalsIgnoreCase(authentication.getName()))) {
            throw new AccessDeniedException("No tienes permiso para acceder a este Lead");
        }
    }

    private boolean isSales(Authentication authentication) { return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SALES")); }
    private boolean isPrivileged(Authentication authentication) { return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER")); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
}
