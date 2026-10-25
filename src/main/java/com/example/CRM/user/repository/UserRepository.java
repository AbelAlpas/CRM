package com.example.CRM.user.repository;

import com.example.CRM.user.entity.User;
import java.util.Optional;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmailIgnoreCase(String email);
    boolean existsByEmailIgnoreCase(String email);
    List<User> findAllByEnabledTrueOrderByFirstNameAscLastNameAsc();
}
