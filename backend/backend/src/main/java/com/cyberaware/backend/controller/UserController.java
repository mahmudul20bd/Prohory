package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.User;
import com.cyberaware.backend.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;
import java.security.Principal;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

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

    // 🔥 ৪. এক্সপার্ট অ্যাকাউন্ট অ্যাপ্রুভ করা (NEW API FOR ADMIN)
    @PutMapping("/{id}/approve")
    public ResponseEntity<?> approveExpert(@PathVariable Long id) {
        return userRepository.findById(id).map(user -> {
            user.setApproved(true);
            userRepository.save(user);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Expert approved successfully");

            return ResponseEntity.ok(response);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ৫. নিজের প্রোফাইল আপডেট করা (Updated to include About Me & Cover)
    @PutMapping("/profile")
    public ResponseEntity<?> updateProfile(Principal principal, @RequestBody Map<String, String> payload) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        if (payload.containsKey("email")) user.setEmail(payload.get("email"));
        if (payload.containsKey("aboutMe")) user.setAboutMe(payload.get("aboutMe"));
        if (payload.containsKey("profileImage")) user.setProfileImage(payload.get("profileImage"));
        if (payload.containsKey("coverImage")) user.setCoverImage(payload.get("coverImage"));

        userRepository.save(user);
        return ResponseEntity.ok("Profile Updated Successfully!");
    }



    // ৬. পাসওয়ার্ড পরিবর্তন করা
    @PutMapping("/change-password")
    public ResponseEntity<?> changePassword(Principal principal, @RequestBody Map<String, String> payload) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized");

        String username = principal.getName();
        User user = userRepository.findByUsername(username).orElse(null);
        if (user == null) return ResponseEntity.badRequest().body("User not found");

        String oldPass = payload.get("oldPassword");
        String newPass = payload.get("newPassword");

        if (!passwordEncoder.matches(oldPass, user.getPassword())) {
            return ResponseEntity.badRequest().body("Incorrect Old Password!");
        }

        user.setPassword(passwordEncoder.encode(newPass));
        userRepository.save(user);
        return ResponseEntity.ok("Password Changed Successfully!");
    }

    // ৭. লগ-ইন করা ইউজারের নিজের প্রোফাইল ডেটা ফেচ করা (Updated)
    @GetMapping("/me")
    public ResponseEntity<?> getCurrentUser(Principal principal) {
        if (principal == null) return ResponseEntity.status(401).body("Unauthorized Access");

        String username = principal.getName();

        return userRepository.findByUsername(username).map(user -> {
            Map<String, Object> profile = new HashMap<>();
            profile.put("username", user.getUsername());
            profile.put("email", user.getEmail());
            profile.put("role", user.getRole() != null ? user.getRole() : "User");
            profile.put("clearanceLevel", user.getClearanceLevel() != null ? user.getClearanceLevel() : "Level 1");
            profile.put("isApproved", user.isApproved());
            profile.put("specialization", user.getSpecialization());
            profile.put("aboutMe", user.getAboutMe());
            profile.put("profileImage", user.getProfileImage());
            profile.put("coverImage", user.getCoverImage());
            profile.put("averageRating", user.getAverageRating());
            profile.put("ratingCount", user.getRatingCount());

            return ResponseEntity.ok(profile);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ৮. Expert Rating System
    @PostMapping("/rate-expert")
    public ResponseEntity<?> rateExpert(@RequestBody Map<String, Object> payload) {
        String expertUsername = (String) payload.get("expertUsername");

        Object ratingObj = payload.get("rating");
        int newRating = 0;
        if (ratingObj instanceof Integer) {
            newRating = (Integer) ratingObj;
        } else if (ratingObj instanceof String) {
            newRating = Integer.parseInt((String) ratingObj);
        }

        Optional<User> expertOpt = userRepository.findByUsername(expertUsername);

        if (expertOpt.isPresent()) {
            User expert = expertOpt.get();

            int currentCount = expert.getRatingCount() != null ? expert.getRatingCount() : 0;
            double currentAvg = expert.getAverageRating() != null ? expert.getAverageRating() : 0.0;

            double totalScore = (currentAvg * currentCount) + newRating;
            currentCount += 1;
            double newAvg = totalScore / currentCount;

            expert.setRatingCount(currentCount);
            expert.setAverageRating(newAvg);

            userRepository.save(expert);

            Map<String, Object> response = new HashMap<>();
            response.put("success", true);
            response.put("message", "Rating submitted successfully!");
            response.put("newAverage", newAvg);

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.badRequest().body(Map.of("error", "Expert not found"));
    }
}