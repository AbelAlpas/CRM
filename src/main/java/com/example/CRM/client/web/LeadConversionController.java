package com.example.CRM.client.web;

import com.example.CRM.client.entity.ClientType;
import com.example.CRM.client.service.ClientService;
import com.example.CRM.lead.service.LeadService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@PreAuthorize("isAuthenticated()")
public class LeadConversionController {
    private final LeadService leads;
    private final ClientService clients;
    public LeadConversionController(LeadService leads, ClientService clients) { this.leads = leads; this.clients = clients; }

    @GetMapping("/leads/{leadId}/convert")
    public String form(@PathVariable Long leadId, Authentication authentication, Model model) {
        var lead = leads.findById(leadId, authentication);
        var request = new LeadConversionRequest(); request.setFirstName(lead.getFirstName()); request.setLastName(lead.getLastName()); request.setCompanyName(lead.getCompanyName()); request.setEmail(lead.getEmail()); request.setPhone(lead.getPhone()); request.setClientType(lead.getCompanyName() == null || lead.getCompanyName().isBlank() ? ClientType.PERSON : ClientType.COMPANY);
        model.addAttribute("pageTitle", "Convertir Lead"); model.addAttribute("activeModule", "leads"); model.addAttribute("lead", lead); model.addAttribute("conversionRequest", request); model.addAttribute("types", ClientType.values()); return "clients/convert";
    }

    @PostMapping("/leads/{leadId}/convert")
    public String convert(@PathVariable Long leadId, @Valid @ModelAttribute LeadConversionRequest request, BindingResult result, Authentication authentication, Model model, RedirectAttributes redirect) {
        if (result.hasErrors()) { var lead = leads.findById(leadId, authentication); model.addAttribute("pageTitle", "Convertir Lead"); model.addAttribute("activeModule", "leads"); model.addAttribute("lead", lead); model.addAttribute("types", ClientType.values()); return "clients/convert"; }
        var client = clients.convertLead(leadId, request, authentication); redirect.addFlashAttribute("successMessage", "Lead convertido en Cliente correctamente."); return "redirect:/clients/" + client.getId();
    }
}
