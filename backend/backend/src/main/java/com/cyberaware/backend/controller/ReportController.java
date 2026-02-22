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
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api")
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

    // 🔥 ম্যাপে রিয়েল-টাইম ডেটা পাঠানোর জন্য এটি যোগ করা হলো
    @Autowired
    private SimpMessagingTemplate messagingTemplate;


    // ==========================================================
    // ১. নতুন রিপোর্ট তৈরি (AI স্ক্যান + ইমেল অ্যালার্ট + লগ + WS)
    // ==========================================================
    @PostMapping("/report")
    public Report createReport(@RequestBody Report report) {

        report.setTimestamp(LocalDateTime.now());
        String aiResult = spamDetector.checkForScam(report.getMessage());
        report.setAiResult(aiResult);

        Report savedReport = reportRepository.save(report);

        if ("DANGER".equalsIgnoreCase(aiResult)
                || "SPAM".equalsIgnoreCase(aiResult)
                || "SCAM".equalsIgnoreCase(aiResult)) {

            emailService.sendAlertEmail(savedReport.getSender(), savedReport.getMessage(), aiResult);

            notificationService.sendGlobalAlert(
                    "CRITICAL THREAT DETECTED",
                    "User " + savedReport.getSender() + " encountered a " + aiResult + " message.",
                    aiResult,
                    savedReport
            );

            logRepository.save(new SystemLog(
                    "THREAT_DETECTED",
                    savedReport.getSender(),
                    "AI flagged a message as " + aiResult + ": " + savedReport.getMessage()
            ));
        } else {
            notificationService.sendGlobalAlert(
                    "NEW SAFE REPORT",
                    "User " + savedReport.getSender() + " submitted a SAFE message.",
                    aiResult,
                    savedReport
            );
        }
        return savedReport;
    }

    // ==========================================================
    // 🔥 ২. (NEW) App Telemetry & Spam Scanner Endpoint
    // অ্যাপ থেকে SMS স্ক্যান করলে এটি ডিভিশন ট্র্যাক করে ম্যাপে পাঠাবে!
    // ==========================================================
    @PostMapping("/scan/sms")
    public ResponseEntity<Map<String, Object>> scanSmsOnline(@RequestBody Map<String, String> payload) {
        String messageBody = payload.getOrDefault("message", "");
        // অ্যাপ থেকে আসা ইউজারের লোকেশন (ডিফল্ট ঢাকা)
        String region = payload.getOrDefault("region", "Dhaka");

        // AI স্ক্যান
        String aiResult = spamDetector.checkForScam(messageBody);
        boolean isSpam = "DANGER".equalsIgnoreCase(aiResult) || "SPAM".equalsIgnoreCase(aiResult) || "SCAM".equalsIgnoreCase(aiResult);

        if (isSpam) {
            // 🚀 ম্যাপের জন্য WebSocket ব্রডকাস্ট (Telemetry)
            Map<String, Object> mapAlert = new HashMap<>();
            mapAlert.put("isSpam", true);
            mapAlert.put("region", region);
            mapAlert.put("type", "Phishing SMS Detected");

            // ৫. ম্যাপে রিয়েল-টাইম রেড অ্যালার্ট পাঠিয়ে দেওয়া! (No Red Error)
            notificationService.sendMapTelemetryAlert(region, true, "Phishing SMS Detected");

            // সার্ভার লগে ইভেন্ট সেভ করা
            logRepository.save(new SystemLog(
                    "TELEMETRY_ALERT",
                    "SYSTEM_AUTO",
                    "Spam blocked in " + region + " region."
            ));
        }

        Map<String, Object> response = new HashMap<>();
        response.put("isSpam", isSpam);
        response.put("threatLevel", aiResult);
        response.put("message", "Message scanned successfully");

        return ResponseEntity.ok(response);
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
    public List<Report> getUserReports(@RequestParam String username) {
        return reportRepository.findBySender(username);
    }

    @PutMapping("/report/{id}")
    public ResponseEntity<Report> updateReportStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return reportRepository.findById(id).map(report -> {
            report.setAiResult(status);
            logRepository.save(new SystemLog(
                    "REPORT_UPDATE",
                    "EXPERT",
                    "Report ID #" + id + " status changed to: " + status
            ));
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