package com.cyberaware.backend.model;

import jakarta.persistence.*;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    // 🔥 নতুন ফিল্ড
    private String role;
    private String clearanceLevel;
    public User() {
    }

    public User(String username, String password) {
        this.username = username;
        this.password = password;
    }

    // কনস্ট্রাক্টর আপডেট
    public User(String username, String password, String role, String clearanceLevel) {
        this.username = username;
        this.password = password;
        this.role = role;
        this.clearanceLevel = clearanceLevel;
    }

    // Getters & Setters যোগ করতে ভুলবেন না
    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }
    public String getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }

    // Getters and Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }
}