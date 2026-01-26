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


    // ✅ REGISTER
    @PostMapping("/register")
    public ResponseEntity<AuthResponse> registerUser(@RequestBody AuthRequest request) {
        if (userRepository.existsByUsername(request.getUsername())) {
            return ResponseEntity.badRequest().body(new AuthResponse(false, "Username taken!", null, null, null));
        }

        // 🔥 ডিফল্ট রোল "Agent" এবং লেভেল "Level 1" দিয়ে ইউজার তৈরি
        User newUser = new User(request.getUsername(), request.getPassword(), "Agent", "Level 1");
        userRepository.save(newUser);

        return ResponseEntity.ok(new AuthResponse(true, "Registered!", request.getUsername(), "Agent", "Level 1"));
    }

    // ✅ LOGIN
    @PostMapping("/login")
    public ResponseEntity<AuthResponse> loginUser(@RequestBody AuthRequest request) {
        User user = userRepository.findByUsername(request.getUsername()).orElse(null);

        if (user == null || !user.getPassword().equals(request.getPassword())) {
            return ResponseEntity.status(401).body(new AuthResponse(false, "Invalid Credentials", null, null, null));
        }

        // 🔥 লগইন সফল হলে ডাটাবেস থেকে রোল আর লেভেল পাঠানো হচ্ছে
        return ResponseEntity.ok(new AuthResponse(true, "Login Success", user.getUsername(), user.getRole(), user.getClearanceLevel()));
    }
}