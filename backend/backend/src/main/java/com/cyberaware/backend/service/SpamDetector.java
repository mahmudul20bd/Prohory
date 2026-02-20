package com.cyberaware.backend.service;

import java.util.HashMap;
import java.util.Map;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class SpamDetector {

    // Python API URL (লোকালহোস্ট ৫০০০)
    private final String PYTHON_API_URL = "https://mahmudul20ed-cyberaware-ai.hf.space/predict";

    public String checkForScam(String message) {
        try {
            // 1. RestTemplate তৈরি
            RestTemplate restTemplate = new RestTemplate();

            // 2. ডাটা রেডি করা (JSON)
            Map<String, String> requestBody = new HashMap<>();
            requestBody.put("message", message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<Map<String, String>> entity = new HttpEntity<>(requestBody, headers);

            // 3. Python API কল করা
            ResponseEntity<Map> response = restTemplate.postForEntity(PYTHON_API_URL, entity, Map.class);

            // 4. রেজাল্ট রিসিভ করা
            if (response.getBody() != null) {
                String result = (String) response.getBody().get("result");
                // চাইলে কনফিডেন্স স্কোরও লগ করতে পারেন
                // Double confidence = (Double) response.getBody().get("confidence");
                return result;
            }

        } catch (Exception e) {
            System.err.println("❌ AI Service Down! Fallback to local logic.");
            e.printStackTrace();
            // পাইথন সার্ভার বন্ধ থাকলে ফলব্যাক লজিক
            if (message.toLowerCase().contains("urgent") || message.toLowerCase().contains("win")) {
                return "SUSPICIOUS";
            }
        }

        return "Safe"; // ডিফল্ট সে
    }
}