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

    private String email;

    @Column(unique = true)
    private String agentId;

    @Column(nullable = false)
    private String password;

    private String role;
    private String clearanceLevel;

    // 🔥 Expert Approval & Document Fields
    private boolean isApproved = false; // Default false (Pending)
    @Column(columnDefinition = "TEXT")
    private String profileImage; // To store CV/NID link

    // 🔥 Rating System Fields for Experts
    private Double averageRating = 0.0;
    private Integer ratingCount = 0;

    public User() {}

    public User(String username, String email, String agentId, String password, String role, String clearanceLevel) {
        this.username = username;
        this.email = email;
        this.agentId = agentId;
        this.password = password;
        this.role = role;
        this.clearanceLevel = clearanceLevel;
        this.averageRating = 0.0;
        this.ratingCount = 0;
        this.isApproved = false;
    }

    // Getters & Setters
    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getAgentId() { return agentId; }
    public void setAgentId(String agentId) { this.agentId = agentId; }

    public String getPassword() { return password; }
    public void setPassword(String password) { this.password = password; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getClearanceLevel() { return clearanceLevel; }
    public void setClearanceLevel(String clearanceLevel) { this.clearanceLevel = clearanceLevel; }

    // 🔥 NEW: Getters & Setters for Approval & Document
    public boolean isApproved() { return isApproved; }
    public void setApproved(boolean approved) { isApproved = approved; }

    public String getProfileImage() { return profileImage; }
    public void setProfileImage(String profileImage) { this.profileImage = profileImage; }

    // Getters & Setters for Rating
    public Double getAverageRating() { return averageRating; }
    public void setAverageRating(Double averageRating) { this.averageRating = averageRating; }

    public Integer getRatingCount() { return ratingCount; }
    public void setRatingCount(Integer ratingCount) { this.ratingCount = ratingCount; }
    // 🔥 NEW: Extended Expert Info
    private String specialization;

    @Column(columnDefinition = "LONGTEXT") // LONGTEXT for large Base64 files
    private String cvFile;

    @Column(columnDefinition = "LONGTEXT")
    private String nidFile;

    // Getters and Setters
    public String getSpecialization() { return specialization; }
    public void setSpecialization(String specialization) { this.specialization = specialization; }

    public String getCvFile() { return cvFile; }
    public void setCvFile(String cvFile) { this.cvFile = cvFile; }

    public String getNidFile() { return nidFile; }
    public void setNidFile(String nidFile) { this.nidFile = nidFile; }
}