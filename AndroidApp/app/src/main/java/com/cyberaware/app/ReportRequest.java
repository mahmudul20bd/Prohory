package com.cyberaware.app;

public class ReportRequest {
    private String sender;
    private String message;
    private String reporter; // 🔥 নতুন ফিল্ড

    public ReportRequest(String sender, String message, String reporter) {
        this.sender = sender;
        this.message = message;
        this.reporter = reporter;
    }
}