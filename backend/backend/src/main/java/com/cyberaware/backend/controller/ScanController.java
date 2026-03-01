package com.cyberaware.backend.controller;

import com.cyberaware.backend.service.SpamDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scan")
public class ScanController {

    @Autowired
    private SpamDetector spamDetector;

    // 📩 ১. Text বা SMS স্ক্যান করার API (Android App থেকে কল হবে)
    @PostMapping("/text")
    public ResponseEntity<?> scanText(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        String result = spamDetector.checkForScam(message);

        Map<String, String> response = new HashMap<>();
        response.put("result", result); // SAFE, PROMO, SPAM, DANGER

        return ResponseEntity.ok(response);
    }

    // 📸 ২. Image বা Screenshot (OCR) স্ক্যান করার API (Android App থেকে কল হবে)
    @PostMapping("/image")
    public ResponseEntity<?> scanImage(@RequestParam("file") MultipartFile file) {
        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image file is empty"));
        }

        String result = spamDetector.checkImageForScam(file);

        Map<String, String> response = new HashMap<>();
        response.put("result", result); // SAFE, PROMO, SPAM, DANGER

        return ResponseEntity.ok(response);
    }
}