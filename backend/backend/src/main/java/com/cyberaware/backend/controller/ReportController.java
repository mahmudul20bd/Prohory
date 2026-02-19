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

    // ==========================================================
    // ১. নতুন রিপোর্ট তৈরি (AI স্ক্যান + ইমেল অ্যালার্ট + লগ + WS)
    // ==========================================================
    @PostMapping("/report")
    public Report createReport(@RequestBody Report report) {

        // টাইমস্ট্যাম্প সেট করা
        report.setTimestamp(LocalDateTime.now());

        // AI সার্ভিস থেকে রেজাল্ট নিয়ে আসা
        String aiResult = spamDetector.checkForScam(report.getMessage());
        report.setAiResult(aiResult);

        // ✅ প্রথমে রিপোর্ট DB তে save করা
        Report savedReport = reportRepository.save(report);

        // যদি বিপজ্জনক (DANGER/SPAM/SCAM) কিছু ডিটেক্ট হয়
        if ("DANGER".equalsIgnoreCase(aiResult)
                || "SPAM".equalsIgnoreCase(aiResult)
                || "SCAM".equalsIgnoreCase(aiResult)) {

            // ক. ইমেল অ্যালার্ট পাঠানো
            emailService.sendAlertEmail(
                    savedReport.getSender(),
                    savedReport.getMessage(),
                    aiResult
            );

            // খ. রিয়েল-টাইম ড্যাশবোর্ড অ্যালার্ট (WebSocket) পাঠানো
            // 🔥 FIX: payload এর সাথে full report object পাঠানো হবে
            notificationService.sendGlobalAlert(
                    "CRITICAL THREAT DETECTED",
                    "User " + savedReport.getSender() + " encountered a " + aiResult + " message.",
                    aiResult,
                    savedReport
            );

            // গ. সিস্টেম লগে সিকিউরিটি ইভেন্ট রেকর্ড করা
            logRepository.save(new SystemLog(
                    "THREAT_DETECTED",
                    savedReport.getSender(),
                    "AI flagged a message as " + aiResult + ": " + savedReport.getMessage()
            ));
        }

        // ✅ SAFE হলেও expert dashboard instantly update পাবে
        else {
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
    // ২. অ্যাডমিন ও এক্সপার্ট: সব রিপোর্ট দেখার জন্য
    // ==========================================================
    @GetMapping("/reports")
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    // ==========================================================
    // ৩. গ্রাফের জন্য স্ট্যাটিস্টিকস ডাটা (Date-wise count)
    // ==========================================================
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

    // ==========================================================
    // ৪. ইউজারের নিজস্ব রিপোর্ট দেখা
    // ==========================================================
    @GetMapping("/reports/user")
    public List<Report> getUserReports(@RequestParam String username) {
        return reportRepository.findBySender(username);
    }

    // ==========================================================
    // ৫. রিপোর্টের স্ট্যাটাস বা AI রেজাল্ট আপডেট করা
    // ==========================================================
    @PutMapping("/report/{id}")
    public ResponseEntity<Report> updateReportStatus(
            @PathVariable Long id,
            @RequestParam String status
    ) {
        return reportRepository.findById(id).map(report -> {

            report.setAiResult(status);

            // আপডেট করার ইভেন্ট লগে রেকর্ড করা
            logRepository.save(new SystemLog(
                    "REPORT_UPDATE",
                    "EXPERT",
                    "Report ID #" + id + " status changed to: " + status
            ));

            return ResponseEntity.ok(reportRepository.save(report));

        }).orElse(ResponseEntity.notFound().build());
    }

    // 🔥 ৬. (NEW) ড্যাশবোর্ডের জন্য রিয়েল-টাইম স্ট্যাটিস্টিকস
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
                // বিপদজনক কি-ওয়ার্ড চেক
                if (aiUpper.contains("DANGER") || aiUpper.contains("SPAM") || aiUpper.contains("SCAM")) {
                    blockedScams++;
                }
                // নিরাপদ কি-ওয়ার্ড চেক
                else if (aiUpper.contains("SAFE") || aiUpper.contains("VERIFIED") || aiUpper.contains("CLEAN")) {
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
