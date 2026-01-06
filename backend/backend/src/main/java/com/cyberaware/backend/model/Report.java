package com.cyberaware.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String sender;
    private String message;

    // ডাটাবেসে কলামের নাম ai_result, কিন্তু জাভাতে আমরা aiResult ব্যবহার করি
    @Column(name = "ai_result")
    private String aiResult;

    public Report() {
    }

    public Report(String sender, String message, String aiResult) {
        this.sender = sender;
        this.message = message;
        this.aiResult = aiResult;
    }

    // ==========================================
    // 🔥 GETTERS & SETTERS (এইগুলো মাস্ট লাগবে)
    // ==========================================

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getSender() {
        return sender;
    }

    public void setSender(String sender) {
        this.sender = sender;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    // 🔥🔥 এই মেথডটি মিসিং ছিল বলেই রিফ্রেশ দিলে ডাটা আসছিল না
    public String getAiResult() {
        return aiResult;
    }

    public void setAiResult(String aiResult) {
        this.aiResult = aiResult;
    }
}