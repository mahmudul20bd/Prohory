package com.cyberaware.backend.model;

public class AuthRequest {
    private String username;
    private String email; // 🔥 যোগ করা হলো
    private String password;
    private String role;  // রোল রিসিভ করার জন্য

    // Getters and Setters
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
}