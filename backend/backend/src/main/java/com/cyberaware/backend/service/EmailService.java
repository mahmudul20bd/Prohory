package com.cyberaware.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Async // 🔥 এর মানে: এই মেথডটি আলাদা থ্রেডে রান হবে
    public CompletableFuture<String> sendAlertEmail(String message) {
        System.out.println("📧 Email Service Started... (Thread: " + Thread.currentThread().getName() + ")");

        try {
            // মনে করি ইমেইল পাঠাতে ৫ সেকেন্ড সময় লাগে
            Thread.sleep(5000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("✅ Email Sent to Cyber Cell for report: " + message);
        return CompletableFuture.completedFuture("Email Sent");
    }
}