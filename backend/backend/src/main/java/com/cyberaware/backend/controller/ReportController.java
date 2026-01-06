package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.Report;
import com.cyberaware.backend.model.ReportRequest;
import com.cyberaware.backend.repository.ReportRepository;
import com.cyberaware.backend.service.EmailService;
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

    // ✅ 1. CREATE: ইউজার রিপোর্ট পাঠাচ্ছে (আগের কোড)
    @PostMapping("/report")
    public String receiveReport(@RequestBody ReportRequest request) {
        Report newReport = new Report(request.getSender(), request.getMessage(), "Pending Analysis");
        reportRepository.save(newReport);

        messagingTemplate.convertAndSend("/topic/alerts", "🚨 New Scam Alert: " + request.getMessage());
        emailService.sendAlertEmail(request.getMessage());

        return "Report Received Successfully!";
    }

    // ==========================================
    // 🔥 NEW: RUD OPERATIONS (Admin Features)
    // ==========================================

    // ✅ 2. READ: সব রিপোর্ট দেখা (Admin দেখবে)
    @GetMapping("/reports")
    public List<Report> getAllReports() {
        return reportRepository.findAll();
    }

    // ✅ 3. UPDATE: রিপোর্টের স্ট্যাটাস পাল্টানো (যেমন: "Pending" -> "Verified")
    @PutMapping("/report/{id}")
    public ResponseEntity<String> updateReportStatus(@PathVariable Long id, @RequestParam String status) {
        return reportRepository.findById(id).map(report -> {
            // সেটার (Setter) মেথড ব্যবহার করে স্ট্যাটাস আপডেট (আপনার মডেলে setAiResult বা setStatus থাকতে হবে)
            // আমি ধরে নিচ্ছি আপনার 'aiResult' ফিল্ডটাই স্ট্যাটাস হিসেবে কাজ করছে
            // যদি মডেলে setAiResult না থাকে, তবে Report.java তে গিয়ে অ্যাড করে নেবেন
            report.setAiResult(status);

            reportRepository.save(report);
            return ResponseEntity.ok("Status Updated to: " + status);
        }).orElse(ResponseEntity.notFound().build());
    }

    // ✅ 4. DELETE: কোনো রিপোর্ট ডিলিট করা
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