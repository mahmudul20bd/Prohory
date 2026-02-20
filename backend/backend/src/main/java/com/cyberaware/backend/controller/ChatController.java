package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.ChatMessage;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.stereotype.Controller;
import java.time.LocalDateTime;

@Controller
public class ChatController {

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage processMessage(@Payload ChatMessage message) {
        message.setTimestamp(LocalDateTime.now());
        // এখানে আপনি চাইলে Repository ব্যবহার করে messageটি ডাটাবেসে সেভ করতে পারেন
        return message;
    }
}