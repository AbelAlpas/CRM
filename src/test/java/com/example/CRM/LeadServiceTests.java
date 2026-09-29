package com.example.CRM;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import com.example.CRM.lead.entity.Lead;
import com.example.CRM.lead.entity.LeadSource;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.lead.service.LeadService;
import com.example.CRM.lead.web.LeadForm;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.TestingAuthenticationToken;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class LeadServiceTests {
    @Mock LeadRepository leads;
    @Mock UserRepository users;
    private LeadService service;
    private User salesUser;
    private TestingAuthenticationToken salesAuthentication;

    @BeforeEach void setUp() {
        service = new LeadService(leads, users);
        salesUser = org.mockito.Mockito.mock(User.class);
        when(salesUser.getId()).thenReturn(7L);
        when(salesUser.getEmail()).thenReturn("sales@example.com");
        when(users.findByEmailIgnoreCase("sales@example.com")).thenReturn(Optional.of(salesUser));
        salesAuthentication = new TestingAuthenticationToken("sales@example.com", "password", "ROLE_SALES");
        when(leads.save(any(Lead.class))).thenAnswer(invocation -> invocation.getArgument(0));
    }

    @Test void createSalesLeadStartsNewAndAssignsCurrentUser() {
        LeadForm form = form();
        Lead lead = service.create(form, salesAuthentication);
        assertThat(lead.getStatus()).isEqualTo(LeadStatus.NEW);
        assertThat(lead.getCreatedBy()).isSameAs(salesUser);
        assertThat(lead.getAssignedTo()).isSameAs(salesUser);
    }

    @Test void manualConversionIsRejected() {
        Lead lead = new Lead();
        lead.setAssignedTo(salesUser);
        when(leads.findWithUsersById(10L)).thenReturn(Optional.of(lead));
        LeadForm form = form(); form.setStatus(LeadStatus.CONVERTED);
        assertThatThrownBy(() -> service.update(10L, form, salesAuthentication)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test void salesCannotAccessAnotherUsersLead() {
        Lead lead = new Lead();
        User other = org.mockito.Mockito.mock(User.class);
        when(other.getEmail()).thenReturn("other@example.com");
        lead.setAssignedTo(other);
        when(leads.findWithUsersById(10L)).thenReturn(Optional.of(lead));
        assertThatThrownBy(() -> service.findById(10L, salesAuthentication)).isInstanceOf(AccessDeniedException.class);
    }

    @Test void missingAssignedUserIsRejected() {
        when(users.findById(99L)).thenReturn(Optional.empty());
        LeadForm form = form(); form.setAssignedToId(99L);
        TestingAuthenticationToken manager = new TestingAuthenticationToken("sales@example.com", "password", "ROLE_MANAGER");
        assertThatThrownBy(() -> service.create(form, manager)).isInstanceOf(IllegalArgumentException.class);
    }

    private LeadForm form() {
        LeadForm form = new LeadForm(); form.setFirstName("Carlos"); form.setLastName("Ramirez"); form.setEmail("carlos@example.com"); form.setSource(LeadSource.WEBSITE); return form;
    }
}
