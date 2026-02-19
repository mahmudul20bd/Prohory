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

    private String email; // 🔥 নতুন ফিল্ড যোগ করা হলো

    @Column(nullable = false)
    private String password;

    private String role;
    private String clearanceLevel;

    public User() {}

    // কনস্ট্রাক্টর আপডেট
    public User(String username, String email, String password, String role, String clearanceLevel) {
        this.username = username;
        this.email = email;
        this.password = password;
        this.role = role;
        this.clearanceLevel = clearanceLevel;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; } // Getter
    public void setEmail(String email) { this.email = email; } // Setter

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }
}