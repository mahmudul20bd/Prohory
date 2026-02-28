package com.cyberaware.backend.service;

import com.cyberaware.backend.util.UrlExtractor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

@Service
public class SpamDetector {

    @Autowired
    private ExternalScannerService externalScannerService;

    // ==========================================
    // 📩 ১. Text/SMS Scanning Method
    // ==========================================
    public String checkForScam(String message) {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("🛡️ PROHORY ENGINE v4.0 - ALGORITHMIC DEFENSE ACTIVE");
            System.out.println("📩 INCOMING TEXT: " + message);

            // ১. URL Extraction & Unshortening
            List<String> rawUrls = UrlExtractor.extractUrls(message);
            List<String> finalUrls = new ArrayList<>();
            for (String url : rawUrls) {
                finalUrls.add(externalScannerService.expandUrl(url));
            }

            // ২. Get AI Decision
            CompletableFuture<String> aiFuture = externalScannerService.getAiDecision(message);
            String aiLabel = aiFuture.join().toUpperCase();

            int totalRiskScore = 0;
            switch (aiLabel) {
                case "DANGER": totalRiskScore += 10; break;
                case "SPAM":   totalRiskScore += 6;  break;
                case "PROMO":  totalRiskScore += 3;  break;
            }

            System.out.println("\n🤖 [AI Component]: Predicted Label -> " + aiLabel);

            // ৩. Process Each URL in Parallel
            for (String url : finalUrls) {
                System.out.println("\n🌐 Analyzing URL: " + url);

                // 🔥 ALL SCANNERS TRIGGERED IN PARALLEL
                CompletableFuture<Boolean> gsbFuture = externalScannerService.checkGoogleSafeBrowsing(url);
                CompletableFuture<Boolean> vtFuture = externalScannerService.checkVirusTotal(url);
                CompletableFuture<Boolean> fileFuture = externalScannerService.checkDeadLinkOrMaliciousFile(url);
                CompletableFuture<Integer> domainRiskFuture = externalScannerService.checkDomainRisk(url);
                CompletableFuture<Integer> algoFuture = externalScannerService.calculateAdvancedDomainRisk(url, message);
                CompletableFuture<Integer> whoisFuture = externalScannerService.checkDomainAgeRisk(url);

                // 1. Global Intel (GSB/VT)
                if (gsbFuture.join() || vtFuture.join()) {
                    System.out.println("🚨 [Global Intel]: Blacklisted URL found!");
                    totalRiskScore += 20;
                }

                // 2. Malicious File Check (.apk, .exe)
                if (fileFuture.join()) {
                    System.out.println("📁 [File Scan]: Malicious/Dead File Detected!");
                    totalRiskScore += 10;
                }

                // 3. IP Address & Structure Check
                int domainRisk = domainRiskFuture.join();
                if (domainRisk > 0) System.out.println("⚠️ [Structure Risk]: +" + domainRisk + " points (IP/Subdomain)");
                totalRiskScore += domainRisk;

                // 4. Smart Brand & TLD Check (.xyz, etc)
                // আগের লাইনটি মুছে এটি দিন:
                int brandRisk = externalScannerService.checkBrandAndTldRisk(url, message, aiLabel);
                if (brandRisk > 0) System.out.println("🚩 [Brand/TLD Risk]: +" + brandRisk + " points");
                totalRiskScore += brandRisk;

                // 5. Behavioral/Entropy Check
                int algoRisk = algoFuture.join();
                if (algoRisk > 0) System.out.println("🧠 [Behavioral Score]: +" + algoRisk + " points");
                totalRiskScore += algoRisk;

                // 6. WHOIS Check
                int whoisRisk = whoisFuture.join();
                if (whoisRisk > 0) System.out.println("📅 [WHOIS Age Risk]: +" + whoisRisk + " points");
                totalRiskScore += whoisRisk;
            }

            // ৪. Final Decision Matrix
            System.out.println("\n⚖️ Final Aggregated Risk Score: " + totalRiskScore);

            String finalResult;
            if (totalRiskScore >= 12) finalResult = "DANGER";
            else if (totalRiskScore >= 7) finalResult = "SPAM";
            else if (totalRiskScore >= 4) finalResult = "PROMO";
            else finalResult = "SAFE";

            System.out.println("🎯 PROHORY FINAL DECISION: " + finalResult);
            System.out.println("=".repeat(60) + "\n");

            return finalResult;

        } catch (Exception e) {
            System.err.println("❌ Critical Error in Master Brain: " + e.getMessage());
            return "SAFE";
        }
    }

    // ==========================================
    // 📸 ২. OCR Image Scanning Method
    // ==========================================
    public String checkImageForScam(MultipartFile file) {
        try {
            System.out.println("\n" + "=".repeat(60));
            System.out.println("📸 PROHORY OCR IMAGE SCANNER INITIATED");

            // ১. Python-এ ছবি পাঠিয়ে টেক্সট ও AI রেজাল্ট নিয়ে আসা
            Map<String, Object> pythonResponse = externalScannerService.getOcrAndAiDecision(file);

            if (pythonResponse == null || !pythonResponse.containsKey("extracted_text")) {
                System.out.println("❌ Failed to process image or no text found.");
                return "SAFE";
            }

            String extractedText = (String) pythonResponse.get("extracted_text");
            // 🔥 Null Safety Fix
            String aiLabel = pythonResponse.get("label") != null ? ((String) pythonResponse.get("label")).toUpperCase() : "SAFE";

            System.out.println("📝 Extracted Text: " + extractedText);
            System.out.println("🤖 [AI Image Decision]: " + aiLabel);

            // ২. টেক্সট থেকে লিংক বের করা
            List<String> rawUrls = UrlExtractor.extractUrls(extractedText);

            if (rawUrls.isEmpty()) {
                System.out.println("✅ No URLs found in the image. Relying solely on AI.");
                System.out.println("🎯 PROHORY FINAL DECISION: " + aiLabel);
                System.out.println("=".repeat(60) + "\n");
                return aiLabel;
            }

            System.out.println("🔗 URLs Found in Image: " + rawUrls.size());

            // ৩. লিংক থাকলে আমাদের Level-5 Engine চালানো
            int totalRiskScore = 0;
            switch (aiLabel) {
                case "DANGER": totalRiskScore += 10; break;
                case "SPAM":   totalRiskScore += 6;  break;
                case "PROMO":  totalRiskScore += 3;  break;
            }

            for (String url : rawUrls) {
                String finalUrl = externalScannerService.expandUrl(url); // আনশর্ট করা
                System.out.println("\n🌐 Analyzing URL: " + finalUrl);

                // 🔥 Fake Parallelism Fixed (আসল প্যারালাল স্ক্যান)
                CompletableFuture<Boolean> gsbFuture = externalScannerService.checkGoogleSafeBrowsing(finalUrl);
                CompletableFuture<Boolean> vtFuture = externalScannerService.checkVirusTotal(finalUrl);
                CompletableFuture<Boolean> fileFuture = externalScannerService.checkDeadLinkOrMaliciousFile(finalUrl);
                CompletableFuture<Integer> domainRiskFuture = externalScannerService.checkDomainRisk(finalUrl);
                CompletableFuture<Integer> algoFuture = externalScannerService.calculateAdvancedDomainRisk(finalUrl, extractedText);
                CompletableFuture<Integer> whoisFuture = externalScannerService.checkDomainAgeRisk(finalUrl);

                // 1. Global Intel
                if (gsbFuture.join() || vtFuture.join()) {
                    System.out.println("🚨 [Global Intel]: Image contains Blacklisted URL!");
                    totalRiskScore += 20;
                }

                // 2. Malicious File Check (.apk, .exe)
                if (fileFuture.join()) {
                    System.out.println("📁 [File Scan]: Malicious/Dead File Detected in Image!");
                    totalRiskScore += 10;
                }

                // 3. IP Address & Structure Check
                int domainRisk = domainRiskFuture.join();
                if (domainRisk > 0) System.out.println("⚠️ [Structure Risk]: +" + domainRisk + " points (IP/Subdomain)");
                totalRiskScore += domainRisk;

                // 4. Smart Brand & TLD Check (.xyz, etc)
                // আগের লাইনটি মুছে এটি দিন:
                int brandRisk = externalScannerService.checkBrandAndTldRisk(finalUrl, extractedText, aiLabel);
                if (brandRisk > 0) System.out.println("🚩 [Brand/TLD Risk]: +" + brandRisk + " points");
                totalRiskScore += brandRisk;

                // 5. Behavioral/Entropy Check
                int algoRisk = algoFuture.join();
                if (algoRisk > 0) System.out.println("🧠 [Behavioral Score]: +" + algoRisk + " points");
                totalRiskScore += algoRisk;

                // 6. WHOIS Check
                int whoisRisk = whoisFuture.join();
                if (whoisRisk > 0) System.out.println("📅 [WHOIS Age Risk]: +" + whoisRisk + " points");
                totalRiskScore += whoisRisk;
            }

            // ৪. ফাইনাল ডিসিশন
            System.out.println("\n⚖️ Image Final Aggregated Risk Score: " + totalRiskScore);

            String finalResult;
            if (totalRiskScore >= 12) finalResult = "DANGER";
            else if (totalRiskScore >= 7) finalResult = "SPAM";
            else if (totalRiskScore >= 4) finalResult = "PROMO";
            else finalResult = "SAFE";

            System.out.println("🎯 PROHORY FINAL DECISION: " + finalResult);
            System.out.println("=".repeat(60) + "\n");

            return finalResult;

        } catch (Exception e) {
            System.err.println("❌ Critical Error in Image Scan: " + e.getMessage());
            return "SAFE";
        }
    }
}