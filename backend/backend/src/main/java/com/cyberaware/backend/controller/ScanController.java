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
@CrossOrigin("*")
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

        // 🔥 FIX: Manual Scan er khetre SAFE soho shobkichu save hobe
        try {
            Report report = new Report();
            report.setReporter(finalUsername);
            report.setSender("Manual Text Scan");
            report.setMessage(message);
            report.setAiResult(result);
            report.setTimestamp(LocalDateTime.now());
            reportRepository.save(report);

            String logAction = result.equalsIgnoreCase("SAFE") ? "SAFE_SCAN" : "THREAT_" + result.toUpperCase();
            logRepository.save(new SystemLog(logAction, finalUsername, "Text scan resulted in: " + result));

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

        // 🔥 FIX: Manual Scan er khetre SAFE soho shobkichu save hobe
        try {
            Report report = new Report();
            report.setReporter(finalUsername);
            report.setSender("OCR Image Scanner");
            report.setMessage("Image Scan: " + file.getOriginalFilename());
            report.setAiResult(result);
            report.setTimestamp(LocalDateTime.now());
            reportRepository.save(report);

            String logAction = result.equalsIgnoreCase("SAFE") ? "SAFE_SCAN" : "THREAT_" + result.toUpperCase();
            logRepository.save(new SystemLog(logAction, finalUsername, "Image scan resulted in: " + result));

        } catch (Exception e) {
            System.err.println("Error saving image scan report: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("result", result);

        return ResponseEntity.ok(response);
    }
}