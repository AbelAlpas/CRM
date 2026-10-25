package com.example.CRM;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import com.example.CRM.config.SecurityConfig;
import com.example.CRM.pipeline.service.PipelineService;
import com.example.CRM.pipeline.web.PipelineController;
import com.example.CRM.pipeline.web.PipelineOpportunityView;
import com.example.CRM.pipeline.web.PipelineStageView;
import com.example.CRM.pipeline.web.PipelineView;
import com.example.CRM.user.service.UserService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(PipelineController.class)
@Import(SecurityConfig.class)
class PipelineControllerTests {
    @Autowired MockMvc mvc;
    @MockBean PipelineService pipeline;
    @MockBean UserService users;

    @BeforeEach
    void setUp() {
        PipelineView empty = new PipelineView(List.of(), 0, BigDecimal.ZERO, BigDecimal.ZERO);
        when(pipeline.getPipeline(isNull(), isNull(), isNull(), any(Authentication.class))).thenReturn(empty);
        when(pipeline.availableClients(any(Authentication.class))).thenReturn(List.of());
        when(users.findAllEnabled()).thenReturn(List.of());
    }

    @Test
    void unauthenticatedUserIsRedirectedToLogin() throws Exception {
        mvc.perform(get("/pipeline"))
            .andExpect(status().is3xxRedirection())
            .andExpect(redirectedUrl("http://localhost/login"));
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void adminCanOpenPipelineAndUseOwnerFilter() throws Exception {
        PipelineOpportunityView card = new PipelineOpportunityView(25L, "Venta de cemento", 7L,
            "Constructora Huancayo", new BigDecimal("15000.00"), (short) 60,
            new BigDecimal("9000.00"), "Ana Ventas", LocalDate.now().plusDays(3), false,
            "PROSPECTING");
        PipelineStageView stage = new PipelineStageView((short) 1, "PROSPECTING", "Prospeccion",
            (short) 1, List.of(card), 1, new BigDecimal("15000.00"));
        when(pipeline.getPipeline(isNull(), isNull(), isNull(), any(Authentication.class)))
            .thenReturn(new PipelineView(List.of(stage), 1, new BigDecimal("15000.00"),
                new BigDecimal("9000.00")));

        mvc.perform(get("/pipeline"))
            .andExpect(status().isOk())
            .andExpect(view().name("pipeline/board"))
            .andExpect(model().attribute("canAssign", true))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Prospeccion")))
            .andExpect(content().string(org.hamcrest.Matchers.containsString("Venta de cemento")));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void managerCanOpenPipeline() throws Exception {
        mvc.perform(get("/pipeline"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("canAssign", true));
    }

    @Test
    @WithMockUser(roles = "SALES")
    void salesCanOpenPipelineWithoutOwnerControls() throws Exception {
        mvc.perform(get("/pipeline"))
            .andExpect(status().isOk())
            .andExpect(model().attribute("canAssign", false));
    }

    @Test
    @WithMockUser(roles = "MANAGER")
    void validDragRequestMovesOpportunity() throws Exception {
        mvc.perform(post("/pipeline/opportunities/25/stage")
                .with(csrf())
                .param("stageCode", "NEGOTIATION"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.success").value(true));

        verify(pipeline).moveOpportunity(org.mockito.ArgumentMatchers.eq(25L),
            org.mockito.ArgumentMatchers.eq("NEGOTIATION"), any(Authentication.class));
    }

    @Test
    @WithMockUser(roles = "SALES")
    void dragRequestWithoutCsrfIsRejected() throws Exception {
        mvc.perform(post("/pipeline/opportunities/25/stage")
                .param("stageCode", "NEGOTIATION"))
            .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = "SALES")
    void forbiddenMovementReturnsControlledJsonError() throws Exception {
        doThrow(new AccessDeniedException("forbidden"))
            .when(pipeline).moveOpportunity(org.mockito.ArgumentMatchers.eq(25L),
                org.mockito.ArgumentMatchers.eq("PROPOSAL"), any(Authentication.class));

        mvc.perform(post("/pipeline/opportunities/25/stage")
                .with(csrf())
                .param("stageCode", "PROPOSAL"))
            .andExpect(status().isForbidden())
            .andExpect(jsonPath("$.success").value(false));
    }
}
