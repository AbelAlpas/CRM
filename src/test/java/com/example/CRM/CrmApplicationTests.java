package com.example.CRM;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.example.CRM.user.repository.RoleRepository;
import com.example.CRM.user.repository.UserRepository;
import com.example.CRM.user.service.UserService;
import com.example.CRM.lead.repository.LeadRepository;
import com.example.CRM.client.repository.ClientRepository;

@SpringBootTest
class CrmApplicationTests {
	@MockBean UserRepository userRepository;
	@MockBean RoleRepository roleRepository;
	@MockBean UserService userService;
	@MockBean LeadRepository leadRepository;
	@MockBean ClientRepository clientRepository;

	@Test
	void contextLoads() {
	}

}
