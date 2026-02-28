package com.cyberaware.backend.controller;

import com.cyberaware.backend.service.SpamDetector;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api")
public class TestController {

    @Autowired
    private SpamDetector spamDetector;

    @PostMapping("/test-scan")
    public String testScan(@RequestBody Map<String, String> payload) {
        String message = payload.get("message");
        return spamDetector.checkForScam(message);
    }
}