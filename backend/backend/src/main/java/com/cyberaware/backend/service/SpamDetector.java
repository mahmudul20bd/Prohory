package com.cyberaware.backend.service;

import org.springframework.stereotype.Service;
import java.util.Arrays;
import java.util.List;

@Service
public class SpamDetector {

    // স্ক্যাম কিওয়ার্ডের লিস্ট
    private final List<String> scamKeywords = Arrays.asList(
            "win", "winner", "lottery", "prize", "cash", "urgent",
            "bank account", "verify", "password", "otp", "credit card",
            "click here", "link", "claim", "offer", "expire"
    );

    public String checkForScam(String message) {
        if (message == null || message.trim().isEmpty()) {
            return "Safe";
        }

        String lowerCaseMsg = message.toLowerCase();
        int score = 0;

        // সিম্পল স্কোরিং লজিক
        for (String keyword : scamKeywords) {
            if (lowerCaseMsg.contains(keyword)) {
                score++;
            }
        }

        // থ্রেশহোল্ড চেক
        if (score >= 2) {
            return "SCAM";
        } else if (score == 1 || lowerCaseMsg.contains("http")) {
            return "SUSPICIOUS";
        } else {
            return "Safe";
        }
    }
}