package com.cyberaware.app;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username; // সার্ভার থেকে কনফার্ম করা নাম

    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
}