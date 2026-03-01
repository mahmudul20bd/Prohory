package com.cyberaware.backend.controller;

import com.cyberaware.backend.model.ChatMessage;
import com.cyberaware.backend.repository.ChatMessageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.handler.annotation.SendTo;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController; // 🔥 @Controller এর বদলে @RestController ব্যবহার করুন

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
        // টাইম ফরম্যাট সেট করা হচ্ছে
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("hh:mm a");
        message.setTimestamp(LocalDateTime.now().format(formatter));

        // 🔥 মেসেজটি ডাটাবেসে পার্মানেন্টলি সেভ করা হচ্ছে
        chatMessageRepository.save(message);

        return message;
    }

    // 🔥 ফ্রন্টএন্ড বা অ্যান্ড্রয়েড থেকে পূর্বের চ্যাট হিস্ট্রি লোড করার জন্য API
    @GetMapping("/api/chat/history/{username}")
    public List<ChatMessage> getChatHistory(@PathVariable String username) {
        // এই ইউজার যে মেসেজগুলো পাঠিয়েছে বা রিসিভ করেছে সব রিটার্ন করবে
        return chatMessageRepository.findBySenderUsernameOrReceiverUsername(username, username);
    }
}