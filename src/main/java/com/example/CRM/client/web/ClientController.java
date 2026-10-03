package com.example.CRM.client.web;

import com.example.CRM.client.entity.ClientType;
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
@RequestMapping("/clients")
@PreAuthorize("isAuthenticated()")
public class ClientController {
    private final ClientService clients;
    private final com.example.CRM.user.service.UserService users;
    public ClientController(ClientService clients, com.example.CRM.user.service.UserService users) { this.clients = clients; this.users = users; }

    @GetMapping
    public String list(@RequestParam(required = false) String q, @RequestParam(required = false) ClientType type, @RequestParam(required = false) Boolean active, @RequestParam(required = false) Long assignedToId, @RequestParam(defaultValue = "0") int page, Authentication authentication, Model model) {
        var result = clients.search(q, type, active, assignedToId, PageRequest.of(Math.max(page, 0), 10, Sort.by("createdAt").descending()), authentication);
        addCommon(model); model.addAttribute("clients", result); model.addAttribute("q", q); model.addAttribute("selectedType", type); model.addAttribute("selectedActive", active); model.addAttribute("selectedAssignedToId", assignedToId); model.addAttribute("canAssign", canManage(authentication)); return "clients/list";
    }

    @GetMapping("/new")
    public String newForm(Authentication authentication, Model model) { addCommon(model); model.addAttribute("clientForm", new ClientForm()); model.addAttribute("editing", false); model.addAttribute("canAssign", canManage(authentication)); return "clients/form"; }

    @PostMapping
    public String create(@Valid @ModelAttribute ClientForm form, BindingResult result, Authentication authentication, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { addCommon(model); model.addAttribute("editing", false); model.addAttribute("canAssign", canManage(authentication)); return "clients/form"; }
        var client = clients.create(form, authentication); redirect.addFlashAttribute("successMessage", "Cliente creado correctamente."); return "redirect:/clients/" + client.getId();
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Authentication authentication, Model model) { model.addAttribute("pageTitle", "Detalle del Cliente"); model.addAttribute("activeModule", "clients"); model.addAttribute("client", clients.findById(id, authentication)); return "clients/detail"; }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Authentication authentication, Model model) { var client = clients.findById(id, authentication); addCommon(model); model.addAttribute("clientForm", toForm(client)); model.addAttribute("clientId", id); model.addAttribute("editing", true); model.addAttribute("canAssign", canManage(authentication)); return "clients/form"; }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute ClientForm form, BindingResult result, Authentication authentication, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { addCommon(model); model.addAttribute("clientId", id); model.addAttribute("editing", true); model.addAttribute("canAssign", canManage(authentication)); return "clients/form"; }
        clients.update(id, form, authentication); redirect.addFlashAttribute("successMessage", "Cliente actualizado correctamente."); return "redirect:/clients/" + id;
    }

    @PostMapping("/{id}/activate") public String activate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) { clients.activate(id, authentication); redirect.addFlashAttribute("successMessage", "Cliente activado correctamente."); return "redirect:/clients/" + id; }
    @PostMapping("/{id}/deactivate") public String deactivate(@PathVariable Long id, Authentication authentication, RedirectAttributes redirect) { clients.deactivate(id, authentication); redirect.addFlashAttribute("successMessage", "Cliente desactivado correctamente."); return "redirect:/clients/" + id; }

    private void addCommon(Model model) { model.addAttribute("pageTitle", "Clientes"); model.addAttribute("activeModule", "clients"); model.addAttribute("types", Arrays.asList(ClientType.values())); model.addAttribute("salesUsers", users.findAll()); }
    private ClientForm toForm(com.example.CRM.client.entity.Client client) { var form = new ClientForm(); form.setClientType(client.getClientType()); form.setFirstName(client.getFirstName()); form.setLastName(client.getLastName()); form.setCompanyName(client.getCompanyName()); form.setDocumentNumber(client.getDocumentNumber()); form.setEmail(client.getEmail()); form.setPhone(client.getPhone()); form.setAddress(client.getAddress()); form.setActive(client.isActive()); form.setAssignedToId(client.getAssignedTo() == null ? null : client.getAssignedTo().getId()); return form; }
    private boolean canManage(Authentication a) { return a.getAuthorities().stream().anyMatch(x -> x.getAuthority().equals("ROLE_ADMIN") || x.getAuthority().equals("ROLE_MANAGER")); }
}
