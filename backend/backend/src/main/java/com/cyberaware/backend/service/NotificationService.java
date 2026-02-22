package com.cyberaware.backend.service;

import com.cyberaware.backend.model.Report;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;

@Service
public class NotificationService {

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // ✅ Alert + Report object push
    public void sendGlobalAlert(String title, String message, String type, Report report) {

        Map<String, Object> payload = new HashMap<>();
        payload.put("title", title);
        payload.put("message", message);
        payload.put("type", type);
        payload.put("timestamp", java.time.LocalDateTime.now().toString());
        payload.put("report", report);

        messagingTemplate.convertAndSend("/topic/alerts", (Object) payload);
        System.out.println("📢 WebSocket Alert Sent: " + title);
    }

    // 🔥 NEW: Map Telemetry এর জন্য নতুন মেথড (Error Fixed)
    // 🔥 NotificationService.java এর ভেতরের Map Telemetry মেথডটি আপডেট করুন
    public void sendMapTelemetryAlert(String region, boolean isSpam, String type, double confidence, int heatPoints) {

        Map<String, Object> mapAlert = new HashMap<>();
        mapAlert.put("isSpam", isSpam);
        mapAlert.put("region", region);
        mapAlert.put("type", type);
        mapAlert.put("confidence", confidence); // 0.95 or 0.60
        mapAlert.put("heatPoints", heatPoints); // +1, +3, or +5
        mapAlert.put("timestamp", java.time.LocalDateTime.now().toString());

        messagingTemplate.convertAndSend("/topic/alerts", (Object) mapAlert);
        System.out.println("📍 Map Telemetry: " + region + " | Confidence: " + confidence + " | Points: +" + heatPoints);
    }
}