package com.cyberaware.backend.model;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;

    // 🔥 সার্ভার অ্যাপকে এই তথ্যগুলো পাঠাবে
    private String role;
    private String clearanceLevel;

    public AuthResponse(boolean success, String message, String username, String role, String clearanceLevel) {
        this.success = success;
        this.message = message;
        this.username = username;
        this.role = role;
        this.clearanceLevel = clearanceLevel;
    }

    // Getters...
    public String getRole() { return role; }
    public String getClearanceLevel() { return clearanceLevel; }
    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
}