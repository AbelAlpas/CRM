package com.example.CRM.pipeline.web;

import com.example.CRM.pipeline.service.PipelineService;
import com.example.CRM.user.service.UserService;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
@RequestMapping("/pipeline")
@PreAuthorize("isAuthenticated()")
public class PipelineController {
    private final PipelineService pipeline;
    private final UserService users;

    public PipelineController(PipelineService pipeline, UserService users) {
        this.pipeline = pipeline;
        this.users = users;
    }

    @GetMapping
    public String board(@RequestParam(required = false) String search,
                        @RequestParam(required = false) Long ownerId,
                        @RequestParam(required = false) Long clientId,
                        Authentication authentication, Model model) {
        model.addAttribute("pageTitle", "Pipeline de ventas");
        model.addAttribute("activeModule", "pipeline");
        model.addAttribute("pipeline", pipeline.getPipeline(search, ownerId, clientId, authentication));
        model.addAttribute("owners", users.findAllEnabled());
        model.addAttribute("clients", pipeline.availableClients(authentication));
        model.addAttribute("search", search);
        model.addAttribute("selectedOwnerId", ownerId);
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("canAssign", canManage(authentication));
        return "pipeline/board";
    }

    @PostMapping("/opportunities/{id}/stage")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> move(@PathVariable Long id,
                                                     @RequestParam String stageCode,
                                                     Authentication authentication) {
        try {
            pipeline.moveOpportunity(id, stageCode, authentication);
            return ResponseEntity.ok(Map.of("success", true));
        } catch (AccessDeniedException exception) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                .body(Map.of("success", false, "message", "No tienes permiso para mover esta oportunidad."));
        } catch (IllegalArgumentException exception) {
            return ResponseEntity.badRequest()
                .body(Map.of("success", false, "message", exception.getMessage()));
        }
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a ->
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
