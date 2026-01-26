package com.cyberaware.backend.model; // প্যাকেজ চেঞ্জ হয়েছে

public class ReportRequest {
    private String sender;
    private String message;
        private String reporter; // 🔥 নতুন

        // Getters & Setters...
        public String getReporter() { return reporter; }
        public void setReporter(String reporter) { this.reporter = reporter; }
    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
}