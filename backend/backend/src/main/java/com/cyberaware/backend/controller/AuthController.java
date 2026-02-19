package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.SystemLog;
import com.cyberaware.backend.model.User;
import com.cyberaware.backend.repository.SystemLogRepository;
import com.cyberaware.backend.repository.UserRepository;
import com.cyberaware.backend.util.JwtUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private SystemLogRepository logRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private JwtUtil jwtUtil;

    // ১. ইউজার রেজিস্ট্রেশন (Auto Agent ID Generation)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username is already taken!"));
        }

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("Agent"); // Default Prohory Role
        }

        // 🔥 NEW: Generate Unique Prohory Agent ID (e.g., PRH-A1B2C)
        String generatedAgentId = "PRH-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        user.setAgentId(generatedAgentId);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setClearanceLevel("Level 1");

        User savedUser = userRepository.save(user);

        logRepository.save(new SystemLog(
                "REGISTRATION",
                savedUser.getUsername(),
                "New Agent initialized. Assigned ID: " + savedUser.getAgentId()
        ));

        // Return JSON with generated Agent ID
        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully!");
        response.put("agentId", savedUser.getAgentId());

        return ResponseEntity.ok(response);
    }

    // ২. ইউজার লগইন (Login with AgentID, Email, or Username)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String loginId = credentials.get("username"); // Ete AgentID/Email/Username jekono kisu thakte pare
        String password = credentials.get("password");

        // 🔥 NEW: Check all three columns
        Optional<User> userOpt = userRepository.findByUsernameOrEmailOrAgentId(loginId, loginId, loginId);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            User user = userOpt.get();

            String token = jwtUtil.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("agentId", user.getAgentId()); // Send Agent ID back
            response.put("role", user.getRole());
            response.put("message", "Authentication Successful");

            logRepository.save(new SystemLog(
                    "LOGIN",
                    user.getUsername(),
                    "Agent " + user.getAgentId() + " accessed the secure gateway."
            ));

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid Credentials"));
    }
}