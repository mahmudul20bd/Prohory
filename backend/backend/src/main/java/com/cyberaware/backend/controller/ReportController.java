package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.Report;
import com.cyberaware.backend.model.ReportRequest;
import com.cyberaware.backend.repository.ReportRepository;
import com.cyberaware.backend.service.EmailService;
import com.cyberaware.backend.service.SpamDetector; // 🔥 Import SpamDetector
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
@CrossOrigin("*")
public class ReportController {

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Autowired
    private EmailService emailService;

    // 🔥 SpamDetector সার্ভিস ইনজেক্ট করা হলো
    @Autowired
    private SpamDetector spamDetector;

    // ✅ 1. CREATE: রিপোর্ট রিসিভ ও এনালাইসিস
    @PostMapping("/report")
    public String receiveReport(@RequestBody ReportRequest request) {

        // ১. SpamDetector ক্লাস কল করে রেজাল্ট নেওয়া
        String aiResult = spamDetector.checkForScam(request.getMessage());

        // ২. ডাটাবেসে সেভ করা (Reporter সহ)
        Report newReport = new Report(
                request.getSender(),
                request.getMessage(),
                aiResult,
                request.getReporter()
        );
        reportRepository.save(newReport);

        // ৩. অ্যালার্ট পাঠানো (যদি স্ক্যাম হয়)
        if ("SCAM".equals(aiResult) || "SUSPICIOUS".equals(aiResult)) {
            messagingTemplate.convertAndSend("/topic/alerts", "🚨 New Scam Alert: " + request.getMessage());
            // emailService.sendAlertEmail(request.getMessage());
        }

        return aiResult; // অ্যাপকে রেজাল্ট ফেরত
    }

    // ==========================================
    // 🔥 READ OPERATIONS
    // ==========================================

    @GetMapping("/reports")
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    @GetMapping("/reports/user")
    public List<Report> getUserReports(@RequestParam String username) {
        return reportRepository.findByReporter(username);
    }

    // ==========================================
    // 🔥 ADMIN OPERATIONS
    // ==========================================

    @PutMapping("/report/{id}")
    public ResponseEntity<String> updateReportStatus(@PathVariable Long id, @RequestParam String status) {
        return reportRepository.findById(id).map(report -> {
            report.setAiResult(status);
            reportRepository.save(report);
            return ResponseEntity.ok("Status Updated to: " + status);
        }).orElse(ResponseEntity.notFound().build());
    }

    @DeleteMapping("/report/{id}")
    public ResponseEntity<String> deleteReport(@PathVariable Long id) {
        if (reportRepository.existsById(id)) {
            reportRepository.deleteById(id);
            return ResponseEntity.ok("Report Deleted Successfully");
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}