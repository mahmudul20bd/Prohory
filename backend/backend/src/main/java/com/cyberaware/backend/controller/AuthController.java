package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.AuthRequest;
import com.cyberaware.backend.model.AuthResponse;
import com.cyberaware.backend.model.User;
import com.cyberaware.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/auth")
@CrossOrigin("*") // অ্যান্ড্রয়েড অ্যাপ থেকে এক্সেস করার জন্য জরুরি
public class AuthController {

    @Autowired
    private UserRepository userRepository;

    // ✅ 1. রেজিস্ট্রেশন API
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody AuthRequest request) {
        // ১. চেক করি ইউজার আগে থেকেই আছে কিনা
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Username is already taken!", null));
        }

        // ২. নতুন ইউজার সেভ করি
        User newUser = new User(request.getUsername(), request.getPassword());
        userRepository.save(newUser);

        return ResponseEntity.ok(new AuthResponse(true, "User registered successfully!", request.getUsername()));
    }

    // ✅ 2. লগইন API
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody AuthRequest request) {
        // ১. ইউজার ডাটাবেসে আছে কিনা চেক করি
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null) {
            return ResponseEntity.status(404).body(new AuthResponse(false, "User not found!", null));
        }

        // ২. পাসওয়ার্ড মিলছে কিনা দেখি (সিম্পল চেকিং)
        if (!user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body(new AuthResponse(false, "Invalid Credentials!", null));
        }

        // ৩. সব ঠিক থাকলে সাকসেস মেসেজ
        return ResponseEntity.ok(new AuthResponse(true, "Login Successful!", user.getUsername()));
    }
}