package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.ChatMessage;
import com.cyberaware.backend.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

@RestController
public class ChatController {

    @Autowired
    private ChatMessageRepository chatMessageRepository;

    @MessageMapping("/chat.send")
    @SendTo("/topic/messages")
    public ChatMessage processMessage(@Payload ChatMessage message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        message.setTimestamp(LocalDateTime.now().format(formatter));
        chatMessageRepository.save(message);
        return message;
    }

    // ইউজারের নিজের হিস্ট্রি লোড করার জন্য
    @GetMapping("/api/chat/history/{username}")
    public List<ChatMessage> getChatHistory(@PathVariable String username) {
        return chatMessageRepository.findBySenderUsernameOrReceiverUsername(username, username);
    }

    // 🔥 নতুন: এক্সপার্টদের জন্য ডাটাবেসের সব চ্যাট হিস্ট্রি একসাথে লোড করার API
    @GetMapping("/api/chat/all")
    public List<ChatMessage> getAllChats() {
        return chatMessageRepository.findAll();
    }
}