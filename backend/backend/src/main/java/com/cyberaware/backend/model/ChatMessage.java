package com.cyberaware.backend.model;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import lombok.Data;

@Data
@Entity // 🔥 এটি ডাটাবেসে টেবিল তৈরি করবে
public class ChatMessage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id; // 🔥 ডাটাবেসের জন্য প্রাইমারি কি

    private String senderUsername;
    private String receiverUsername;
    private String content;
    private String fileUrl;
    private String timestamp;
}