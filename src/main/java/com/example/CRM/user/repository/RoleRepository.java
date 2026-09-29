package com.example.CRM.user.repository;

import com.example.CRM.user.entity.Role;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Short> {
    Optional<Role> findByName(String name);
}
