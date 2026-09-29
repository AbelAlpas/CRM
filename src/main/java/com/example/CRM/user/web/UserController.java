package com.example.CRM.user.web;

import com.example.CRM.user.service.UserService;
import jakarta.validation.Valid;
import java.util.Set;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/users")
@PreAuthorize("hasRole('ADMIN')")
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service = service; }
    @GetMapping public String list(Model model) { model.addAttribute("users", service.findAll()); return "users/list"; }
    @GetMapping("/new") public String createForm(Model model) { model.addAttribute("userForm", new UserForm()); model.addAttribute("editing", false); return "users/form"; }
    @PostMapping public String create(@Valid @ModelAttribute UserForm form, BindingResult result) {
        if (form.getRoles().isEmpty()) result.rejectValue("roles", "roles.required", "Selecciona al menos un rol");
        if (form.getPassword() == null || form.getPassword().isBlank()) result.rejectValue("password", "password.required", "La contrasena es obligatoria");
        if (result.hasErrors()) return "users/form";
        service.create(form.getFirstName(), form.getLastName(), form.getEmail(), form.getPassword(), form.getRoles()); return "redirect:/users";
    }
    @GetMapping("/{id}/edit") public String editForm(@PathVariable Long id, Model model) {
        var user = service.findById(id); var form = new UserForm();
        form.setFirstName(user.getFirstName()); form.setLastName(user.getLastName()); form.setEmail(user.getEmail()); form.setEnabled(user.isEnabled()); form.setRoles(user.getRoles().stream().map(r -> r.getName()).collect(java.util.stream.Collectors.toSet()));
        model.addAttribute("userForm", form); model.addAttribute("userId", id); model.addAttribute("editing", true); return "users/form";
    }
    @PostMapping("/{id}") public String update(@PathVariable Long id, @Valid @ModelAttribute UserForm form, BindingResult result) {
        if (form.getRoles().isEmpty()) result.rejectValue("roles", "roles.required", "Selecciona al menos un rol");
        if (result.hasErrors()) return "users/form";
        service.update(id, form.getFirstName(), form.getLastName(), form.getEmail(), form.getPassword(), form.isEnabled(), form.getRoles()); return "redirect:/users";
    }
    @PostMapping("/{id}/delete") public String delete(@PathVariable Long id) { service.delete(id); return "redirect:/users"; }
    @ModelAttribute("availableRoles") public Set<String> availableRoles() { return Set.of("ADMIN", "MANAGER", "SALES"); }
}
