package com.cyberaware.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "system_logs")
public class SystemLog {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String action;
    private String performedBy;
    private String details;
    private LocalDateTime timestamp;

    public SystemLog() {}
    public SystemLog(String action, String user, String details) {
        this.action = action;
        this.performedBy = user;
        this.details = details;
        this.timestamp = LocalDateTime.now();
    }
    // Getters and Setters...
    public Long getId() { return id; }
    public String getAction() { return action; }
    public String getPerformedBy() { return performedBy; }
    public String getDetails() { return details; }
    public LocalDateTime getTimestamp() { return timestamp; }
}