package com.example.CRM.security.service;

import com.example.CRM.user.repository.UserRepository;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

@Service
public class DatabaseUserDetailsService implements UserDetailsService {
    private final UserRepository users;
    public DatabaseUserDetailsService(UserRepository users) { this.users = users; }
    @Override public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        var user = users.findByEmailIgnoreCase(username).orElseThrow(() -> new UsernameNotFoundException("Usuario no encontrado"));
        var authorities = user.getRoles().stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role.getName())).toList();
        return org.springframework.security.core.userdetails.User.withUsername(user.getEmail()).password(user.getPassword()).disabled(!user.isEnabled()).authorities(authorities).build();
    }
}
