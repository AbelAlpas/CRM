package com.example.CRM.lead.web;

import com.example.CRM.lead.entity.LeadSource;
import com.example.CRM.lead.entity.LeadStatus;
import com.example.CRM.lead.service.LeadService;
import com.example.CRM.user.service.UserService;
import com.example.CRM.client.service.ClientService;
import jakarta.validation.Valid;
import java.util.Arrays;
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
@RequestMapping("/leads")
@PreAuthorize("isAuthenticated()")
public class LeadController {
    private final LeadService leads;
    private final UserService users;
    private final ClientService clients;

    public LeadController(LeadService leads, UserService users, ClientService clients) { this.leads = leads; this.users = users; this.clients = clients; }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) LeadStatus status,
                       @RequestParam(required = false) LeadSource source, @RequestParam(required = false) Long assignedToId,
                       @RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        var result = leads.search(q, status, source, assignedToId, PageRequest.of(Math.max(page, 0), 10, Sort.by("createdAt").descending()), authentication);
        addCommonModel(model); model.addAttribute("leads", result); model.addAttribute("q", q); model.addAttribute("selectedStatus", status); model.addAttribute("selectedSource", source); model.addAttribute("selectedAssignedToId", assignedToId); model.addAttribute("canAssign", canManage(authentication));
        return "leads/list";
    }

    @GetMapping("/new")
    public String newForm(Authentication authentication, Model model) { addCommonModel(model); model.addAttribute("leadForm", new LeadForm()); model.addAttribute("editing", false); model.addAttribute("canAssign", canManage(authentication)); return "leads/form"; }

    @PostMapping
    public String create(@Valid @ModelAttribute LeadForm form, BindingResult result, Authentication authentication, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { addCommonModel(model); model.addAttribute("editing", false); model.addAttribute("canAssign", canManage(authentication)); return "leads/form"; }
        var lead = leads.create(form, authentication); redirect.addFlashAttribute("successMessage", "Lead creado correctamente."); return "redirect:/leads/" + lead.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) { var lead = leads.findById(id, authentication); model.addAttribute("pageTitle", "Detalle del Lead"); model.addAttribute("activeModule", "leads"); model.addAttribute("lead", lead); model.addAttribute("originClient", clients.findBySourceLeadId(id)); model.addAttribute("canConvert", lead.getStatus() == LeadStatus.QUALIFIED && canManage(authentication) || lead.getStatus() == LeadStatus.QUALIFIED && isAssignedSales(lead, authentication)); return "leads/detail"; }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) { var lead = leads.findById(id, authentication); addCommonModel(model); model.addAttribute("leadForm", toForm(lead)); model.addAttribute("leadId", id); model.addAttribute("editing", true); model.addAttribute("canAssign", canManage(authentication)); return "leads/form"; }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute LeadForm form, BindingResult result, Authentication authentication, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { addCommonModel(model); model.addAttribute("leadId", id); model.addAttribute("editing", true); model.addAttribute("canAssign", canManage(authentication)); return "leads/form"; }
        leads.update(id, form, authentication); redirect.addFlashAttribute("successMessage", "Lead actualizado correctamente."); return "redirect:/leads/" + id;
    }

    @PostMapping("/{id}/status")
    public String changeStatus(@PathVariable Long id, @RequestParam LeadStatus status, Authentication authentication, RedirectAttributes redirect) { leads.changeStatus(id, status, authentication); redirect.addFlashAttribute("successMessage", "Estado actualizado correctamente."); return "redirect:/leads/" + id; }

    private void addCommonModel(Model model) { model.addAttribute("pageTitle", "Leads"); model.addAttribute("activeModule", "leads"); model.addAttribute("statuses", Arrays.stream(LeadStatus.values()).filter(s -> s != LeadStatus.CONVERTED).toList()); model.addAttribute("sources", LeadSource.values()); model.addAttribute("salesUsers", users.findAll()); }
    private LeadForm toForm(com.example.CRM.lead.entity.Lead lead) { var form = new LeadForm(); form.setFirstName(lead.getFirstName()); form.setLastName(lead.getLastName()); form.setCompanyName(lead.getCompanyName()); form.setEmail(lead.getEmail()); form.setPhone(lead.getPhone()); form.setSource(lead.getSource()); form.setStatus(lead.getStatus()); form.setNotes(lead.getNotes()); form.setAssignedToId(lead.getAssignedTo() == null ? null : lead.getAssignedTo().getId()); return form; }
    private boolean canManage(Authentication a) { return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN") || x.getAuthority().equals("ROLE_MANAGER")); }
    private boolean isAssignedSales(com.example.CRM.lead.entity.Lead lead, Authentication a) { return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_SALES")) && lead.getAssignedTo() != null && lead.getAssignedTo().getEmail().equalsIgnoreCase(a.getName()); }
}
