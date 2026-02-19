package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.User;
import com.cyberaware.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder; // 🔥 এটি ইমপোর্ট করতে হবে
import org.springframework.web.bind.annotation.*;
import java.security.Principal; // ইমপোর্ট নিশ্চিত করুন

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder; // 🔥 পাসওয়ার্ড এনক্রিপশনের জন্য ইনজেক্ট

    // ১. সব ইউজারদের লিস্ট দেখা (Admin Only)
    @GetMapping
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    // ২. ইউজারের রোল চেঞ্জ করা (User -> Expert -> Admin)
    @PutMapping("/{id}/role")
    public ResponseEntity<User> updateUserRole(@PathVariable Long id, @RequestBody Map<String, String> payload) {
        return userRepository.findById(id).map(user -> {
            String newRole = payload.get("role");
            user.setRole(newRole);
            return ResponseEntity.ok(userRepository.save(user));
        }).orElse(ResponseEntity.notFound().build());
    }

    // ৩. ইউজার ডিলিট করা
    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            userRepository.delete(user);
            return ResponseEntity.ok().build();
        }).orElse(ResponseEntity.notFound().build());
    }

    // 🔥 ৪. (NEW) নিজের প্রোফাইল আপডেট করা (Email Update) -- [এই মেথডটি মিসিং ছিল]
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(@RequestParam String username, @RequestBody Map<String, String> payload) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        if (payload.containsKey("email")) {
            user.setEmail(payload.get("email"));
        }

        userRepository.save(user);
        return ResponseEntity.ok("Profile Updated Successfully!");
    }

    // 🔥 ৫. (NEW) পাসওয়ার্ড পরিবর্তন করা -- [এই মেথডটিও মিসিং ছিল]
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(@RequestParam String username, @RequestBody Map<String, String> payload) {
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        String oldPass = payload.get("oldPassword");
        String newPass = payload.get("newPassword");

        // চেক করুন আগের পাসওয়ার্ড ঠিক আছে কিনা
        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
            return ResponseEntity.badRequest().body("Incorrect Old Password!");
        }

        // নতুন পাসওয়ার্ড সেট করুন
        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);

        return ResponseEntity.ok("Password Changed Successfully!");
    }

    // 🔥 ৬. (NEW) লগ-ইন করা ইউজারের নিজের প্রোফাইল ডেটা ফেচ করা
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) {
            return ResponseEntity.status(401).body("Unauthorized Access");
        }

        String username = principal.getName(); // JWT টোকেন থেকে নাম নিবে

        return userRepository.findByUsername(username).map(user -> {
            // শুধুমাত্র দরকারি ডেটাগুলো ম্যাপ করে পাঠানো হচ্ছে
            Map<String, String> profile = new HashMap<>();
            profile.put("username", user.getUsername());
            profile.put("role", user.getRole() != null ? user.getRole() : "User");
            profile.put("clearanceLevel", user.getClearanceLevel() != null ? user.getClearanceLevel() : "Level 1");

            return ResponseEntity.ok(profile);
        }).orElse(ResponseEntity.notFound().build());
    }
}