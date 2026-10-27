package com.example.CRM.activity.web;

import com.example.CRM.activity.entity.ActivityPriority;
import com.example.CRM.activity.entity.ActivityStatus;
import com.example.CRM.activity.entity.ActivityType;
import com.example.CRM.activity.entity.ActivityViewScope;
import com.example.CRM.activity.service.ActivityService;
import com.example.CRM.user.service.UserService;
import jakarta.validation.Valid;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/activities")
@PreAuthorize("isAuthenticated()")
public class ActivityController {
    private final ActivityService activities;
    private final UserService users;

    public ActivityController(ActivityService activities, UserService users) {
        this.activities = activities;
        this.users = users;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) ActivityType type,
                       @RequestParam(required = false) ActivityStatus status,
                       @RequestParam(required = false) ActivityPriority priority,
                       @RequestParam(required = false) Long ownerId,
                       @RequestParam(required = false) Long leadId,
                       @RequestParam(required = false) Long clientId,
                       @RequestParam(required = false) Long opportunityId,
                       @RequestParam(defaultValue = "ALL") ActivityViewScope scope,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication authentication, Model model) {
        var result = activities.search(q, type, status, priority, ownerId, leadId, clientId,
            opportunityId, scope, PageRequest.of(Math.max(page, 0), 10), authentication);
        addListModel(model, authentication);
        model.addAttribute("activities", result);
        model.addAttribute("q", q);
        model.addAttribute("selectedType", type);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedPriority", priority);
        model.addAttribute("selectedOwnerId", ownerId);
        model.addAttribute("selectedLeadId", leadId);
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedOpportunityId", opportunityId);
        model.addAttribute("selectedScope", scope);
        return "activities/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long leadId,
                          @RequestParam(required = false) Long clientId,
                          @RequestParam(required = false) Long opportunityId,
                          Authentication authentication, Model model) {
        ActivityCreateRequest request = activities.prepareCreateRequest(leadId, clientId, opportunityId, authentication);
        addFormModel(model, authentication);
        model.addAttribute("activityForm", request);
        model.addAttribute("editing", false);
        model.addAttribute("formAction", "/activities");
        model.addAttribute("pageTitle", "Nueva Actividad");
        return "activities/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("activityForm") ActivityCreateRequest request,
                         BindingResult result, Authentication authentication, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            addFormModel(model, authentication);
            model.addAttribute("editing", false);
            model.addAttribute("formAction", "/activities");
            model.addAttribute("pageTitle", "Nueva Actividad");
            return "activities/form";
        }
        try {
            var activity = activities.create(request, authentication);
            redirect.addFlashAttribute("successMessage", "Actividad creada correctamente.");
            return "redirect:/activities/" + activity.getId();
        } catch (IllegalArgumentException exception) {
            result.reject("activity.invalid", exception.getMessage());
            addFormModel(model, authentication);
            model.addAttribute("editing", false);
            model.addAttribute("formAction", "/activities");
            model.addAttribute("pageTitle", "Nueva Actividad");
            return "activities/form";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        model.addAttribute("pageTitle", "Detalle de la Actividad");
        model.addAttribute("activeModule", "activities");
        model.addAttribute("activity", activities.findById(id, authentication));
        return "activities/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        var activity = activities.findById(id, authentication);
        if (!activity.isOpen()) throw new IllegalArgumentException("La actividad cerrada no puede editarse");
        addFormModel(model, authentication);
        model.addAttribute("activity", activity);
        model.addAttribute("activityId", id);
        model.addAttribute("activityForm", activities.toUpdateRequest(activity));
        model.addAttribute("editing", true);
        model.addAttribute("formAction", "/activities/" + id + "/edit");
        model.addAttribute("pageTitle", "Editar Actividad");
        return "activities/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("activityForm") ActivityUpdateRequest request,
                         BindingResult result, Authentication authentication, Model model,
                         RedirectAttributes redirect) {
        var activity = activities.findById(id, authentication);
        if (!activity.isOpen()) throw new IllegalArgumentException("La actividad cerrada no puede editarse");
        if (result.hasErrors()) {
            addFormModel(model, authentication);
            model.addAttribute("activity", activity);
            model.addAttribute("activityId", id);
            model.addAttribute("editing", true);
            model.addAttribute("formAction", "/activities/" + id + "/edit");
            model.addAttribute("pageTitle", "Editar Actividad");
            return "activities/form";
        }
        try {
            activities.update(id, request, authentication);
            redirect.addFlashAttribute("successMessage", "Actividad actualizada correctamente.");
            return "redirect:/activities/" + id;
        } catch (IllegalArgumentException exception) {
            result.reject("activity.invalid", exception.getMessage());
            addFormModel(model, authentication);
            model.addAttribute("activity", activity);
            model.addAttribute("activityId", id);
            model.addAttribute("editing", true);
            model.addAttribute("formAction", "/activities/" + id + "/edit");
            model.addAttribute("pageTitle", "Editar Actividad");
            return "activities/form";
        }
    }

    @PostMapping("/{id}/start")
    public String start(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        activities.markInProgress(id, authentication);
        redirect.addFlashAttribute("successMessage", "Actividad marcada en progreso.");
        return "redirect:/activities/" + id;
    }

    @PostMapping("/{id}/complete")
    public String complete(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        activities.complete(id, authentication);
        redirect.addFlashAttribute("successMessage", "Actividad completada correctamente.");
        return "redirect:/activities/" + id;
    }

    @PostMapping("/{id}/cancel")
    public String cancel(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        activities.cancel(id, authentication);
        redirect.addFlashAttribute("successMessage", "Actividad cancelada.");
        return "redirect:/activities/" + id;
    }

    private void addListModel(Model model, Authentication authentication) {
        addCommonModel(model, authentication);
        model.addAttribute("leads", activities.availableLeads(authentication));
        model.addAttribute("clients", activities.availableClients(authentication));
        model.addAttribute("opportunities", activities.availableOpportunities(authentication));
        model.addAttribute("scopes", ActivityViewScope.values());
    }

    private void addFormModel(Model model, Authentication authentication) {
        addCommonModel(model, authentication);
        model.addAttribute("leads", activities.availableLeads(authentication));
        model.addAttribute("clients", activities.availableClients(authentication));
        model.addAttribute("opportunities", activities.availableOpportunities(authentication));
    }

    private void addCommonModel(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Actividades");
        model.addAttribute("activeModule", "activities");
        model.addAttribute("types", ActivityType.values());
        model.addAttribute("statuses", ActivityStatus.values());
        model.addAttribute("priorities", ActivityPriority.values());
        model.addAttribute("owners", users.findAllEnabled());
        model.addAttribute("canAssign", canManage(authentication));
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(authority ->
            authority.getAuthority().equals("ROLE_ADMIN")
                || authority.getAuthority().equals("ROLE_MANAGER"));
    }
}
