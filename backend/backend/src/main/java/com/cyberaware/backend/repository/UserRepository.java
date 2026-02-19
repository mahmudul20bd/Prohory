package com.cyberaware.backend.repository;

import com.cyberaware.backend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Boolean existsByUsername(String username);

    // 🔥 NEW: Username, Email or Agent ID jekono ekta diye DB theke user khuje ber korbe
    Optional<User> findByUsernameOrEmailOrAgentId(String username, String email, String agentId);
}