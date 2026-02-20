package com.cyberaware.backend.model;

import lombok.Data;
import java.time.LocalDateTime;

@Data
public class ChatMessage {
    private String senderUsername;
    private String receiverUsername;
    private String content;
    private String fileUrl; // স্ক্রিনশট বা ফাইলের লিংকের জন্য
    private LocalDateTime timestamp;
}