package com.example.CRM;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import com.example.CRM.user.repository.RoleRepository;
import com.example.CRM.user.repository.UserRepository;
import com.example.CRM.user.service.UserService;

@SpringBootTest
class CrmApplicationTests {
	@MockBean UserRepository userRepository;
	@MockBean RoleRepository roleRepository;
	@MockBean UserService userService;

	@Test
	void contextLoads() {
	}

}
