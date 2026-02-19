package com.cyberaware.backend.service;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Async // এটি ব্যাকগ্রাউন্ডে রান হবে (মেইন থ্রেড স্লো হবে না)
    public CompletableFuture<String> sendAlertEmail(String user, String message, String threatLevel) {
        System.out.println("\n========== 🚨 SECURITY ALERT TRIGGERED 🚨 ==========");
        System.out.println("⚡ Threat Level: " + threatLevel);
        System.out.println("👤 Reported By: " + user);
        System.out.println("📝 Message Content: \"" + message + "\"");
        System.out.println("----------------------------------------------------");
        System.out.println("🔄 Routing to Cyber Cell...");

        try {
            // ইমেইল পাঠানোর সিমুলেশন (২ সেকেন্ড সময় নিচ্ছে)
            Thread.sleep(2000);
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("✅ ALERT SENT SUCCESSFULLY TO ADMIN & EXPERTS.");
        System.out.println("====================================================\n");

        return CompletableFuture.completedFuture("Email Sent");
    }
}