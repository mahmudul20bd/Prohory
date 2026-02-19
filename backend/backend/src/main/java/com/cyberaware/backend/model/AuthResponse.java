package com.cyberaware.backend.model;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;
    private String role;
    private String clearanceLevel;
    private String token; // 🔥 নতুন ফিল্ড: টোকেন

    // কনস্ট্রাক্টর আপডেট
    public AuthResponse(boolean success, String message, String username, String role, String clearanceLevel, String token) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
        this.clearanceLevel = clearanceLevel;
        this.token = token;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
    public String getRole() { return role; }
    public String getClearanceLevel() { return clearanceLevel; }
    public String getToken() { return token; } // 🔥 টোকেন গেটার
}