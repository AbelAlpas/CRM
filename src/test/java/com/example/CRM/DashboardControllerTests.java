package com.example.CRM;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.CRM.config.SecurityConfig;
import com.example.CRM.dashboard.controller.DashboardController;
import com.example.CRM.dashboard.model.DashboardPeriod;
import com.example.CRM.dashboard.model.DashboardViewModel;
import com.example.CRM.dashboard.service.DashboardService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(DashboardController.class)
@Import(SecurityConfig.class)
class DashboardControllerTests {
    @Autowired MockMvc mvc;
    @MockBean DashboardService dashboard;

    @BeforeEach
    void setUp() {
        when(dashboard.getDashboard(any(), any())).thenReturn(emptyDashboard());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/dashboard"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    void adminManagerAndSalesCanOpenDashboard() throws Exception {
        for (String role : List.of("ADMIN", "MANAGER", "SALES")) {
            mvc.perform(get("/dashboard").with(user("user@example.com").roles(role)))
                .andExpect(status().isOk())
                .andExpect(view().name("dashboard/index"))
                .andExpect(model().attributeExists("dashboard"));
        }
    }

    @Test
    void invalidPeriodIsHandledAsMonth() throws Exception {
        mvc.perform(get("/dashboard").with(user("admin@example.com").roles("ADMIN")).param("period", "INVALID"))
            .andExpect(status().isOk());

        verify(dashboard).getDashboard(any(), eq(DashboardPeriod.MONTH));
    }

    @Test
    void periodIsForwardedToService() throws Exception {
        mvc.perform(get("/dashboard").with(user("admin@example.com").roles("ADMIN")).param("period", "YEAR"))
            .andExpect(status().isOk());

        verify(dashboard).getDashboard(any(), eq(DashboardPeriod.YEAR));
    }

    private DashboardViewModel emptyDashboard() {
        return new DashboardViewModel(
            DashboardPeriod.MONTH,
            new DashboardViewModel.LeadMetrics(0),
            new DashboardViewModel.ClientMetrics(0, 0),
            new DashboardViewModel.OpportunityMetrics(0, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0, BigDecimal.ZERO.setScale(1)),
            new DashboardViewModel.ActivityMetrics(0, 0, 0, 0),
            List.of(), List.of(), List.of(), List.of(), List.of(), List.of(), List.of());
    }
}
