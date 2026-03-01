package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.Report;
import com.cyberaware.backend.model.SystemLog;
import com.cyberaware.backend.repository.ReportRepository;
import com.cyberaware.backend.repository.SystemLogRepository;
import com.cyberaware.backend.service.SpamDetector;
import com.cyberaware.backend.service.EmailService;
import com.cyberaware.backend.service.NotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SystemLogRepository logRepository;

    @Autowired
    private SpamDetector spamDetector;

    @Autowired
    private EmailService emailService;

    @Autowired
    private NotificationService notificationService;

    // ==========================================================
    // ১. ম্যানুয়াল রিপোর্ট / OCR (AI স্ক্যান + ইমেল + লগ + WS)
    // ==========================================================
    @PostMapping("/report")
    public Report createReport(@RequestBody Report report, HttpServletRequest request) {

        report.setTimestamp(LocalDateTime.now());
        String aiResult = spamDetector.checkForScam(report.getMessage());
        report.setAiResult(aiResult);

        // 🔥 আপডেট: ম্যানুয়াল স্ক্যানের ক্ষেত্রে মেসেজ আর হাইড হবে না, ফুল মেসেজটাই ডাটাবেসে সেভ হবে
        Report savedReport = reportRepository.save(report);

        // 🚀 ZERO-TRUST LOCATION HANDLING
        String finalRegion;
        double confidenceScore;

        if (savedReport.getRegion() != null && !savedReport.getRegion().isEmpty() && !savedReport.getRegion().equalsIgnoreCase("UNKNOWN")) {
            finalRegion = savedReport.getRegion();
            confidenceScore = 0.95;
        } else {
            String clientIp = getClientIp(request);
            finalRegion = getRegionFromIp(clientIp);
            confidenceScore = "UNKNOWN".equals(finalRegion) ? 0.0 : 0.60;
        }

        // 🚀 THREAT HEAT INDEX (+1, +3, +5)
        int heatPoints = 0;
        String threatType = "Safe Scan";

        if ("DANGER".equalsIgnoreCase(aiResult) || "SPAM".equalsIgnoreCase(aiResult) || "SCAM".equalsIgnoreCase(aiResult)) {
            // NullPointerException এড়ানোর জন্য চেক
            String msgLow = savedReport.getMessage() != null ? savedReport.getMessage().toLowerCase() : "";

            if (msgLow.contains("apk") || msgLow.contains("install") || msgLow.contains("malware")) {
                heatPoints = 5;
                threatType = "Malware Payload";
            } else if (msgLow.contains("bit.ly") || msgLow.contains("http") || msgLow.contains("lottery") || msgLow.contains("win")) {
                heatPoints = 3;
                threatType = "Phishing Link";
            } else {
                heatPoints = 1;
                threatType = "General Spam";
            }

            emailService.sendAlertEmail(savedReport.getSender(), savedReport.getMessage(), aiResult);
            notificationService.sendGlobalAlert("CRITICAL THREAT DETECTED", "User " + savedReport.getSender() + " encountered a " + aiResult + " message.", aiResult, savedReport);

            String logDetails = String.format("AI flagged %s. Device: %s (OS: %s). Location Confidence: %.2f", aiResult, savedReport.getDeviceModel(), savedReport.getOsVersion(), confidenceScore);
            logRepository.save(new SystemLog("THREAT_DETECTED", savedReport.getSender(), logDetails));

            notificationService.sendMapTelemetryAlert(finalRegion, true, threatType, confidenceScore, heatPoints);
        } else {
            notificationService.sendGlobalAlert("NEW SAFE REPORT", "User " + savedReport.getSender() + " submitted a SAFE message.", aiResult, savedReport);
            logRepository.save(new SystemLog("SAFE_SCAN", savedReport.getSender(), "Safe content scanned."));
            notificationService.sendMapTelemetryAlert(finalRegion, false, "Safe Scan", confidenceScore, 0);
        }

        return savedReport;
    }

    // ==========================================================
    // ২. অটোমেটিক ব্যাকগ্রাউন্ড SMS স্ক্যানার (App Telemetry)
    // ==========================================================
    @PostMapping("/scan/sms")
    public ResponseEntity<Map<String, Object>> scanSmsOnline(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String messageBody = payload.getOrDefault("message", "");
        String region = payload.getOrDefault("region", "UNKNOWN");
        String username = payload.getOrDefault("username", "SYSTEM_AUTO");
        String sender = payload.getOrDefault("sender", "Unknown");

        // 🚀 Zero-Trust Location
        String finalRegion;
        double confidenceScore;

        if (!region.isEmpty() && !region.equalsIgnoreCase("UNKNOWN")) {
            finalRegion = region;
            confidenceScore = 0.95;
        } else {
            String clientIp = getClientIp(request);
            finalRegion = getRegionFromIp(clientIp);
            confidenceScore = "UNKNOWN".equals(finalRegion) ? 0.0 : 0.60;
        }

        String aiResult = spamDetector.checkForScam(messageBody);
        boolean isSpam = "DANGER".equalsIgnoreCase(aiResult) || "SPAM".equalsIgnoreCase(aiResult) || "SCAM".equalsIgnoreCase(aiResult);

        // 🔥 FIX: ড্যাশবোর্ডে ডাটা দেখানোর জন্য Safe এবং Spam দুটোই সেভ হবে (প্রাইভেসি বজায় রেখে)
        try {
            Report report = new Report();
            report.setReporter(username);
            report.setAiResult(aiResult);
            report.setTimestamp(LocalDateTime.now());
            report.setRegion(finalRegion);

            if (isSpam) {
                report.setSender(sender);
                report.setMessage(messageBody); // স্প্যাম হলে অরিজিনাল ডাটা সেভ হবে
            } else {
                report.setSender("Private SMS");
                report.setMessage("Safe Background SMS (Private)"); // সেফ হলে প্রাইভেট টেক্সট সেভ হবে
            }
            reportRepository.save(report);

        } catch (Exception e) {
            System.err.println("Error saving background SMS report: " + e.getMessage());
        }

        if (isSpam) {
            int heatPoints = 3;
            String msgLow = messageBody.toLowerCase();
            if (msgLow.contains("apk") || msgLow.contains("install") || msgLow.contains("malware")) heatPoints = 5;
            else if (!msgLow.contains("bit.ly") && !msgLow.contains("http") && !msgLow.contains("lottery")) heatPoints = 1;

            notificationService.sendMapTelemetryAlert(finalRegion, true, "Phishing SMS Detected", confidenceScore, heatPoints);
            logRepository.save(new SystemLog("TELEMETRY_ALERT", username, "Spam blocked from " + finalRegion + ". Confidence: " + confidenceScore));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isSpam", isSpam);
        response.put("threatLevel", aiResult);
        response.put("message", "Message scanned successfully");

        return ResponseEntity.ok(response);
    }

    // --- Helper Methods ---
    private String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        }
        return ip.split(",")[0];
    }

    private String getRegionFromIp(String ip) {
        try {
            if ("127.0.0.1".equals(ip) || "0:0:0:0:0:0:0:1".equals(ip)) return "UNKNOWN";

            org.springframework.web.client.RestTemplate restTemplate = new org.springframework.web.client.RestTemplate();
            String response = restTemplate.getForObject("http://ip-api.com/json/" + ip, String.class);

            ObjectMapper mapper = new ObjectMapper();
            JsonNode json = mapper.readTree(response);

            if ("success".equals(json.path("status").asText())) {
                return json.path("regionName").asText().replace(" Division", "").trim();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return "UNKNOWN";
    }

    @GetMapping("/reports")
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @GetMapping("/reports/stats")
    public List<Map<String, Object>> getReportStats() {
        return reportRepository.findAll().stream()
                .filter(r -> r.getTimestamp() != null)
                .collect(Collectors.groupingBy(
                        r -> r.getTimestamp().toLocalDate().toString(),
                        Collectors.counting()
                ))
                .entrySet().stream()
                .map(e -> {
                    Map<String, Object> map = new HashMap<>();
                    map.put("date", e.getKey());
                    map.put("count", e.getValue());
                    return map;
                })
                .sorted(Comparator.comparing(m -> (String) m.get("date")))
                .collect(Collectors.toList());
    }

    @GetMapping("/reports/user")
    public ResponseEntity<List<Report>> getUserReports(@RequestParam String username) {
        List<Report> userReports = reportRepository.findAll().stream()
                .filter(r -> username.equalsIgnoreCase(r.getReporter()))
                .collect(Collectors.toList());

        return ResponseEntity.ok(userReports);
    }

    @PutMapping("/report/{id}")
    public ResponseEntity<Report> updateReportStatus(@PathVariable Long id, @RequestParam String status) {
        return reportRepository.findById(id).map(report -> {
            report.setAiResult(status);
            logRepository.save(new SystemLog("REPORT_UPDATE", "EXPERT", "Report ID #" + id + " status changed to: " + status));
            return ResponseEntity.ok(reportRepository.save(report));
        }).orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/reports/dashboard-stats")
    public ResponseEntity<Map<String, Integer>> getDashboardStats() {
        List<Report> allReports = reportRepository.findAll();
        int totalScans = allReports.size();
        int blockedScams = 0;
        int safeLinks = 0;

        for (Report r : allReports) {
            String aiResult = r.getAiResult();
            if (aiResult != null) {
                String aiUpper = aiResult.toUpperCase();
                if (aiUpper.contains("DANGER") || aiUpper.contains("SPAM") || aiUpper.contains("SCAM")) {
                    blockedScams++;
                } else if (aiUpper.contains("SAFE") || aiUpper.contains("VERIFIED") || aiUpper.contains("CLEAN")) {
                    safeLinks++;
                }
            }
        }

        Map<String, Integer> stats = new HashMap<>();
        stats.put("totalScans", totalScans);
        stats.put("blockedScams", blockedScams);
        stats.put("safeLinks", safeLinks);

        return ResponseEntity.ok(stats);
    }
}