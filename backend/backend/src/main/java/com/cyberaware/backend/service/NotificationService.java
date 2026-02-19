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

        // 🔥 Add full report data also
        payload.put("report", report);

        messagingTemplate.convertAndSend("/topic/alerts", (Object) payload);

        System.out.println("📢 WebSocket Alert Sent: " + title);
    }
}
