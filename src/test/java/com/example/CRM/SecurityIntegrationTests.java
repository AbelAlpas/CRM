package com.example.CRM;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.CRM.user.repository.RoleRepository;
import com.example.CRM.user.repository.UserRepository;
import com.example.CRM.user.service.UserService;
import com.example.CRM.security.service.DatabaseUserDetailsService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.crypto.password.PasswordEncoder;
import static org.mockito.Mockito.when;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class SecurityIntegrationTests {
    @Autowired MockMvc mvc;
    @MockBean UserRepository userRepository;
    @MockBean RoleRepository roleRepository;
    @MockBean UserService userService;
    @MockBean DatabaseUserDetailsService databaseUserDetailsService;
    @Autowired PasswordEncoder passwordEncoder;

    @org.junit.jupiter.api.BeforeEach void configureLoginUser() {
        when(databaseUserDetailsService.loadUserByUsername("user@example.com"))
            .thenReturn(User.withUsername("user@example.com").password(passwordEncoder.encode("secret123")).roles("SALES").build());
    }

    @Test void unauthenticatedUserIsRedirectedToLogin() throws Exception { mvc.perform(get("/dashboard")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("http://localhost/login")); }
    @Test void correctLoginFormIsAccepted() throws Exception { mvc.perform(post("/login").with(csrf()).param("username", "user@example.com").param("password", "secret123")).andExpect(status().is3xxRedirection()); }
    @Test void incorrectLoginFormIsRejected() throws Exception { mvc.perform(post("/login").with(csrf()).param("username", "user@example.com").param("password", "bad")).andExpect(status().is3xxRedirection()).andExpect(redirectedUrl("/login?error")); }
    @Test @WithMockUser(roles = "ADMIN") void adminCanAccessUsers() throws Exception { mvc.perform(get("/users")).andExpect(status().isOk()); }
    @Test @WithMockUser(roles = "SALES") void salesCannotAccessUsers() throws Exception { mvc.perform(get("/users")).andExpect(status().isForbidden()); }
}
