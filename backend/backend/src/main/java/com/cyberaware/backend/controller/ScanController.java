package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.Report;
import com.cyberaware.backend.model.SystemLog;
import com.cyberaware.backend.repository.ReportRepository;
import com.cyberaware.backend.repository.SystemLogRepository;
import com.cyberaware.backend.service.NotificationService;
import com.cyberaware.backend.service.SpamDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import jakarta.servlet.http.HttpServletRequest;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/scan")
// 🔥 CORS এরর এড়াতে @CrossOrigin("*") বাদ দেওয়া হয়েছে, কারণ আপনার গ্লোবাল কনফিগারেশন আছে
public class ScanController {

    @Autowired
    private SpamDetector spamDetector;

    @Autowired
    private ReportRepository reportRepository;

    @Autowired
    private SystemLogRepository logRepository;

    // 🔥 ম্যাপে রিয়েল-টাইম সিগন্যাল পাঠানোর জন্য NotificationService অ্যাড করা হলো
    @Autowired
    private NotificationService notificationService;

    @PostMapping("/text")
    public ResponseEntity<?> scanText(@RequestBody Map<String, String> payload, HttpServletRequest request) {
        String message = payload.get("message");
        String username = payload.get("username");
        String region = payload.get("region"); // অ্যাপ থেকে পাঠানো ডিভিশন

        if (message == null || message.trim().isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Message cannot be empty"));
        }

        String result = spamDetector.checkForScam(message);
        String finalUsername = (username != null && !username.isEmpty()) ? username : "App User";

        // 🚀 Location Tracker: অ্যাপ থেকে ডিভিশন পেলে সেটা ব্যবহার করবে, না পেলে IP থেকে নেবে
        String finalRegion;
        double confidenceScore;

        if (region != null && !region.trim().isEmpty() && !region.equalsIgnoreCase("UNKNOWN") && !region.equalsIgnoreCase("Select Division")) {
            finalRegion = region;
            confidenceScore = 0.95; // ইউজার নিজে সিলেক্ট করলে কনফিডেন্স বেশি
        } else {
            String clientIp = getClientIp(request);
            finalRegion = getRegionFromIp(clientIp);
            confidenceScore = "UNKNOWN".equals(finalRegion) ? 0.0 : 0.85;
        }

        try {
            Report report = new Report();
            report.setSender(finalUsername);
            report.setAiResult(result);
            report.setTimestamp(LocalDateTime.now());
            report.setRegion(finalRegion); // 🔥 ডাটাবেসে ডিভিশন সেভ করা হলো

            int heatPoints = 0;

            if (result != null && result.equalsIgnoreCase("SAFE")) {
                report.setMessage("Safe Text Scan (Private)");
                logRepository.save(new SystemLog("SAFE_SCAN", finalUsername, "Safe text scanned."));

                // ম্যাপে সেফ স্ক্যানের সিগন্যাল
                notificationService.sendMapTelemetryAlert(finalRegion, false, "Safe Scan", confidenceScore, 0);
            } else {
                report.setMessage(message); // Threat বা Spam হলে অরিজিনাল মেসেজ সেভ হবে
                String logAction = "THREAT_" + result.toUpperCase();
                logRepository.save(new SystemLog(logAction, finalUsername, "Text scan resulted in: " + result));

                // 🔥 ম্যাপে স্প্যাম/ডেঞ্জার সিগন্যাল ব্রডকাস্ট করা হচ্ছে
                heatPoints = result.equalsIgnoreCase("DANGER") ? 5 : 3;
                notificationService.sendMapTelemetryAlert(finalRegion, true, result + " text detected", confidenceScore, heatPoints);
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
            @RequestParam(value = "username", required = false) String username,
            @RequestParam(value = "localText", required = false) String localText,
            @RequestParam(value = "region", required = false) String region, // অ্যাপ থেকে পাঠানো ডিভিশন
            HttpServletRequest request) {

        if (file.isEmpty()) {
            return ResponseEntity.badRequest().body(Map.of("error", "Image file is empty"));
        }

        String result = spamDetector.checkImageForScam(file, localText);
        String finalUsername = (username != null && !username.isEmpty()) ? username : "App User";

        // 🚀 Location Tracker: অ্যাপ থেকে ডিভিশন পেলে সেটা ব্যবহার করবে, না পেলে IP থেকে নেবে
        String finalRegion;
        double confidenceScore;

        if (region != null && !region.trim().isEmpty() && !region.equalsIgnoreCase("UNKNOWN") && !region.equalsIgnoreCase("Select Division")) {
            finalRegion = region;
            confidenceScore = 0.95;
        } else {
            String clientIp = getClientIp(request);
            finalRegion = getRegionFromIp(clientIp);
            confidenceScore = "UNKNOWN".equals(finalRegion) ? 0.0 : 0.85;
        }

        try {
            Report report = new Report();
            report.setSender(finalUsername);
            report.setAiResult(result);
            report.setTimestamp(LocalDateTime.now());
            report.setRegion(finalRegion); // 🔥 ডাটাবেসে ডিভিশন সেভ করা হলো

            int heatPoints = 0;

            if (result != null && result.equalsIgnoreCase("SAFE")) {
                report.setMessage("Safe Image Scan (Private)");
                logRepository.save(new SystemLog("SAFE_SCAN", finalUsername, "Safe image scanned."));

                // ম্যাপে সেফ স্ক্যানের সিগন্যাল
                notificationService.sendMapTelemetryAlert(finalRegion, false, "Safe Image Scan", confidenceScore, 0);
            } else {
                // Threat হলে Android এর লোকাল টেক্সট (লিংক) ডাটাবেসে সেভ হবে
                String savedMsg = (localText != null && !localText.isEmpty()) ? "Image Scan (Link: " + localText + ")" : "Image Scan: " + file.getOriginalFilename();
                report.setMessage(savedMsg);
                String logAction = "THREAT_" + result.toUpperCase();
                logRepository.save(new SystemLog(logAction, finalUsername, "Image scan resulted in: " + result));

                // 🔥 ম্যাপে থ্রেট সিগন্যাল ব্রডকাস্ট করা হচ্ছে
                heatPoints = result.equalsIgnoreCase("DANGER") ? 5 : 3;
                notificationService.sendMapTelemetryAlert(finalRegion, true, result + " image detected", confidenceScore, heatPoints);
            }

            reportRepository.save(report);

        } catch (Exception e) {
            System.err.println("Error saving image scan report: " + e.getMessage());
        }

        Map<String, String> response = new HashMap<>();
        response.put("result", result);
        response.put("extractedText", localText != null ? localText : "");
        return ResponseEntity.ok(response);
    }

    // ==========================================
    // 🔥 IP থেকে লোকেশন বের করার Helper Methods
    // ==========================================
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
}