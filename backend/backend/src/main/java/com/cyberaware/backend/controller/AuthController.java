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

    // ১. ইউজার রেজিস্ট্রেশন
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body(Map.of("success", false, "message", "Username is already taken!"));
        }

        // 🔥 FIX: Default role ekhn theke "User" hobe (Age "Agent" chilo)
        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("User");
        }

        // Generate Unique Prohory Agent ID (e.g., PRH-A1B2C)
        String generatedAgentId = "PRH-" + UUID.randomUUID().toString().substring(0, 5).toUpperCase();
        user.setAgentId(generatedAgentId);

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setClearanceLevel("Level 1");

        User savedUser = userRepository.save(user);

        logRepository.save(new SystemLog(
                "REGISTRATION",
                savedUser.getUsername(),
                "New User initialized. Assigned ID: " + savedUser.getAgentId()
        ));

        Map<String, Object> response = new HashMap<>();
        response.put("success", true);
        response.put("message", "User registered successfully!");
        response.put("agentId", savedUser.getAgentId());

        return ResponseEntity.ok(response);
    }

    // ২. ইউজার লগইন (Login with AgentID, Email, or Username)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String loginId = credentials.get("username"); // AgentID/Email/Username
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByUsernameOrEmailOrAgentId(loginId, loginId, loginId);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            User user = userOpt.get();

            // 🔥 NEW: Check if the user is an Expert but NOT approved yet
            if ("Expert".equalsIgnoreCase(user.getRole()) && !user.isApproved()) {
                return ResponseEntity.status(403).body(Map.of(
                        "success", false,
                        "message", "Your account is pending approval by the Admin. Please wait."
                ));
            }

            String token = jwtUtil.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("agentId", user.getAgentId());
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