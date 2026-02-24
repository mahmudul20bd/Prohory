package com.cyberaware.backend.service;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;
import java.util.concurrent.CompletableFuture;

@Service
public class EmailService {

    @Autowired
    private JavaMailSender mailSender; // Real email sender

    @Autowired
    private TemplateEngine templateEngine; // Advanced HTML Template Engine

    // ==========================================================
    // ১. আপনার আগের Alert System (যেটা কনসোলে প্রিন্ট করে)
    // ==========================================================
    @Async // এটি ব্যাকগ্রাউন্ডে রান হবে
    public CompletableFuture<String> sendAlertEmail(String user, String message, String threatLevel) {
        System.out.println("\n========== \uD83D\uDEA8 SECURITY ALERT TRIGGERED \uD83D\uDEA8 ==========");
        System.out.println("⚡ Threat Level: " + threatLevel);
        System.out.println("\uD83D\uDC64 Reported By: " + user);
        System.out.println("\uD83D\uDCDD Message Content: \"" + message + "\"");
        System.out.println("----------------------------------------------------");
        System.out.println("\uD83D\uDD04 Routing to Cyber Cell...");

        try {
            Thread.sleep(2000); // সিমুলেশন
        } catch (InterruptedException e) {
            e.printStackTrace();
        }

        System.out.println("✅ ALERT SENT SUCCESSFULLY TO ADMIN & EXPERTS.");
        System.out.println("====================================================\n");

        return CompletableFuture.completedFuture("Email Sent");
    }

    // ==========================================================
    // 🔥 ২. নতুন Advanced OTP System (আসল ইমেইলে HTML পাঠাবে)
    // ==========================================================
    @Async // মেইন থ্রেড ব্লক করবে না, সুপার ফাস্ট রেসপন্স দেবে
    public CompletableFuture<String> sendOtpEmail(String toEmail, String otpCode) {
        try {
            // ১. HTML ফাইলের জন্য ডাইনামিক ডেটা সেট করা
            Context context = new Context();
            context.setVariable("otpCode", otpCode);

            // ২. 'otp-email.html' ফাইলটিকে প্রসেস করা
            String htmlContent = templateEngine.process("otp-email", context);

            // ৩. ইমেইল তৈরি ও পাঠানো
            MimeMessage message = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(message, true, "UTF-8");

            helper.setTo(toEmail);
            helper.setSubject("System Access Recovery - Prohory");
            helper.setText(htmlContent, true); // true মানে এটা HTML ফরমেট

            mailSender.send(message); // আসল ইমেইল সেন্ড হবে

            System.out.println("✅ Security OTP Email successfully sent to: " + toEmail);
            return CompletableFuture.completedFuture("OTP Sent");

        } catch (MessagingException e) {
            System.err.println("❌ Failed to send security email to: " + toEmail);
            e.printStackTrace();
            return CompletableFuture.completedFuture("Failed");
        }
    }
}