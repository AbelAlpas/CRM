package com.example.CRM.config;

import com.example.CRM.user.service.UserService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class BootstrapAdminConfiguration {
    @Bean CommandLineRunner bootstrapAdmin(UserService users,
            @Value("${crm.security.bootstrap-admin.enabled:false}") boolean enabled,
            @Value("${crm.security.bootstrap-admin.first-name:Development}") String firstName,
            @Value("${crm.security.bootstrap-admin.last-name:Administrator}") String lastName,
            @Value("${crm.security.bootstrap-admin.email:}") String email,
            @Value("${crm.security.bootstrap-admin.password:}") String password) {
        return args -> { if (!enabled) return; if (email.isBlank() || password.length() < 8) throw new IllegalStateException("El administrador inicial requiere email y password de al menos 8 caracteres"); users.ensureBootstrapAdmin(firstName, lastName, email, password); };
    }
}
