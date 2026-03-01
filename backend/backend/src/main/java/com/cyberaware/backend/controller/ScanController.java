package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.Report;
import com.cyberaware.backend.repository.ReportRepository;
import com.cyberaware.backend.service.SpamDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scan")
@CrossOrigin("*")
public class ScanController {

    @Autowired
    private SpamDetector spamDetector;

    @Autowired
    private ReportRepository reportRepository;

    // 📩 ১. Text বা SMS স্ক্যান করার API (Android App থেকে কল হবে)
    @PostMapping("/text")
    public ResponseEntity<?> scanText(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        String username = payload.get("username"); // 🔥 Fetching username if provided

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        String result = spamDetector.checkForScam(message);

        // 🔥 FIX: শুধুমাত্র SPAM, DANGER বা SCAM হলেই ডাটাবেসে সেভ হবে
        if (result != null && (result.toUpperCase().contains("SPAM") || result.toUpperCase().contains("DANGER") || result.toUpperCase().contains("SCAM"))) {
            try {
                Report report = new Report();
                report.setReporter(username != null ? username : "App User");
                report.setSender("Manual Text Scan");
                report.setMessage(message);
                report.setAiResult(result);
                report.setTimestamp(LocalDateTime.now());
                reportRepository.save(report);
            } catch (Exception e) {
                System.err.println("Error saving text scan report: " + e.getMessage());
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("result", result); // SAFE, PROMO, SPAM, DANGER

        return ResponseEntity.ok(response);
    }

    // 📸 ২. Image বা Screenshot (OCR) স্ক্যান করার API (Android App থেকে কল হবে)
    @PostMapping("/image")
    public ResponseEntity<?> scanImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "username", required = false) String username) { // 🔥 Username is optional

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image file is empty"));
        }

        String result = spamDetector.checkImageForScam(file);

        // 🔥 FIX: শুধুমাত্র SPAM, DANGER বা SCAM হলেই ডাটাবেসে সেভ হবে
        if (result != null && (result.toUpperCase().contains("SPAM") || result.toUpperCase().contains("DANGER") || result.toUpperCase().contains("SCAM"))) {
            try {
                Report report = new Report();
                report.setReporter(username != null ? username : "App User");
                report.setSender("OCR Image Scanner");
                report.setMessage("Image Scan: " + file.getOriginalFilename());
                report.setAiResult(result);
                report.setTimestamp(LocalDateTime.now());
                reportRepository.save(report);
            } catch (Exception e) {
                System.err.println("Error saving image scan report: " + e.getMessage());
            }
        }

        Map<String, String> response = new HashMap<>();
        response.put("result", result); // SAFE, PROMO, SPAM, DANGER

        return ResponseEntity.ok(response);
    }
}