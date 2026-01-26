package com.cyberaware.app;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;

    // 🔥 নতুন ফিল্ড (সার্ভার থেকে আসবে)
    private String role;
    private String clearanceLevel;

    // Getters
    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getUsername() {
        return username;
    }

    public String getRole() {
        return role;
    }

    public String getClearanceLevel() {
        return clearanceLevel;
    }
}