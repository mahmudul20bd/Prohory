package com.cyberaware.app;

public class Report {
    private Long id;
    private String sender;
    private String message;
    private String aiResult; // সার্ভারের ফিল্ডের সাথে মিল থাকতে হবে
    private String timestamp;

    // Getters
    public String getSender() { return sender; } // এই মেথডটি জরুরি
    public String getMessage() { return message; }
    public String getAiResult() { return aiResult; }
    public String getTimestamp() { return timestamp; }
}