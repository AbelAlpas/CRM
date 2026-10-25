package com.example.CRM.user.service;

import com.example.CRM.user.entity.Role;
import com.example.CRM.user.entity.User;
import com.example.CRM.user.repository.RoleRepository;
import com.example.CRM.user.repository.UserRepository;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class UserService {
    private final UserRepository users;
    private final RoleRepository roles;
    private final PasswordEncoder encoder;

    public UserService(UserRepository users, RoleRepository roles, PasswordEncoder encoder) {
        this.users = users; this.roles = roles; this.encoder = encoder;
    }

    @Transactional(readOnly = true)
    public List<User> findAll() { return users.findAll(); }

    @Transactional(readOnly = true)
    public List<User> findAllEnabled() { return users.findAllByEnabledTrueOrderByFirstNameAscLastNameAsc(); }

    @Transactional(readOnly = true)
    public User findById(Long id) { return users.findById(id).orElseThrow(); }

    @Transactional
    public User create(String firstName, String lastName, String email, String rawPassword, Set<String> roleNames) {
        if (users.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("El email ya esta registrado");
        var user = new User(firstName.trim(), lastName.trim(), email.trim().toLowerCase(), encoder.encode(rawPassword));
        user.setRoles(resolveRoles(roleNames));
        return users.save(user);
    }

    @Transactional
    public User update(Long id, String firstName, String lastName, String email, String rawPassword, boolean enabled, Set<String> roleNames) {
        var user = findById(id);
        if (!user.getEmail().equalsIgnoreCase(email) && users.existsByEmailIgnoreCase(email)) throw new IllegalArgumentException("El email ya esta registrado");
        user.setFirstName(firstName.trim()); user.setLastName(lastName.trim()); user.setEmail(email.trim().toLowerCase()); user.setEnabled(enabled); user.setRoles(resolveRoles(roleNames));
        if (rawPassword != null && !rawPassword.isBlank()) user.setPassword(encoder.encode(rawPassword));
        return users.save(user);
    }

    @Transactional
    public void delete(Long id) { users.deleteById(id); }

    @Transactional
    public User ensureBootstrapAdmin(String firstName, String lastName, String email, String rawPassword) {
        return users.findByEmailIgnoreCase(email).orElseGet(() -> create(firstName, lastName, email, rawPassword, Set.of("ADMIN")));
    }

    private Set<Role> resolveRoles(Set<String> names) {
        var result = new HashSet<Role>();
        for (String name : names) result.add(roles.findByName(name).orElseThrow(() -> new IllegalArgumentException("Rol invalido: " + name)));
        return result;
    }
}
