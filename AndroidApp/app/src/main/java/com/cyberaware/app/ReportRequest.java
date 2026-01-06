package com.cyberaware.app;

public class ReportRequest {
    private String sender;
    private String message;

    public ReportRequest(String sender, String messageBody) {
        this.sender = sender;
        this.message = messageBody;
    }
}