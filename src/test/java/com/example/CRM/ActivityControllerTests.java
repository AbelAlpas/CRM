package com.example.CRM;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.CRM.activity.entity.Activity;
import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityStatus;
import com.example.CRM.activity.entity.ActivityType;
import com.example.CRM.activity.service.ActivityService;
import com.example.CRM.activity.web.ActivityController;
import com.example.CRM.activity.web.ActivityCreateRequest;
import com.example.CRM.activity.web.ActivityUpdateRequest;
import com.example.CRM.config.SecurityConfig;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.service.UserService;
import java.time.Instant;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Page;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(ActivityController.class)
@Import(SecurityConfig.class)
class ActivityControllerTests {
    @Autowired MockMvc mvc;
    @MockBean ActivityService activities;
    @MockBean UserService users;

    @BeforeEach
    void setUp() {
        when(activities.search(any(), any(), any(), any(), any(), any(), any(), any(), any(), any(), any(Authentication.class)))
            .thenReturn(Page.empty());
        when(activities.availableLeads(any(Authentication.class))).thenReturn(List.of());
        when(activities.availableClients(any(Authentication.class))).thenReturn(List.of());
        when(activities.availableOpportunities(any(Authentication.class))).thenReturn(List.of());
        when(users.findAllEnabled()).thenReturn(List.of());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/activities"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenActivityList() throws Exception {
        mvc.perform(get("/activities"))
            .andExpect(status().isOk())
            .andExpect(view().name("activities/list"))
            .andExpect(model().attribute("canAssign", true))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Actividades")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanOpenActivityList() throws Exception {
        mvc.perform(get("/activities"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("canAssign", true));
    }

    @Test
    @WithMockUser(roles = "SALES")
    void salesCanOpenListWithoutOwnerAssignmentControls() throws Exception {
        mvc.perform(get("/activities"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("canAssign", false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void opensCreateFormWithPreselectedRelation() throws Exception {
        ActivityCreateRequest request = new ActivityCreateRequest();
        request.setLeadId(10L);
        when(activities.prepareCreateRequest(eq(10L), any(), any(), any(Authentication.class))).thenReturn(request);

        mvc.perform(get("/activities/new").param("leadId", "10"))
            .andExpect(status().isOk())
            .andExpect(view().name("activities/form"))
            .andExpect(model().attribute("editing", false));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void validCreationRedirectsToDetail() throws Exception {
        Activity created = mock(Activity.class);
        when(created.getId()).thenReturn(25L);
        when(activities.create(any(ActivityCreateRequest.class), any(Authentication.class))).thenReturn(created);

        mvc.perform(post("/activities").with(csrf())
                .param("activityType", "CALL")
                .param("subject", "Llamar al prospecto")
                .param("priority", "HIGH")
                .param("ownerId", "8"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/activities/25"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void invalidCreationReturnsFormWithoutCallingService() throws Exception {
        mvc.perform(post("/activities").with(csrf())
                .param("activityType", "TASK")
                .param("subject", "")
                .param("priority", "MEDIUM"))
            .andExpect(status().isOk())
            .andExpect(view().name("activities/form"))
            .andExpect(model().attributeHasFieldErrors("activityForm", "subject"));

        verify(activities, never()).create(any(), any());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void activityDetailRendersForAuthorizedUser() throws Exception {
        Activity activity = detailActivity();
        when(activities.findById(eq(25L), any(Authentication.class))).thenReturn(activity);

        mvc.perform(get("/activities/25"))
            .andExpect(status().isOk())
            .andExpect(view().name("activities/detail"))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Llamar al prospecto")));
    }

    @Test
    @WithMockUser(roles = "SALES")
    void salesCannotOpenAnotherOwnersActivity() throws Exception {
        when(activities.findById(eq(25L), any(Authentication.class)))
            .thenThrow(new AccessDeniedException("denegado"));

        mvc.perform(get("/activities/25"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void startCompleteAndCancelEndpointsDelegateToService() throws Exception {
        mvc.perform(post("/activities/25/start").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/activities/25/complete").with(csrf())).andExpect(status().is3xxRedirection());
        mvc.perform(post("/activities/25/cancel").with(csrf())).andExpect(status().is3xxRedirection());

        verify(activities).markInProgress(eq(25L), any(Authentication.class));
        verify(activities).complete(eq(25L), any(Authentication.class));
        verify(activities).cancel(eq(25L), any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void updateFormAndSubmissionWork() throws Exception {
        Activity activity = detailActivity();
        ActivityUpdateRequest request = new ActivityUpdateRequest();
        request.setActivityType(ActivityType.CALL);
        request.setSubject("Llamar al prospecto");
        request.setPriority(ActivityPriority.HIGH);
        when(activities.findById(eq(25L), any(Authentication.class))).thenReturn(activity);
        when(activities.toUpdateRequest(activity)).thenReturn(request);

        mvc.perform(get("/activities/25/edit"))
            .andExpect(status().isOk())
            .andExpect(view().name("activities/form"))
            .andExpect(model().attribute("editing", true));

        mvc.perform(post("/activities/25/edit").with(csrf())
                .param("activityType", "MEETING")
                .param("subject", "Reunion actualizada")
                .param("priority", "MEDIUM")
                .param("ownerId", "8"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("/activities/25"));
    }

    @Test
    @WithMockUser(roles = "SALES")
    void postWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/activities/25/complete"))
            .andExpect(status().isForbidden());
    }

    private Activity detailActivity() {
        Activity activity = mock(Activity.class);
        User owner = new User("Ana", "Ventas", "ana@example.com", "encoded");
        Instant now = Instant.now();
        when(activity.getId()).thenReturn(25L);
        when(activity.getSubject()).thenReturn("Llamar al prospecto");
        when(activity.getActivityType()).thenReturn(ActivityType.CALL);
        when(activity.getPriority()).thenReturn(ActivityPriority.HIGH);
        when(activity.getStatus()).thenReturn(ActivityStatus.PENDING);
        when(activity.getOwner()).thenReturn(owner);
        when(activity.getCreatedBy()).thenReturn(owner);
        when(activity.getCreatedAt()).thenReturn(now);
        when(activity.getUpdatedAt()).thenReturn(now);
        when(activity.isOpen()).thenReturn(true);
        return activity;
    }
}
