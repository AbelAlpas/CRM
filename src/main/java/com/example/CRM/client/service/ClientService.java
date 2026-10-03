package com.example.CRM.client.service;

import com.example.CRM.client.entity.Client;
import com.example.CRM.client.entity.ClientType;
import com.example.CRM.client.repository.ClientRepository;
import com.example.CRM.client.web.ClientForm;
import com.example.CRM.client.web.LeadConversionRequest;
import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.time.OffsetDateTime;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ClientService {
    private final ClientRepository clients;
    private final LeadRepository leads;
    private final UserRepository users;

    public ClientService(ClientRepository clients, LeadRepository leads, UserRepository users) { this.clients = clients; this.leads = leads; this.users = users; }

    @Transactional(readOnly = true)
    public Page<Client> search(String term, ClientType type, Boolean active, Long assignedToId, Pageable pageable, Authentication authentication) {
        User current = currentUser(authentication);
        return clients.search(normalize(term), type, active, isSales(authentication) ? current.getId() : assignedToId, pageable);
    }

    @Transactional(readOnly = true)
    public Client findById(Long id, Authentication authentication) {
        Client client = clients.findWithRelationsById(id).orElseThrow(() -> new IllegalArgumentException("Cliente no encontrado"));
        assertCanAccess(client, authentication);
        return client;
    }

    @Transactional(readOnly = true)
    public Client findBySourceLeadId(Long leadId) { return clients.findBySourceLeadId(leadId).orElse(null); }

    @Transactional
    public Client create(ClientForm form, Authentication authentication) {
        validateFields(form.getClientType(), form.getFirstName(), form.getCompanyName());
        ensureDocumentAvailable(form.getDocumentNumber(), null);
        User current = currentUser(authentication);
        Client client = new Client();
        copy(form, client, authentication);
        client.setCreatedBy(current); client.setActive(true);
        return clients.save(client);
    }

    @Transactional
    public Client update(Long id, ClientForm form, Authentication authentication) {
        Client client = findById(id, authentication);
        validateFields(form.getClientType(), form.getFirstName(), form.getCompanyName());
        ensureDocumentAvailable(form.getDocumentNumber(), id);
        copy(form, client, authentication);
        client.setActive(form.isActive());
        return clients.save(client);
    }

    @Transactional
    public Client convertLead(Long leadId, LeadConversionRequest request, Authentication authentication) {
        Lead lead = leads.findWithUsersForUpdateById(leadId).orElseThrow(() -> new IllegalArgumentException("Lead no encontrado"));
        assertCanConvert(lead, authentication);
        if (clients.existsBySourceLeadId(leadId)) throw new IllegalArgumentException("Este Lead ya fue convertido en Cliente");
        validateFields(request.getClientType(), request.getFirstName(), request.getCompanyName());
        if (request.getDocumentNumber() != null && !request.getDocumentNumber().isBlank() && clients.existsByDocumentNumberIgnoreCase(request.getDocumentNumber().trim())) {
            throw new IllegalArgumentException("El documento ya pertenece a otro Cliente");
        }
        Client client = new Client();
        client.setClientType(request.getClientType()); client.setFirstName(value(request.getFirstName(), lead.getFirstName())); client.setLastName(value(request.getLastName(), lead.getLastName())); client.setCompanyName(value(request.getCompanyName(), lead.getCompanyName())); client.setEmail(value(request.getEmail(), lead.getEmail())); client.setPhone(value(request.getPhone(), lead.getPhone())); client.setDocumentNumber(trimToNull(request.getDocumentNumber())); client.setAddress(trimToNull(request.getAddress())); client.setSourceLead(lead); client.setAssignedTo(lead.getAssignedTo()); client.setCreatedBy(currentUser(authentication)); client.setActive(true);
        try {
            Client saved = clients.save(client);
            clients.flush();
            lead.setStatus(LeadStatus.CONVERTED); lead.setConvertedAt(OffsetDateTime.now());
            leads.save(lead);
            return saved;
        } catch (DataIntegrityViolationException exception) {
            throw new IllegalArgumentException("No se pudo convertir el Lead; puede haber sido convertido en otra solicitud", exception);
        }
    }

    @Transactional public Client activate(Long id, Authentication authentication) { return setActive(id, true, authentication); }
    @Transactional public Client deactivate(Long id, Authentication authentication) { return setActive(id, false, authentication); }

    private Client setActive(Long id, boolean active, Authentication authentication) { Client client = findById(id, authentication); if (!isPrivileged(authentication)) throw new AccessDeniedException("No tienes permiso para cambiar el estado del Cliente"); client.setActive(active); return clients.save(client); }
    private void copy(ClientForm form, Client client, Authentication authentication) { client.setClientType(form.getClientType()); client.setFirstName(trimToNull(form.getFirstName())); client.setLastName(trimToNull(form.getLastName())); client.setCompanyName(trimToNull(form.getCompanyName())); client.setDocumentNumber(trimToNull(form.getDocumentNumber())); client.setEmail(trimToNull(form.getEmail())); client.setPhone(trimToNull(form.getPhone())); client.setAddress(trimToNull(form.getAddress())); client.setAssignedTo(isSales(authentication) ? currentUser(authentication) : resolveUser(form.getAssignedToId())); }
    private void validateFields(ClientType type, String firstName, String companyName) { if (type == ClientType.PERSON && (firstName == null || firstName.isBlank())) throw new IllegalArgumentException("El nombre es obligatorio para una Persona"); if (type == ClientType.COMPANY && (companyName == null || companyName.isBlank())) throw new IllegalArgumentException("La empresa es obligatoria para una Empresa"); }
    private void assertCanConvert(Lead lead, Authentication authentication) { if (lead.getStatus() != LeadStatus.QUALIFIED) throw new IllegalArgumentException("Sólo se pueden convertir Leads QUALIFIED"); if (isSales(authentication) && (lead.getAssignedTo() == null || !lead.getAssignedTo().getEmail().equalsIgnoreCase(authentication.getName()))) throw new AccessDeniedException("No tienes permiso para convertir este Lead"); }
    private void assertCanAccess(Client client, Authentication authentication) { if (isSales(authentication) && (client.getAssignedTo() == null || !client.getAssignedTo().getEmail().equalsIgnoreCase(authentication.getName()))) throw new AccessDeniedException("No tienes permiso para acceder a este Cliente"); }
    private User resolveUser(Long id) { return id == null ? null : users.findById(id).orElseThrow(() -> new IllegalArgumentException("Usuario asignado no encontrado")); }
    private User currentUser(Authentication authentication) { return users.findByEmailIgnoreCase(authentication.getName()).orElseThrow(() -> new IllegalArgumentException("Usuario autenticado no encontrado")); }
    private boolean isSales(Authentication authentication) { return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_SALES")); }
    private boolean isPrivileged(Authentication authentication) { return authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER")); }
    private String normalize(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String trimToNull(String value) { return value == null || value.isBlank() ? null : value.trim(); }
    private String value(String preferred, String fallback) { return preferred == null || preferred.isBlank() ? fallback : preferred.trim(); }
    private void ensureDocumentAvailable(String documentNumber, Long currentId) { String document = trimToNull(documentNumber); if (document != null && (currentId == null ? clients.existsByDocumentNumberIgnoreCase(document) : clients.existsByDocumentNumberIgnoreCaseAndIdNot(document, currentId))) throw new IllegalArgumentException("El documento ya pertenece a otro Cliente"); }
}
