package com.example.CRM.opportunity.web;

import com.example.CRM.client.entity.Client;
import com.example.CRM.opportunity.entity.Opportunity;
import com.example.CRM.opportunity.entity.OpportunityStatus;
import com.example.CRM.opportunity.service.OpportunityService;
import com.example.CRM.user.service.UserService;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
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
@RequestMapping("/opportunities")
@PreAuthorize("isAuthenticated()")
public class OpportunityController {
    private final OpportunityService opportunities;
    private final UserService users;

    public OpportunityController(OpportunityService opportunities, UserService users) {
        this.opportunities = opportunities;
        this.users = users;
    }

    @GetMapping
    public String list(@RequestParam(required = false) String q,
                       @RequestParam(required = false) OpportunityStatus status,
                       @RequestParam(required = false) Short stageId,
                       @RequestParam(required = false) Long ownerId,
                       @RequestParam(required = false) Long clientId,
                       @RequestParam(required = false) LocalDate expectedCloseDate,
                       @RequestParam(defaultValue = "0") int page,
                       Authentication authentication, Model model) {
        var result = opportunities.search(q, status, stageId, ownerId, clientId, expectedCloseDate,
            PageRequest.of(Math.max(page, 0), 10, Sort.by("createdAt").descending()), authentication);
        addListModel(model, authentication);
        model.addAttribute("opportunities", result);
        model.addAttribute("q", q);
        model.addAttribute("selectedStatus", status);
        model.addAttribute("selectedStageId", stageId);
        model.addAttribute("selectedOwnerId", ownerId);
        model.addAttribute("selectedClientId", clientId);
        model.addAttribute("selectedExpectedCloseDate", expectedCloseDate);
        return "opportunities/list";
    }

    @GetMapping("/new")
    public String newForm(@RequestParam(required = false) Long clientId, Authentication authentication, Model model) {
        OpportunityCreateRequest request = new OpportunityCreateRequest();
        request.setClientId(clientId);
        List<Client> availableClients = opportunities.availableClients(authentication);
        if (clientId != null && canManage(authentication)) {
            availableClients.stream().filter(c -> clientId.equals(c.getId())).findFirst()
                .map(Client::getAssignedTo).ifPresent(owner -> request.setOwnerId(owner.getId()));
        }
        addCreateModel(model, authentication, availableClients);
        model.addAttribute("opportunityCreateRequest", request);
        return "opportunities/form-create";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute OpportunityCreateRequest opportunityCreateRequest,
                         BindingResult result, Authentication authentication, Model model,
                         RedirectAttributes redirect) {
        if (result.hasErrors()) {
            addCreateModel(model, authentication, opportunities.availableClients(authentication));
            return "opportunities/form-create";
        }
        try {
            Opportunity opportunity = opportunities.create(opportunityCreateRequest, authentication);
            redirect.addFlashAttribute("successMessage", "Oportunidad creada correctamente.");
            return "redirect:/opportunities/" + opportunity.getId();
        } catch (IllegalArgumentException exception) {
            result.reject("opportunity.invalid", exception.getMessage());
            addCreateModel(model, authentication, opportunities.availableClients(authentication));
            return "opportunities/form-create";
        }
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) {
        Opportunity opportunity = opportunities.findById(id, authentication);
        model.addAttribute("pageTitle", "Detalle de la Oportunidad");
        model.addAttribute("activeModule", "opportunities");
        model.addAttribute("opportunity", opportunity);
        model.addAttribute("openStages", opportunities.openStages());
        model.addAttribute("stageHistory", opportunities.getStageHistory(id, authentication));
        return "opportunities/detail";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) {
        Opportunity opportunity = opportunities.findById(id, authentication);
        if (!opportunity.isOpen()) throw new IllegalArgumentException("La oportunidad cerrada no puede editarse");
        addEditModel(model, authentication, opportunity);
        model.addAttribute("opportunityUpdateRequest", toUpdateRequest(opportunity));
        return "opportunities/form-edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute OpportunityUpdateRequest opportunityUpdateRequest,
                         BindingResult result, Authentication authentication, Model model,
                         RedirectAttributes redirect) {
        Opportunity opportunity = opportunities.findById(id, authentication);
        if (!opportunity.isOpen()) throw new IllegalArgumentException("La oportunidad cerrada no puede editarse");
        if (result.hasErrors()) {
            addEditModel(model, authentication, opportunity);
            return "opportunities/form-edit";
        }
        try {
            opportunities.update(id, opportunityUpdateRequest, authentication);
            redirect.addFlashAttribute("successMessage", "Oportunidad actualizada correctamente.");
            return "redirect:/opportunities/" + id;
        } catch (IllegalArgumentException exception) {
            result.reject("opportunity.invalid", exception.getMessage());
            addEditModel(model, authentication, opportunity);
            return "opportunities/form-edit";
        }
    }

    @PostMapping("/{id}/stage")
    public String changeStage(@PathVariable Long id, @RequestParam String targetStageCode,
                              Authentication authentication, RedirectAttributes redirect) {
        opportunities.changeStage(id, targetStageCode, authentication);
        redirect.addFlashAttribute("successMessage", "Etapa actualizada correctamente.");
        return "redirect:/opportunities/" + id;
    }

    @PostMapping("/{id}/win")
    public String win(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) {
        opportunities.markAsWon(id, authentication);
        redirect.addFlashAttribute("successMessage", "Oportunidad marcada como ganada.");
        return "redirect:/opportunities/" + id;
    }

    @GetMapping("/{id}/lose")
    public String loseForm(@PathVariable Long id, Authentication authentication, Model model) {
        Opportunity opportunity = opportunities.findById(id, authentication);
        if (!opportunity.isOpen()) throw new IllegalArgumentException("La oportunidad ya está cerrada");
        model.addAttribute("pageTitle", "Marcar oportunidad como perdida");
        model.addAttribute("activeModule", "opportunities");
        model.addAttribute("opportunity", opportunity);
        model.addAttribute("opportunityLostRequest", new OpportunityLostRequest());
        return "opportunities/lose";
    }

    @PostMapping("/{id}/lose")
    public String lose(@PathVariable Long id,
                       @Valid @ModelAttribute OpportunityLostRequest opportunityLostRequest,
                       BindingResult result, Authentication authentication, Model model,
                       RedirectAttributes redirect) {
        Opportunity opportunity = opportunities.findById(id, authentication);
        if (!opportunity.isOpen()) throw new IllegalArgumentException("La oportunidad ya está cerrada");
        if (result.hasErrors()) {
            model.addAttribute("pageTitle", "Marcar oportunidad como perdida");
            model.addAttribute("activeModule", "opportunities");
            model.addAttribute("opportunity", opportunity);
            return "opportunities/lose";
        }
        opportunities.markAsLost(id, opportunityLostRequest.getLostReason(), authentication);
        redirect.addFlashAttribute("successMessage", "Oportunidad marcada como perdida.");
        return "redirect:/opportunities/" + id;
    }

    private void addListModel(Model model, Authentication authentication) {
        model.addAttribute("pageTitle", "Oportunidades");
        model.addAttribute("activeModule", "opportunities");
        model.addAttribute("statuses", Arrays.asList(OpportunityStatus.values()));
        model.addAttribute("stages", opportunities.allStages());
        model.addAttribute("owners", users.findAllEnabled());
        model.addAttribute("clients", opportunities.availableClients(authentication));
        model.addAttribute("canAssign", canManage(authentication));
    }

    private void addCreateModel(Model model, Authentication authentication, List<Client> availableClients) {
        model.addAttribute("pageTitle", "Nueva Oportunidad");
        model.addAttribute("activeModule", "opportunities");
        model.addAttribute("clients", availableClients);
        model.addAttribute("owners", users.findAllEnabled());
        model.addAttribute("canAssign", canManage(authentication));
    }

    private void addEditModel(Model model, Authentication authentication, Opportunity opportunity) {
        model.addAttribute("pageTitle", "Editar Oportunidad");
        model.addAttribute("activeModule", "opportunities");
        model.addAttribute("opportunity", opportunity);
        model.addAttribute("owners", users.findAllEnabled());
        model.addAttribute("canAssign", canManage(authentication));
    }

    private OpportunityUpdateRequest toUpdateRequest(Opportunity opportunity) {
        OpportunityUpdateRequest request = new OpportunityUpdateRequest();
        request.setName(opportunity.getName());
        request.setDescription(opportunity.getDescription());
        request.setAmount(opportunity.getAmount());
        request.setProbability(opportunity.getProbability());
        request.setExpectedCloseDate(opportunity.getExpectedCloseDate());
        request.setOwnerId(opportunity.getOwner() == null ? null : opportunity.getOwner().getId());
        return request;
    }

    private boolean canManage(Authentication authentication) {
        return authentication.getAuthorities().stream().anyMatch(a ->
            a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_MANAGER"));
    }
}
