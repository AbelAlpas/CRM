package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.example.CRM.client.entity.Client;
import com.example.CRM.client.entity.ClientType;
import com.example.CRM.client.repository.ClientRepository;
import com.example.CRM.client.service.ClientService;
import com.example.CRM.client.web.ClientForm;
import com.example.CRM.client.web.LeadConversionRequest;
import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class ClientServiceTests {
    @Mock ClientRepository clients;
    @Mock LeadRepository leads;
    @Mock UserRepository users;
    private ClientService service;
    private User admin;
    private Authentication adminAuth;

    @BeforeEach void setUp() {
        service = new ClientService(clients, leads, users);
        admin = user(1L, "admin@example.com");
        adminAuth = new TestingAuthenticationToken("admin@example.com", "", "ROLE_ADMIN");
        lenient().when(users.findByEmailIgnoreCase("admin@example.com")).thenReturn(Optional.of(admin));
        lenient().when(clients.save(any(Client.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void createsPersonAndNeverStoresPasswordLikeData() {
        ClientForm form = new ClientForm();
        form.setClientType(ClientType.PERSON);
        form.setFirstName(" Ana ");
        form.setEmail("ana@example.com");

        Client client = service.create(form, adminAuth);

        assertThat(client.getFirstName()).isEqualTo("Ana");
        assertThat(client.getCreatedBy()).isSameAs(admin);
        assertThat(client.isActive()).isTrue();
    }

    @Test void rejectsCompanyWithoutCompanyName() {
        ClientForm form = new ClientForm();
        form.setClientType(ClientType.COMPANY);

        assertThatThrownBy(() -> service.create(form, adminAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("empresa");
        verify(clients, never()).save(any());
    }

    @Test void convertsQualifiedLeadAndInheritsAssignment() {
        User seller = user(2L, "seller@example.com");
        Lead lead = new Lead();
        lead.setStatus(LeadStatus.QUALIFIED);
        lead.setFirstName("Luis");
        lead.setEmail("luis@example.com");
        lead.setAssignedTo(seller);
        when(leads.findWithUsersForUpdateById(7L)).thenReturn(Optional.of(lead));
        when(clients.existsBySourceLeadId(7L)).thenReturn(false);

        LeadConversionRequest request = new LeadConversionRequest();
        request.setClientType(ClientType.PERSON);
        request.setFirstName("Luis");
        Client client = service.convertLead(7L, request, adminAuth);

        assertThat(client.getSourceLead()).isSameAs(lead);
        assertThat(client.getAssignedTo()).isSameAs(seller);
        assertThat(client.getFirstName()).isEqualTo("Luis");
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.CONVERTED);
        assertThat(lead.getConvertedAt()).isNotNull();
        verify(leads).save(lead);
    }

    @Test void refusesSecondConversion() {
        Lead lead = new Lead();
        lead.setStatus(LeadStatus.QUALIFIED);
        when(leads.findWithUsersForUpdateById(8L)).thenReturn(Optional.of(lead));
        when(clients.existsBySourceLeadId(8L)).thenReturn(true);

        assertThatThrownBy(() -> service.convertLead(8L, new LeadConversionRequest(), adminAuth))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("ya fue convertido");
    }

    private User user(Long id, String email) {
        User user = new User("Test", "User", email, "encoded");
        user.setEmail(email);
        user.setFirstName("Test");
        user.setLastName("User");
        user.setPassword("encoded");
        return user;
    }
}
