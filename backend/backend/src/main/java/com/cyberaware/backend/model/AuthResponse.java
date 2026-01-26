package com.cyberaware.backend.model;

public class AuthResponse {
    private boolean success;
    private String message;
    private String username;

    public AuthResponse(boolean success, String message, String username) {
        this.success = success;
        this.message = message;
        this.username = username;
    }

    // Getters
    public boolean isSuccess() { return success; }
    public String getMessage() { return message; }
    public String getUsername() { return username; }
}