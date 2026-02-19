package com.cyberaware.backend.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "reports")
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String message;
    private String sender;   // যে ইউজার পাঠিয়েছে
    private String reporter; // রিপোর্টার নাম (অপশনাল)

    // 🔥 মিসিং ফিল্ডগুলো অ্যাড করা হয়েছে
    private String aiResult;
    private LocalDateTime timestamp;

    // --- Constructors ---
    public Report() {}

    public Report(String message, String sender, String reporter) {
        this.message = message;
        this.sender = sender;
        this.reporter = reporter;
        this.timestamp = LocalDateTime.now(); // ডিফল্ট টাইমস্ট্যাম্প
    }

    // --- Getters and Setters ---

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }

    public String getSender() { return sender; }
    public void setSender(String sender) { this.sender = sender; }

    public String getReporter() { return reporter; }
    public void setReporter(String reporter) { this.reporter = reporter; }

    // 🔥 AI Result Getter/Setter (এরর ফিক্স)
    public String getAiResult() { return aiResult; }
    public void setAiResult(String aiResult) { this.aiResult = aiResult; }

    // 🔥 Timestamp Getter/Setter (এরর ফিক্স)
    public LocalDateTime getTimestamp() { return timestamp; }
    public void setTimestamp(LocalDateTime timestamp) { this.timestamp = timestamp; }
}