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

    // ১. ইউজার রেজিস্ট্রেশন (উইথ লগ)
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody User user) {
        if (userRepository.findByUsername(user.getUsername()).isPresent()) {
            return ResponseEntity.badRequest().body("Error: Username is already taken!");
        }

        if (user.getRole() == null || user.getRole().isEmpty()) {
            user.setRole("User");
        }

        user.setPassword(passwordEncoder.encode(user.getPassword()));
        user.setClearanceLevel("Level 1");

        User savedUser = userRepository.save(user);

        // সিস্টেম লগ জেনারেট
        logRepository.save(new SystemLog(
                "REGISTRATION",
                savedUser.getUsername(),
                "New account initialized with role: " + savedUser.getRole()
        ));

        return ResponseEntity.ok("User registered successfully!");
    }

    // ২. ইউজার লগইন (উইথ রিয়েল JWT এবং লগ)
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody Map<String, String> credentials) {
        String username = credentials.get("username");
        String password = credentials.get("password");

        Optional<User> userOpt = userRepository.findByUsername(username);

        if (userOpt.isPresent() && passwordEncoder.matches(password, userOpt.get().getPassword())) {
            User user = userOpt.get();

            // 🔥 আসল JWT টোকেন জেনারেট করা হচ্ছে
            String token = jwtUtil.generateToken(user.getUsername());

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("token", token);
            response.put("username", user.getUsername());
            response.put("role", user.getRole());
            response.put("message", "Authentication Successful");

            // লগইন ইভেন্ট লগে সেভ
            logRepository.save(new SystemLog(
                    "LOGIN",
                    user.getUsername(),
                    "User accessed the secure gateway with JWT authentication."
            ));

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(401).body(Map.of("success", false, "message", "Invalid Credentials"));
    }
}