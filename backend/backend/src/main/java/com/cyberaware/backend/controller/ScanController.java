package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.Report;
import com.cyberaware.backend.model.SystemLog;
import com.cyberaware.backend.repository.ReportRepository;
import com.cyberaware.backend.repository.SystemLogRepository;
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
// 🔥 CORS এরর এড়াতে @CrossOrigin("*") বাদ দেওয়া হয়েছে, কারণ আপনার গ্লোবাল কনফিগারেশন আছে
public class ScanController {

    @Autowired
    private SpamDetector spamDetector;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SystemLogRepository logRepository;

    @PostMapping("/text")
    public ResponseEntity<?> scanText(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        String username = payload.get("username");

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        String result = spamDetector.checkForScam(message);
        String finalUsername = (username != null && !username.isEmpty()) ? username : "App User";

        try {
            Report report = new Report();
            // 🔥 FIX 1: username কে sender হিসেবে সেট করা হলো, যাতে ReportController ডাটা খুঁজে পায়
            report.setSender(finalUsername);
            report.setAiResult(result);
            report.setTimestamp(LocalDateTime.now());

            // 🔥 FIX 2: Safe এবং Danger দুটোই ডাটাবেসে সেভ হবে, কিন্তু Safe হলে প্রাইভেসি ঠিক থাকবে
            if (result != null && result.equalsIgnoreCase("SAFE")) {
                report.setMessage("Safe Text Scan (Private)");
                logRepository.save(new SystemLog("SAFE_SCAN", finalUsername, "Safe text scanned."));
            } else {
                report.setMessage(message); // Threat বা Spam হলে অরিজিনাল মেসেজ সেভ হবে
                String logAction = "THREAT_" + result.toUpperCase();
                logRepository.save(new SystemLog(logAction, finalUsername, "Text scan resulted in: " + result));
            }

            reportRepository.save(report);

        } catch (Exception e) {
            System.err.println("Error saving text scan report: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("result", result);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/image")
    public ResponseEntity<?> scanImage(
            @RequestParam("file") MultipartFile file,
            @RequestParam(value = "username", required = false) String username) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image file is empty"));
        }

        String result = spamDetector.checkImageForScam(file);
        String finalUsername = (username != null && !username.isEmpty()) ? username : "App User";

        try {
            Report report = new Report();
            // 🔥 FIX 1: username কে sender হিসেবে সেট করা হলো
            report.setSender(finalUsername);
            report.setAiResult(result);
            report.setTimestamp(LocalDateTime.now());

            // 🔥 FIX 2: Safe এবং Danger দুটোই ডাটাবেসে সেভ হবে
            if (result != null && result.equalsIgnoreCase("SAFE")) {
                report.setMessage("Safe Image Scan (Private)");
                logRepository.save(new SystemLog("SAFE_SCAN", finalUsername, "Safe image scanned."));
            } else {
                report.setMessage("Image Scan: " + file.getOriginalFilename());
                String logAction = "THREAT_" + result.toUpperCase();
                logRepository.save(new SystemLog(logAction, finalUsername, "Image scan resulted in: " + result));
            }

            reportRepository.save(report);

        } catch (Exception e) {
            System.err.println("Error saving image scan report: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("result", result);
        return ResponseEntity.ok(response);
    }
}